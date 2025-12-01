"""
Routes package initialization.
Exports all route modules for easy importing.
"""

from . import auth_routes, account_routes, transfer_routes, fund_routes

__all__ = [
    "auth_routes",
    "account_routes",
    "transfer_routes",
    "fund_routes"
]
