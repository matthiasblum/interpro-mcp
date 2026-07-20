# `getEntry`

Retrieve full metadata for a single InterPro entry or member-database signature by accession:
name, type, description, GO terms, integrated/member signatures, hierarchy, literature,
cross-references and counters (proteins / structures / taxa).

## Arguments

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| `accession` | string | yes | Entry accession — an InterPro id (`IPRxxxxxx`) or a member-DB signature such as Pfam `PF00069`, PANTHER `PTHR11961`, CATH-Gene3D `G3DSA:1.10.760.10`, SMART `SM00220`, CDD `cd00108`. The source database is resolved automatically from the accession (prefix, then an EBI Search id lookup that also disambiguates PROSITE patterns vs profiles). |

## Returns

The InterPro API's entry JSON, under `metadata`: `accession`, `name`, `type`, `description`,
`go_terms`, `member_databases` / `integrated`, `hierarchy`, `literature`, `cross_references`
and a `counters` object (`proteins`, `structures`, `taxa`, `proteomes`,
`domain_architectures`). If the accession can't be resolved, a short plain-text "not found"
message is returned instead.

## Examples

- `accession="IPR000001"`: the Kringle domain (InterPro).
- `accession="PF00069"`: the Pfam protein-kinase domain (resolves to `pfam`, integrated into
  IPR000719).
- `accession="PS50070"`: resolves to a PROSITE profile.
