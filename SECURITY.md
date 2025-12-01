# Security Considerations

## ⚠️ Important Disclaimer

This document explains the security vulnerabilities **intentionally included** in Racing Bank for educational purposes. It also provides guidance on how to properly secure a financial application for production use.

**DO NOT use this code in production!**

---

## Intentional Vulnerabilities

### 1. No Race Condition Protection

**Vulnerability**: No locking mechanisms for concurrent operations

**Impact**: 
- Double spending attacks
- Lost updates
- Accounting inconsistencies
- Data corruption

**Present in**:
- Transfers
- Deposits
- Withdrawals
- Fund operations

**How to fix**: See [RACE_CONDITIONS.md](RACE_CONDITIONS.md) for detailed fixes

---

### 2. Weak Password Hashing

**Vulnerability**: Passwords hashed with SHA-256 instead of bcrypt/argon2

**Current implementation**:
```python
import hashlib

def hash_password(password: str) -> str:
    return hashlib.sha256(password.encode()).hexdigest()
```

**Why it's bad**:
- SHA-256 is too fast (vulnerable to brute force)
- No salt (vulnerable to rainbow tables)
- Not designed for password hashing

**How to fix**:
```python
from passlib.context import CryptContext

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def hash_password(password: str) -> str:
    return pwd_context.hash(password)

def verify_password(plain_password: str, hashed_password: str) -> bool:
    return pwd_context.verify(plain_password, hashed_password)
```

---

### 3. TOTP Secrets Exposed

**Vulnerability**: TOTP secrets printed in logs and returned in API responses

**Impact**:
- Anyone with log access can bypass 2FA
- TOTP secrets visible in API responses
- Secrets stored in plain text

**Current implementation**:
```python
# Bad: Secret returned in response
return {
    "message": "User created",
    "totp_secret": totp_secret  # ❌ Never do this!
}
```

**How to fix**:
```python
# Good: Secret encrypted and never exposed
from cryptography.fernet import Fernet

def encrypt_totp_secret(secret: str, key: bytes) -> str:
    f = Fernet(key)
    return f.encrypt(secret.encode()).decode()

def decrypt_totp_secret(encrypted: str, key: bytes) -> str:
    f = Fernet(key)
    return f.decrypt(encrypted.encode()).decode()

# Store encrypted, never return in API
user.totp_secret = encrypt_totp_secret(totp_secret, encryption_key)
# Return QR code URL instead, and show secret only once during setup
```

---

### 4. No Rate Limiting

**Vulnerability**: No limits on API requests

**Impact**:
- Brute force attacks possible
- Easy to trigger race conditions
- DoS attacks
- Resource exhaustion

**How to fix**:
```python
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded

limiter = Limiter(key_func=get_remote_address)
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

@app.post("/login")
@limiter.limit("5/minute")  # Max 5 attempts per minute
async def login(request: Request, credentials: LoginRequest):
    # Login logic
    pass

@app.post("/transfer")
@limiter.limit("10/minute")  # Max 10 transfers per minute
async def transfer(request: Request, transfer_req: TransferRequest):
    # Transfer logic
    pass
```

---

### 5. No Input Validation

**Vulnerability**: Insufficient input validation and sanitization

**Impact**:
- Negative amount transactions
- Excessive amounts
- SQL injection (if queries were vulnerable)
- XSS in logs

**Current validation**:
```python
# Bad: Minimal validation
def deposit(amount: float):
    if amount > 0:
        # Process deposit
        pass
```

**How to fix**:
```python
from pydantic import BaseModel, Field, validator
from decimal import Decimal

class DepositRequest(BaseModel):
    amount: Decimal = Field(..., gt=0, le=1000000)
    
    @validator('amount')
    def validate_amount(cls, v):
        # Check for reasonable precision
        if v.as_tuple().exponent < -2:
            raise ValueError('Amount cannot have more than 2 decimal places')
        
        # Check for reasonable range
        if v > Decimal('1000000'):
            raise ValueError('Amount exceeds maximum allowed')
        
        return v

    class Config:
        # Use Decimal for money
        json_encoders = {
            Decimal: lambda v: str(v)
        }
```

---

### 6. No Idempotency

**Vulnerability**: No idempotency keys for operations

**Impact**:
- Duplicate transactions on network retry
- Same transfer executed multiple times
- No way to prevent duplicates

