package uk.ac.ebi.interpro.mcp.tools;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** PDB structures associated with an InterPro entry / member-database signature. */
@Component
public class StructureTool {

    private final InterProClient client;
    private final ObjectMapper mapper;

    public StructureTool(InterProClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Tool(name = "getEntryStructures",
          description = """
                  List the experimentally determined PDB structures that contain an InterPro entry or
                  member-database signature. Input an entry accession (e.g. IPR000001 or PF00069). Returns the
                  total structure count and a page of PDB entries (PDB id, title, experiment type, resolution, and
                  the residue ranges where the domain maps), plus a 'next' cursor URL for the following page. Use
                  to find structural representatives of a domain.""")
    public String getEntryStructures(
            @ToolParam(description = "Entry accession, e.g. 'IPR000001' or 'PF00069'.")
            String accession,
            @ToolParam(required = false, description = "Structures per page (default 20, max 100).")
            Integer pageSize) {
        String acc = accession == null ? "" : accession.trim();
        if (acc.isEmpty()) {
            return "{\"error\":\"Provide an entry accession, e.g. IPR000001.\"}";
        }
        String raw = client.entryStructures(acc, pageSize);
        if (ApiClient.isError(raw)) {
            return raw;
        }

        try {
            JsonNode root = mapper.readTree(raw);
            ObjectNode out = mapper.createObjectNode();
            out.put("accession", acc);
            out.put("count", root.path("count").asLong(0));
            if (root.hasNonNull("next")) {
                out.put("next", root.path("next").asText());
            }
            ArrayNode structures = out.putArray("structures");
            for (JsonNode r : root.path("results")) {
                JsonNode meta = r.path("metadata");
                ObjectNode s = mapper.createObjectNode();
                s.put("accession", meta.path("accession").asText());
                s.put("name", meta.path("name").asText(null));
                s.put("experiment_type", meta.path("experiment_type").asText(null));
                if (meta.hasNonNull("resolution")) {
                    s.put("resolution", meta.path("resolution").asDouble());
                }
                ArrayNode locs = s.putArray("locations");
                for (JsonNode entry : r.path("entries")) {
                    for (JsonNode loc : entry.path("entry_protein_locations")) {
                        for (JsonNode frag : loc.path("fragments")) {
                            ObjectNode f = mapper.createObjectNode();
                            f.put("start", frag.path("start").asInt());
                            f.put("end", frag.path("end").asInt());
                            locs.add(f);
                        }
                    }
                }
                structures.add(s);
            }
            return mapper.writeValueAsString(out);
        } catch (Exception e) {
            return raw; // fall back to the raw API response on any parse issue
        }
    }
}
