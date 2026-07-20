package uk.ac.ebi.interpro.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** Lookup and browsing of InterPro entries / member-database signatures. */
@Component
public class EntryTool {

    private final InterProClient client;

    public EntryTool(InterProClient client) {
        this.client = client;
    }

    @Tool(name = "getEntry",
          description = """
                  Retrieve full metadata for a single InterPro entry or member-database signature by accession.
                  Works for InterPro accessions (IPRxxxxxx) and member-DB signatures such as Pfam PF00069,
                  PANTHER PTHR11961, CATH-Gene3D G3DSA:1.10.760.10, SMART SM00220 or CDD cd00108. Returns the
                  name, type, description, GO terms, integrated/member signatures, hierarchy, literature,
                  cross-references and counters (proteins/structures/taxa).""")
    public String getEntry(
            @ToolParam(description = "Entry accession, e.g. 'IPR000001' or 'PF00069'.")
            String accession) {
        return client.entry(accession);
    }

    @Tool(name = "browseEntries",
          description = """
                  Browse/filter InterPro (or member-database) entries, paginated. Use to list entries of a
                  given type, from a given member database, or annotated with a given GO term. Returns a page of
                  entries (accession, name, type, member databases) plus a 'next' cursor URL for the following page.""")
    public String browseEntries(
            @ToolParam(required = false,
                    description = """
                            Source database to list from (default 'interpro'), e.g. interpro, pfam, panther,
                            cathgene3d, smart, cdd.""")
            String database,
            @ToolParam(required = false,
                    description = """
                            Entry-type filter: family, domain, homologous_superfamily, repeat, conserved_site,
                            binding_site, active_site, ptm.""")
            String type,
            @ToolParam(required = false, description = "GO term identifier to filter by, e.g. 'GO:0004672'.")
            String goTerm,
            @ToolParam(required = false, description = "Entries per page (default 20, max 100).")
            Integer pageSize) {
        return client.browseEntries(database, type, goTerm, pageSize);
    }

    @Tool(name = "searchByGoTerm",
          description = """
                  Find InterPro entries annotated with a given Gene Ontology (GO) term. Input a GO identifier
                  (e.g. 'GO:0004672', or just '0004672'); returns the InterPro entries (accession, name, type,
                  member databases, GO terms) associated with that term, paginated. Use this to go from a
                  molecular-function / biological-process / cellular-component GO term to the InterPro
                  domains/families that carry it.""")
    public String searchByGoTerm(
            @ToolParam(description = "GO term identifier, e.g. 'GO:0004672'.")
            String goTerm,
            @ToolParam(required = false, description = "Entries per page (default 20, max 100).")
            Integer pageSize) {
        String go = normalizeGoTerm(goTerm);
        if (go == null) {
            return "{\"error\":\"Provide a GO term, e.g. GO:0004672.\"}";
        }
        return client.entriesByGoTerm(go, pageSize);
    }

    /** Normalise to canonical {@code GO:nnnnnnn} form; pad bare numeric ids to 7 digits. */
    static String normalizeGoTerm(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim();
        String digits = t.toUpperCase().startsWith("GO:") ? t.substring(3).trim() : t;
        if (digits.matches("\\d{1,7}")) {
            return String.format("GO:%07d", Integer.parseInt(digits));
        }
        return t;
    }
}
