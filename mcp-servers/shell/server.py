# mcp-servers/shell/server.py
import asyncio
import shlex
from typing import Any, Dict, List

from mcp_protocol.base import MCPServer, Tool, ToolResult, Resource


class ShellServer(MCPServer):
    def __init__(self, allowed_commands: List[str] = None):
        super().__init__(name="shell", version="1.0.0")
        self.allowed_commands = allowed_commands or []

    async def initialize(self) -> None:
        """Register tools"""
        self.register_tool(
            Tool(
                name="execute",
                description="Execute a shell command",
                parameters={
                    "command": {"type": "string", "description": "Command to execute"},
                    "timeout": {"type": "integer", "description": "Timeout in seconds"}
                }
            ),
            self.execute
        )

        self.register_tool(
            Tool(
                name="execute_safe",
                description="Execute command from allowed list only",
                parameters={
                    "command_name": {"type": "string", "description": "Allowed command name"},
                    "args": {"type": "array", "description": "Command arguments"}
                }
            ),
            self.execute_safe
        )

        # Register resources
        self.register_resource(Resource(
            uri="shell://",
            name="Shell Server",
            description="Shell command execution server"
        ))

    async def call_tool(self, name: str, args: Dict[str, Any]) -> ToolResult:
        """Dispatch tool calls"""
        handlers = {
            "execute": self.execute,
            "execute_safe": self.execute_safe,
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

    async def execute(self, command: str, timeout: int = 30) -> Dict[str, Any]:
        """Execute a shell command"""
        try:
            proc = await asyncio.create_subprocess_shell(
                command,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE
            )
            stdout, stderr = await asyncio.wait_for(
                proc.communicate(),
                timeout=timeout
            )
            return {
                "stdout": stdout.decode(),
                "stderr": stderr.decode(),
                "returncode": proc.returncode
            }
        except asyncio.TimeoutError:
            raise TimeoutError(f"Command timed out after {timeout} seconds")

    async def execute_safe(self, command_name: str, args: List[str]) -> Dict[str, Any]:
        """Execute from allowed commands only"""
        if command_name not in self.allowed_commands:
            raise ValueError(f"Command '{command_name}' is not in allowed list")

        full_command = shlex.join([command_name] + args)
        return await self.execute(full_command)
