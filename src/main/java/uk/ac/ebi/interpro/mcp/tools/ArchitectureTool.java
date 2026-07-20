package uk.ac.ebi.interpro.mcp.tools;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** Domain-architecture (IDA) tools: the architectures a single entry occurs in, and search by organisation. */
@Component
public class ArchitectureTool {

    private final InterProClient client;
    private final ObjectMapper mapper;

    public ArchitectureTool(InterProClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Tool(name = "getDomainArchitectures",
          description = """
                  List the distinct domain architectures (InterPro Domain Architectures, IDAs) in which an
                  entry occurs - i.e. the recurring ordered domain combinations it is part of across UniProt
                  proteins. For each architecture it returns the domain composition (the 'ida' string, member-DB
                  signature : InterPro entry, '-'-separated in N->C order), a representative protein, and how many
                  proteins share that architecture (unique_proteins). Input is an entry accession (e.g. IPR000007
                  or PF01167). Use to understand the structural/functional context a domain appears in. To go the
                  other way - find the architectures matching a set of domains - use searchDomainArchitectures.""")
    public String getDomainArchitectures(
            @ToolParam(description = "Entry accession, e.g. 'IPR000007' or 'PF01167'.")
            String accession,
            @ToolParam(required = false, description = "Architectures per page (default 20, max 100).")
            Integer pageSize) {
        String acc = accession == null ? "" : accession.trim();
        if (acc.isEmpty()) {
            return "{\"error\":\"Provide an entry accession, e.g. IPR000007.\"}";
        }

        String raw = client.domainArchitectures(acc, pageSize);
        if (ApiClient.isError(raw)) {
            return raw;
        }

        try {
            JsonNode root = mapper.readTree(raw);
            ObjectNode out = mapper.createObjectNode();
            out.put("accession", acc);
            addArchitectures(root, out);
            return mapper.writeValueAsString(out);
        } catch (Exception e) {
            return raw; // fall back to the raw API response on any parse issue
        }
    }

    @Tool(name = "searchDomainArchitectures",
          description = """
                  Search for the domain architectures (InterPro Domain Architectures, IDAs) that MATCH a given
                  domain organisation - i.e. find the architectures, and how many proteins have them, that are
                  built from a specified set of domains. This is the inverse of getDomainArchitectures: instead of
                  the architectures ONE entry occurs in, you give two or more domain accessions and get every
                  architecture containing them. Input Pfam and/or InterPro accessions (e.g. 'PF00051,PF00089' or
                  'IPR000001-IPR001254'). By default any architecture containing all the given domains matches, in
                  any order and possibly with others; set 'ordered' to require the given N->C order, and 'exact' to
                  require exactly those domains and no others. Returns the number of matching architectures and, for
                  each, its domain composition (the 'ida' string), a representative protein and how many proteins
                  share it (unique_proteins).""")
    public String searchDomainArchitectures(
            @ToolParam(description = """
                    Two or more domain accessions defining the organisation to search for - Pfam (PFxxxxx) and/or
                    InterPro (IPRxxxxxx) - separated by commas or dashes, e.g. 'PF00051,PF00089' or
                    'IPR000001-IPR001254'. When 'ordered' is true they are read in N->C order.""")
            String domains,
            @ToolParam(required = false,
                    description = "Require the domains to occur in the given N->C order (default false = any order).")
            Boolean ordered,
            @ToolParam(required = false,
                    description = "Match only architectures that contain EXACTLY these domains and no others "
                            + "(default false = additional domains allowed).")
            Boolean exact,
            @ToolParam(required = false, description = "Architectures per page (default 20, max 100).")
            Integer pageSize) {
        String query = normalizeDomains(domains);
        if (query == null) {
            return "{\"error\":\"Provide one or more domain accessions (Pfam PFxxxxx or InterPro IPRxxxxxx), "
                    + "e.g. PF00051,PF00089.\"}";
        }
        boolean ord = ordered != null && ordered;
        boolean exa = exact != null && exact;

        String raw = client.searchDomainArchitectures(query, ord, exa, pageSize);
        if (ApiClient.isError(raw)) {
            return raw;
        }

        try {
            JsonNode root = mapper.readTree(raw);
            ObjectNode out = mapper.createObjectNode();
            out.put("query", query);
            out.put("ordered", ord);
            out.put("exact", exa);
            addArchitectures(root, out);
            return mapper.writeValueAsString(out);
        } catch (Exception e) {
            return raw; // fall back to the raw API response on any parse issue
        }
    }

    /** Copy {@code count}, an optional {@code next} cursor, and the projected {@code architectures} array into {@code out}. */
    private void addArchitectures(JsonNode root, ObjectNode out) {
        out.put("count", root.path("count").asInt());
        if (root.hasNonNull("next")) {
            out.put("next", root.path("next").asText());
        }
        ArrayNode architectures = out.putArray("architectures");
        for (JsonNode r : root.path("results")) {
            ObjectNode a = mapper.createObjectNode();
            a.put("ida", r.path("ida").asText());
            a.put("ida_id", r.path("ida_id").asText());
            a.put("unique_proteins", r.path("unique_proteins").asInt());

            JsonNode rep = r.path("representative");
            ObjectNode repOut = a.putObject("representative");
            repOut.put("accession", rep.path("accession").asText());
            repOut.put("length", rep.path("length").asInt());
            ArrayNode doms = repOut.putArray("domains");
            for (JsonNode d : rep.path("domains")) {
                ObjectNode dd = mapper.createObjectNode();
                dd.put("accession", d.path("accession").asText());
                dd.put("name", d.path("name").asText(null));
                doms.add(dd);
            }
            architectures.add(a);
        }
    }

    /**
     * Split a Pfam/InterPro accession list (comma-, dash- or whitespace-separated) into the
     * comma-separated value the {@code ida_search} API expects, e.g. {@code "PF00051-PF00089"} or
     * {@code "pf00051, PF00089"} -> {@code "PF00051,PF00089"}. Returns {@code null} if empty.
     */
    static String normalizeDomains(String domains) {
        if (domains == null || domains.isBlank()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (String token : domains.trim().toUpperCase().split("[,\\-\\s]+")) {
            if (!token.isBlank()) {
                parts.add(token);
            }
        }
        return parts.isEmpty() ? null : String.join(",", parts);
    }
}
