# mcp-servers/mcp_protocol/auth.py
import os
import secrets
from functools import wraps
from typing import Optional
from fastapi import HTTPException, Security, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials

security = HTTPBearer()

# In production, use a proper token store
API_TOKENS: set = set()


def init_auth() -> None:
    """Initialize authentication with tokens from environment"""
    tokens = os.getenv("MCP_API_TOKENS", "").split(",")
    API_TOKENS.update(filter(None, tokens))

    # Generate a default token if none provided
    if not API_TOKENS:
        default_token = secrets.token_urlsafe(32)
        print(f"Generated default API token: {default_token}")
        API_TOKENS.add(default_token)


def validate_token(token: str) -> bool:
    """Validate an API token"""
    return token in API_TOKENS


def require_auth(func):
    """Decorator to require authentication on endpoints"""
    @wraps(func)
    async def wrapper(
        credentials: HTTPAuthorizationCredentials = Security(security)
    ):
        if not validate_token(credentials.credentials):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid or missing API token"
            )
        return await func(credentials.credentials)
    return wrapper
