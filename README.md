# InterPro MCP Server

A minimal [Model Context Protocol](https://modelcontextprotocol.io) (MCP) server
that exposes [InterPro](https://www.ebi.ac.uk/interpro/) to AI clients over Streamable HTTP.

## Tools

| Tool | Source | What it does |
|------|--------|--------------|
| `searchInterPro(query, type?, size?)` | [EBI Search](https://www.ebi.ac.uk/ebisearch/) | Free-text / keyword discovery when you don't know an accession (e.g. "zinc finger"). Returns candidate entries (accession, name, type, description). |
| `getEntry(accession)` | [InterPro API](https://www.ebi.ac.uk/interpro/api/) | Full metadata for one InterPro entry or member-DB signature (GO terms, signatures, hierarchy, literature, cross-refs). |
| `browseEntries(database?, type?, goTerm?, pageSize?)` | InterPro API | Paginated browse/filter of entries by member database, type and/or GO term. |
| `searchByGoTerm(goTerm, pageSize?)` | InterPro API | Go from a GO term (e.g. `GO:0004672`) to the InterPro entries annotated with it. |
| `getProteinEntries(uniprotAccession)` | InterPro API + [Matches API](https://www.ebi.ac.uk/interpro/matches/api/) | All annotations of a UniProt protein: looks up its sequence (InterPro API), then returns every matching signature / integrated entry with residue locations (Matches API). |
| `getDomainArchitectures(accession, pageSize?)` | InterPro API | Distinct domain architectures an entry occurs in: ordered domain composition, a representative protein, and how many proteins share each. |
| `searchDomainArchitectures(domains, ordered?, exact?, pageSize?)` | InterPro API | The inverse: given a set of domains (Pfam/InterPro, e.g. `PF00051,PF00089`), find the architectures built from them. `ordered` requires N→C order; `exact` requires only those domains. |
| `getTaxonomicDistribution(accession, depth?, minProteins?)` | InterPro API | Protein counts across the NCBI taxonomy tree for an entry (superkingdom → kingdom → … → species) in one call. `depth` selects how many levels to return (1 = superkingdoms; omit for the full tree); `minProteins` prunes rare lineages. Answers "is this domain bacterial or eukaryote-specific?" and "exactly where does it occur?". |
| `getEntryProteins(accession, reviewedOnly?, taxId?, pageSize?)` | InterPro API | List the UniProt proteins matched by an entry (optionally reviewed-only and/or restricted to a taxon): accession, name, organism, length, AlphaFold flag, matched residue ranges. |
| `getEntryStructures(accession, pageSize?)` | InterPro API | List the PDB structures that contain an entry: PDB id, title, experiment type, resolution, and where the domain maps. |
| `matchSequences(sequencesOrMd5s)` | Matches API | Precomputed matches for one or more protein sequences (MD5s computed automatically) or MD5s — up to 100, one per line. |
| `analyzeSequence(sequence, applications?, goTerms?, pathways?)` | InterProScan 6 ([Job Dispatcher](https://www.ebi.ac.uk/jdispatcher/)) | Runs InterProScan on a sequence (works for novel sequences, unlike the Matches API lookup). Submits a job and waits briefly; returns the full result, or a `jobId` to retrieve later. |
| `getSequenceAnalysis(jobId)` | InterProScan 6 (Job Dispatcher) | Retrieve a previously submitted `analyzeSequence` job by id: the result if finished, else its status. |

## Requirements

- Java 17+ (the build targets Java 17; a newer JDK is fine)
- Maven 3.6.3+
- Outbound HTTPS access to `wwwdev.ebi.ac.uk` or `www.ebi.ac.uk`
- Claude CLI for local testing

## Build & run

During development:

```bash
mvn spring-boot:run
```

To build target jar:

```bash
mvn clean package
java -jar target/interpro-mcp-0.1.0.jar
```

The server starts on **http://localhost:8082** with:

- the MCP endpoint at `POST /mcp` (Streamable HTTP transport), and
- a health check at `GET /mcp/actuator/health`.

### Configuration

Settings live in `src/main/resources/application.properties` and can be overridden on the
command line (`--key=value`), e.g.

```bash
java -jar target/interpro-mcp-0.1.0.jar --server.port=9000 --interproscan.email=you@example.org
```

## Documentation

The site under `docs/` is Sphinx + MyST Markdown:

```bash
pip install -r docs/requirements.txt

# Serve live docs
make -C docs serve

# Build docs in docs/_build/html/
make -C docs build
```

## Register with an MCP client

### Claude Code

```bash
claude mcp add --transport http interpro http://localhost:8082/mcp
claude mcp list        # interpro -> ✓ Connected
```

`mcp add` defaults to **local** scope (just you, just this project). Use `-s project` to
write a shareable `.mcp.json` into the repo, or `-s user` to make it available in all your
projects. Inside a session, `/mcp` lists the server and its tools; the model sees them
namespaced as `mcp__interpro__getEntry`, `mcp__interpro__getTaxonomicDistribution`, ….

### Claude Desktop

Claude Desktop launches MCP servers as local commands (stdio), so bridge to the HTTP endpoint
with [`mcp-remote`](https://www.npmjs.com/package/mcp-remote) (needs Node.js). In
`claude_desktop_config.json` (macOS:
`~/Library/Application Support/Claude/claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "interpro": {
      "command": "npx",
      "args": ["-y", "mcp-remote", "http://localhost:8082/mcp"]
    }
  }
}
```

Restart Claude Desktop; the InterPro tools appear under the tools (plug) menu.

### Other MCP clients

Any client supporting Streamable HTTP can point directly at the URL:

```json
{
  "mcpServers": {
    "interpro": {
      "type": "streamable-http",
      "url": "http://localhost:8082/mcp"
    }
  }
}
```

### Browser clients (MCP Inspector, web front-ends)

Browsers enforce CORS, so a page on another origin can only reach `/mcp` if the server sends
the right headers. 
To enable localhost origins for browser testing by running with the `local` profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
# or
java -jar target/interpro-mcp-0.1.0.jar --spring.profiles.active=local
```

And start MCP Inspector:

```bash
npx @modelcontextprotocol/inspector
# connect to http://localhost:8082/mcp  (transport: Streamable HTTP)
```

The `local` profile allows any `localhost` / `127.0.0.1` port, which covers the MCP Inspector
UI on `:6274`. To let a front-end served from elsewhere connect, set its origin explicitly:

```bash
java -jar target/interpro-mcp-0.1.0.jar \
  --mcp.cors.allowed-origins=http://localhost:[*],https://my-frontend.example.org
```

> [!TIP]
> Check out [MCPJam](https://app.mcpjam.com/), an improved inspector for testing and debugging MCP servers.

## Testing & evaluation

Two layers: a **contract** test that calls each tool directly over JSON-RPC (no LLM), 
and an **agentic** eval that drives the server through the Claude CLI. 
Both run with `pytest` against a locally running server. Install the test deps once:

```bash
pip install -r eval/requirements.txt
```

Start the server and leave it running in a separate terminal:

```bash
mvn spring-boot:run                       # serves on http://localhost:8082
```

Then run the tests:

```bash
# Contract test: deterministic, no LLM
pytest eval -m contract

# Register the server with the Claude CLI
claude mcp add --transport http interpro http://localhost:8082/mcp

# Agentic eval: drives the server via `claude -p` (needs Claude installed and logged in)
export ANTHROPIC_MODEL="claude-sonnet-5"
pytest eval -m agentic
```
