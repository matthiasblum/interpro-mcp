# By tool

One-shot examples, roughly one per tool, showing what each tool is for on its own. The agent
picks the tools itself from their descriptions; seeing the mapping helps you understand what the
server can do.

## Identify an unknown domain from a keyword

> **Prompt:** "I keep seeing 'kringle domain' in papers. What is it, and what InterPro
> entry describes it?"

**Tools:** [`searchInterPro`](../tools/searchInterPro.md) → [`getEntry`](../tools/getEntry.md)

Free-text search has no accession to start from, so the agent searches for `kringle domain`,
finds candidate entries, then pulls full metadata (definition, GO terms, member signatures,
literature) for the best hit, InterPro **IPR000001**.

## Look up a signature and place it on the tree of life

> **Prompt:** "What is Pfam PF00069, and is it mostly a eukaryotic or a bacterial domain?"

**Tools:** [`getEntry`](../tools/getEntry.md) → [`getTaxonomicDistribution`](../tools/getTaxonomicDistribution.md)

`getEntry` resolves `PF00069` to the protein-kinase domain (integrated into `IPR000719`).
`getTaxonomicDistribution` with `depth: 1` returns the superkingdom breakdown: the domain is
overwhelmingly eukaryotic, without downloading the large full species-level tree.

## Annotate a UniProt protein

> **Prompt:** "What domains are in UniProt P99999, and where are they?"

**Tools:** [`getProteinEntries`](../tools/getProteinEntries.md)

One call fetches the sequence, computes its MD5, and returns every matching member-database
signature and integrated InterPro entry with residue locations.

## Go from a function (GO term) to the domains that carry it

> **Prompt:** "Which InterPro entries are annotated with the protein-kinase activity term
> GO:0004672?"

**Tools:** [`searchByGoTerm`](../tools/searchByGoTerm.md)

Returns the InterPro entries associated with that molecular-function term, paginated.

## Find example proteins for a domain, filtered to a species

> **Prompt:** "List some reviewed human proteins that contain the kringle domain IPR000001."

**Tools:** [`getEntryProteins`](../tools/getEntryProteins.md)

With `reviewedOnly: true` and `taxId: "9606"`, the agent gets Swiss-Prot human proteins
carrying the domain (plasminogen, prothrombin, HGF, etc.), each with the matched residue ranges
and an AlphaFold-model flag.

## Find structural representatives of a domain

> **Prompt:** "What PDB structures contain the protein-kinase domain PF00069?"

**Tools:** [`getEntryStructures`](../tools/getEntryStructures.md)

Returns the PDB entries that include the domain: PDB id, title, experiment type, resolution,
and where in each structure the domain maps.

## Understand the context a domain appears in

> **Prompt:** "What domain architectures does the Tubby domain IPR000007 occur in?"

**Tools:** [`getDomainArchitectures`](../tools/getDomainArchitectures.md)

Returns the recurring ordered domain combinations the entry is part of across UniProt, each
with a representative protein and how many proteins share that architecture.

## Analyse a brand-new sequence

> **Prompt:** "Run InterProScan on this sequence and tell me what domains it has:
> `MGDVEKGKKIFIMKCSQCHTVEKGGKHKTGPNLHGLFGRKTGQAPGYSYTAANKNKGIIWGEDTLMEYLENPKKYIPGTKMIFVGIKKKEERADLIAYLKKATNE`"

**Tools:** [`analyzeSequence`](../tools/analyzeSequence.md) → [`getSequenceAnalysis`](../tools/getSequenceAnalysis.md)

Because the sequence may not be in InterPro's precomputed set, the agent runs a live
InterProScan 6 job on the EMBL-EBI Job Dispatcher. `analyzeSequence` submits and waits a bounded
time; if the job finishes it returns the full result, otherwise a `jobId` the agent retrieves
a moment later with `getSequenceAnalysis`. The verdict here: a cytochrome c domain (Pfam
`PF00034`, InterPro `IPR009056` / `IPR036909`).

## Screen a batch of sequences

> **Prompt:** "Here are 20 protein sequences. Which already have precomputed InterPro
> matches?"

**Tools:** [`matchSequences`](../tools/matchSequences.md)

Looks up all of them in one call (up to 100), keyed by MD5, reporting for each whether it was
found and its matches.
