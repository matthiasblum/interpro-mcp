"""Shared pytest fixtures for the InterPro MCP test suite.

Two layers, selected by marker:

  * contract: call each MCP tool directly over JSON-RPC and assert on
    known-answer cases. No LLM. This is what CI runs.
  * agentic: drive the server through the Claude CLI and check tool
    selection + answer. Needs claude on PATH; skipped automatically if absent.

Both need a server that is already running.
Point MCP_URL at it (default http://localhost:8082/mcp)
"""

import os
import shutil
import subprocess
import sys
import tempfile

import pytest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import mcp_client

MCP_URL = os.environ.get("MCP_URL", "http://localhost:8082/mcp")


def pytest_configure(config):
    config.addinivalue_line(
        "markers", "contract: deterministic JSON-RPC checks against the tools (no LLM)"
    )
    config.addinivalue_line(
        "markers",
        "agentic: end-to-end checks driving the server via the Claude CLI (needs `claude`)",
    )


@pytest.fixture(scope="session")
def mcp_server():
    """URL of an already-running MCP server; the suite never starts one itself.
    If nothing is listening, the whole suite is skipped.
    """
    if not mcp_client.server_up(MCP_URL):
        pytest.skip(
            f"MCP server not reachable at {MCP_URL} — start it first "
            f"(e.g. `make run`); the test suite does not start a server."
        )
    return MCP_URL


@pytest.fixture
def call(mcp_server):
    """call(tool, arguments) -> the tool's text content (contract layer)."""

    def _call(tool, arguments):
        return mcp_client.call_tool(mcp_server, tool, arguments)

    return _call


@pytest.fixture(scope="session")
def claude_cli():
    """Path to the `claude` binary, or skip the agentic layer if it's absent."""
    exe = shutil.which("claude")
    if exe is None:
        pytest.skip("`claude` CLI not found on PATH. The agentic layer needs it")
    return exe


@pytest.fixture(scope="session")
def mcp_config(mcp_server):
    """A temp `--mcp-config` file pinning only the interpro server"""
    import json

    cfg = {"mcpServers": {"interpro": {"type": "http", "url": mcp_server}}}
    with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False) as f:
        json.dump(cfg, f)
        path = f.name

    yield path
    # os.unlink(path)


@pytest.fixture
def run_agent(claude_cli, mcp_config):
    """run_agent(prompt) -> (tools_called, final_answer, stderr) via headless `claude -p`."""
    import json

    def _run(prompt, timeout=180):
        proc = subprocess.run(
            [
                claude_cli,
                "-p",
                prompt,
                "--mcp-config",
                mcp_config,
                "--strict-mcp-config",
                "--allowedTools",
                "mcp__interpro",
                "--output-format",
                "stream-json",
                "--verbose",
            ],
            stdin=subprocess.DEVNULL,
            capture_output=True,
            text=True,
            timeout=timeout,
        )
        tools, final = [], ""
        for line in proc.stdout.splitlines():
            try:
                ev = json.loads(line)
            except json.JSONDecodeError:
                continue
            if ev.get("type") == "assistant":
                for block in ev.get("message", {}).get("content", []):
                    if block.get("type") == "tool_use":
                        tools.append(block.get("name", ""))
            elif ev.get("type") == "result":
                final = ev.get("result", "") or ""
        return tools, final, proc.stderr

    return _run
