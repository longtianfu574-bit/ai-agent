# mcp-servers/mcp_protocol/base.py
from abc import ABC, abstractmethod
from typing import Any, Dict, List, Optional
from pydantic import BaseModel


class ToolResult(BaseModel):
    success: bool
    output: Any
    error: Optional[str] = None


class Tool(BaseModel):
    name: str
    description: str
    parameters: Dict[str, Any]

    class Config:
        arbitrary_types_allowed = True


class Resource(BaseModel):
    uri: str
    name: str
    description: str
    mime_type: str = "text/plain"


class MCPServer(ABC):
    def __init__(self, name: str, version: str = "1.0.0"):
        self.name = name
        self.version = version
        self._tools: Dict[str, Tool] = {}
        self._resources: Dict[str, Resource] = {}

    @abstractmethod
    async def initialize(self) -> None:
        """Initialize the server"""
        pass

    @abstractmethod
    async def call_tool(self, name: str, args: Dict[str, Any]) -> ToolResult:
        """Call a tool by name with arguments"""
        pass

    @abstractmethod
    async def read_resource(self, uri: str) -> Any:
        """Read a resource by URI"""
        pass

    def register_tool(self, tool: Tool, handler) -> None:
        """Register a tool and its handler"""
        self._tools[tool.name] = tool

    def register_resource(self, resource: Resource) -> None:
        """Register a resource"""
        self._resources[resource.uri] = resource

    def list_tools(self) -> List[Tool]:
        """List all available tools"""
        return list(self._tools.values())

    def list_resources(self) -> List[Resource]:
        """List all available resources"""
        return list(self._resources.values())
