# Connecting

The InterPro MCP server is hosted by EMBL-EBI and speaks MCP over
Streamable HTTP. You don't need to install, run or configure
anything: just point your AI client at the public endpoint:

```{admonition} Endpoint
:class: tip

`https://www.ebi.ac.uk/interpro/mcp`
```

It's a public, read-only service: no account, API key or authentication is needed. Once your
client is connected, ask questions in plain language; the assistant discovers the tools and
picks them itself.

The steps below cover the common clients.

## Claude Code (CLI)

Add the server with one command:

```bash
claude mcp add --transport http interpro https://www.ebi.ac.uk/interpro/mcp
```

By default this registers it for the current project only; add `-s user` to make it available in
**all** your projects:

```bash
claude mcp add -s user --transport http interpro https://www.ebi.ac.uk/interpro/mcp
```

Check the connection, then use it:

```bash
claude mcp list        # interpro  -> ✓ Connected
```

Inside a session, `/mcp` lists the server and its tools; the model sees them namespaced as
`mcp__interpro__getEntry`, `mcp__interpro__searchInterPro`, ….

## Claude Desktop & claude.ai

Open **Settings → Connectors**, choose **Add custom connector**, 
give it a name (e.g. `InterPro`) and paste the URL:

```text
https://www.ebi.ac.uk/interpro/mcp
```

Then enable it from the tools / connectors menu in a chat, and the InterPro tools become
available to the model.

## Other MCP clients

Any client that supports the Streamable HTTP transport can point straight at the URL. A typical
configuration entry:

```json
{
  "mcpServers": {
    "interpro": {
      "type": "streamable-http",
      "url": "https://www.ebi.ac.uk/interpro/mcp"
    }
  }
}
```

## Check it's working

Ask your assistant something that needs InterPro, for example:

> *"Using InterPro, what is the kringle domain and which entry describes it?"*

If the server is connected the assistant will call a tool (e.g. `searchInterPro`) and answer
with an accession such as **IPR000001**. In Claude Code, `/mcp` also shows the live connection
status and the tool list.