**How to fix**:
```python
from uuid import UUID

# Add idempotency key to requests
class TransferRequest(BaseModel):
    to_username: str
    amount: Decimal
    idempotency_key: UUID

# Store processed keys
processed_operations = {}  # Use Redis in production

@app.post("/transfer")
async def transfer(request: TransferRequest):
    # Check if already processed
    if request.idempotency_key in processed_operations:
        # Return cached result
        return processed_operations[request.idempotency_key]
    
    # Process transfer
    result = execute_transfer(request)
    
    # Cache result
    processed_operations[request.idempotency_key] = result
    
    return result
```

---

### 7. Detailed Error Messages

**Vulnerability**: Error messages leak system information

**Current implementation**:
```python
# Bad: Exposes internal details
except Exception as e:
    return JSONResponse(
        status_code=500,
        content={"detail": str(e)}  # ❌ Leaks stack traces
    )
```

**How to fix**:
```python
import logging

logger = logging.getLogger(__name__)

# Good: Generic errors to client, details in logs
except InsufficientFundsError as e:
    return JSONResponse(
        status_code=400,
        content={"detail": "Insufficient funds"}
    )
except Exception as e:
    # Log full error internally
    logger.error(f"Unexpected error: {e}", exc_info=True)
    
    # Return generic error to client
    return JSONResponse(
        status_code=500,
        content={"detail": "Internal server error"}
    )
```

---

### 8. No HTTPS/TLS

**Vulnerability**: API runs over HTTP in production

**Impact**:
- Credentials transmitted in plain text
- Tokens can be intercepted
- Man-in-the-middle attacks
- Session hijacking

**How to fix**:
```python
# Use HTTPS with proper certificates
# In production, use:
# - Let's Encrypt for certificates
# - Reverse proxy (nginx/traefik) with TLS
# - HSTS headers

from fastapi import FastAPI

app = FastAPI()

# Add security headers
from starlette.middleware.trustedhost import TrustedHostMiddleware
from starlette.middleware.httpsredirect import HTTPSRedirectMiddleware

app.add_middleware(TrustedHostMiddleware, allowed_hosts=["example.com"])
app.add_middleware(HTTPSRedirectMiddleware)

@app.middleware("http")
async def add_security_headers(request, call_next):
    response = await call_next(request)
    response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["X-XSS-Protection"] = "1; mode=block"
    return response
```

---

### 9. No Audit Logging

**Vulnerability**: No comprehensive audit trail

**Impact**:
- Can't track who did what
- No forensics capability
- Can't detect fraud
- Compliance violations

**How to fix**:
```python
import logging
from datetime import datetime

# Create audit logger
audit_logger = logging.getLogger('audit')
audit_logger.setLevel(logging.INFO)

# Add handler to separate file
handler = logging.FileHandler('audit.log')
handler.setFormatter(logging.Formatter(
    '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
))
audit_logger.addHandler(handler)

def audit_log(
    user_id: int,
    action: str,
    resource: str,
    result: str,
    details: dict = None
):
    audit_logger.info(
        f"User: {user_id} | Action: {action} | "
        f"Resource: {resource} | Result: {result} | "
        f"Details: {details}"
    )

# Use in endpoints
@app.post("/transfer")
async def transfer(request: TransferRequest, current_user: User):
    try:
        result = execute_transfer(request)
        
        audit_log(
            user_id=current_user.id,
            action="TRANSFER",
            resource=f"accounts/{request.to_username}",
            result="SUCCESS",
            details={"amount": request.amount}
        )
        
        return result
    except Exception as e:
        audit_log(
            user_id=current_user.id,
            action="TRANSFER",
            resource=f"accounts/{request.to_username}",
            result="FAILURE",
            details={"error": str(e)}
        )
        raise
```

---

### 10. JWT Without Refresh Tokens

**Vulnerability**: Long-lived JWT with no refresh mechanism

**Impact**:
- Stolen tokens valid until expiration
- No way to revoke tokens
- Forced logout not possible

**How to fix**:
```python
from datetime import datetime, timedelta
import redis

# Use short-lived access tokens and long-lived refresh tokens
ACCESS_TOKEN_EXPIRE = timedelta(minutes=15)
REFRESH_TOKEN_EXPIRE = timedelta(days=7)

redis_client = redis.Redis()

def create_tokens(user_id: int):
    # Short-lived access token
    access_token = create_jwt(
        data={"sub": str(user_id)},
        expires_delta=ACCESS_TOKEN_EXPIRE
    )
    
    # Long-lived refresh token
    refresh_token = create_jwt(
        data={"sub": str(user_id), "type": "refresh"},
        expires_delta=REFRESH_TOKEN_EXPIRE
    )
    
    # Store refresh token in Redis
    redis_client.setex(
        f"refresh_token:{user_id}",
        REFRESH_TOKEN_EXPIRE,
        refresh_token
    )
    
    return {
        "access_token": access_token,
        "refresh_token": refresh_token
    }

@app.post("/refresh")
async def refresh_token(refresh_token: str):
    # Validate refresh token
    payload = decode_jwt(refresh_token)
    user_id = payload["sub"]
    
    # Check if token is in Redis
    stored_token = redis_client.get(f"refresh_token:{user_id}")
    if not stored_token or stored_token.decode() != refresh_token:
        raise HTTPException(401, "Invalid refresh token")
    
    # Create new access token
    return create_tokens(user_id)

@app.post("/logout")
async def logout(current_user: User):
    # Revoke refresh token
    redis_client.delete(f"refresh_token:{current_user.id}")
    return {"message": "Logged out"}
```

