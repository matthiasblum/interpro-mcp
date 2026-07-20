# `getDomainArchitectures`

List the distinct domain architectures in which an entry occurs; 
the recurring ordered domain combinations it is part of across UniProt proteins. 
Use it to understand the structural / functional context a domain appears in.

## Arguments

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| `accession` | string | yes | Entry accession, e.g. `IPR000007` or `PF01167`. The source database is resolved automatically from the accession. |
| `pageSize` | integer | no | Architectures per page. Default `20`, max `100`. |

## Returns

A JSON object with the total `count`, an optional `next` cursor URL, and an `architectures`
array. Each architecture has the `ida` composition string (member database signature : InterPro
entry, `-`-separated in N→C order), an `ida_id`, `unique_proteins` (how many proteins share it)
and a `representative` protein (`accession`, `length`, and its ordered `domains`).

```json
{
  "accession": "IPR000007",
  "count": 171,
  "next": "https://www.ebi.ac.uk/interpro/api/...",
  "architectures": [
    {
      "ida": "PF01167:IPR000007",
      "ida_id": "0d00151a...",
      "unique_proteins": 5820,
      "representative": {
        "accession": "O80699",
        "length": 265,
        "domains": [ { "accession": "PF01167", "name": "Tub" } ]
      }
    }
  ]
}
```

## Examples

- `accession="IPR000007"`: architectures the Tubby C-terminal domain participates in.
- `accession="PF01167", pageSize=5`: the top five, one API page.
