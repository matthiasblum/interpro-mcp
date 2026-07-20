"""Minimal MCP-over-Streamable-HTTP client used by the contract layer.

Talks JSON-RPC to the server's `POST /mcp` endpoint, 
so the CI job needs nothing beyond `pytest`. The server replies over
SSE (`text/event-stream`), so responses are un-framed here before parsing.
"""

import json
import urllib.error
import urllib.request
from urllib.parse import urlparse


def health_url(mcp_url):
    """Derive the actuator health URL (…/mcp/actuator/health) from an …/mcp URL.

    Actuator is served under /mcp (management.endpoints.web.base-path=/mcp/actuator)
    so the whole app lives beneath one public path.
    """
    p = urlparse(mcp_url)
    return f"{p.scheme}://{p.netloc}/mcp/actuator/health"


def server_up(mcp_url, timeout=2):
    """True if the server answers /mcp/actuator/health with status UP."""
    try:
        with urllib.request.urlopen(health_url(mcp_url), timeout=timeout) as resp:
            return resp.status == 200 and b'"UP"' in resp.read()
    except (urllib.error.URLError, OSError):
        return False


def call_tool(mcp_url, tool, arguments, timeout=60):
    """Call one MCP tool via tools/call and return its text content.

    Raises RuntimeError on a JSON-RPC error or an unexpected response shape.
    """
    payload = json.dumps(
        {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "tools/call",
            "params": {"name": tool, "arguments": arguments},
        }
    ).encode()
    req = urllib.request.Request(
        mcp_url,
        data=payload,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "Accept": "application/json, text/event-stream",
        },
    )
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        body = resp.read().decode()

    obj = _parse_jsonrpc(body)
    if "error" in obj:
        raise RuntimeError(f"JSON-RPC error from {tool}: {obj['error']}")
    try:
        return obj["result"]["content"][0]["text"]
    except (KeyError, IndexError, TypeError) as exc:
        raise RuntimeError(
            f"unexpected result shape from {tool}: {body[:300]}"
        ) from exc


def _parse_jsonrpc(body):
    """Extract the JSON-RPC object from a response that may be SSE-framed.

    Streamable HTTP wraps the reply as `data: {…}` event lines; a stateless
    server may also answer with plain JSON. Return the last data line (or the
    whole body) that parses to an object carrying `result` or `error`.
    """
    body = body.strip()
    candidates = []
    if "data:" in body:
        for line in body.splitlines():
            line = line.strip()
            if line.startswith("data:"):
                candidates.append(line[len("data:") :].strip())
    if not candidates:
        candidates = [body]

    last_err = None
    for chunk in reversed(candidates):
        if not chunk:
            continue
        try:
            obj = json.loads(chunk)
        except json.JSONDecodeError as exc:
            last_err = exc
            continue
        if isinstance(obj, dict) and ("result" in obj or "error" in obj):
            return obj
    detail = f" ({last_err})" if last_err else ""
    raise RuntimeError(f"could not parse JSON-RPC response: {body[:300]}{detail}")
