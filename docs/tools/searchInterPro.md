# `searchInterPro`

Free-text / keyword search of InterPro and its member-database signatures. Use this as the
entry point when you don't have an accession, e.g. "zinc finger", "kinase domain",
"cytochrome c". Follow up with [`getEntry`](getEntry.md) for full details of a hit.

## Arguments

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| `query` | string | yes | Free-text query, e.g. `zinc finger`. Field syntax is allowed, e.g. `kinase AND type:domain`. |
| `type` | string | no | Entry-type filter, appended as `AND type:<type>`. One of `family`, `domain`, `homologous_superfamily`, `repeat`, `conserved_site`, `binding_site`, `active_site`, `ptm`. |
| `size` | integer | no | Max results to return. Default `10`, max `100`. |

## Returns

The native EBI Search JSON response: a `hitCount` and an `entries` array. Each entry has an
`id` (the accession) and a `fields` object with `name`, `type`, `description` and
`source_database`.

```json
{
  "hitCount": 42,
  "entries": [
    {
      "id": "IPR000001",
      "fields": {
        "name": ["Kringle"],
        "type": ["domain"],
        "source_database": ["INTERPRO"]
      }
    }
  ]
}
```

## Examples

- `query="zinc finger"`: candidate zinc-finger families and domains.
- `query="kinase", type="domain"`: restrict to entries typed as domains.
- `query="cytochrome c", size=5`: the top five cytochrome c hits.
