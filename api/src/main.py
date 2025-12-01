"""
Bankao API - Main Application

A banking API with intentional race condition vulnerabilities for educational purposes.

Features:
- User onboarding with 2FA (TOTP)
- Cash deposits and withdrawals
- Account-to-account transfers
- Investment fund subscriptions and redemptions
- Transaction history

WARNING: This application has NO race condition protections!
All database operations are vulnerable to concurrent access issues.
DO NOT use in production!
"""

import time

from fastapi import FastAPI, Request, Response
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware

from database import ensure_database
from routes import auth_routes, account_routes, transfer_routes, fund_routes


# Create FastAPI application
app = FastAPI(
    title="Bankao API",
    version="3.0.0",
    description=(
        "A banking API with intentional race condition vulnerabilities. "
        "**WARNING: For educational purposes only!**"
    )
)

origins = [
    "http://localhost:8000",                # The origin where ReDoc is being served (if different)
    "https://bankao-api.hacknroll.academy", # Example for a deployed frontend
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],  # Or specify specific methods like ["GET", "POST"]
    allow_headers=["*"],  # Or specify specific headers
)

# Initialize database on startup
@app.on_event("startup")
def startup_event():
    """Initialize database and print TOTP secrets for demo users."""
    ensure_database()
    print("\n" + "="*60)
    print("Bankao API started successfully!")
    print("="*60)
    print("\nIMPORTANT: This API has NO race condition protections!")
    print("See comments in code for vulnerability details.\n")


@app.middleware("http")
async def add_execution_time_header(request: Request, call_next):
    """
    Middleware to add the X-Execution-Time header to every HTTP response.
    Measures the time taken to process each request and includes it in the response headers (in milliseconds).
    """
    start = time.perf_counter()
    response: Response = await call_next(request)
    duration = time.perf_counter() - start

    # Add the header in milliseconds with 4 decimal places
    response.headers["X-Execution-Time"] = f"{duration * 1000:.4f}ms"
    return response

# Include routers
app.include_router(auth_routes.router)
app.include_router(account_routes.router)
app.include_router(transfer_routes.router)
app.include_router(fund_routes.router)


# Root endpoint
@app.get("/")
def root():
    """API information and available endpoints."""
    return {
        "name": "Bankao API",
        "version": "3.0.0",
        "description": "Banking API with user authentication, transfers, and investment funds",
        "warning": "This API has NO race condition protections - for educational purposes only!",
        "endpoints": {
            "authentication": {
                "POST /onboarding": "Register a new user",
                "POST /login": "First-factor authentication (username + password)",
                "POST /2fa": "Second-factor authentication (TOTP token)"
            },
            "account": {
                "POST /deposit": "Deposit cash into account",
                "POST /withdrawal": "Withdraw cash from account",
                "GET /balance": "Get current balances"
            },
            "transfers": {
                "POST /transfer": "Transfer money to another user",
                "GET /statement": "Get transaction history and statement"
            },
            "fund": {
                "POST /subscribe": "Invest in fund (buy shares)",
                "POST /redemption": "Divest from fund (sell shares)",
                "GET /fund/info": "Get fund information"
            },
            "system": {
                "GET /health": "Health check endpoint",
                "GET /docs": "Interactive API documentation (Swagger UI)",
                "GET /redoc": "Alternative API documentation (ReDoc)"
            }
        },
        "demo_users": {
            "alice": {
                "username": "alice",
                "password": "alice123",
                "initial_balance": 1000.00
            },
            "bob": {
                "username": "bob",
                "password": "bob123",
                "initial_balance": 500.00
            }
        },
        "race_conditions": {
            "warning": "This API is INTENTIONALLY vulnerable to race conditions!",
            "vulnerable_operations": [
                "Deposits - concurrent deposits can lose updates",
                "Withdrawals - can result in negative balances",
                "Transfers - double-spending is possible",
                "Fund subscriptions - share price calculations are not atomic",
                "Fund redemptions - fund depletion is possible",
                "Balance queries - can show inconsistent state"
            ],
            "see_code_comments": "Check source code for detailed race condition scenarios"
        }
    }


@app.get("/health")
def health_check():
    """
    Health check endpoint.
    Tests database connectivity.
    """
    from database import get_session
    from sqlalchemy import text
    
    try:
        with get_session() as session:
            # Simple query to test database connection
            session.execute(text("SELECT 1"))
        
        return JSONResponse(
            status_code=200,
            content={
                "status": "healthy",
                "database": "connected"
            }
        )
    except Exception as e:
        return JSONResponse(
            status_code=503,
            content={
                "status": "unhealthy",
                "database": "disconnected",
                "error": str(e)
            }
        )


# For direct execution (development)
if __name__ == "__main__":
    import uvicorn
    
    print("\n" + "="*60)
    print("Starting Bankao API in development mode")
    print("="*60 + "\n")
    
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True
    )
