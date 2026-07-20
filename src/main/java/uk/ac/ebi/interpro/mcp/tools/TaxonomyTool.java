package uk.ac.ebi.interpro.mcp.tools;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Taxonomic distribution of an InterPro entry across the NCBI taxonomy tree.
 * Built from the InterPro {@code ?taxa} modifier, which returns the entry's whole
 * distribution tree (already pruned to taxa where it occurs) in one call. The caller
 * chooses how many levels deep to report; the full tree (down to species) is the default.
 */
@Component
public class TaxonomyTool {

    private final InterProClient client;
    private final ObjectMapper mapper;

    public TaxonomyTool(InterProClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Tool(name = "getTaxonomicDistribution",
          description = """
                  Show how an InterPro entry (or member-database signature) is distributed across the NCBI
                  taxonomy, as a tree of UniProt protein counts. The tree starts at the superkingdoms/domains
                  (Eukaryota, Bacteria, Archaea, Viruses) and descends by rank (kingdom, phylum, class, order,
                  family, genus, species). Use to answer 'is this domain bacterial or eukaryote-specific?' (read
                  the superkingdom level) or to drill into where exactly it occurs. Input an entry accession (e.g.
                  IPR000719 or PF00069). 'depth' controls how many levels are returned: 1 = superkingdoms only, 2
                  adds kingdoms, and so on; omit it for the FULL tree down to species. The full tree can be very
                  large for ubiquitous domains (tens of thousands of nodes) - pass a small 'depth' (e.g. 1 or 2)
                  and/or 'minProteins' when you only need a high-level answer.""")
    public String getTaxonomicDistribution(
            @ToolParam(description = "Entry accession, e.g. 'IPR000719' or 'PF00069'.")
            String accession,
            @ToolParam(required = false,
                    description = """
                            How many taxonomic levels to return, counting the superkingdom/domain level as 1
                            (2 adds kingdoms, 3 phyla, ...). Omit for the full tree down to species.""")
            Integer depth,
            @ToolParam(required = false,
                    description = """
                            Only include taxa with at least this many matching proteins (prunes the long tail
                            of rare lineages). Default 0 (include all).""")
            Integer minProteins) {
        String acc = accession == null ? "" : accession.trim();
        if (acc.isEmpty()) {
            return "{\"error\":\"Provide an entry accession, e.g. IPR000719.\"}";
        }
        String db = client.dbOrInterpro(acc);

        String raw = client.entryTaxa(acc);
        if (ApiClient.isError(raw)) {
            return raw;
        }

        int maxDepth = (depth == null) ? Integer.MAX_VALUE : Math.max(1, depth);
        long minP = (minProteins == null || minProteins < 0) ? 0 : minProteins;

        try {
            JsonNode root = mapper.readTree(raw).path("taxa");
            long total = root.path("proteins").asLong(0);

            ObjectNode out = mapper.createObjectNode();
            out.put("accession", acc);
            out.put("name", entryName(acc));
            out.put("source_database", db);
            out.put("total_proteins", total);
            out.put("total_species", root.path("species").asLong(0));
            if (depth == null) {
                out.put("depth", "full");
            } else {
                out.put("depth", maxDepth);
            }
            if (minP > 0) {
                out.put("min_proteins", minP);
            }

            int[] nodeCount = {0};
            ArrayNode dist = out.putArray("distribution");
            for (JsonNode child : root.path("children")) {
                if (child.path("proteins").asLong(0) >= minP) {
                    dist.add(project(child, 1, maxDepth, minP, total, nodeCount));
                }
            }
            out.put("node_count", nodeCount[0]);
            if (depth == null) {
                out.put("note", "Full tree to species level. Pass 'depth' (1 = superkingdoms, 2 adds kingdoms, ...) "
                        + "or 'minProteins' to summarise. Subtree counts roll up, so a parent's count includes its "
                        + "children's.");
            }
            return mapper.writeValueAsString(out);
        } catch (Exception e) {
            return "{\"error\":\"Failed to compute taxonomic distribution for '" + acc + "': " + e.getMessage() + "\"}";
        }
    }

    /** Recursively project a taxon node, descending until {@code level} reaches {@code maxDepth}. */
    private ObjectNode project(JsonNode node, int level, int maxDepth, long minP, long total, int[] nodeCount) {
        nodeCount[0]++;
        long proteins = node.path("proteins").asLong(0);

        ObjectNode o = mapper.createObjectNode();
        o.put("taxId", node.path("id").asText());
        String name = node.path("name").asText(null);
        o.put("name", (name == null || name.isBlank()) ? "unclassified" : name);
        if (node.hasNonNull("rank")) {
            o.put("rank", node.path("rank").asText());
        }
        o.put("proteins", proteins);
        if (total > 0) {
            o.put("percent", Math.round(10000.0 * proteins / total) / 100.0);
        }
        o.put("species", node.path("species").asLong(0));

        if (level < maxDepth) {
            ArrayNode children = mapper.createArrayNode();
            for (JsonNode child : node.path("children")) {
                if (child.path("proteins").asLong(0) >= minP) {
                    children.add(project(child, level + 1, maxDepth, minP, total, nodeCount));
                }
            }
            if (!children.isEmpty()) {
                o.set("children", children);
            }
        }
        return o;
    }

    /** Best-effort entry name for the summary (the taxa tree itself doesn't carry it). */
    private String entryName(String accession) {
        try {
            String entryJson = client.entry(accession);
            if (!ApiClient.isError(entryJson)) {
                JsonNode nameNode = mapper.readTree(entryJson).path("metadata").path("name");
                return nameNode.isObject() ? nameNode.path("name").asText(null) : nameNode.asText(null);
            }
        } catch (Exception ignored) {
            // name is optional; fall through
        }
        return null;
    }
}
