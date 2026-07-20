package uk.ac.ebi.interpro.mcp.tools;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Wrapper for EBI Search (https://www.ebi.ac.uk/ebisearch/ws/rest). */
@Component
public class EbiSearchClient extends ApiClient {

    private final ObjectMapper mapper;

    public EbiSearchClient(@Qualifier("ebiSearchRestClient") RestClient restClient, ObjectMapper mapper) {
        super(restClient);
        this.mapper = mapper;
    }

    public String searchInterpro(String query, int size, String fields) {
        return get(uri -> uri.path("/interpro7")
                .queryParam("query", query)
                .queryParam("format", "json")
                .queryParam("fields", fields)
                .queryParam("size", size)
                .build());
    }

    /**
     * Resolve the InterPro source database for an accession by an exact id lookup in EBI Search,
     * returning the lowercase database name (the InterPro API path segment) or {@code null} if the
     * accession is unknown. This is the authoritative way to disambiguate accessions the prefix
     * heuristic cannot, notably PROSITE (PSxxxxx -> 'prosite' patterns vs 'profile' profiles).
     * The id value is quoted so accessions containing ':' (e.g. G3DSA:1.10.760.10) parse correctly.
     */
    public String databaseFor(String accession) {
        if (accession == null || accession.isBlank()) {
            return null;
        }
        String acc = accession.trim();
        String body = get(uri -> uri.path("/interpro")
                .queryParam("query", "id:\"" + acc + "\"")
                .queryParam("format", "json")
                .queryParam("fields", "source_database")
                .build());
        if (ApiClient.isError(body)) {
            return null;
        }
        try {
            JsonNode entries = mapper.readTree(body).path("entries");
            if (entries.isArray() && !entries.isEmpty()) {
                JsonNode sd = entries.get(0).path("fields").path("source_database");
                String db = sd.isArray()
                        ? (sd.isEmpty() ? null : sd.get(0).asText(null))
                        : sd.asText(null);
                if (db != null && !db.isBlank()) {
                    return db.trim().toLowerCase();
                }
            }
        } catch (Exception e) {
            // unparseable response -> treat as unresolved
        }
        return null;
    }
}
