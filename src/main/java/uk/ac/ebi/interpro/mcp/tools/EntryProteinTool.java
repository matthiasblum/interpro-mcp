package uk.ac.ebi.interpro.mcp.tools;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** List the UniProt proteins matched by an InterPro entry / member-database signature. */
@Component
public class EntryProteinTool {

    private final InterProClient client;
    private final ObjectMapper mapper;

    public EntryProteinTool(InterProClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Tool(name = "getEntryProteins",
          description = """
                  List the UniProt proteins matched by an InterPro entry or member-database signature.
                  Complements getTaxonomicDistribution (which only counts): use this to retrieve example/member
                  proteins, optionally restricted to reviewed (Swiss-Prot) proteins and/or a taxon. Input an entry
                  accession (e.g. IPR000001 or PF00069). Returns the total match count and a page of proteins
                  (accession, name, organism, length, whether an AlphaFold model exists, and the matched residue
                  ranges), plus a 'next' cursor URL for the following page.""")
    public String getEntryProteins(
            @ToolParam(description = "Entry accession, e.g. 'IPR000001' or 'PF00069'.")
            String accession,
            @ToolParam(required = false, description = "Restrict to reviewed (Swiss-Prot) proteins only (default false).")
            Boolean reviewedOnly,
            @ToolParam(required = false,
                    description = """
                            Optional NCBI taxon id to restrict to a taxon subtree, e.g. '9606' (human) or
                            '2' (Bacteria).""")
            String taxId,
            @ToolParam(required = false, description = "Proteins per page (default 20, max 100).")
            Integer pageSize) {
        String acc = accession == null ? "" : accession.trim();
        if (acc.isEmpty()) {
            return "{\"error\":\"Provide an entry accession, e.g. IPR000001.\"}";
        }
        boolean reviewed = reviewedOnly != null && reviewedOnly;
        String raw = client.entryProteins(acc, reviewed, taxId, pageSize);
        if (ApiClient.isError(raw)) {
            return raw;
        }

        try {
            JsonNode root = mapper.readTree(raw);
            ObjectNode out = mapper.createObjectNode();
            out.put("accession", acc);
            out.put("reviewed_only", reviewed);
            if (taxId != null && !taxId.isBlank()) {
                out.put("tax_id", taxId.trim());
            }
            out.put("count", root.path("count").asLong(0));
            if (root.hasNonNull("next")) {
                out.put("next", root.path("next").asText());
            }
            ArrayNode proteins = out.putArray("proteins");
            for (JsonNode r : root.path("results")) {
                JsonNode meta = r.path("metadata");
                ObjectNode p = mapper.createObjectNode();
                p.put("accession", meta.path("accession").asText());
                p.put("name", meta.path("name").asText(null));
                p.put("source_database", meta.path("source_database").asText(null));
                p.put("length", meta.path("length").asInt());
                JsonNode org = meta.path("source_organism");
                if (org.isObject()) {
                    ObjectNode o = p.putObject("organism");
                    o.put("taxId", org.path("taxId").asText(null));
                    o.put("scientificName", org.path("scientificName").asText(null));
                }
                if (meta.path("in_alphafold").asBoolean(false)) {
                    p.put("in_alphafold", true);
                }
                ArrayNode locs = p.putArray("locations");
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
                proteins.add(p);
            }
            return mapper.writeValueAsString(out);
        } catch (Exception e) {
            return raw; // fall back to the raw API response on any parse issue
        }
    }
}
