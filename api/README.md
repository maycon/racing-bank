# Bankao API - Educational Banking System

A **deliberately vulnerable** banking API built with FastAPI to demonstrate race condition vulnerabilities in concurrent systems.

⚠️ **WARNING: This application has NO race condition protections! For educational purposes only!**

## Features

- 🔐 Two-factor authentication (TOTP)
- 👤 User onboarding/registration
- 💰 Cash deposits and withdrawals
- 💸 Account-to-account transfers
- 📈 Investment fund subscriptions and redemptions
- 📊 Transaction history and statements
- 🗄️ MariaDB database backend

## ⚠️ Educational Purpose

This API is **intentionally built without race condition protections** to demonstrate common concurrency issues in financial systems. All database operations are vulnerable to:

- **Lost updates** - Concurrent modifications overwrite each other
- **Dirty reads** - Reading uncommitted or intermediate data
- **Phantom reads** - Queries return different results due to concurrent changes
- **Double spending** - Same money spent twice due to lack of locking
- **Overdrafts** - Negative balances from concurrent withdrawals
- **Incorrect calculations** - Fund share prices computed from stale data

**DO NOT use this code in production!** See comments in source code for detailed vulnerability scenarios.

## Project Structure

```
.
├── config.py                 # Configuration and environment variables
├── database.py               # SQLAlchemy models and database setup
├── auth.py                   # Authentication utilities (JWT, TOTP)
├── schemas.py                # Pydantic request/response models
├── routes/
│   ├── __init__.py          # Routes package initialization
│   ├── auth_routes.py       # Onboarding, login, 2FA endpoints
│   ├── account_routes.py    # Deposit, withdrawal, balance endpoints
│   ├── transfer_routes.py   # Transfer and statement endpoints
│   └── fund_routes.py       # Fund subscription/redemption endpoints
├── main.py                   # FastAPI application entry point
├── requirements.txt          # Python dependencies
├── Dockerfile                # Docker container definition
├── compose.yaml              # Docker Compose orchestration
└── README.md                 # This file
```

## Requirements

### Using Docker (Recommended)
- Docker 20.10+
- Docker Compose 2.0+

### Manual Installation
- Python 3.11+
- MariaDB 11+
- pip or uv package manager

## Quick Start with Docker

1. **Clone or download the project**

2. **Start the services**
   ```bash
   docker-compose up -d
   ```

3. **View logs to get TOTP secrets for demo users**
   ```bash
   docker-compose logs api
   ```
   
   You'll see output like:
   ```
   Demo user 'alice' created - TOTP secret: JBSWY3DPEHPK3PXP
   Demo user 'bob' created   - TOTP secret: HXDMVJECJJWSRB3H
   ```

4. **Access the API**
   - API: http://localhost:8000
   - Interactive docs: http://localhost:8000/docs
   - Alternative docs: http://localhost:8000/redoc

5. **Stop the services**
   ```bash
   docker-compose down
   ```

## Manual Installation

1. **Install dependencies**
   ```bash
   pip install -r requirements.txt
   ```

2. **Set up MariaDB**
   ```bash
   # Install MariaDB (Ubuntu/Debian)
   sudo apt-get install mariadb-server
   
   # Start MariaDB service
   sudo systemctl start mariadb
   
   # Create database
   sudo mysql -e "CREATE DATABASE bankao;"
   ```

3. **Configure environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your database credentials
   ```

4. **Run the application**
   ```bash
   python main.py
   # or
   uvicorn main:app --reload
   ```

## API Endpoints

### Authentication

#### 1. Register New User (Onboarding)
```bash
POST /onboarding
Content-Type: application/json

{
  "username": "john",
  "password": "secure123"
}
```

Response includes TOTP secret for 2FA setup.

#### 2. Login (First Factor)
```bash
POST /login
Content-Type: application/json

{
  "username": "john",
  "password": "secure123"
}
```

#### 3. Two-Factor Authentication
```bash
POST /2fa
Content-Type: application/json

{
  "username": "john",
  "token": "123456"  # 6-digit TOTP code
}
```

Response includes JWT access token.

### Account Management

All endpoints require `Authorization: Bearer <token>` header.

#### Deposit Cash
```bash
POST /deposit
Authorization: Bearer <token>
Content-Type: application/json

{
  "amount": 500.00
}
```

#### Withdraw Cash
```bash
POST /withdrawal
Authorization: Bearer <token>
Content-Type: application/json

{
  "amount": 100.00
}
```

#### Get Balance
```bash
GET /balance
Authorization: Bearer <token>
```

### Transfers

#### Transfer Money
```bash
POST /transfer
Authorization: Bearer <token>
Content-Type: application/json

{
  "to_username": "alice",
  "amount": 50.00
}
```

#### Get Statement
```bash
GET /statement
Authorization: Bearer <token>
```

### Investment Fund

#### Subscribe to Fund (Invest)
```bash
POST /subscribe
Authorization: Bearer <token>
Content-Type: application/json

{
  "amount": 1000.00
}
```

#### Redeem from Fund (Divest)
```bash
POST /redemption
Authorization: Bearer <token>
Content-Type: application/json

{
  "amount": 500.00
}
```

#### Get Fund Information
```bash
GET /fund/info
```

## Complete Usage Example

### Step 1: Generate TOTP Token

Use the TOTP secret from logs with any authenticator app (Google Authenticator, Authy, etc.) or generate programmatically:

```python
import pyotp

# Use secret from logs
totp = pyotp.TOTP("JBSWY3DPEHPK3PXP")
print(totp.now())  # Prints current 6-digit token
```

### Step 2: Login Flow

```bash
# 1. First factor authentication
curl -X POST http://localhost:8000/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "alice123"}'
# Get the temp_token from response


