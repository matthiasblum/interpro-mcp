# `getEntryStructures`

List the experimentally determined PDB structures that contain an entry. Use it to find
structural representatives of a domain.

## Arguments

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| `accession` | string | yes | Entry accession, e.g. `IPR000001` or `PF00069`. The source database is resolved automatically from the accession. |
| `pageSize` | integer | no | Structures per page. Default `20`, max `100`. |

## Returns

A JSON object with the entry `accession`, the total structure `count`, an optional `next`
cursor URL, and a `structures` array. Each structure has `accession` (PDB id), `name` (title),
`experiment_type`, `resolution` (when available), and the residue ranges where the domain maps
in `locations` (`start`, `end`).

```json
{
  "accession": "PF00069",
  "count": 5393,
  "structures": [
    {
      "accession": "1atp",
      "name": "cAMP-dependent protein kinase",
      "experiment_type": "x-ray",
      "resolution": 2.2,
      "locations": [ { "start": 43, "end": 297 } ]
    }
  ]
}
```

## Examples

- `accession="PF00069"`: PDB structures containing the protein-kinase domain.
- `accession="IPR000001", pageSize=10`: ten structures containing the kringle domain.
