# mcp-servers/filesystem/server.py
import os
import shutil
from pathlib import Path
from typing import Any, Dict, List

from mcp_protocol.base import MCPServer, Tool, ToolResult, Resource


class FilesystemServer(MCPServer):
    def __init__(self, root_path: str = "/data"):
        super().__init__(name="filesystem", version="1.0.0")
        self.root_path = Path(root_path)
        self._ensure_root_exists()

    def _ensure_root_exists(self):
        """Ensure root path exists"""
        self.root_path.mkdir(parents=True, exist_ok=True)

    def _safe_path(self, path: str) -> Path:
        """Ensure path is within root directory"""
        full_path = (self.root_path / path).resolve()
        try:
            full_path.relative_to(self.root_path.resolve())
            return full_path
        except ValueError:
            raise ValueError(f"Path {path} is outside root directory")

    async def initialize(self) -> None:
        """Register tools and resources"""
        # Register tools
        self.register_tool(
            Tool(
                name="read_file",
                description="Read contents of a file",
                parameters={
                    "path": {"type": "string", "description": "File path relative to root"}
                }
            ),
            self.read_file
        )

        self.register_tool(
            Tool(
                name="write_file",
                description="Write contents to a file",
                parameters={
                    "path": {"type": "string", "description": "File path"},
                    "content": {"type": "string", "description": "File content"}
                }
            ),
            self.write_file
        )

        self.register_tool(
            Tool(
                name="list_directory",
                description="List contents of a directory",
                parameters={
                    "path": {"type": "string", "description": "Directory path"}
                }
            ),
            self.list_directory
        )

        self.register_tool(
            Tool(
                name="delete_file",
                description="Delete a file",
                parameters={
                    "path": {"type": "string", "description": "File path"}
                }
            ),
            self.delete_file
        )

        # Register resources
        self.register_resource(Resource(
            uri="filesystem://",
            name="Filesystem Root",
            description="Root of the filesystem"
        ))

    async def call_tool(self, name: str, args: Dict[str, Any]) -> ToolResult:
        """Dispatch tool calls"""
        handlers = {
            "read_file": self.read_file,
            "write_file": self.write_file,
            "list_directory": self.list_directory,
            "delete_file": self.delete_file,
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
        """Read a resource"""
        raise NotImplementedError("Resource reading not implemented")

    # Tool implementations
    async def read_file(self, path: str) -> str:
        """Read file contents"""
        safe_path = self._safe_path(path)
        if not safe_path.exists():
            raise FileNotFoundError(f"File not found: {path}")
        return safe_path.read_text()

    async def write_file(self, path: str, content: str) -> str:
        """Write file contents"""
        safe_path = self._safe_path(path)
        safe_path.parent.mkdir(parents=True, exist_ok=True)
        safe_path.write_text(content)
        return f"Successfully wrote to {path}"

    async def list_directory(self, path: str) -> List[Dict[str, Any]]:
        """List directory contents"""
        safe_path = self._safe_path(path)
        if not safe_path.exists():
            raise FileNotFoundError(f"Directory not found: {path}")
        if not safe_path.is_dir():
            raise NotADirectoryError(f"Not a directory: {path}")

        result = []
        for item in safe_path.iterdir():
            result.append({
                "name": item.name,
                "type": "directory" if item.is_dir() else "file",
                "size": item.stat().st_size if item.is_file() else None
            })
        return result

    async def delete_file(self, path: str) -> str:
        """Delete a file"""
        safe_path = self._safe_path(path)
        if not safe_path.exists():
            raise FileNotFoundError(f"File not found: {path}")
        if safe_path.is_dir():
            raise IsADirectoryError(f"Cannot delete directory: {path}")
        safe_path.unlink()
        return f"Successfully deleted {path}"
