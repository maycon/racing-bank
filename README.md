# 🏦 Racing Bank - Educational Banking System

![Version](https://img.shields.io/badge/version-1.0.0-cyan)
![API](https://img.shields.io/badge/API-FastAPI-green)
![Android](https://img.shields.io/badge/Android-Kotlin-purple)
![License](https://img.shields.io/badge/license-Educational-orange)

## ⚠️ WARNING

This application was developed for **educational purposes only** to demonstrate race condition vulnerabilities in concurrent systems. It has **intentional security vulnerabilities** and **MUST NOT BE USED IN PRODUCTION**.

---

## 📋 About

**Racing Bank** is a complete banking system consisting of:

1. **Backend API** - FastAPI with intentional race condition vulnerabilities
2. **Android App** - Mobile application with retro cyberpunk visual design

This project was created as an educational demonstration for [TREM](https://github.com/otavioarj/TREM), showing in practice how race conditions can affect financial systems.

---

## 🎯 Features

### Complete Banking System
- ✅ Two-factor authentication (TOTP)
- ✅ Deposits and withdrawals
- ✅ Account-to-account transfers
- ✅ Investment fund operations
- ✅ Transaction statements
- ✅ Interactive dashboard

### Educational Vulnerabilities
- ❌ Double spending in transfers
- ❌ Lost updates in deposits
- ❌ Investment inconsistencies
- ❌ Phantom reads in queries
- ❌ Race conditions in all operations

---

## 🏗️ Project Structure

```
racing-bank/
├── api/                          # FastAPI Backend
│   ├── routes/                   # API endpoints
│   │   ├── auth_routes.py       # Authentication & onboarding
│   │   ├── account_routes.py    # Deposits & withdrawals
│   │   ├── transfer_routes.py   # Transfers
│   │   └── fund_routes.py       # Investments
│   ├── config.py                 # Configuration
│   ├── database.py               # SQLAlchemy models
│   ├── auth.py                   # JWT & TOTP
│   ├── schemas.py                # Pydantic schemas
│   ├── main.py                   # Entry point
│   ├── requirements.txt          # Python dependencies
│   ├── Dockerfile                # API container
│   ├── compose.yaml              # Docker Compose
│   └── README.md                 # API documentation
│
├── app/                          # Android Application
│   ├── src/main/
│   │   ├── java/com/hacknroll/bank/
│   │   │   ├── data/            # Repository & API client
│   │   │   ├── ui/              # Activities & Fragments
│   │   │   │   ├── auth/        # Login & Registration
│   │   │   │   ├── main/        # Dashboard & Operations
│   │   │   │   └── splash/      # Splash screen
│   │   │   └── utils/           # Utilities
│   │   └── res/                 # Resources (layouts, themes)
│   ├── build.gradle.kts         # Build configuration
│   └── README.md                # App documentation
│
├── docs/                         # Additional documentation
│   ├── RACE_CONDITIONS.md       # Detailed vulnerability examples
│   ├── API_GUIDE.md             # Complete API reference
│   ├── ANDROID_SETUP.md         # Android development setup
│   └── SECURITY.md              # Security considerations
│
├── scripts/                      # Utility scripts
│   ├── setup.sh                 # Quick setup script
│   ├── demo.sh                  # Demo data loader
│   └── test_race_conditions.py  # Race condition tests
│
├── .gitignore                    # Git ignore rules
├── LICENSE                       # License file
└── README.md                     # This file
```

---

## 🚀 Quick Start

### Option 1: Docker (Recommended)

```bash
# 1. Clone the repository
git clone https://github.com/maycon/racing-bank.git
cd racing-bank

# 2. Start the API with Docker Compose
cd api
docker-compose up -d

# 3. View logs to get TOTP secrets for demo users
docker-compose logs api

# API will be available at http://localhost:8000
# Interactive docs at http://localhost:8000/docs
```

### Option 2: Manual Setup

#### API Setup

```bash
# Navigate to API directory
cd api

# Install Python dependencies
pip install -r requirements.txt

# Start MariaDB (or use Docker)
docker run -d -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=bankao \
  mariadb:11

# Run the API
python main.py
```

#### Android App Setup

```bash
# Open Android Studio
# File → Open → Select racing-bank/app directory

# Configure API endpoint (if not using emulator)
# Edit app/src/main/java/.../data/api/RetrofitClient.kt
# Change BASE_URL to your machine's IP

# Run the app on emulator or device
```

---

## 📱 Using the System

### 1. Start the API
```bash
cd api
docker-compose up -d
```

### 2. Get Demo User Credentials
```bash
docker-compose logs api | grep "TOTP secret"
```

Demo users:
- **alice** / alice123 (Balance: $1,000)
- **bob** / bob123 (Balance: $500)

### 3. Setup 2FA
- Use Google Authenticator or similar app
- Scan QR code or enter TOTP secret manually

### 4. Login via Android App
- Open the app
- Enter username and password
- Enter 6-digit TOTP code
- Start banking!

### 5. Try API Directly
```bash
# Login
curl -X POST http://localhost:8000/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "alice123"}'

# 2FA (use TOTP code from authenticator)
curl -X POST http://localhost:8000/2fa \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <temp_token>" \
  -d '{"token": "123456"}'

# Check balance
curl -X GET http://localhost:8000/balance \
  -H "Authorization: Bearer <access_token>"
```

---

## 🐛 Race Condition Demonstrations

### Example 1: Double Spending Attack

```bash
# Alice has $100, attempts to transfer $80 twice simultaneously
# Both checks pass ($100 >= $80) ✓
# Both transfers execute
# Result: Alice has -$60 (overdraft!)

# Terminal 1
curl -X POST http://localhost:8000/transfer \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"to_username": "bob", "amount": 80}' &

# Terminal 2 (run immediately)
curl -X POST http://localhost:8000/transfer \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"to_username": "bob", "amount": 80}' &
```

### Example 2: Lost Updates

```python
# Using Python to trigger concurrent deposits
import concurrent.futures
import requests

def deposit(amount):
    return requests.post(
        "http://localhost:8000/deposit",
        headers={"Authorization": f"Bearer {TOKEN}"},
        json={"amount": amount}
    )

# Start balance: $100
# Execute 10 concurrent deposits of $10 each
# Expected: $200
# Actual: ~$110-$150 (lost updates!)

with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
    futures = [executor.submit(deposit, 10) for _ in range(10)]
    results = [f.result() for f in futures]
```

### Example 3: Investment Fund Inconsistency

```bash
# Multiple users subscribing to fund simultaneously
# Can result in incorrect share price calculations

for i in {1..5}; do
  curl -X POST http://localhost:8000/subscribe \
    -H "Authorization: Bearer $TOKEN_USER_$i" \
    -d '{"amount": 1000}' &
done
```

See `docs/RACE_CONDITIONS.md` for detailed explanations and more examples.

---

## 🛠️ Technology Stack

### Backend (API)
- **Framework**: FastAPI 0.115.0
- **Server**: Uvicorn (ASGI)
- **Database**: MariaDB 11
- **ORM**: SQLAlchemy 2.0
- **Auth**: PyJWT + PyOTP (TOTP)
- **Validation**: Pydantic v2

### Frontend (Android)
- **Language**: Kotlin 1.9.0
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Architecture**: MVVM + Repository Pattern
- **Networking**: Retrofit 2 + OkHttp 3
- **UI**: Material Design 3
- **Security**: EncryptedSharedPreferences

---

## 📚 Documentation

- [API Documentation](api/README.md) - Complete API reference
- [Android App Documentation](app/README.md) - App features and setup
- [Race Conditions Guide](docs/RACE_CONDITIONS.md) - Detailed vulnerability examples
- [Security Considerations](docs/SECURITY.md) - What NOT to do in production

---

## 🔒 Security Notes

### ⚠️ Intentional Vulnerabilities

This project demonstrates the following security issues:

1. **No database transaction isolation** - All operations are vulnerable
2. **No pessimistic/optimistic locking** - Concurrent modifications not prevented
3. **No idempotency checks** - Operations can be duplicated
4. **No rate limiting** - Enables automated attacks
5. **Weak password hashing** - SHA-256 instead of bcrypt/argon2
6. **TOTP secrets exposed** - Printed in logs for demo purposes

### ✅ For Production Use

To make this production-ready, you must:

1. **Implement proper database transactions**
   ```python
   with session.begin():
       account = session.query(Account).with_for_update().filter_by(id=user_id).one()
       account.balance -= amount
       session.commit()
   ```

2. **Use optimistic locking with version numbers**
   ```python
   class Account(Base):
       version = Column(Integer, default=0)
   ```

3. **Implement idempotency keys**
   ```python
   @app.post("/transfer")
   async def transfer(
       request: TransferRequest,
       idempotency_key: str = Header(...)
   ):
       # Check if operation already executed
   ```

4. **Add rate limiting**
   ```python
   from slowapi import Limiter
   limiter = Limiter(key_func=get_remote_address)
   
   @app.post("/transfer")
   @limiter.limit("10/minute")
   async def transfer(...):
   ```

5. **Use strong password hashing**
   ```python
   from passlib.context import CryptContext
   pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
   ```

6. **Never expose secrets in responses or logs**

7. **Always use HTTPS in production**

8. **Implement proper error handling without leaking information**

---

## 🧪 Testing

### Unit Tests (Demonstrating Race Conditions)

```bash
cd scripts
python test_race_conditions.py
```

### Load Testing

```bash
# Install Apache Bench
sudo apt-get install apache2-utils

# Test concurrent deposits
ab -n 1000 -c 50 -m POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -p deposit.json \
  http://localhost:8000/deposit
```

### Android UI Tests

```bash
cd app
./gradlew connectedAndroidTest
```

---

## 🤝 Contributing

This is an educational project. Contributions are welcome for:

1. **Additional race condition examples**
2. **Better documentation**
3. **More test scenarios**
4. **UI/UX improvements in the Android app**

**Important**: Keep the vulnerabilities intact - that's the educational purpose!

### How to Contribute

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-example`)
3. Commit your changes (`git commit -m 'Add race condition example'`)
4. Push to the branch (`git push origin feature/amazing-example`)
5. Open a Pull Request

---

## 📖 Learning Resources

### Race Conditions & Concurrency
- [OWASP: Race Conditions](https://owasp.org/www-community/vulnerabilities/Race_condition)
- [Database Isolation Levels](https://www.postgresql.org/docs/current/transaction-iso.html)
- [Distributed Systems Consistency](https://jepsen.io/consistency)

### FastAPI & SQLAlchemy
- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [SQLAlchemy Transactions](https://docs.sqlalchemy.org/en/20/core/connections.html#using-transactions)

### Android Development
- [Android Developer Guide](https://developer.android.com/guide)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

---

## 👥 Authors

- **Maycon Vitali** - *Initial work* - [GitHub](https://github.com/maycon)

---

## 🙏 Acknowledgments

- Created as demonstration for [TREM](https://github.com/otavioarj/TREM)
- Inspired by real-world banking vulnerabilities
- Thanks to the open-source community

---

## 📄 License

This project is for **educational purposes only**. 

**DO NOT USE IN PRODUCTION** - The code intentionally contains security vulnerabilities to demonstrate race conditions in concurrent systems.

---

## ⚡ Support

For questions about race conditions or concurrency issues:
- Open an issue on GitHub
- Check the documentation in `docs/`
- Review the code comments explaining each vulnerability

---

**🎮 Hack N Roll Racing Bank - Banking with intentional bugs for educational purposes!** 💰🐛