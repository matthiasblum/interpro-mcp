# `analyzeSequence`

Run InterProScan 6 on a protein sequence via the EMBL-EBI Job Dispatcher and return its
InterPro / member database matches (signatures, integrated entries, locations, GO terms and
pathways). Unlike [`matchSequences`](matchSequences.md) — a lookup of precomputed results — this
actually runs the analysis, so it works for novel sequences not yet in InterPro/UniParc.
Reach for it when `matchSequences` returns `found: false`.

The job is asynchronous: the tool submits it and waits a bounded time
(`interproscan.poll.max-wait-seconds`, default 60s). If the job finishes it returns the full
result; otherwise it returns a `jobId` and status to retrieve later with
[`getSequenceAnalysis`](getSequenceAnalysis.md).

## Arguments

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| `sequence` | string | yes | A protein amino-acid sequence (raw or FASTA; whitespace ignored). |
| `applications` | string | no | Comma-separated member-database applications to run, e.g. `Pfam,CATH-Gene3D,PANTHER`. Omit to run the default set of all applications. |
| `goTerms` | boolean | no | Include GO term annotations. Default `true`. |
| `pathways` | boolean | no | Include pathway annotations. Default `true`. |

## Returns

When the job finishes within the wait window:

```json
{
  "jobId": "iprscan6-R20260626-231923-0712-31578623-p1m",
  "status": "FINISHED",
  "result": {
    "interproscan-version": "6.0.1",
    "interpro-version": "109.0",
    "results": [ { "sequence": "…", "md5": "…", "matches": [ { "signature": { "…": "…" } } ] } ]
  }
}
```

When it's still running after the wait:

```json
{ "jobId": "iprscan6-…-p1m", "status": "RUNNING",
  "note": "InterProScan is still running. Call getSequenceAnalysis with this jobId…" }
```

Submission or contact-email problems, and `ERROR` / `FAILURE` job states, are returned as a
small `{"error": …}` (or `{"jobId": …, "status": "ERROR", …}`) object.

## Examples

- `sequence="MGDVEKGKKIFIMKCSQCH…"`: full default analysis of a cytochrome c sequence.
- `sequence="…", applications="Pfam,CATH-Gene3D"`: restrict to two member databases.
- `sequence="…", goTerms=false, pathways=false`: matches only, no GO / pathway annotations.
