# `getTaxonomicDistribution`

Show how an entry is distributed across the NCBI taxonomy, as a tree of UniProt protein counts.
The tree starts at the superkingdoms / domains (Eukaryota, Bacteria, Archaea, Viruses) and
descends by rank (kingdom, phylum, class, order, family, genus, species). Answers "is this
domain bacterial or eukaryote-specific?" (read the top level) and "exactly where does it occur?"

## Arguments

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| `accession` | string | yes | Entry accession, e.g. `IPR000719` or `PF00069`. The source database is resolved automatically from the accession. |
| `depth` | integer | no | How many taxonomic levels to return, counting the superkingdom / domain level as `1` (`2` adds kingdoms, `3` phyla, …). **Omit for the full tree down to species.** |
| `minProteins` | integer | no | Only include taxa with at least this many matching proteins (prunes the long tail of rare lineages). Default `0`. |

```{warning}
The full tree can be very large for ubiquitous domains. 
For a high-level answer pass a small `depth` (e.g. `1` or `2`) and/or a
`minProteins` threshold rather than downloading the whole tree.
```

## Returns

A JSON summary with `accession`, `name`, `source_database`, `total_proteins`, `total_species`,
the requested `depth` (a number, or `"full"`), an optional `min_proteins`, a `node_count`, and a
nested `distribution` array. Each node has `taxId`, `name` (or `"unclassified"` for synthetic
nodes), `rank`, `proteins`, `percent` (of `total_proteins`), `species`, and `children` until the depth
limit is reached.

```json
{
  "accession": "IPR000719",
  "name": "Protein kinase domain",
  "total_proteins": 1954321,
  "depth": 1,
  "distribution": [
    { "taxId": "2759", "name": "Eukaryota", "rank": "superkingdom",
      "proteins": 1799900, "percent": 92.09, "species": 41234 },
    { "taxId": "2", "name": "Bacteria", "rank": "superkingdom",
      "proteins": 151200, "percent": 7.74, "species": 8123 }
  ]
}
```

## Examples

- `accession="IPR000719", depth=1`: the four-way superkingdom breakdown (fast; answers
  "eukaryotic or bacterial?").
- `accession="PF00069", depth=2, minProteins=50000`: kingdoms, dropping rare lineages.
- `accession="IPR000001"`: the full tree for a lineage-specific domain (small enough to
  return in full).
