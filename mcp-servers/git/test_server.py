# mcp-servers/git/test_server.py
import pytest
import sys
import tempfile
from pathlib import Path

# Add parent directory to path for mcp_protocol import
sys.path.insert(0, str(Path(__file__).parent.parent))

from server import GitServer


@pytest.fixture
def temp_git_server():
    with tempfile.TemporaryDirectory() as tmpdir:
        server = GitServer(root_path=tmpdir)
        yield server


@pytest.mark.asyncio
async def test_initialize_registers_tools(temp_git_server):
    await temp_git_server.initialize()
    tools = temp_git_server.list_tools()
    assert len(tools) == 5
    tool_names = [t.name for t in tools]
    assert "clone" in tool_names
    assert "status" in tool_names
    assert "commit" in tool_names
    assert "init" in tool_names
    assert "log" in tool_names


@pytest.mark.asyncio
async def test_init_repository(temp_git_server):
    await temp_git_server.initialize()

    result = await temp_git_server.call_tool("init", {
        "path": "test-repo"
    })
    assert result.success
    assert "Initialized" in result.output["message"]


@pytest.mark.asyncio
async def test_status_empty_repository(temp_git_server):
    await temp_git_server.initialize()

    # First init a repo
    await temp_git_server.call_tool("init", {"path": "test-repo"})

    result = await temp_git_server.call_tool("status", {"path": "test-repo"})
    assert result.success
    # Fresh repo has no commits yet, branch will be HEAD or initial branch name
    assert result.output["branch"] in ["HEAD", "master", "main"]
    assert len(result.output["untracked"]) == 0


@pytest.mark.asyncio
async def test_commit(temp_git_server):
    await temp_git_server.initialize()

    # Init and create a file
    await temp_git_server.call_tool("init", {"path": "test-repo"})
    repo_path = Path(temp_git_server.root_path) / "test-repo"
    (repo_path / "test.txt").write_text("Hello World")

    # Add and commit
    repo = GitServer(root_path=temp_git_server.root_path)._get_repo("test-repo")
    repo.index.add(["test.txt"])

    result = await temp_git_server.call_tool("commit", {
        "path": "test-repo",
        "message": "Initial commit"
    })
    assert result.success
    assert "commit_hash" in result.output


@pytest.mark.asyncio
async def test_log(temp_git_server):
    await temp_git_server.initialize()

    # Init, create file, and commit
    await temp_git_server.call_tool("init", {"path": "test-repo"})
    repo_path = Path(temp_git_server.root_path) / "test-repo"
    (repo_path / "test.txt").write_text("Hello World")

    repo = GitServer(root_path=temp_git_server.root_path)._get_repo("test-repo")
    repo.index.add(["test.txt"])
    repo.index.commit("Initial commit")

    result = await temp_git_server.call_tool("log", {
        "path": "test-repo",
        "count": 5
    })
    assert result.success
    assert len(result.output) >= 1
    assert "hash" in result.output[0]
    assert "message" in result.output[0]


@pytest.mark.asyncio
async def test_safe_path_prevents_traversal(temp_git_server):
    with pytest.raises(ValueError, match="outside root directory"):
        temp_git_server._safe_path("../etc/passwd")


@pytest.mark.asyncio
async def test_resource_registration(temp_git_server):
    await temp_git_server.initialize()
    resources = temp_git_server.list_resources()
    assert len(resources) == 1
    assert resources[0].name == "Git Server"
