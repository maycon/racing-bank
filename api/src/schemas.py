"""
Pydantic schemas for request and response validation.
Defines data models for API endpoints.
"""

from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime


# ==================== Auth Schemas ====================

class OnboardingRequest(BaseModel):
    """Request schema for user registration/onboarding."""
    username: str = Field(..., min_length=3, max_length=64, description="Unique username")
    password: str = Field(..., min_length=6, description="User password")


class OnboardingResponse(BaseModel):
    """Response schema after successful onboarding."""
    username: str
    totp_secret: str
    totp_uri: str
    message: str


class LoginRequest(BaseModel):
    """Request schema for first-factor authentication."""
    username: str
    password: str

class LoginUserResponse(BaseModel):
    """Response schema after successful login."""
    username: str
    created_at: datetime

class LoginResponse(BaseModel):
    """Response schema after successful login."""
    message: str
    token: str
    user: LoginUserResponse


class TwoFactorRequest(BaseModel):
    """Request schema for two-factor authentication."""
    token: str = Field(..., min_length=6, max_length=6, description="6-digit TOTP token")


class TokenResponse(BaseModel):
    """Response schema containing JWT access token."""
    access_token: str
    token_type: str = "bearer"


# ==================== Account Schemas ====================

class DepositRequest(BaseModel):
    """Request schema for cash deposit."""
    amount: float = Field(..., gt=0, description="Amount to deposit (must be positive)")


class WithdrawalRequest(BaseModel):
    """Request schema for cash withdrawal."""
    amount: float = Field(..., gt=0, description="Amount to withdraw (must be positive)")


class BalanceResponse(BaseModel):
    """Response schema showing account balances."""
    username: str
    cash_balance: float
    fund_shares: float
    fund_value: float
    total_portfolio: float


# ==================== Transfer Schemas ====================

class TransferRequest(BaseModel):
    """Request schema for account-to-account transfer."""
    to_username: str = Field(..., description="Recipient username")
    amount: float = Field(..., gt=0, description="Amount to transfer (must be positive)")


class TransferResponse(BaseModel):
    """Response schema after successful transfer."""
    status: str
    transfer_id: int
    from_username: str
    to_username: str
    amount: float
    new_balance: float


# ==================== Fund Schemas ====================

class SubscriptionRequest(BaseModel):
    """Request schema for fund subscription (investing)."""
    amount: float = Field(..., gt=0, description="Amount to invest in fund (must be positive)")


class RedemptionRequest(BaseModel):
    """Request schema for fund redemption (divesting)."""
    amount: float = Field(..., gt=0, description="Amount to redeem from fund (must be positive)")


class FundOperationResponse(BaseModel):
    """Response schema after fund operation."""
    status: str
    operation: str
    amount: float
    shares: float
    share_price: float
    new_cash_balance: float
    new_fund_shares: float


class FundInfoResponse(BaseModel):
    """Response schema showing fund information."""
    total_value: float
    total_shares: float
    share_price: float
    updated_at: datetime


# ==================== Transaction Schemas ====================

class TransactionItem(BaseModel):
    """Schema for a single transaction in history."""
    id: int
    type: str
    amount: float
    description: Optional[str]
    created_at: str


class TransferItem(BaseModel):
    """Schema for a single transfer in history."""
    id: int
    from_username: str
    to_username: str
    amount: float
    created_at: str


class StatementResponse(BaseModel):
    """Response schema for account statement."""
    username: str
    cash_balance: float
    fund_shares: float
    fund_value: float
    transactions: List[TransactionItem]
    transfers: List[TransferItem]


# ==================== Health Check Schema ====================

class HealthResponse(BaseModel):
    """Response schema for health check endpoint."""
    status: str
    database: str
