# `searchDomainArchitectures`

Search for the domain architectures that **match a given domain organisation**, 
i.e. find the architectures, and how many proteins have them, that are
built from a set of domains you specify. This is the inverse of
[`getDomainArchitectures`](getDomainArchitectures.md): instead of the architectures a *single*
entry occurs in, you give *two or more* domain accessions and get every architecture
containing them.

## Arguments

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| `domains` | string | yes | Two or more domain accessions, Pfam (`PFxxxxx`) and/or InterPro (`IPRxxxxxx`), separated by commas or dashes, e.g. `PF00051,PF00089` or `IPR000001-IPR001254`. When `ordered` is true they are read in N→C order. |
| `ordered` | boolean | no | Require the domains to occur in the given N→C order. Default `false` (any order). |
| `exact` | boolean | no | Match only architectures containing **exactly** these domains and no others. Default `false` (additional domains allowed). |
| `pageSize` | integer | no | Architectures per page. Default `20`, max `100`. |

## Returns

A JSON object echoing the query and listing the matching architectures:

```json
{
  "query": "PF00051,PF00089",
  "ordered": true,
  "exact": true,
  "count": 1,
  "architectures": [
    {
      "ida": "PF00051:IPR000001-PF00089:IPR001254",
      "ida_id": "…",
      "unique_proteins": 1234,
      "representative": {
        "accession": "P00747",
        "length": 810,
        "domains": [ { "accession": "PF00051", "name": "Kringle" }, { "accession": "PF00089", "name": "Trypsin" } ]
      }
    }
  ]
}
```

`count` is the number of matching architectures (not proteins); `unique_proteins` is how many
proteins share each one. A `next` cursor URL is included when more pages exist.

```{note}
A single very common domain matches a huge number of proteins and can time out server-side.
This tool is meant for **combinations** of domains. To list the architectures one entry occurs
in, use [`getDomainArchitectures`](getDomainArchitectures.md) instead.
```

## Examples

- `domains="PF00051,PF00089"`: every architecture containing both the Kringle and Trypsin
  domains, in any order.
- `domains="PF00051,PF00089", ordered=true`: only those where Kringle precedes Trypsin (N→C).
- `domains="PF00051,PF00089", ordered=true, exact=true`: the single architecture that is
  *exactly* Kringle→Trypsin and nothing else.
- `domains="IPR000001-IPR001254"`: the same query written with InterPro accessions and a dash.
