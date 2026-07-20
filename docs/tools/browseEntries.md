# `browseEntries`

Browse / filter InterPro (or member database) entries, paginated. Use it to list entries of a
given type, from a given member database, or annotated with a given GO term. To go from a
GO term to entries specifically, [`searchByGoTerm`](searchByGoTerm.md) is tool to use.

## Arguments

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| `database` | string | no | Source database to list from. Default `interpro`. e.g. `interpro`, `pfam`, `panther`, `cathgene3d`, `smart`, `cdd`. |
| `type` | string | no | Entry-type filter: `family`, `domain`, `homologous_superfamily`, `repeat`, `conserved_site`, `binding_site`, `active_site`, `ptm`. |
| `goTerm` | string | no | GO term identifier to filter by, e.g. `GO:0004672`. |
| `pageSize` | integer | no | Entries per page. Default `20`, max `100`. |

## Returns

The InterPro API's paginated list JSON: a top-level `count`, `next` / `previous` cursor URLs,
and a `results` array of entries (each with a `metadata` object — `accession`, `name`, `type`,
member databases). Page through with the `next` URL.

## Examples

- `database="pfam", type="domain"`: a page of Pfam domain entries.
- `type="homologous_superfamily"`: InterPro homologous superfamilies.
- `goTerm="GO:0004672", pageSize=50`: entries annotated with protein-kinase activity, 50 per
  page.
