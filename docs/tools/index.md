# Overview

The server exposes **13 tools**. Each one is documented below with its purpose, arguments,
return shape, and the upstream EMBL-EBI service it draws on. Every tool returns a JSON string.

Most tools that take an entry accession accept **either** an InterPro accession (`IPRxxxxxx`)
**or** a member-database signature (Pfam `PF00069`, PANTHER `PTHR11961`, CATH-Gene3D
`G3DSA:1.10.760.10`, SMART `SM00220`, CDD `cd00108`, …). The source database is resolved
automatically from the accession — first from the accession prefix, then, when that is ambiguous
(notably PROSITE patterns vs profiles), from an EBI Search id lookup — so you never pass a
database yourself. (`browseEntries` is the exception: there `database` selects which collection
to list.)

## Tools at a glance

| Tool | Backend | What it does |
| --- | --- | --- |
| [`searchInterPro`](searchInterPro.md) | EBI Search | Free-text / keyword discovery when you don't know an accession. |
| [`getEntry`](getEntry.md) | InterPro API | Full metadata for one entry or member-DB signature. |
| [`browseEntries`](browseEntries.md) | InterPro API | Paginated browse/filter of entries by database, type and/or GO term. |
| [`searchByGoTerm`](searchByGoTerm.md) | InterPro API | GO term → the InterPro entries annotated with it. |
| [`getProteinEntries`](getProteinEntries.md) | InterPro + Matches API | All domain annotations of a UniProt protein, with locations. |
| [`getDomainArchitectures`](getDomainArchitectures.md) | InterPro API | The recurring domain architectures (IDAs) an entry occurs in. |
| [`searchDomainArchitectures`](searchDomainArchitectures.md) | InterPro API | The domain architectures matching a set of domains (organisation search). |
| [`getTaxonomicDistribution`](getTaxonomicDistribution.md) | InterPro API | Protein counts across the NCBI taxonomy tree for an entry. |
| [`getEntryProteins`](getEntryProteins.md) | InterPro API | List UniProt proteins matched by an entry (reviewed / taxon filters). |
| [`getEntryStructures`](getEntryStructures.md) | InterPro API | List PDB structures that contain an entry. |
| [`matchSequences`](matchSequences.md) | Matches API | Precomputed matches for one or more sequences/MD5s (up to 100). |
| [`analyzeSequence`](analyzeSequence.md) | InterProScan 6 | **Runs** InterProScan on a (possibly novel) sequence. |
| [`getSequenceAnalysis`](getSequenceAnalysis.md) | InterProScan 6 | Retrieve a previously submitted analysis job by id. |
