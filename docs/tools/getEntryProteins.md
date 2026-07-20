# `getEntryProteins`

List the UniProt proteins matched by an entry. Complements
[`getTaxonomicDistribution`](getTaxonomicDistribution.md) (which only counts): use this to
retrieve example / member proteins, optionally restricted to reviewed (Swiss-Prot) proteins
and/or a taxon.

## Arguments

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| `accession` | string | yes | Entry accession, e.g. `IPR000001` or `PF00069`. The source database is resolved automatically from the accession. |
| `reviewedOnly` | boolean | no | Restrict to reviewed (Swiss-Prot) proteins only. Default `false`. |
| `taxId` | string | no | NCBI taxon id to restrict to a taxon subtree, e.g. `9606` (human) or `2` (Bacteria). |
| `pageSize` | integer | no | Proteins per page. Default `20`, max `100`. |

## Returns

A JSON object with the entry `accession`, the applied `reviewed_only` flag and optional
`tax_id`, the total match `count`, an optional `next` cursor URL, and a `proteins` array. Each
protein has `accession`, `name`, `source_database` (`reviewed` / `unreviewed`), `length`, an
`organism` (`taxId`, `scientificName`), an `in_alphafold` flag when a model exists, and the
matched residue ranges in `locations` (`start`, `end`).

```json
{
  "accession": "IPR000001",
  "reviewed_only": true,
  "tax_id": "9606",
  "count": 18,
  "proteins": [
    {
      "accession": "P00747",
      "name": "Plasminogen",
      "source_database": "reviewed",
      "length": 810,
      "organism": { "taxId": "9606", "scientificName": "Homo sapiens" },
      "in_alphafold": true,
      "locations": [ { "start": 103, "end": 181 }, { "start": 184, "end": 262 } ]
    }
  ]
}
```

## Examples

- `accession="IPR000001", reviewedOnly=true, taxId="9606"`: reviewed human kringle proteins.
- `accession="PF00069", pageSize=50`: a page of 50 proteins with the kinase domain.
- `accession="IPR000719", taxId="2"`: bacterial proteins carrying the protein-kinase domain.
