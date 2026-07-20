# `getSequenceAnalysis`

Retrieve a previously submitted InterProScan job by its `jobId`, the id returned by
[`analyzeSequence`](analyzeSequence.md) when the job was still running. Returns the full
InterProScan result if the job has finished, otherwise its current status.

## Arguments

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| `jobId` | string | yes | Job id from `analyzeSequence`, e.g. `iprscan6-R20260626-231923-0712-31578623-p1m`. Validated against the Job Dispatcher id format. |

## Returns

The same shape as [`analyzeSequence`](analyzeSequence.md):

- **Finished**: `{ "jobId": …, "status": "FINISHED", "result": { … } }` (the full InterProScan
  JSON).
- **Still pending**: `{ "jobId": …, "status": "QUEUED" | "RUNNING", "note": "…" }`.
- **Failed / unknown**: `{ "jobId": …, "status": "ERROR" | "FAILURE" | "NOT_FOUND", "error": "…" }`.

A malformed `jobId` returns `{"error": "Provide a valid InterProScan jobId, …"}` without calling
the backend.

## Examples

- `jobId="iprscan6-R20260626-231923-0712-31578623-p1m"`: fetch the result (or current status)
  of that job.
