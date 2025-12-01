"""
Investment fund routes.
Handles fund subscriptions (investing) and redemptions (divesting).
"""

from fastapi import APIRouter, Depends, HTTPException, Request, status

from database import User, Account, FundAccount, Fund, Transaction, get_session
from schemas import (
    SubscriptionRequest, RedemptionRequest,
    FundOperationResponse, FundInfoResponse
)
from auth import AccessLevel, access, get_current_user


router = APIRouter(tags=["Investment Fund"])


@router.post("/subscribe", response_model=FundOperationResponse)
@access(AccessLevel.FULL)
def subscribe_to_fund(
    request: Request, 
    data: SubscriptionRequest,
    current_user: User = Depends(get_current_user)
):
    """
    Subscribe to investment fund (invest cash to buy fund shares).
    
    RACE CONDITION WARNING - CRITICAL:
    - Multiple calculations without atomic operations
    - Share price calculation uses non-locked data
    - Cash deduction and share allocation are separate operations
    
    Race condition scenarios:
    
    1. INCORRECT SHARE PRICE:
       - Thread A: Read fund value $100,000, shares 100,000 → price $1.00
       - Thread B: Subscribe $10,000
       - Thread A: Calculate shares at $1.00 = 10,000 shares
       - Thread B's subscription changes fund value
       - Thread A: Allocates 10,000 shares at wrong price
       - Result: User gets wrong number of shares
    
    2. FUND VALUE INCONSISTENCY:
       - Thread A: Reads fund.total_value = $100,000
       - Thread B: Updates fund.total_value = $110,000
       - Thread A: Adds $5,000 to old value
       - Thread A: Sets fund.total_value = $105,000
       - Result: Thread B's $10,000 subscription is lost!
    
    3. CASH DEDUCTION RACE:
       - User has $100
       - Thread A: Subscribe $80, check pass
       - Thread B: Subscribe $80, check pass
       - Thread A: Deduct $80, balance = $20
       - Thread B: Deduct $80, balance = -$60
       - Result: Overdraft on cash account
    
    4. PARTIAL FAILURE:
       - Cash deducted but server crashes before shares allocated
       - Money lost from system!
    
    Args:
        data: Subscription amount
        current_user: Authenticated user (from JWT)
    
    Returns:
        Subscription confirmation with share details
    
    Raises:
        HTTPException: If insufficient funds
    """
    if data.amount <= 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Subscription amount must be positive"
        )
    
    with get_session() as session:
        # Get user's cash account
        # NO LOCKING: Multiple subscriptions can proceed simultaneously
        account = session.query(Account).filter_by(user_id=current_user.id).one()
        cash_balance = float(account.balance)
        
        # Check sufficient funds
        # WARNING: NOT ATOMIC! Balance could change before deduction
        if cash_balance < data.amount:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Insufficient funds. Available: ${cash_balance:.2f}"
            )
        
        # Get fund
        # NO LOCKING: Fund data can change during this operation
        fund = session.query(Fund).first()
        if not fund:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Fund not initialized"
            )
        
        # Calculate current share price
        # RACE CONDITION: Fund values could change after this calculation
        total_value = float(fund.total_value)
        total_shares = float(fund.total_shares)
        
        if total_shares > 0:
            share_price = total_value / total_shares
        else:
            share_price = 1.0  # Initial price
        
        # Calculate shares to allocate
        # VULNERABLE: Using potentially stale share_price
        shares_to_add = data.amount / share_price
        
        # Get user's fund account
        fund_account = session.query(FundAccount).filter_by(user_id=current_user.id).one()
        
        # CRITICAL RACE CONDITION ZONE:
        # Multiple threads can be here simultaneously with different calculations
        
        # Deduct from cash account (VULNERABLE TO LOST UPDATES)
        account.balance = cash_balance - data.amount
        
        # Update fund totals (VULNERABLE TO LOST UPDATES)
        fund.total_value = total_value + data.amount
        fund.total_shares = total_shares + shares_to_add
        
        # Allocate shares to user (VULNERABLE TO LOST UPDATES)
        current_shares = float(fund_account.shares)
        fund_account.shares = current_shares + shares_to_add
        
        # Record transaction
        transaction = Transaction(
            user_id=current_user.id,
            type="subscription",
            amount=data.amount,
            description=f"Fund subscription: {shares_to_add:.4f} shares at ${share_price:.2f}"
        )
        session.add(transaction)
        
        # Commit all changes
        # RACE CONDITION: Last write wins, previous updates can be lost
        session.commit()
        
        return FundOperationResponse(
            status="success",
            operation="subscription",
            amount=data.amount,
            shares=shares_to_add,
            share_price=share_price,
            new_cash_balance=float(account.balance),
            new_fund_shares=float(fund_account.shares)
        )


