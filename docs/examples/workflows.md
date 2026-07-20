# Workflows

Multi-step investigations where the agent chains several tools, each step's output feeding the
next. The agent picks the tools itself from their descriptions (i.e. you don't name them) but seeing
the mapping shows how the tools compose.

For what each tool does on its own, see the [by-tool examples](by-tool.md).

## Characterise an unfamiliar domain, end to end

> **Prompt:** "I keep running into the 'kringle domain' in papers. What is it, where does it sit
> on the tree of life, which human proteins carry it, and is there a structure I can look at?"

**Chain:** [`searchInterPro`](../tools/searchInterPro.md) → [`getEntry`](../tools/getEntry.md) →
[`getTaxonomicDistribution`](../tools/getTaxonomicDistribution.md) →
[`getEntryProteins`](../tools/getEntryProteins.md) → [`getEntryStructures`](../tools/getEntryStructures.md)

1. Find the entry. There's no accession to start from, so the agent runs
   `searchInterPro("kringle domain")` and takes the best hit: the InterPro domain
   `IPR000001` ("Kringle").
2. Understand it. `getEntry("IPR000001")` returns the definition, GO terms, literature, and
   the member signatures it integrates (Pfam `PF00051`, SMART `SM00130`, PROSITE `PS50070`, CDD
   `cd00108`), confirming it's a *domain*, not a family.
3. Place it on the tree of life. That same accession flows into
   `getTaxonomicDistribution("IPR000001", depth: 3)`, whose superkingdom breakdown shows kringles
   are mainly eukaryotic: a metazoan module of the clotting and fibrinolysis machinery.
4. List concrete carriers. `getEntryProteins("IPR000001", reviewedOnly: true, taxId: "9606")`
   returns reviewed human proteins containing it: plasminogen (`P00747`), prothrombin, hepatocyte
   growth factor, apolipoprotein(a), each with the residue ranges of every kringle copy.
5. Get something to look at. `getEntryStructures("IPR000001")` lists the PDB entries that
   include a kringle domain, with title, method and resolution, so the agent can point you at a
   solved structure.

One accession, discovered in step 1, drives steps 2–5. You never type an accession by hand.

## Identify an unknown sequence and find its relatives

> **Prompt:** "I pulled this protein out of an assembly and don't recognise it. What domains does
> it have, and what known proteins are built the same way? `<your sequence>`"

**Chain:** [`matchSequences`](../tools/matchSequences.md) → [`analyzeSequence`](../tools/analyzeSequence.md)
→ [`getEntry`](../tools/getEntry.md) → [`searchDomainArchitectures`](../tools/searchDomainArchitectures.md)

1. **Try the instant lookup first.** `matchSequences("<your sequence>")` hashes the sequence to an
   MD5 and checks InterPro's precomputed set. For something straight out of an assembly this often
   comes back `found: false`, i.e. it isn't in UniParc yet.
2. **Run the real analysis.** Because it's novel, the agent falls back to
   `analyzeSequence("<your sequence>")`, which runs InterProScan 6 on the EMBL-EBI Job Dispatcher. Say
   it reports two domains: a **Kringle** (`IPR000001` / Pfam `PF00051`) and a trypsin-like
   serine-protease domain (`IPR001254` / Pfam `PF00089`). If the job doesn't finish inside the
   wait window it returns a `jobId`; the agent retrieves it a moment later with
   [`getSequenceAnalysis`](../tools/getSequenceAnalysis.md).
3. **Confirm the key domain.** `getEntry("IPR001254")` names it: "Serine proteases, trypsin
   domain" (peptidase family S1), with its member signatures and GO terms, so you know it's a
   catalytic protease module, not just a fold.
4. **Find proteins built the same way.** Feeding both accessions to
   `searchDomainArchitectures("IPR000001-IPR001254", ordered: true)` returns the known domain
   architectures that pair a kringle with a trypsin domain, and how many proteins share each, led
   by plasminogen (`P00747`), with HGF, HGF activator and coagulation factors among the relatives.
   That places your unknown protein squarely in the plasminogen / fibrinolysis family.

A raw sequence with no identifier becomes two accessions in step 2, and those accessions locate
its relatives in step 4.
