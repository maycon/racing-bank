"""
Transfer routes.
Handles account-to-account money transfers.
"""

from fastapi import APIRouter, Depends, HTTPException, Request, status

from database import Transaction, User, Account, Transfer, get_session
from schemas import TransferRequest, TransferResponse, TransferItem, StatementResponse, TransactionItem
from auth import AccessLevel, access, get_current_user


router = APIRouter(tags=["Transfers"])


@router.post("/transfer", response_model=TransferResponse)
@access(AccessLevel.FULL)
def transfer_money(
    request: Request, 
    data: TransferRequest,
    current_user: User = Depends(get_current_user)
):
    """
    Transfer money from current user to another user.
    
    RACE CONDITION WARNING - CRITICAL:
    - This is a classic double-spending vulnerability!
    - No pessimistic locking (SELECT FOR UPDATE)
    - No optimistic locking (version numbers)
    - Balance check and updates are not atomic
    
    Race condition scenarios:
    
    1. DOUBLE SPENDING:
       Thread A: Check sender balance $100 >= $80 ✓
       Thread B: Check sender balance $100 >= $80 ✓
       Thread A: Deduct $80, sender = $20
       Thread B: Deduct $80, sender = -$60 (overdraft!)
       Result: $160 transferred from $100 account
    
    2. LOST UPDATES (receiver side):
       Receiver has $50
       Thread A: Transfer $30, receiver = $80
       Thread B: Transfer $40, receiver = $90
       One update overwrites the other
       Result: Receiver gets $90 instead of $120
    
    3. PHANTOM READS:
       Thread A: Starts transfer
       Thread B: Completes transfer and commits
       Thread A: Reads stale balance
       Thread A: Overwrites Thread B's changes
    
    4. INCONSISTENT STATE:
       If crash occurs between deducting sender and crediting receiver,
       money disappears from the system!
    
    Args:
        data: Transfer details (recipient and amount)
        current_user: Authenticated user (from JWT)
    
    Returns:
        Transfer confirmation with new balance
    
    Raises:
        HTTPException: If recipient not found, insufficient funds, or self-transfer
    """
    if data.amount <= 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Transfer amount must be positive"
        )
    
    with get_session() as session:
        # Retrieve sender (current user)
        # NO LOCKING: Multiple transfers can read simultaneously
        sender = session.query(User).filter_by(id=current_user.id).one()
        
        # Retrieve recipient
        recipient = session.query(User).filter_by(username=data.to_username).one_or_none()
        if not recipient:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Recipient not found"
            )
        
        # Prevent self-transfer
        if sender.id == recipient.id:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Cannot transfer to yourself"
            )
        
        # Get sender's account
        # NO LOCKING: Other threads can modify this simultaneously
        sender_account = session.query(Account).filter_by(user_id=sender.id).one()
        sender_balance = float(sender_account.balance)
        
        # Check sufficient funds
        # WARNING: NOT ATOMIC! Balance could change before we deduct
        if sender_balance < data.amount:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Insufficient funds. Available: ${sender_balance:.2f}"
            )
        
        # Get recipient's account
        # NO LOCKING: Concurrent transfers to same recipient will race
        recipient_account = session.query(Account).filter_by(user_id=recipient.id).one()
        
        # CRITICAL RACE CONDITION ZONE:
        # Between reading balances above and updating below,
        # other transactions can modify these accounts
        
        # Deduct from sender (VULNERABLE TO RACE CONDITIONS)
        sender_account.balance = sender_balance - data.amount
        
        # Credit to recipient (VULNERABLE TO LOST UPDATES)
        recipient_balance = float(recipient_account.balance)
        recipient_account.balance = recipient_balance + data.amount
        
        # Record transfer
        transfer = Transfer(
            from_user_id=sender.id,
            to_user_id=recipient.id,
            amount=data.amount
        )
        session.add(transfer)
        
        # Commit all changes
        # RACE CONDITION: If multiple transfers happen simultaneously,
        # the last commit wins and previous updates are lost
        session.commit()
        
        # Get final balance for response
        new_balance = float(sender_account.balance)
        
        return TransferResponse(
            status="success",
            transfer_id=transfer.id,
            from_username=sender.username,
            to_username=recipient.username,
            amount=data.amount,
            new_balance=new_balance
        )


@router.get("/statement", response_model=StatementResponse)
@access(AccessLevel.FULL)
def get_statement(
    request: Request,
    current_user: User = Depends(get_current_user)
):
    """
    Get account statement with transaction history.
    
    RACE CONDITION WARNING:
    - Reads from multiple tables without transaction isolation
    - Statement might show inconsistent state
    - New transactions could be added while reading
    
    Example scenario:
    1. Read account balance: $100
    2. Another thread completes a transfer
    3. Read transfer history: includes the new transfer
    4. Statement shows transfer but balance doesn't reflect it
    
    Args:
        current_user: Authenticated user (from JWT)
    
    Returns:
        Complete statement with balances and transaction history
    """
    with get_session() as session:
        # Get user with relationships
        user = session.query(User).filter_by(id=current_user.id).one()
        
        # Get balances (separate queries, not atomic)
        account = session.query(Account).filter_by(user_id=user.id).one()
        cash_balance = float(account.balance)
        
        from database import FundAccount, Fund
        fund_account = session.query(FundAccount).filter_by(user_id=user.id).one()
        fund_shares = float(fund_account.shares)
        
        # Calculate fund value
        fund = session.query(Fund).first()
        if fund and fund.total_shares > 0:
            share_price = float(fund.total_value) / float(fund.total_shares)
            fund_value = fund_shares * share_price
        else:
            fund_value = 0.0
        
        # Get all transactions for this user
        transactions = session.query(Transaction).filter_by(
            user_id=user.id
        ).order_by(Transaction.created_at.desc()).limit(50).all() # type: ignore
        
        # Get all transfers involving this user
        transfers = session.query(Transfer).filter(
            (Transfer.from_user_id == user.id) | (Transfer.to_user_id == user.id)
        ).order_by(Transfer.created_at.desc()).limit(50).all()
        
        # Build transaction list
        transaction_list = [
            TransactionItem(
                id=t.id,
                type=t.type,
                amount=float(t.amount),
                description=t.description,
                created_at=t.created_at.isoformat() + "Z" # type: ignore
            )
            for t in transactions
        ]
        
        # Build transfer list
        transfer_list = []
        for t in transfers:
            from_user = session.query(User).filter_by(id=t.from_user_id).one()
            to_user = session.query(User).filter_by(id=t.to_user_id).one()
            transfer_list.append(
                TransferItem(
                    id=t.id,
                    from_username=from_user.username,
                    to_username=to_user.username,
                    amount=float(t.amount),
                    created_at=t.created_at.isoformat() + "Z"
                )
            )
        
        return StatementResponse(
            username=user.username,
            cash_balance=cash_balance,
            fund_shares=fund_shares,
            fund_value=fund_value,
            transactions=transaction_list,
            transfers=transfer_list
        )
