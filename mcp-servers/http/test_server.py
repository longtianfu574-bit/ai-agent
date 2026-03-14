# mcp-servers/http/test_server.py
import pytest
import sys
from pathlib import Path

# Add parent directory to path for mcp_protocol import
sys.path.insert(0, str(Path(__file__).parent.parent))

from server import HttpServer


@pytest.fixture
def http_server():
    server = HttpServer()
    return server


@pytest.mark.asyncio
async def test_initialize_registers_tools(http_server):
    await http_server.initialize()
    tools = http_server.list_tools()
    assert len(tools) == 3
    tool_names = [t.name for t in tools]
    assert "request" in tool_names
    assert "get" in tool_names
    assert "post" in tool_names


@pytest.mark.asyncio
async def test_get_request(http_server):
    await http_server.initialize()

    result = await http_server.call_tool("get", {
        "url": "https://httpbin.org/get"
    })
    assert result.success
    assert result.output["status"] == 200


@pytest.mark.asyncio
async def test_post_request(http_server):
    await http_server.initialize()

    result = await http_server.call_tool("post", {
        "url": "https://httpbin.org/post",
        "body": '{"test": "data"}',
        "headers": {"Content-Type": "application/json"}
    })
    assert result.success
    assert result.output["status"] == 200


@pytest.mark.asyncio
async def test_request_with_method(http_server):
    await http_server.initialize()

    result = await http_server.call_tool("request", {
        "url": "https://httpbin.org/delete",
        "method": "DELETE"
    })
    assert result.success
    assert result.output["status"] == 200


@pytest.mark.asyncio
async def test_resource_registration(http_server):
    await http_server.initialize()
    resources = http_server.list_resources()
    assert len(resources) == 1
    assert resources[0].name == "HTTP Server"
