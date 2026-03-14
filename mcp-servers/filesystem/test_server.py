# mcp-servers/filesystem/test_server.py
import pytest
import sys
import tempfile
from pathlib import Path

# Add parent directory to path for mcp_protocol import
sys.path.insert(0, str(Path(__file__).parent.parent))

from server import FilesystemServer


@pytest.fixture
def temp_server():
    with tempfile.TemporaryDirectory() as tmpdir:
        server = FilesystemServer(root_path=tmpdir)
        yield server


@pytest.mark.asyncio
async def test_initialize_registers_tools(temp_server):
    await temp_server.initialize()
    tools = temp_server.list_tools()
    assert len(tools) == 4
    tool_names = [t.name for t in tools]
    assert "read_file" in tool_names
    assert "write_file" in tool_names
    assert "list_directory" in tool_names
    assert "delete_file" in tool_names


@pytest.mark.asyncio
async def test_write_and_read_file(temp_server):
    await temp_server.initialize()

    # Write a file
    result = await temp_server.call_tool("write_file", {
        "path": "test.txt",
        "content": "Hello, World!"
    })
    assert result.success

    # Read it back
    result = await temp_server.call_tool("read_file", {"path": "test.txt"})
    assert result.success
    assert result.output == "Hello, World!"


@pytest.mark.asyncio
async def test_list_directory(temp_server):
    await temp_server.initialize()

    # Create some files
    await temp_server.call_tool("write_file", {"path": "a.txt", "content": "a"})
    await temp_server.call_tool("write_file", {"path": "b.txt", "content": "b"})

    result = await temp_server.call_tool("list_directory", {"path": "."})
    assert result.success
    assert len(result.output) == 2


@pytest.mark.asyncio
async def test_delete_file(temp_server):
    await temp_server.initialize()

    # Write a file
    await temp_server.call_tool("write_file", {"path": "to_delete.txt", "content": "delete me"})

    # Delete it
    result = await temp_server.call_tool("delete_file", {"path": "to_delete.txt"})
    assert result.success

    # Verify it's gone
    result = await temp_server.call_tool("read_file", {"path": "to_delete.txt"})
    assert not result.success


@pytest.mark.asyncio
async def test_safe_path_prevents_traversal(temp_server):
    await temp_server.initialize()

    with pytest.raises(ValueError, match="outside root directory"):
        temp_server._safe_path("../etc/passwd")


@pytest.mark.asyncio
async def test_resource_registration(temp_server):
    await temp_server.initialize()
    resources = temp_server.list_resources()
    assert len(resources) == 1
    assert resources[0].name == "Filesystem Root"
