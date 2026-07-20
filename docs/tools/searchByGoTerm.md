# `searchByGoTerm`

Find the InterPro entries annotated with a given Gene Ontology (GO) term. Use it to go from a
molecular-function / biological-process / cellular-component term to the InterPro
domains and families that carry it.

## Arguments

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| `goTerm` | string | yes | GO term identifier. Accepts the canonical `GO:0004672` form, or a bare number (`0004672`, `4672`) which is normalised and zero-padded to seven digits. |
| `pageSize` | integer | no | Entries per page. Default `20`, max `100`. |

## Returns

The InterPro API's paginated list JSON: `count`, `next` / `previous`, and a `results` array of
InterPro entries (accession, name, type, member databases, GO terms) associated with the term.
An invalid/empty GO term returns a small `{"error": ...}` object.

## Examples

- `goTerm="GO:0004672"`: InterPro entries with protein-kinase activity.
- `goTerm="GO:0016020", pageSize=100`: entries annotated with "membrane", 100 per page.
