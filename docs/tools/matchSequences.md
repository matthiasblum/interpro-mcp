# `matchSequences`

Look up precomputed InterPro matches for one or more protein sequences (or MD5s), up to 100
at once, one per line.

## Arguments

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| `sequencesOrMd5s` | string | yes | Up to 100 protein sequences or MD5 hex strings, one per line. Each line is hashed if it isn't already an MD5; duplicates are collapsed; extras beyond 100 are ignored. |

## Returns

The Matches API batch result, keyed by MD5, for each input, whether it was found and its
matches (member database signatures and integrated InterPro entries with locations).

## Examples

- Three sequences, one per line:

  ```
  MKTAYIAKQR…
  MGDVEKGKKIFIMKCSQCH…
  MSEQNNTEMTFQIQRIYTK…
  ```

- A mix of sequences and precomputed MD5s, one per line (both are accepted).
