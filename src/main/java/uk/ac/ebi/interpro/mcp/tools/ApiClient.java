package uk.ac.ebi.interpro.mcp.tools;

import java.net.URI;
import java.util.function.Function;

import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

/**
 * Thin base for the backend client wrappers. Calls return the raw JSON body as a String
 * (consumed directly by the LLM). Failures are split by intent:
 * <ul>
 *   <li><b>Not found</b> — a legitimate negative ("this accession/sequence does not exist").
 *       Backends signal this differently (the Matches API with 404, the InterPro API with an
 *       empty <b>204 No Content</b>); both map to the {@link #NOT_FOUND} sentinel, surfaced by
 *       the tools as a normal result (MCP {@code isError:false}).</li>
 *   <li><b>Any other HTTP status, plus network errors and timeouts</b> — a hard failure: the
 *       request could not be completed. Thrown as a {@link BackendException} so it propagates
 *       out of the tool method and the MCP layer reports the call as {@code isError:true},
 *       instead of a transient outage looking like an authoritative "no data".</li>
 * </ul>
 */
public abstract class ApiClient {

    /** Sentinel for a not-found result — a valid negative, not a failure. Recognised by {@link #isError}. */
    static final String NOT_FOUND = "No data available: the requested accession, identifier or sequence was not found.";

    protected final RestClient restClient;

    protected ApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    protected String get(Function<UriBuilder, URI> uriFunction) {
        try {
            return orNotFound(restClient.get().uri(uriFunction).retrieve().body(String.class));
        } catch (RestClientResponseException e) {
            return notFoundOrThrow(e);
        } catch (ResourceAccessException e) {
            throw unreachable(e);
        }
    }

    protected String get(String uriTemplate, Object... uriVars) {
        try {
            return orNotFound(restClient.get().uri(uriTemplate, uriVars).retrieve().body(String.class));
        } catch (RestClientResponseException e) {
            return notFoundOrThrow(e);
        } catch (ResourceAccessException e) {
            throw unreachable(e);
        }
    }

    protected String post(String uriTemplate, String jsonBody) {
        try {
            return orNotFound(restClient.post().uri(uriTemplate)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonBody)
                    .retrieve()
                    .body(String.class));
        } catch (RestClientResponseException e) {
            return notFoundOrThrow(e);
        } catch (ResourceAccessException e) {
            throw unreachable(e);
        }
    }

    /** Map an empty/no-content success (e.g. the InterPro API's 204 for an unknown entry) to the not-found sentinel. */
    private static String orNotFound(String body) {
        return (body == null || body.isBlank()) ? NOT_FOUND : body;
    }

    /** 404 → not-found sentinel (valid negative); every other error status → hard failure. */
    protected String notFoundOrThrow(RestClientResponseException e) {
        if (e.getStatusCode().value() == 404) {
            return NOT_FOUND;
        }
        throw hardFailure(e);
    }

    /** A non-404 HTTP error status: the request reached the service but failed. */
    protected BackendException hardFailure(RestClientResponseException e) {
        int code = e.getStatusCode().value();
        StringBuilder msg = new StringBuilder("The backend service returned HTTP ").append(code);
        String reason = e.getStatusText();
        if (reason != null && !reason.isBlank()) {
            msg.append(' ').append(reason);
        }
        msg.append(". This is a service-side failure, not a 'not found'; the request could not be completed.");
        // For a client error the body often explains what was rejected — surface a short snippet.
        if (e.getStatusCode().is4xxClientError()) {
            String body = e.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                String snippet = body.strip().replaceAll("\\s+", " ");
                if (snippet.length() > 300) {
                    snippet = snippet.substring(0, 300) + "…";
                }
                msg.append(" Details: ").append(snippet);
            }
        }
        return new BackendException(msg.toString());
    }

    /** No HTTP response at all: DNS/connection failure or timeout. */
    protected BackendException unreachable(ResourceAccessException e) {
        return new BackendException("Could not reach the backend service (network error or timeout): "
                + e.getMostSpecificCause().getMessage() + ". The request could not be completed.");
    }

    /** True if {@code body} is the not-found sentinel or an empty response (i.e. no usable data). */
    public static boolean isError(String body) {
        return body == null || body.isBlank() || body.startsWith(NOT_FOUND);
    }
}
