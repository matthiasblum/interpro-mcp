package uk.ac.ebi.interpro.mcp.tools;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * All InterPro annotations (matches) of a UniProt protein: look up the protein's
 * sequence via the InterPro API, then return all precomputed matches for that
 * sequence (by MD5) from the Matches API in a single call.
 */
@Component
public class ProteinTool {

    private static final String NOT_FOUND_NOTE =
            "The protein's sequence was retrieved, but its MD5 is not in InterPro's precomputed match set "
            + "(the Matches API is a lookup of precomputed results, not InterProScan).";

    private final InterProClient interpro;
    private final MatchesClient matches;
    private final ObjectMapper mapper;

    public ProteinTool(InterProClient interpro, MatchesClient matches, ObjectMapper mapper) {
        this.interpro = interpro;
        this.matches = matches;
        this.mapper = mapper;
    }

    @Tool(name = "getProteinEntries",
          description = """
                  Get all InterPro annotations of a UniProt protein - every member-database signature and
                  integrated InterPro entry matching the protein's sequence (of any type: domains, families,
                  sites, repeats, ...), with their residue locations. Fetches the protein's amino-acid sequence
                  from the InterPro API, then returns the precomputed matches from the InterPro Matches API in
                  one call. Input is a UniProt accession (e.g. P99999 or P04637).""")
    public String getProteinEntries(
            @ToolParam(description = "UniProt accession, e.g. 'P99999'.")
            String uniprotAccession) {
        String acc = uniprotAccession == null ? "" : uniprotAccession.trim();
        if (acc.isEmpty()) {
            return "{\"error\":\"Provide a UniProt accession, e.g. P99999.\"}";
        }

        String proteinJson = interpro.protein(acc);
        if (ApiClient.isError(proteinJson)) {
            return proteinJson;
        }

        try {
            JsonNode meta = mapper.readTree(proteinJson).path("metadata");
            String sequence = meta.path("sequence").asText(null);
            if (sequence == null || sequence.isBlank()) {
                return "{\"error\":\"No sequence available for '" + acc + "'.\"}";
            }
            String md5 = MatchesClient.md5(sequence);

            ObjectNode out = mapper.createObjectNode();
            out.put("accession", meta.path("accession").asText(acc));
            out.put("name", meta.path("name").asText(null));
            out.put("length", meta.path("length").asInt());
            if (meta.has("source_organism")) {
                out.set("source_organism", meta.path("source_organism"));
            }
            out.put("md5", md5);

            String matchesJson = matches.byMd5(md5);
            if (ApiClient.isError(matchesJson)) {
                out.put("found", false);
                out.put("note", NOT_FOUND_NOTE);
            } else {
                out.set("matches", mapper.readTree(matchesJson));
            }
            return mapper.writeValueAsString(out);
        } catch (BackendException e) {
            throw e; // a hard backend failure must surface as isError:true, not be masked as a processing error
        } catch (Exception e) {
            return "{\"error\":\"Failed to process protein '" + acc + "': " + e.getMessage() + "\"}";
        }
    }
}
