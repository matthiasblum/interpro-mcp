package uk.ac.ebi.interpro.mcp.tools;

/**
 * Signals that a backend request failed in a way that is <em>not</em> a clean "not found":
 * an HTTP error other than 404 (4xx validation / 5xx server error), a network error, or a
 * timeout. Thrown out of the tool method so the MCP layer reports the call with
 * {@code isError: true} — telling the model the query could not be completed, rather than
 * letting a transient outage masquerade as an authoritative "no data".
 *
 * <p>Contrast with a 404, which {@link ApiClient} returns as the {@link ApiClient#NOT_FOUND}
 * sentinel: a legitimate negative result the tools surface normally (isError:false).
 */
public class BackendException extends RuntimeException {

    public BackendException(String message) {
        super(message);
    }
}
