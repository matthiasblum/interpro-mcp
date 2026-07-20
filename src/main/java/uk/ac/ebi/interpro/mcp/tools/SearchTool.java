package uk.ac.ebi.interpro.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** Text/keyword discovery over InterPro via EBI Search. */
@Component
public class SearchTool {

    private static final String FIELDS = "id,name,type,description,source_database";

    private final EbiSearchClient client;

    public SearchTool(EbiSearchClient client) {
        this.client = client;
    }

    @Tool(name = "searchInterPro",
          description = """
                  Free-text/keyword search of InterPro and its member-database signatures via EBI Search.
                  Use this when you do NOT know an accession - e.g. 'zinc finger', 'kinase domain', 'cytochrome c'.
                  Returns matching entries with their accession (id), name, type (family/domain/...), short
                  description and source_database. Follow up with getEntry for full details of a hit.""")
    public String searchInterPro(
            @ToolParam(description = """
                    Free-text query, e.g. 'zinc finger'. Field syntax is allowed,
                    e.g. 'kinase AND type:domain'.""")
            String query,
            @ToolParam(required = false,
                    description = """
                            Optional entry-type filter: family, domain, homologous_superfamily, repeat,
                            conserved_site, binding_site, active_site, ptm.""")
            String type,
            @ToolParam(required = false, description = "Max results to return (default 10, max 100).")
            Integer size) {
        String q = (type == null || type.isBlank()) ? query : query + " AND type:" + type.trim();
        int n = (size == null) ? 10 : Math.max(1, Math.min(size, 100));
        return client.searchInterpro(q, n, FIELDS);
    }
}
