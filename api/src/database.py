"""
Database models and connection setup for Bankao API.
Defines SQLAlchemy models for users, accounts, funds, and transactions.
"""

from datetime import datetime
from sqlalchemy import create_engine, text, Column, Integer, String, DateTime, ForeignKey, DECIMAL, Enum
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship, Session
from sqlalchemy.exc import OperationalError
import hashlib
import pyotp

from config import SERVER_URL, DB_URL, DB_NAME, INITIAL_FUND_VALUE


# Base class for all models
class Base(DeclarativeBase):
    pass


class User(Base):
    """
    User model - stores user credentials and TOTP secret.
    """
    __tablename__ = "users"
    
    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    username: Mapped[str] = mapped_column(String(64), unique=True, nullable=False, index=True)
    password_hash: Mapped[str] = mapped_column(String(128), nullable=False)
    totp_secret: Mapped[str] = mapped_column(String(32), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, nullable=False)
    
    # Relationships
    account: Mapped["Account"] = relationship("Account", back_populates="user", uselist=False)
    fund_account: Mapped["FundAccount"] = relationship("FundAccount", back_populates="user", uselist=False)
    outgoing_transfers: Mapped[list["Transfer"]] = relationship(
        "Transfer", back_populates="from_user", foreign_keys="Transfer.from_user_id"
    )
    incoming_transfers: Mapped[list["Transfer"]] = relationship(
        "Transfer", back_populates="to_user", foreign_keys="Transfer.to_user_id"
    )
    transactions: Mapped[list["Transaction"]] = relationship("Transaction", back_populates="user")


class Account(Base):
    """
    Account model - stores user's cash balance.
    
    RACE CONDITION WARNING:
    - No row-level locking implemented
    - Concurrent transfers/deposits/withdrawals can lead to incorrect balances
    - Lost updates possible when multiple transactions modify balance simultaneously
    """
    __tablename__ = "accounts"
    
    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False, unique=True)
    balance: Mapped[float] = mapped_column(DECIMAL(18, 2), default=0.00, nullable=False)
    
    # Relationships
    user: Mapped[User] = relationship("User", back_populates="account")


class FundAccount(Base):
    """
    Fund Account model - stores user's investment fund balance.
    
    RACE CONDITION WARNING:
    - No locking on fund share calculations
    - Concurrent subscriptions/redemptions can cause incorrect share allocations
    - Fund value updates not atomic with user balance updates
    """
    __tablename__ = "fund_accounts"
    
    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False, unique=True)
    shares: Mapped[float] = mapped_column(DECIMAL(18, 6), default=0.000000, nullable=False)
    
    # Relationships
    user: Mapped[User] = relationship("User", back_populates="fund_account")


class Fund(Base):
    """
    Fund model - stores global fund information.
    Single row table that tracks total fund value and shares.
    
    RACE CONDITION WARNING:
    - Total value and shares updated without transaction isolation
    - Multiple simultaneous subscriptions can corrupt fund state
    - No serialization of fund operations
    """
    __tablename__ = "fund"
    
    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    total_value: Mapped[float] = mapped_column(DECIMAL(18, 2), nullable=False)
    total_shares: Mapped[float] = mapped_column(DECIMAL(18, 6), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


class Transfer(Base):
    """
    Transfer model - records account-to-account transfers.
    
    RACE CONDITION WARNING:
    - Balance checks and updates are not atomic
    - Double-spending possible with concurrent transfers
    - No pessimistic locking on sender/receiver accounts
    """
    __tablename__ = "transfers"
    
    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    from_user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False, index=True)
    to_user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False, index=True)
    amount: Mapped[float] = mapped_column(DECIMAL(18, 2), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, nullable=False)
    
    # Relationships
    from_user: Mapped[User] = relationship("User", foreign_keys=[from_user_id])
    to_user: Mapped[User] = relationship("User", foreign_keys=[to_user_id])