# 2. Second factor with TOTP token
curl -X POST http://localhost:8000/2fa \
  -H "Content-Type: application/json" \
   -H "Authorization: Bearer $TEMP_TOKEN" \
  -d '{"token": "123456"}'

# Save the access_token from response
```

### Step 3: Make Transactions

```bash
# Set your token
TOKEN="your_access_token_here"

# Deposit cash
curl -X POST http://localhost:8000/deposit \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount": 500.00}'

# Subscribe to fund
curl -X POST http://localhost:8000/subscribe \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount": 200.00}'

# Transfer to another user
curl -X POST http://localhost:8000/transfer \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"to_username": "bob", "amount": 50.00}'

# Check balance
curl -X GET http://localhost:8000/balance \
  -H "Authorization: Bearer $TOKEN"

# Get statement
curl -X GET http://localhost:8000/statement \
  -H "Authorization: Bearer $TOKEN"
```

## Demo Users

The application initializes with two demo users:

| Username | Password  | Initial Balance |
|----------|-----------|----------------|
| alice    | alice123  | $1,000.00      |
| bob      | bob123    | $500.00        |

TOTP secrets are printed in console logs on first startup.

## Race Condition Examples

### Example 1: Double Spending via Concurrent Transfers

```bash
# Alice has $100, tries to send $80 to Bob twice simultaneously
# Both requests check balance ($100 >= $80) ✓
# Both requests deduct $80
# Alice ends with -$60 (overdraft!)

# Terminal 1:
curl -X POST http://localhost:8000/transfer \
  -H "Authorization: Bearer $ALICE_TOKEN" \
  -d '{"to_username": "bob", "amount": 80}' &

# Terminal 2 (immediately):
curl -X POST http://localhost:8000/transfer \
  -H "Authorization: Bearer $ALICE_TOKEN" \
  -d '{"to_username": "bob", "amount": 80}' &
```

### Example 2: Lost Updates in Deposits

```bash
# Account has $100
# Deposit $50 and $30 simultaneously
# Expected: $180, Actual: $130 or $150 (lost update!)

curl -X POST http://localhost:8000/deposit \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount": 50}' &

curl -X POST http://localhost:8000/deposit \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount": 30}' &
```

### Example 3: Fund Share Price Manipulation

```bash
# Multiple subscriptions with different calculated share prices
# Can result in incorrect share allocations

for i in {1..10}; do
  curl -X POST http://localhost:8000/subscribe \
    -H "Authorization: Bearer $TOKEN" \
    -d '{"amount": 100}' &
done
```

## How to Fix Race Conditions

To make this API production-ready, you would need to:

1. **Use database transactions with proper isolation levels**
   ```python
   session.begin()
   try:
       # operations
       session.commit()
   except:
       session.rollback()
   ```

2. **Implement pessimistic locking**
   ```python
   account = session.query(Account).with_for_update().filter_by(id=user_id).one()
   ```

3. **Use optimistic locking with version numbers**
   ```python
   class Account:
       version = Column(Integer, default=0)
       # Check version on update
   ```

4. **Implement distributed locks (Redis, etc.)**
   ```python
   with redis_lock(f"user:{user_id}"):
       # critical section
   ```

5. **Use atomic database operations**
   ```python
   session.execute(
       update(Account)
       .where(Account.id == user_id)
       .values(balance=Account.balance + amount)
   )
   ```

6. **Implement idempotency keys for operations**

7. **Use message queues for sequential processing**

## Testing

### Unit Testing (to demonstrate race conditions)

Create a test script to trigger race conditions:

```python
import concurrent.futures
import requests

def concurrent_transfers():
    # Trigger double-spending
    with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
        futures = [
            executor.submit(transfer, 80)
            for _ in range(10)
        ]
        results = [f.result() for f in futures]
    return results
```

### Load Testing

```bash
# Using Apache Bench
ab -n 1000 -c 50 -m POST \
  -H "Authorization: Bearer $TOKEN" \
  -p deposit.json \
  http://localhost:8000/deposit
```

## Security Notes

⚠️ **This is a demonstration project. For production:**

1. **Never** store passwords as SHA-256 hashes (use bcrypt or argon2)
2. **Never** expose TOTP secrets in API responses
3. **Always** use HTTPS in production
4. **Always** implement rate limiting
5. **Always** use proper database transactions with locking
6. **Always** use environment variables for secrets
7. **Always** implement proper logging and monitoring
8. **Always** validate and sanitize all inputs
9. **Always** implement proper error handling
10. **Never** return detailed error messages to clients

## Technology Stack

- **Framework**: FastAPI 0.115.0
- **Server**: Uvicorn
- **Database**: MariaDB 11
- **ORM**: SQLAlchemy 2.0
- **Authentication**: JWT (PyJWT) + TOTP (PyOTP)
- **Validation**: Pydantic v2

## License

This project is for educational purposes only. Use at your own risk.

## Contributing

This is an educational project demonstrating security vulnerabilities. If you'd like to contribute additional race condition examples or documentation, please:

1. Keep vulnerabilities intact (this is the point!)
2. Add detailed comments explaining the issues
3. Document new race condition scenarios

## Additional Resources

- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [SQLAlchemy Documentation](https://docs.sqlalchemy.org/)
- [Database Transaction Isolation Levels](https://en.wikipedia.org/wiki/Isolation_(database_systems))
- [Race Conditions in Databases](https://www.postgresql.org/docs/current/transaction-iso.html)
- [OWASP: Race Conditions](https://owasp.org/www-community/vulnerabilities/Race_condition)

## Support

This is an educational project. For questions about race conditions or database concurrency, please refer to the resources above or consult database documentation.