@router.post("/redemption", response_model=FundOperationResponse)
@access(AccessLevel.FULL)
def redeem_from_fund(
    request: Request, 
    data: RedemptionRequest,
    current_user: User = Depends(get_current_user)
):
    """
    Redeem from investment fund (sell fund shares to get cash).
    
    RACE CONDITION WARNING - CRITICAL:
    - Same issues as subscription, but in reverse
    - Share calculations without locking
    - Multiple non-atomic operations
    
    Race condition scenarios:
    
    1. SHARE PRICE MANIPULATION:
       - Thread A: Calculates redemption at $1.00/share
       - Thread B: Redeems large amount, changes price to $0.90
       - Thread A: Gets cash at $1.00 price (overpayment!)
       - Result: Fund loses money
    
    2. INSUFFICIENT SHARES CHECK:
       - User has 100 shares
       - Thread A: Redeem 80 shares, check pass
       - Thread B: Redeem 80 shares, check pass
       - Thread A: Deduct 80 shares, balance = 20
       - Thread B: Deduct 80 shares, balance = -60
       - Result: Negative share balance (impossible state)
    
    3. FUND DEPLETION:
       - Fund has $1,000 total value
       - Thread A: Redeem $800
       - Thread B: Redeem $800
       - Both calculate they can redeem
       - Fund.total_value becomes negative!
    
    4. CASH CREDIT RACE:
       - Thread A: Add $100 to user's $500 balance = $600
       - Thread B: Add $200 to user's $500 balance = $700
       - Result: User gets $700 instead of $800 (lost $100)
    
    Args:
        data: Redemption amount (in dollars)
        current_user: Authenticated user (from JWT)
    
    Returns:
        Redemption confirmation with share details
    
    Raises:
        HTTPException: If insufficient shares
    """
    if data.amount <= 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Redemption amount must be positive"
        )
    
    with get_session() as session:
        # Get user's fund account
        # NO LOCKING: Multiple redemptions can proceed concurrently
        fund_account = session.query(FundAccount).filter_by(user_id=current_user.id).one()
        user_shares = float(fund_account.shares)
        
        # Get fund
        # NO LOCKING: Fund can be modified by other threads
        fund = session.query(Fund).first()
        if not fund:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Fund not initialized"
            )
        
        # Calculate current share price
        # RACE CONDITION: These values can change during calculation
        total_value = float(fund.total_value)
        total_shares = float(fund.total_shares)
        
        if total_shares > 0:
            share_price = total_value / total_shares
        else:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Fund has no shares"
            )
        
        # Calculate shares to redeem
        # VULNERABLE: Using potentially stale share_price
        shares_to_redeem = data.amount / share_price
        
        # Check if user has enough shares
        # WARNING: NOT ATOMIC! Shares could change before deduction
        if user_shares < shares_to_redeem:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Insufficient shares. You have {user_shares:.6f} shares "
                       f"(worth ${user_shares * share_price:.2f})"
            )
        
        # Check if fund has enough value
        # WARNING: NOT ATOMIC! Fund could be depleted by other redemptions
        if total_value < data.amount:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Fund has insufficient liquidity for this redemption"
            )
        
        
        # CRITICAL RACE CONDITION ZONE:
        # All these updates happen without coordination
        
        # Deduct shares from user (VULNERABLE TO LOST UPDATES)
        fund_account.shares = user_shares - shares_to_redeem
        
        # Update fund totals (VULNERABLE TO LOST UPDATES)
        fund.total_value = total_value - data.amount
        fund.total_shares = total_shares - shares_to_redeem

        # Get user's cash account
        account = session.query(Account).filter_by(user_id=current_user.id).one()

        # Credit cash to user (VULNERABLE TO LOST UPDATES)
        account.balance = float(account.balance) + data.amount
        
        # Record transaction
        transaction = Transaction(
            user_id=current_user.id,
            type="redemption",
            amount=data.amount,
            description=f"Fund redemption: {shares_to_redeem:.6f} shares at ${share_price:.2f}"
        )
        session.add(transaction)
        
        # Commit all changes
        # RACE CONDITION: Concurrent redemptions can corrupt all these values
        session.commit()
        
        return FundOperationResponse(
            status="success",
            operation="redemption",
            amount=data.amount,
            shares=shares_to_redeem,
            share_price=share_price,
            new_cash_balance=float(account.balance),
            new_fund_shares=float(fund_account.shares)
        )


@router.get("/fund/info", response_model=FundInfoResponse)
@access(AccessLevel.FULL)
def get_fund_info(request: Request):
    """
    Get current fund information.
    
    RACE CONDITION WARNING:
    - Fund values can change between reading and response
    - Share price calculation uses non-atomic data
    - Information might be stale immediately after retrieval
    
    Args:
        None (public endpoint)
    
    Returns:
        Current fund statistics
    """
    with get_session() as session:
        # Get fund (NO SNAPSHOT ISOLATION)
        fund = session.query(Fund).first()
        if not fund:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Fund not initialized"
            )
        
        # Read fund values (can be inconsistent with ongoing operations)
        total_value = float(fund.total_value)
        total_shares = float(fund.total_shares)
        
        # Calculate share price
        # RACE CONDITION: Values could have changed during calculation
        if total_shares > 0:
            share_price = total_value / total_shares
        else:
            share_price = 0.0
        
        return FundInfoResponse(
            total_value=total_value,
            total_shares=total_shares,
            share_price=share_price,
            updated_at=fund.updated_at
        )
