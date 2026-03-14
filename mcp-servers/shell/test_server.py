# mcp-servers/shell/test_server.py
import pytest
import sys
from pathlib import Path

# Add parent directory to path for mcp_protocol import
sys.path.insert(0, str(Path(__file__).parent.parent))

from server import ShellServer


@pytest.fixture
def shell_server():
    server = ShellServer(allowed_commands=["echo", "pwd", "ls", "dir"])
    return server


@pytest.mark.asyncio
async def test_initialize_registers_tools(shell_server):
    await shell_server.initialize()
    tools = shell_server.list_tools()
    assert len(tools) == 2
    tool_names = [t.name for t in tools]
    assert "execute" in tool_names
    assert "execute_safe" in tool_names


@pytest.mark.asyncio
async def test_execute_command(shell_server):
    await shell_server.initialize()

    result = await shell_server.call_tool("execute", {
        "command": "echo Hello World",
        "timeout": 10
    })
    assert result.success
    assert "Hello World" in result.output["stdout"]
    assert result.output["returncode"] == 0


@pytest.mark.asyncio
async def test_execute_safe_allowed_command(shell_server):
    await shell_server.initialize()

    result = await shell_server.call_tool("execute_safe", {
        "command_name": "echo",
        "args": ["Hello", "Safe"]
    })
    assert result.success
    assert "Hello Safe" in result.output["stdout"]


@pytest.mark.asyncio
async def test_execute_safe_blocked_command(shell_server):
    await shell_server.initialize()

    result = await shell_server.call_tool("execute_safe", {
        "command_name": "rm",
        "args": ["-rf", "/"]
    })
    assert not result.success
    assert "not in allowed list" in result.error


@pytest.mark.asyncio
async def test_execute_timeout(shell_server):
    await shell_server.initialize()

    result = await shell_server.call_tool("execute", {
        "command": "sleep 5",
        "timeout": 1
    })
    assert not result.success
    assert "timed out" in result.error.lower()


@pytest.mark.asyncio
async def test_resource_registration(shell_server):
    await shell_server.initialize()
    resources = shell_server.list_resources()
    assert len(resources) == 1
    assert resources[0].name == "Shell Server"
