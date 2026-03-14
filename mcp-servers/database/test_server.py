# mcp-servers/database/test_server.py
import pytest
import sys
import tempfile
from pathlib import Path

# Add parent directory to path for mcp_protocol import
sys.path.insert(0, str(Path(__file__).parent.parent))

from server import DatabaseServer


@pytest.fixture
def temp_db_server():
    with tempfile.TemporaryDirectory() as tmpdir:
        db_path = Path(tmpdir) / "test.db"
        server = DatabaseServer(connection_string=f"sqlite:///{db_path}")
        yield server


@pytest.mark.asyncio
async def test_initialize_registers_tools(temp_db_server):
    await temp_db_server.initialize()
    tools = temp_db_server.list_tools()
    assert len(tools) == 2
    tool_names = [t.name for t in tools]
    assert "query" in tool_names
    assert "execute" in tool_names


@pytest.mark.asyncio
async def test_create_table(temp_db_server):
    await temp_db_server.initialize()

    result = await temp_db_server.call_tool("execute", {
        "sql": "CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT, email TEXT)"
    })
    assert result.success


@pytest.mark.asyncio
async def test_insert_and_query(temp_db_server):
    await temp_db_server.initialize()

    # Create table
    await temp_db_server.call_tool("execute", {
        "sql": "CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT, email TEXT)"
    })

    # Insert data
    result = await temp_db_server.call_tool("execute", {
        "sql": "INSERT INTO users (name, email) VALUES (:name, :email)",
        "params": {"name": "John Doe", "email": "john@example.com"}
    })
    assert result.success

    # Query data
    result = await temp_db_server.call_tool("query", {
        "sql": "SELECT * FROM users WHERE name = :name",
        "params": {"name": "John Doe"}
    })
    assert result.success
    assert len(result.output) == 1
    assert result.output[0]["name"] == "John Doe"
    assert result.output[0]["email"] == "john@example.com"


@pytest.mark.asyncio
async def test_update_query(temp_db_server):
    await temp_db_server.initialize()

    # Create table and insert
    await temp_db_server.call_tool("execute", {
        "sql": "CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT, email TEXT)"
    })
    await temp_db_server.call_tool("execute", {
        "sql": "INSERT INTO users (name, email) VALUES (:name, :email)",
        "params": {"name": "John Doe", "email": "john@example.com"}
    })

    # Update
    result = await temp_db_server.call_tool("execute", {
        "sql": "UPDATE users SET email = :email WHERE name = :name",
        "params": {"email": "new@example.com", "name": "John Doe"}
    })
    assert result.success

    # Verify update
    result = await temp_db_server.call_tool("query", {
        "sql": "SELECT email FROM users WHERE name = :name",
        "params": {"name": "John Doe"}
    })
    assert result.output[0]["email"] == "new@example.com"


@pytest.mark.asyncio
async def test_delete_query(temp_db_server):
    await temp_db_server.initialize()

    # Create table and insert
    await temp_db_server.call_tool("execute", {
        "sql": "CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT)"
    })
    await temp_db_server.call_tool("execute", {
        "sql": "INSERT INTO users (name) VALUES (:name)",
        "params": {"name": "ToDelete"}
    })

    # Delete
    result = await temp_db_server.call_tool("execute", {
        "sql": "DELETE FROM users WHERE name = :name",
        "params": {"name": "ToDelete"}
    })
    assert result.success

    # Verify deletion
    result = await temp_db_server.call_tool("query", {
        "sql": "SELECT * FROM users WHERE name = :name",
        "params": {"name": "ToDelete"}
    })
    assert len(result.output) == 0


@pytest.mark.asyncio
async def test_resource_registration(temp_db_server):
    await temp_db_server.initialize()
    resources = temp_db_server.list_resources()
    assert len(resources) == 1
    assert resources[0].name == "Database Server"
