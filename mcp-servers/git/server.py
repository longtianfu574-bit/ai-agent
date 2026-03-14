# mcp-servers/git/server.py
import os
from pathlib import Path
from typing import Any, Dict, List
from git import Repo
from mcp_protocol.base import MCPServer, Tool, ToolResult, Resource


class GitServer(MCPServer):
    def __init__(self, root_path: str = "/data/repos"):
        super().__init__(name="git", version="1.0.0")
        self.root_path = Path(root_path)
        self.root_path.mkdir(parents=True, exist_ok=True)

    async def initialize(self) -> None:
        """Register tools"""
        self.register_tool(
            Tool(
                name="clone",
                description="Clone a git repository",
                parameters={
                    "url": {"type": "string", "description": "Repository URL"},
                    "path": {"type": "string", "description": "Local path"}
                }
            ),
            self.clone
        )
        self.register_tool(
            Tool(
                name="status",
                description="Get git status",
                parameters={
                    "path": {"type": "string", "description": "Repository path"}
                }
            ),
            self.status
        )
        self.register_tool(
            Tool(
                name="commit",
                description="Commit changes",
                parameters={
                    "path": {"type": "string", "description": "Repository path"},
                    "message": {"type": "string", "description": "Commit message"}
                }
            ),
            self.commit
        )
        self.register_tool(
            Tool(
                name="init",
                description="Initialize a git repository",
                parameters={
                    "path": {"type": "string", "description": "Directory path"}
                }
            ),
            self.init
        )
        self.register_tool(
            Tool(
                name="log",
                description="Get git log",
                parameters={
                    "path": {"type": "string", "description": "Repository path"},
                    "count": {"type": "integer", "description": "Number of commits"}
                }
            ),
            self.log
        )

        # Register resources
        self.register_resource(Resource(
            uri="git://",
            name="Git Server",
            description="Git operations server"
        ))

    async def call_tool(self, name: str, args: Dict[str, Any]) -> ToolResult:
        """Dispatch tool calls"""
        handlers = {
            "clone": self.clone,
            "status": self.status,
            "commit": self.commit,
            "init": self.init,
            "log": self.log,
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

    def _get_repo(self, path: str) -> Repo:
        repo_path = self.root_path / path
        return Repo(repo_path)

    def _safe_path(self, path: str) -> Path:
        """Ensure path is within root directory"""
        full_path = (self.root_path / path).resolve()
        try:
            full_path.relative_to(self.root_path.resolve())
            return full_path
        except ValueError:
            raise ValueError(f"Path {path} is outside root directory")

    async def clone(self, url: str, path: str) -> Dict[str, Any]:
        """Clone a git repository"""
        repo_path = self._safe_path(path)
        repo = Repo.clone_from(url, repo_path)
        return {
            "success": True,
            "message": f"Cloned {url} to {path}",
            "branch": repo.active_branch.name
        }

    async def init(self, path: str) -> Dict[str, Any]:
        """Initialize a git repository"""
        repo_path = self._safe_path(path)
        repo_path.mkdir(parents=True, exist_ok=True)
        repo = Repo.init(repo_path)
        return {
            "success": True,
            "message": f"Initialized git repository in {path}",
            "branch": repo.active_branch.name if repo.active_branch else "master"
        }

    async def status(self, path: str) -> Dict[str, Any]:
        """Get git status"""
        repo = self._get_repo(path)
        # Handle fresh repository with no commits
        try:
            branch_name = repo.active_branch.name if repo.active_branch else "HEAD"
        except TypeError:
            # Detached HEAD or no commits yet
            branch_name = "HEAD"
        return {
            "branch": branch_name,
            "untracked": repo.untracked_files,
            "modified": [item.a_path for item in repo.index.diff(None)],
            "staged": [item.a_path for item in repo.index.diff("HEAD")] if repo.head.is_valid() else []
        }

    async def commit(self, path: str, message: str) -> Dict[str, Any]:
        """Commit changes"""
        repo = self._get_repo(path)
        commit = repo.index.commit(message)
        return {
            "success": True,
            "message": f"Committed: {message}",
            "commit_hash": commit.hexsha[:7]
        }

    async def log(self, path: str, count: int = 10) -> List[Dict[str, Any]]:
        """Get git log"""
        repo = self._get_repo(path)
        commits = []
        for commit in repo.iter_commits(max_count=count):
            commits.append({
                "hash": commit.hexsha[:7],
                "message": commit.message.strip(),
                "author": str(commit.author),
                "date": commit.committed_datetime.isoformat()
            })
        return commits