class Transaction(Base):
    """
    Transaction model - records all account operations (deposits, withdrawals, subscriptions, redemptions).
    
    RACE CONDITION WARNING:
    - Transaction records created after balance updates
    - If process crashes between balance update and transaction log, inconsistency occurs
    - No two-phase commit or compensating transactions
    """
    __tablename__ = "transactions"
    
    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False, index=True)
    type: Mapped[str] = mapped_column(
        Enum("deposit", "withdrawal", "subscription", "redemption", name="transaction_type"),
        nullable=False
    )
    amount: Mapped[float] = mapped_column(DECIMAL(18, 2), nullable=False)
    description: Mapped[str] = mapped_column(String(255), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, nullable=False)
    
    # Relationships
    user: Mapped[User] = relationship("User", back_populates="transactions")


# Database engine instances
server_engine = None
db_engine = None


def make_engine(url: str):
    """Create a SQLAlchemy engine with connection pool settings."""
    return create_engine(url, pool_pre_ping=True, future=True)


def ensure_database():
    """
    Initialize database and seed initial data if needed.
    Creates database, tables, and demo users.
    
    RACE CONDITION WARNING:
    - No distributed locking for database initialization
    - Multiple containers starting simultaneously may try to seed data concurrently
    - Can result in duplicate demo users or constraint violations
    """
    global db_engine, server_engine
    
    # Create server engine to connect without database selection
    server_engine = make_engine(SERVER_URL)
    
    try:
        # Create database if it doesn't exist
        with server_engine.connect() as conn:
            conn.execute(text(
                f"CREATE DATABASE IF NOT EXISTS `{DB_NAME}` "
                f"CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
            ))
            conn.commit()
    except OperationalError as e:
        raise RuntimeError(f"Cannot reach MariaDB at {SERVER_URL}. Error: {e}") from e
    
    # Create engine for specific database
    db_engine = make_engine(DB_URL)
    
    # Create all tables
    Base.metadata.create_all(db_engine)
    
    # Seed demo data if database is empty
    with Session(db_engine) as session:
        user_count = session.execute(text("SELECT COUNT(*) FROM users")).scalar_one()
        
        if user_count == 0:
            print("=== Initializing database with demo data ===")
            
    #         # Create demo users: alice and bob
    #         alice_secret = pyotp.random_base32()
    #         bob_secret = pyotp.random_base32()
            
    #         def hash_password(pw: str) -> str:
    #             return hashlib.sha256(pw.encode("utf-8")).hexdigest()
            
    #         alice = User(
    #             username="alice",
    #             password_hash=hash_password("alice123"),
    #             totp_secret=alice_secret
    #         )
    #         bob = User(
    #             username="bob",
    #             password_hash=hash_password("bob123"),
    #             totp_secret=bob_secret
    #         )
            
    #         session.add_all([alice, bob])
    #         session.flush()  # Get user IDs
            
    #         # Create accounts with initial balances
    #         alice_account = Account(user_id=alice.id, balance=1000.00)
    #         bob_account = Account(user_id=bob.id, balance=500.00)
            
    #         # Create fund accounts (initially empty)
    #         alice_fund = FundAccount(user_id=alice.id, shares=0.000000)
    #         bob_fund = FundAccount(user_id=bob.id, shares=0.000000)
            
    #         session.add_all([alice_account, bob_account, alice_fund, bob_fund])
            
            # Initialize the investment fund
            fund = Fund(
                total_value=INITIAL_FUND_VALUE,
                total_shares=INITIAL_FUND_VALUE  # Initial share price = $1.00
            )
            session.add(fund)
            
            session.commit()
            
    #         print(f"Demo user 'alice' created - TOTP secret: {alice_secret}")
    #         print(f"Demo user 'bob' created   - TOTP secret: {bob_secret}")
    #         print(f"Investment fund initialized with ${INITIAL_FUND_VALUE:,.2f}")
        else:
            # Print existing users' TOTP secrets for convenience
            print("=== Existing users ===")
            results = session.execute(text("SELECT username, totp_secret FROM users")).all()
            for row in results:
                print(f"{row.username}: {row.totp_secret}")


def get_session() -> Session:
    """
    Get a database session.
    Ensures database is initialized on first call.
    """
    if db_engine is None:
        ensure_database()
    return Session(db_engine)