---

## Production Security Checklist

### Before Going to Production

#### Authentication & Authorization
- [ ] Use bcrypt/argon2 for password hashing
- [ ] Implement refresh token rotation
- [ ] Add token revocation mechanism
- [ ] Encrypt TOTP secrets at rest
- [ ] Implement account lockout after failed attempts
- [ ] Add device fingerprinting
- [ ] Implement session management

#### API Security
- [ ] Enable HTTPS/TLS everywhere
- [ ] Implement rate limiting
- [ ] Add CORS policies
- [ ] Validate all inputs
- [ ] Sanitize outputs
- [ ] Use API keys for service-to-service
- [ ] Implement idempotency keys
- [ ] Add request signing

#### Database Security
- [ ] Use pessimistic locking for transactions
- [ ] Set proper isolation levels (SERIALIZABLE)
- [ ] Encrypt sensitive data at rest
- [ ] Use connection pooling
- [ ] Implement read replicas
- [ ] Regular backups
- [ ] Audit logging

#### Infrastructure
- [ ] Use WAF (Web Application Firewall)
- [ ] DDoS protection
- [ ] Monitoring and alerting
- [ ] Intrusion detection
- [ ] Security scanning
- [ ] Penetration testing
- [ ] Disaster recovery plan

#### Compliance
- [ ] GDPR compliance (if applicable)
- [ ] PCI DSS compliance (for payments)
- [ ] SOC 2 compliance
- [ ] Regular security audits
- [ ] Privacy policy
- [ ] Terms of service

---

## Security Testing

### Tools for Testing

**Static Analysis**:
- Bandit (Python security linter)
- Safety (dependency checker)
- SonarQube

**Dynamic Analysis**:
- OWASP ZAP
- Burp Suite
- SQLMap

**Load Testing**:
- Apache Bench
- Locust
- K6

### Example Security Tests

```python
# test_security.py
import pytest
import requests

def test_sql_injection_protection():
    """Test that SQL injection is prevented"""
    response = requests.post(
        "http://localhost:8000/login",
        json={
            "username": "admin' OR '1'='1",
            "password": "password"
        }
    )
    assert response.status_code == 401

def test_rate_limiting():
    """Test that rate limiting works"""
    for i in range(10):
        response = requests.post(
            "http://localhost:8000/login",
            json={"username": "test", "password": "test"}
        )
    
    # 11th request should be rate limited
    response = requests.post(
        "http://localhost:8000/login",
        json={"username": "test", "password": "test"}
    )
    assert response.status_code == 429

def test_xss_protection():
    """Test that XSS is prevented"""
    response = requests.post(
        "http://localhost:8000/register",
        json={
            "username": "<script>alert('xss')</script>",
            "password": "password"
        }
    )
    # Should be rejected or sanitized
    assert "<script>" not in response.text
```

---

## Additional Resources

### Security Standards
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [OWASP API Security Top 10](https://owasp.org/www-project-api-security/)
- [CWE Top 25](https://cwe.mitre.org/top25/)

### Best Practices
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)
- [PCI DSS Standards](https://www.pcisecuritystandards.org/)
- [GDPR Guidelines](https://gdpr.eu/)

### Learning Resources
- [Web Security Academy](https://portswigger.net/web-security)
- [OWASP WebGoat](https://owasp.org/www-project-webgoat/)
- [HackTheBox](https://www.hackthebox.eu/)

---

## Conclusion

Racing Bank intentionally demonstrates what **NOT** to do in a production system. Every vulnerability shown here has been exploited in real-world systems with devastating consequences.

**Key takeaways**:
1. Always use database transactions with proper isolation
2. Never trust user input
3. Implement defense in depth
4. Security is not optional for financial systems
5. Regular security audits are essential

**Remember**: This is educational software. Real financial systems require much more security than shown here!