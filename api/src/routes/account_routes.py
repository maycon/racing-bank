"""
Account management routes.
Handles deposits, withdrawals, and balance queries.
"""

from fastapi import APIRouter, Depends, HTTPException, Request, status

from database import User, Account, FundAccount, Fund, Transaction, get_session
from schemas import DepositRequest, WithdrawalRequest, BalanceResponse
from auth import AccessLevel, access, get_current_user


router = APIRouter(tags=["Account Management"])


@router.post("/deposit")
@access(AccessLevel.FULL)
def deposit(
    request: Request, 
    data: DepositRequest,
    current_user: User = Depends(get_current_user)
):
    """
    Deposit cash into account.
    
    RACE CONDITION WARNING:
    - Balance is read, modified, and written in separate operations
    - No row-level locking or optimistic locking
    - Concurrent deposits can lead to lost updates
    
    Example scenario:
    1. Thread A reads balance: $100
    2. Thread B reads balance: $100
    3. Thread A deposits $50, writes $150
    4. Thread B deposits $30, writes $130
    5. Final balance is $130 instead of $180 (lost $50 deposit!)
    
    Args:
        data: Deposit amount
        current_user: Authenticated user (from JWT)
    
    Returns:
        Success message with new balance
    """
    with get_session() as session:
        # Retrieve user's account
        # NO LOCKING: Multiple requests can read the same balance simultaneously
        account = session.query(Account).filter_by(user_id=current_user.id).one()
        
        # Read current balance (RACE CONDITION VULNERABLE)
        old_balance = float(account.balance)
        
        # Calculate new balance
        new_balance = old_balance + data.amount
        
        # Update balance (RACE CONDITION VULNERABLE)
        # If another transaction modified balance after we read it,
        # that update will be lost
        account.balance = new_balance
        
        # Record transaction
        transaction = Transaction(
            user_id=current_user.id,
            type="deposit",
            amount=data.amount,
            description=f"Cash deposit of ${data.amount:.2f}"
        )
        session.add(transaction)
        
        # Commit changes
        # RACE CONDITION: Another thread's changes could be overwritten here
        session.commit()
        
        return {
            "status": "success",
            "message": f"Deposited ${data.amount:.2f}",
            "previous_balance": old_balance,
            "new_balance": new_balance
        }


@router.post("/withdrawal")
@access(AccessLevel.FULL)
def withdrawal(
    request: Request,
    data: WithdrawalRequest,
    current_user: User = Depends(get_current_user)
):
    """
    Withdraw cash from account.
    
    RACE CONDITION WARNING:
    - Balance check and deduction are not atomic
    - Two concurrent withdrawals can both pass balance check
    - Can result in negative balance (overdraft)
    
    Example scenario:
    1. Account has $100
    2. Thread A checks: $100 >= $80 ✓
    3. Thread B checks: $100 >= $80 ✓
    4. Thread A withdraws $80, balance = $20
    5. Thread B withdraws $80, balance = -$60 (overdraft!)
    
    Args:
        data: Withdrawal amount
        current_user: Authenticated user (from JWT)
    
    Returns:
        Success message with new balance
    
    Raises:
        HTTPException: If insufficient funds (but race condition can bypass this!)
    """
    with get_session() as session:
        # Retrieve user's account
        # NO LOCKING: Multiple withdrawals can proceed concurrently
        account = session.query(Account).filter_by(user_id=current_user.id).one()
        
        # Check sufficient funds
        # WARNING: This check is not atomic!
        # Another transaction could withdraw funds between this check and the update
        if float(account.balance) < data.amount:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Insufficient funds. Available: ${float(account.balance):.2f}"
            )
        
        
        # Update balance (RACE CONDITION VULNERABLE)
        account.balance = float(account.balance) - data.amount
        
        # Record transaction
        transaction = Transaction(
            user_id=current_user.id,
            type="withdrawal",
            amount=data.amount,
            description=f"Cash withdrawal of ${data.amount:.2f}"
        )
        session.add(transaction)
        
        # Commit changes
        # RACE CONDITION: Balance could go negative if concurrent withdrawals occurred
        session.commit()
        
        return {
            "status": "success",
            "message": f"Withdrew ${data.amount:.2f}",
            "previous_balance": float(account.balance) + data.amount,
            "new_balance": account.balance
        }


@router.get("/balance", response_model=BalanceResponse)
@access(AccessLevel.FULL)
def get_balance(
    request: Request, 
    current_user: User = Depends(get_current_user)
):
    """
    Get current account balances.
    
    RACE CONDITION WARNING:
    - Balances are read at different times (not a snapshot)
    - Cash balance, fund shares, and fund value might be inconsistent
    - Values could change between reads within this function
    
    Example scenario:
    1. Read cash balance: $100
    2. Another thread completes a fund subscription
    3. Read fund shares: reflects the subscription
    4. Read cash balance again would show different value
    5. Response contains inconsistent state
    
    Args:
        current_user: Authenticated user (from JWT)
    
    Returns:
        Current balances for cash and fund accounts
    """
    with get_session() as session:
        # Retrieve account (NO TRANSACTION ISOLATION)
        account = session.query(Account).filter_by(user_id=current_user.id).one()
        cash_balance = float(account.balance)
        
        # Retrieve fund account (separate query, possible inconsistency)
        fund_account = session.query(FundAccount).filter_by(user_id=current_user.id).one()
        fund_shares = float(fund_account.shares)
        
        # Get fund information (another separate query)
        fund = session.query(Fund).first()
        
        # Calculate fund value and share price
        if fund and fund.total_shares > 0:
            share_price = float(fund.total_value) / float(fund.total_shares)
            fund_value = fund_shares * share_price
        else:
            fund_value = 0.0
        
        # Calculate total portfolio value
        # WARNING: This calculation uses data from multiple non-atomic reads
        total_portfolio = cash_balance + fund_value
        
        return BalanceResponse(
            username=current_user.username,
            cash_balance=cash_balance,
            fund_shares=fund_shares,
            fund_value=fund_value,
            total_portfolio=total_portfolio
        )
