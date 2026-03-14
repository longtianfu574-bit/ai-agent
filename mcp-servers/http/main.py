# mcp-servers/http/main.py
import os
import sys
from pathlib import Path
from fastapi import FastAPI
from pydantic import BaseModel
from typing import Any, Dict, Optional

# Add parent directory to path for mcp_protocol import
sys.path.insert(0, str(Path(__file__).parent.parent))

from mcp_protocol import require_auth, init_auth
from server import HttpServer

app = FastAPI(title="MCP HTTP Server")

# Initialize
init_auth()
server = HttpServer()


@app.on_event("startup")
async def startup():
    await server.initialize()


@app.on_event("shutdown")
async def shutdown():
    await server.client.aclose()


class ToolCallRequest(BaseModel):
    tool_name: str
    args: Dict[str, Any]


class ToolCallResponse(BaseModel):
    success: bool
    output: Any
    error: Optional[str] = None


@app.get("/health")
async def health():
    return {"status": "healthy", "server": server.name}


@app.get("/tools")
async def list_tools():
    return [t.model_dump() for t in server.list_tools()]


@app.get("/resources")
async def list_resources():
    return [r.model_dump() for r in server.list_resources()]


@app.post("/tools/call", response_model=ToolCallResponse)
@require_auth
async def call_tool(request: ToolCallRequest):
    result = await server.call_tool(request.tool_name, request.args)
    return ToolCallResponse(
        success=result.success,
        output=result.output,
        error=result.error
    )
