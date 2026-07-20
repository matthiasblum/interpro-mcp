# `getProteinEntries`

Get all InterPro annotations of a UniProt protein — every member-database signature and
integrated InterPro entry matching its sequence, of any type (domains, families, sites,
repeats, …), with residue locations. In one call it fetches the protein's amino-acid sequence
from the InterPro API, computes its MD5, and returns the precomputed matches from the Matches API.

## Arguments

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| `uniprotAccession` | string | yes | UniProt accession, e.g. `P99999` or `P04637`. |

## Returns

A JSON object describing the protein and its matches:

```json
{
  "accession": "P99999",
  "name": "Cytochrome c",
  "length": 105,
  "source_organism": { "taxId": "9606", "scientificName": "Homo sapiens" },
  "md5": "8f27...",
  "matches": { "…": "precomputed Matches-API result: signatures + integrated entries + locations" }
}
```

If the sequence's MD5 is not in InterPro's precomputed set, `matches` is replaced by
`"found": false` and a `note` explaining that the Matches API is a lookup, not InterProScan;
use [`analyzeSequence`](analyzeSequence.md) to analyse a novel sequence.

## Examples

- `uniprotAccession="P99999"`: cytochrome c, the cytochrome c family and heme-binding site.
- `uniprotAccession="P04637"`: human p53, its DNA-binding and tetramerisation domains.
