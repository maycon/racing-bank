"""
Configuration module for Bankao API.
Loads environment variables and defines application constants.
"""

import os

# JWT Configuration
SECRET_KEY = os.getenv("SECRET_KEY", "change-this-in-production")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES", "120"))

# Database Configuration
DB_HOST = os.getenv("DB_HOST", "db")
DB_PORT = int(os.getenv("DB_PORT", "3306"))
DB_USER = os.getenv("DB_USERNAME", "root")
DB_PASSWORD = os.getenv("DB_ROOT_PASSWORD", "rootpassword")
DB_NAME = os.getenv("DB_DATABASE", "bankao")

# Build database URLs
# Use MySQL driver for MariaDB (compatible)
SERVER_URL = f"mysql+pymysql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}"
DB_URL = f"{SERVER_URL}/{DB_NAME}"

# Fund Configuration
INITIAL_FUND_VALUE = 100000.00  # Initial total fund value in USD
