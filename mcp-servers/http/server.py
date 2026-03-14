# mcp-servers/http/server.py
from typing import Any, Dict
import httpx
from mcp_protocol.base import MCPServer, Tool, ToolResult, Resource


class HttpServer(MCPServer):
    def __init__(self):
        super().__init__(name="http", version="1.0.0")
        self.client = httpx.AsyncClient()

    async def initialize(self) -> None:
        """Register tools"""
        self.register_tool(
            Tool(
                name="request",
                description="Make an HTTP request",
                parameters={
                    "url": {"type": "string", "description": "URL to request"},
                    "method": {"type": "string", "description": "HTTP method"},
                    "headers": {"type": "object", "description": "Request headers"},
                    "body": {"type": "string", "description": "Request body"}
                }
            ),
            self.request
        )

        self.register_tool(
            Tool(
                name="get",
                description="Make a GET request",
                parameters={
                    "url": {"type": "string", "description": "URL to request"},
                    "headers": {"type": "object", "description": "Request headers"}
                }
            ),
            self.get
        )

        self.register_tool(
            Tool(
                name="post",
                description="Make a POST request",
                parameters={
                    "url": {"type": "string", "description": "URL to request"},
                    "body": {"type": "string", "description": "Request body"},
                    "headers": {"type": "object", "description": "Request headers"}
                }
            ),
            self.post
        )

        # Register resources
        self.register_resource(Resource(
            uri="http://",
            name="HTTP Server",
            description="HTTP request execution server"
        ))

    async def call_tool(self, name: str, args: Dict[str, Any]) -> ToolResult:
        """Dispatch tool calls"""
        handlers = {
            "request": self.request,
            "get": self.get,
            "post": self.post,
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

    async def request(
        self,
        url: str,
        method: str = "GET",
        headers: Dict = None,
        body: str = None
    ) -> Dict[str, Any]:
        """Make an HTTP request"""
        response = await self.client.request(
            method,
            url,
            headers=headers,
            content=body
        )
        return {
            "status": response.status_code,
            "headers": dict(response.headers),
            "body": response.text
        }

    async def get(self, url: str, headers: Dict = None) -> Dict[str, Any]:
        """Make a GET request"""
        return await self.request(url, method="GET", headers=headers)

    async def post(self, url: str, body: str = None, headers: Dict = None) -> Dict[str, Any]:
        """Make a POST request"""
        return await self.request(url, method="POST", headers=headers, body=body)

    async def __aenter__(self):
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        await self.client.aclose()
