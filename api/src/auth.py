"""
Authentication and security utilities.
Handles password hashing, JWT tokens, and TOTP verification.
"""

import hashlib
from inspect import iscoroutinefunction
import secrets
from datetime import datetime, timedelta
from typing import Optional
from enum import Enum
from functools import wraps
from typing import Callable, Any, Optional

import jwt
import pyotp
from fastapi import Depends, HTTPException, status, Request, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from config import SECRET_KEY, ALGORITHM, ACCESS_TOKEN_EXPIRE_MINUTES
from database import User, get_session


# Security scheme for bearer token
security = HTTPBearer()

SECRET_KEY = "CHANGE_ME"
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 30

class AccessLevel(str, Enum):
    ANONYMOUS = "anonymous"         # No authentication required
    TWOFA_PENDING = "2fa_pending"   # Authenticated but 2FA still pending
    FULL = "full"                   # Authenticated and 2FA verified

def access(level: AccessLevel) -> Callable:
    """
    Decorator that enforces access control based on authentication and 2FA status.
    
    Requirements:
      - The decorated endpoint must receive a 'request: Request' parameter.
      - It uses the existing decode_access_token() to validate JWT tokens.
    """
    def _decorator(func: Callable) -> Callable:
        @wraps(func)
        async def _aw(*args: Any, **kwargs: Any):
            # Attempt to locate the Request object from args or kwargs
            request: Optional[Request] = kwargs.get("request")
            if request is None:
                for a in args:
                    if isinstance(a, Request):
                        request = a
                        break
            if request is None:
                raise RuntimeError(
                    "Endpoint decorated with @access must include 'request: Request'."
                )

            # PUBLIC routes: token is optional
            if level == AccessLevel.ANONYMOUS:
                try:
                    token = request.headers.get("authorization", "").split(" ", 1)[1]
                    payload = decode_access_token(token)
                    request.state.jwt = payload
                except Exception:
                    # No token or invalid token -> ignore for PUBLIC endpoints
                    request.state.jwt = None
                # Proceed to endpoint
                return await func(*args, **kwargs) if iscoroutinefunction(func) else func(*args, **kwargs)

            # For TWOFA_PENDING and FULL, a valid token is required
            auth_header = request.headers.get("authorization")
            if not auth_header or not auth_header.lower().startswith("bearer "):
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Authorization header missing or invalid"
                )

            token = auth_header.split(" ", 1)[1].strip()
            payload = decode_access_token(token)
            request.state.jwt = payload

            # FULL access: requires twofa_verified == True or equivalent
            status_claim = payload.get("status") or payload.get("auth_status") or ""
            if level == AccessLevel.FULL and status_claim != "authenticated":
                raise HTTPException(
                    status_code=status.HTTP_403_FORBIDDEN,
                    detail="Two-factor authentication required"
                )

            # TWOFA_PENDING: requires valid token but not yet authenticated
            if level == AccessLevel.TWOFA_PENDING and status_claim == "authenticated":
                raise HTTPException(
                    status_code=status.HTTP_403_FORBIDDEN,
                    detail="2FA already verified; use full-access endpoints"
                )

            return await func(*args, **kwargs) if iscoroutinefunction(func) else func(*args, **kwargs)

        setattr(_aw, "_access_level", level)
        return _aw
    return _decorator

def hash_password(password: str) -> str:
    """
    Hash a password using SHA-256.
    
    NOTE: In production, use bcrypt or argon2 instead of SHA-256!
    SHA-256 is used here for simplicity only.
    """
    return hashlib.sha256(password.encode("utf-8")).hexdigest()


def verify_password(plain_password: str, password_hash: str) -> bool:
    """
    Verify a password against its hash.
    Uses constant-time comparison to prevent timing attacks.
    """
    return secrets.compare_digest(hash_password(plain_password), password_hash)


def create_access_token(data: dict, expires_delta: Optional[timedelta] = None) -> str:
    """
    Create a JWT access token.
    
    Args:
        data: Dictionary containing claims to encode in the token
        expires_delta: Optional custom expiration time
    
    Returns:
        Encoded JWT token string
    """
    to_encode = data.copy()
    
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    
    to_encode.update({
        "exp": expire,
        "iat": datetime.utcnow()
    })
    
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)


def decode_access_token(token: str) -> dict:
    """
    Decode and validate a JWT access token.
    
    Args:
        token: JWT token string to decode
    
    Returns:
        Dictionary containing decoded token claims
    
    Raises:
        HTTPException: If token is expired or invalid
    """
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        return payload
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token has expired"
        )
    except jwt.InvalidTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token"
        )


def verify_totp(secret: str, token: str) -> bool:
    """
    Verify a TOTP (Time-based One-Time Password) token.
    
    Args:
        secret: User's TOTP secret key
        token: 6-digit TOTP token to verify
    
    Returns:
        True if token is valid, False otherwise
    """
    totp = pyotp.TOTP(secret)
    # valid_window=1 allows tokens from previous/next 30-second window
    # to account for clock skew
    return totp.verify(token, valid_window=1)


def generate_totp_uri(username: str, secret: str, issuer: str = "Bankao") -> str:
    """
    Generate a TOTP URI for QR code generation.
    
    Args:
        username: User's username
        secret: User's TOTP secret
        issuer: Application name (default: "Bankao")
    
    Returns:
        TOTP URI string that can be encoded in a QR code
    """
    totp = pyotp.TOTP(secret)
    return totp.provisioning_uri(name=username, issuer_name=issuer)


def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security)
) -> User:
    """
    Dependency to get the current authenticated user from JWT token.
    
    This function is used as a FastAPI dependency to protect endpoints.
    It extracts and validates the JWT token, then retrieves the user.
    
    Args:
        credentials: HTTP authorization credentials containing the bearer token
    
    Returns:
        User object of the authenticated user
    
    Raises:
        HTTPException: If token is invalid or user not found
    """
    # Decode the JWT token
    payload: dict = decode_access_token(credentials.credentials)
    if not payload:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Could not validate credentials"
        )
    
    # Extract username from token payload
    username: str | None = payload.get("sub")
    auth_status: str | None = payload.get("auth_status")
    if not username:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token payload: missing subject"
        )
    
    # Retrieve user from database
    with get_session() as session:
        user = session.query(User).filter_by(username=username).one_or_none()
        if not user:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="User not found"
            )
        
        # Detach user from session so it can be used outside the session context
        session.expunge(user)
        return user
