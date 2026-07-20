package uk.ac.ebi.interpro.mcp.tools;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Wrapper for the EMBL-EBI Job Dispatcher InterProScan 6 service
 * (https://www.ebi.ac.uk/Tools/services/rest/iprscan6). Unlike the Matches API,
 * this <em>runs</em> InterProScan on an arbitrary sequence: submit a job, poll its
 * status, then fetch the result. A contact {@code email} is required by the service.
 */
@Component
public class InterProScanClient extends ApiClient {

    /** A Job Dispatcher job id, e.g. {@code iprscan6-R20260626-231923-0712-31578623-p1m}. */
    private static final Pattern JOB_ID = Pattern.compile("^iprscan\\d?-\\S+$");

    private final String email;

    public InterProScanClient(@Qualifier("interProScanRestClient") RestClient restClient,
                              @Value("${interproscan.email:}") String email) {
        super(restClient);
        this.email = email == null ? "" : email.trim();
    }

    public boolean hasEmail() {
        return !email.isBlank();
    }

    public static boolean isJobId(String s) {
        return s != null && JOB_ID.matcher(s.trim()).matches();
    }

    /**
     * Submit a protein sequence for analysis. Returns the job id on success, or an
     * error string (the service answers HTTP 200 with an XML {@code <error>} body for
     * validation problems, so callers should check {@link #isJobId}).
     */
    public String submit(String sequence, String applications, boolean goTerms, boolean pathways) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("email", email);
        form.add("sequence", sequence);
        form.add("stype", "p");
        form.add("goterms", Boolean.toString(goTerms));
        form.add("pathways", Boolean.toString(pathways));
        if (applications != null && !applications.isBlank()) {
            form.add("appl", applications.trim());
        }
        try {
            return restClient.post().uri("/run")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            // Validation problems come back as HTTP 200 with an XML <error> body (handled by the
            // caller via isJobId); an actual HTTP error status here is an infrastructure failure.
            throw hardFailure(e);
        } catch (ResourceAccessException e) {
            throw unreachable(e);
        }
    }

    /** Job status: one of QUEUED, RUNNING, FINISHED, ERROR, FAILURE, NOT_FOUND. */
    public String status(String jobId) {
        return get("/status/{id}", jobId.trim());
    }

    /** Full InterProScan result in JSON (only meaningful once status is FINISHED). */
    public String resultJson(String jobId) {
        return get("/result/{id}/json", jobId.trim());
    }
}
