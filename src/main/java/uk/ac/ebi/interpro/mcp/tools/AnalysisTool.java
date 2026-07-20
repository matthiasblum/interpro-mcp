package uk.ac.ebi.interpro.mcp.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Run InterProScan 6 on an arbitrary protein sequence via the EMBL-EBI Job
 * Dispatcher. This is the analysis counterpart to the Matches API lookup: it works
 * for novel sequences not yet in InterPro/UniParc. Jobs are asynchronous, so
 * {@link #analyzeSequence} submits and waits a bounded time; if the job is still
 * running it returns the job id to retrieve later with {@link #getSequenceAnalysis}.
 */
@Component
public class AnalysisTool {

    private static final Pattern XML_DESCRIPTION = Pattern.compile("<description>(.*?)</description>", Pattern.DOTALL);

    private final InterProScanClient client;
    private final ObjectMapper mapper;
    private final long maxWaitMs;
    private final long intervalMs;

    public AnalysisTool(InterProScanClient client, ObjectMapper mapper,
                        @Value("${interproscan.poll.max-wait-seconds:60}") long maxWaitSeconds,
                        @Value("${interproscan.poll.interval-seconds:5}") long intervalSeconds) {
        this.client = client;
        this.mapper = mapper;
        this.maxWaitMs = Math.max(0, maxWaitSeconds) * 1000L;
        this.intervalMs = Math.max(1, intervalSeconds) * 1000L;
    }

    @Tool(name = "analyzeSequence",
          description = """
                  Run InterProScan 6 on a protein sequence via the EMBL-EBI Job Dispatcher and return its
                  InterPro/member-database matches (signatures, integrated entries, locations, GO terms and
                  pathways). Unlike matchSequences (a lookup of precomputed results), this actually RUNS the
                  analysis, so it works for NOVEL sequences not yet in InterPro/UniParc -
                  use it when matchSequences returns found:false. The job is asynchronous:
                  this waits a bounded time and returns the full result if it finishes, 
                  otherwise a jobId and status to retrieve later with getSequenceAnalysis.
                  Input a raw amino-acid sequence (or FASTA).""")
    public String analyzeSequence(
            @ToolParam(description = "A protein amino-acid sequence (raw or FASTA; whitespace ignored).")
            String sequence,
            @ToolParam(required = false,
                    description = """
                            Optional comma-separated member-database applications to run (e.g.
                            'Pfam,CATH-Gene3D,PANTHER'). Omit to run the default set of all applications.""")
            String applications,
            @ToolParam(required = false, description = "Include GO term annotations (default true).")
            Boolean goTerms,
            @ToolParam(required = false, description = "Include pathway annotations (default true).")
            Boolean pathways) {
        String seq = sequence == null ? "" : sequence.trim();
        if (seq.isEmpty()) {
            return "{\"error\":\"Provide a protein sequence to analyse.\"}";
        }
        if (!client.hasEmail()) {
            return "{\"error\":\"InterProScan submission requires a contact email. Set 'interproscan.email' in the "
                    + "server configuration (required by the EBI Job Dispatcher).\"}";
        }

        String submitResponse = client.submit(seq, applications,
                goTerms == null || goTerms, pathways == null || pathways);
        if (!InterProScanClient.isJobId(submitResponse)) {
            return "{\"error\":\"InterProScan job submission failed: " + escape(describe(submitResponse)) + "\"}";
        }
        String jobId = submitResponse.trim();

        String status = waitFor(jobId);
        return forStatus(jobId, status);
    }

    @Tool(name = "getSequenceAnalysis",
          description = """
                  Retrieve a previously submitted InterProScan job by its jobId (as returned by
                  analyzeSequence when the job was still running). Returns the full InterProScan result if the job
                  has finished, otherwise its current status (QUEUED / RUNNING / ERROR / FAILURE).""")
    public String getSequenceAnalysis(
            @ToolParam(description = "Job id from analyzeSequence, e.g. 'iprscan6-R20260626-231923-0712-31578623-p1m'.")
            String jobId) {
        String id = jobId == null ? "" : jobId.trim();
        if (!InterProScanClient.isJobId(id)) {
            return "{\"error\":\"Provide a valid InterProScan jobId, e.g. iprscan6-R...-p1m.\"}";
        }
        String status = client.status(id);
        return forStatus(id, status);
    }

    /** Poll the job until it leaves a pending state or the bounded wait elapses. */
    private String waitFor(String jobId) {
        long deadline = System.currentTimeMillis() + maxWaitMs;
        String status = client.status(jobId);
        while (isPending(status) && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            status = client.status(jobId);
        }
        return status;
    }

    /** Build the tool response from a job's current status. */
    private String forStatus(String jobId, String status) {
        String s = status == null ? "" : status.trim();
        if ("FINISHED".equals(s)) {
            String result = client.resultJson(jobId);
            if (ApiClient.isError(result)) {
                return "{\"jobId\":\"" + jobId + "\",\"status\":\"FINISHED\",\"error\":\""
                        + escape(result) + "\"}";
            }
            try {
                ObjectNode out = mapper.createObjectNode();
                out.put("jobId", jobId);
                out.put("status", "FINISHED");
                out.set("result", mapper.readTree(result));
                return mapper.writeValueAsString(out);
            } catch (Exception e) {
                return result; // fall back to the raw InterProScan JSON
            }
        }
        if ("ERROR".equals(s) || "FAILURE".equals(s) || "NOT_FOUND".equals(s)) {
            return "{\"jobId\":\"" + jobId + "\",\"status\":\"" + s + "\",\"error\":\"InterProScan job did not "
                    + "complete successfully.\"}";
        }
        // still QUEUED / RUNNING (or unknown) after the bounded wait
        return "{\"jobId\":\"" + jobId + "\",\"status\":\"" + escape(s) + "\",\"note\":\"InterProScan is still "
                + "running. Call getSequenceAnalysis with this jobId to retrieve the result.\"}";
    }

    private static boolean isPending(String status) {
        String s = status == null ? "" : status.trim();
        return s.isEmpty() || "QUEUED".equals(s) || "RUNNING".equals(s);
    }

    /** Pull a human-readable message out of a Job Dispatcher XML {@code <error>} body, else the body itself. */
    private static String describe(String body) {
        if (body == null) {
            return "no response";
        }
        Matcher m = XML_DESCRIPTION.matcher(body);
        if (m.find()) {
            return m.group(1).trim();
        }
        return body.strip();
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ");
    }
}
