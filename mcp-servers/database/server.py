# mcp-servers/database/server.py
import os
from typing import Any, Dict, Optional
from contextlib import contextmanager

import sqlalchemy as sa
from mcp_protocol.base import MCPServer, Tool, ToolResult, Resource


class DatabaseServer(MCPServer):
    def __init__(self, connection_string: Optional[str] = None):
        super().__init__(name="database", version="1.0.0")
        self.connection_string = connection_string or os.getenv("DATABASE_URL", "sqlite:///./data.db")
        self.engine = None

    async def initialize(self) -> None:
        """Initialize database connection and register tools"""
        self.engine = sa.create_engine(self.connection_string)

        self.register_tool(
            Tool(
                name="query",
                description="Execute a SQL query",
                parameters={
                    "sql": {"type": "string", "description": "SQL query"},
                    "params": {"type": "object", "description": "Query parameters"}
                }
            ),
            self.query
        )

        self.register_tool(
            Tool(
                name="execute",
                description="Execute a SQL statement (INSERT, UPDATE, DELETE, etc.)",
                parameters={
                    "sql": {"type": "string", "description": "SQL statement"},
                    "params": {"type": "object", "description": "Statement parameters"}
                }
            ),
            self.execute
        )

        # Register resources
        self.register_resource(Resource(
            uri="database://",
            name="Database Server",
            description="Database query execution server"
        ))

    @contextmanager
    def get_connection(self):
        conn = self.engine.connect()
        try:
            yield conn
        finally:
            conn.close()

    async def call_tool(self, name: str, args: Dict[str, Any]) -> ToolResult:
        """Dispatch tool calls"""
        handlers = {
            "query": self.query,
            "execute": self.execute,
        }
        handler = handlers.get(name)
        if not handler:
            return ToolResult(success=False, output=None, error=f"Unknown tool: {name}")
        try:
            result = await handler(**args)
            return ToolResult(success=True, output=result)
        except Exception as e:
            return ToolResult(success=False, output=None, error=str(e))

    async def read_resource(self, uri: str) -> Any:
        raise NotImplementedError("Resource reading not implemented")

    async def query(self, sql: str, params: Optional[Dict] = None) -> list:
        """Execute a SQL query and return results"""
        with self.get_connection() as conn:
            result = conn.execute(sa.text(sql), params or {})
            if result.returns_rows:
                return [dict(row._mapping) for row in result]
            return [{"rows_affected": result.rowcount}]

    async def execute(self, sql: str, params: Optional[Dict] = None) -> Dict[str, Any]:
        """Execute a SQL statement and commit"""
        with self.get_connection() as conn:
            result = conn.execute(sa.text(sql), params or {})
            conn.commit()
            return {
                "rows_affected": result.rowcount,
                "success": True
            }
