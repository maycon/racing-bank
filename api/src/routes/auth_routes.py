"""
Authentication routes.
Handles user onboarding, login, and two-factor authentication.
"""

import pyotp
from fastapi import APIRouter, Depends, HTTPException, Request, status

from database import User, Account, FundAccount, get_session
from schemas import (
    LoginUserResponse, OnboardingRequest, OnboardingResponse,
    LoginRequest, LoginResponse,
    TwoFactorRequest, TokenResponse
)
from auth import AccessLevel, access, get_current_user, hash_password, verify_password, create_access_token, verify_totp, generate_totp_uri


router = APIRouter(tags=["Authentication"])


@router.post("/onboarding", response_model=OnboardingResponse)
def onboarding(
    request: Request,
    data: OnboardingRequest
):
    """
    Register a new user (onboarding process).
    
    Creates a new user with:
    - Username and password
    - TOTP secret for 2FA
    - Empty cash account
    - Empty fund account
    
    RACE CONDITION WARNING:
    - No transaction isolation for user creation
    - If two requests with same username arrive simultaneously, both might pass
      the uniqueness check before either commits
    - Can result in database constraint violation or duplicate users
    - No distributed locking mechanism
    
    Args:
        data: User registration details
    
    Returns:
        User details with TOTP secret and URI for QR code
    
    Raises:
        HTTPException: If username already exists
    """
    with get_session() as session:
        # Check if username already exists
        # WARNING: This check is not atomic with the insert below!
        # Another request could insert the same username between this check and commit
        existing_user = session.query(User).filter_by(username=data.username).one_or_none()
        if existing_user:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Username already exists"
            )
        
        # Generate TOTP secret for 2FA
        totp_secret = pyotp.random_base32()
        
        # Create new user
        new_user = User(
            username=data.username,
            password_hash=hash_password(data.password),
            totp_secret=totp_secret
        )
        session.add(new_user)
        session.flush()  # Get user ID without committing
        
        # Create empty cash account for user
        account = Account(user_id=new_user.id, balance=0.00)
        
        # Create empty fund account for user
        fund_account = FundAccount(user_id=new_user.id, shares=0.000000)
        
        session.add_all([account, fund_account])
        
        # RACE CONDITION: Commit happens here
        # If another request is processing the same username, both might succeed
        # up to this point, causing a constraint violation on commit
        session.commit()
        
        # Generate TOTP URI for QR code (for authenticator apps)
        totp_uri = generate_totp_uri(new_user.username, totp_secret)
        
        return OnboardingResponse(
            username=new_user.username,
            totp_secret=totp_secret,
            totp_uri=totp_uri,
            message="User created successfully. Save your TOTP secret and scan the QR code."
        )


@router.post("/login", response_model=LoginResponse)
@access(AccessLevel.ANONYMOUS)
def login(
    request: Request,
    data: LoginRequest
):
    """
    First-factor authentication (username and password).
    
    Validates user credentials. If successful, user must proceed to
    /2fa endpoint with a TOTP token to get an access token.
    
    Args:
        data: Login credentials
    
    Returns:
        Success message with username
    
    Raises:
        HTTPException: If credentials are invalid
    """
    with get_session() as session:
        # Retrieve user by username
        user = session.query(User).filter_by(username=data.username).one_or_none()
        
        # Verify user exists and password is correct
        if not user or not verify_password(data.password, user.password_hash):
            # Don't reveal whether username or password is wrong (security best practice)
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid username or password"
            )
        
        return LoginResponse(
            message="First factor authenticated. Provide TOTP token at /2fa to complete login.",
            token=create_access_token({"sub": user.username, "status": "2fa_pending"}),
            user=LoginUserResponse(
                username=user.username,
                created_at=user.created_at
            )
        )


@router.post("/2fa", response_model=TokenResponse)
@access(AccessLevel.TWOFA_PENDING)
def two_factor_auth(
    request: Request, 
    data: TwoFactorRequest,
    current_user: User = Depends(get_current_user)
):
    """
    Second-factor authentication (TOTP token).
    
    Validates TOTP token and issues JWT access token upon success.
    
    Args:
        data: Username and TOTP token
    
    Returns:
        JWT access token for API authentication
    
    Raises:
        HTTPException: If user not found or TOTP token invalid
    """
    with get_session() as session:
        # Retrieve user
        user = session.query(User).filter_by(username=current_user.username).one_or_none()
        if not user:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="User not found"
            )
        
        # Verify TOTP token
        if not verify_totp(user.totp_secret, data.token):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid TOTP token"
            )
        
        # Create JWT access token
        access_token = create_access_token({"sub": user.username, "status": "authenticated"})
        
        return TokenResponse(access_token=access_token)
