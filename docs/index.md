# InterPro MCP Server

A [Model Context Protocol](https://modelcontextprotocol.io) (MCP) server that exposes
[InterPro](https://www.ebi.ac.uk/interpro/) to AI clients such as Claude
Desktop, Claude Code and [claude.ai](http://claude.ai/).

InterPro classifies proteins into families and predicts domains and important functional
sites by combining the signatures of its member databases (Pfam, PANTHER, CATH-Gene3D,
PROSITE, SMART, CDD, NCBIfam, etc.). This server puts that behind a small set of
well-described tools, and coordinates several EMBL-EBI services behind each one so an
agent can answer a question with a single call instead of chaining raw REST requests:

- [EBI Search](https://www.ebi.ac.uk/ebisearch/): free-text / keyword discovery when
  you don't yet have an accession ("zinc finger", "kinase domain").
- [InterPro REST API](https://www.ebi.ac.uk/interpro/api/): authoritative lookups for
  entries, proteins, structures, domain architectures and taxonomy once an accession is
  known.
- [InterPro Matches API](https://www.ebi.ac.uk/interpro/matches/api/): precomputed
  matches for a protein sequence, keyed by its MD5 checksum.
- InterProScan 6 ([Job Dispatcher](https://www.ebi.ac.uk/jdispatcher/)):
  runs a full analysis on a novel sequence that isn't in InterPro yet.

## What you can ask

Once the server is connected, you talk to your assistant in plain language and it picks the
tools itself. Questions the 13 tools cover:

- *"What is the kringle domain, and which InterPro entry describes it?"*
- *"Is the protein kinase domain PF00069 mostly eukaryotic or bacterial?"*
- *"What domains are in UniProt P99999, and where are they?"*
- *"Which InterPro entries are annotated with GO:0004672?"*
- *"Which domain architectures pair a kringle domain with a trypsin domain?"*
- *"List reviewed human proteins containing IPR000001."*
- *"What PDB structures contain PF00069?"*
- *"Run InterProScan on this sequence and tell me what domains it has."*

See the example **[workflows](examples/workflows.md)** and **[by-tool examples](examples/by-tool.md)**
for how each one plays out, and the **[tool reference](tools/index.md)** for the exact arguments
and return shapes.

## Connecting

The server is hosted by EMBL-EBI and speaks MCP over Streamable HTTP at
`https://www.ebi.ac.uk/interpro/mcp`. Point your AI client at that URL. 
**[Connecting](connecting.md)** has step-by-step setup for Claude Code, 
Claude Desktop, claude.ai and other clients. No account or API key is needed.

```{toctree}
:hidden:

connecting
```

```{toctree}
:caption: Examples
:maxdepth: 1
:hidden:

examples/workflows
examples/by-tool
```

```{toctree}
:caption: Tool reference
:maxdepth: 1
:hidden:

tools/index
tools/searchInterPro
tools/getEntry
tools/browseEntries
tools/searchByGoTerm
tools/getProteinEntries
tools/getDomainArchitectures
tools/searchDomainArchitectures
tools/getTaxonomicDistribution
tools/getEntryProteins
tools/getEntryStructures
tools/matchSequences
tools/analyzeSequence
tools/getSequenceAnalysis
```

