package uk.ac.ebi.interpro.mcp.tools;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Wrapper for the InterPro REST API (https://www.ebi.ac.uk/interpro/api). */
@Component
public class InterProClient extends ApiClient {

    private final EbiSearchClient ebiSearch;

    public InterProClient(@Qualifier("interProRestClient") RestClient restClient, EbiSearchClient ebiSearch) {
        super(restClient);
        this.ebiSearch = ebiSearch;
    }

    /** Full metadata for a single entry. The source database is resolved from the accession. */
    public String entry(String accession) {
        String db = inferDatabase(accession);
        if (db == null) {
            return "Accession '" + accession + "' was not found in InterPro. Check that the accession is correct.";
        }
        return get(uri -> uri.path("/entry/{db}/{accession}/")
                .queryParam("format", "json")
                .build(db, accession));
    }

    /** Browse/filter entries (paginated) by source database, type and/or GO term. */
    public String browseEntries(String database, String type, String goTerm, Integer pageSize) {
        String db = (database == null || database.isBlank()) ? "interpro" : database.trim().toLowerCase();
        int size = (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, 100);
        return get(uri -> uri.path("/entry/{db}/")
                .queryParam("format", "json")
                .queryParamIfPresent("type", opt(type))
                .queryParamIfPresent("go_term", opt(goTerm))
                .queryParam("page_size", size)
                .build(db));
    }

    /** All InterPro/member matches on a UniProt protein, with residue locations. */
    public String proteinEntries(String uniprotAccession) {
        return get(uri -> uri.path("/entry/all/protein/uniprot/{acc}/")
                .queryParam("format", "json")
                .build(uniprotAccession.trim()));
    }

    /** Protein metadata for a UniProt accession, including the amino-acid {@code sequence}. */
    public String protein(String uniprotAccession) {
        return get(uri -> uri.path("/protein/uniprot/{acc}/")
                .queryParam("format", "json")
                .build(uniprotAccession.trim()));
    }

    /** Distinct domain architectures (IDAs) an entry occurs in, paginated. */
    public String domainArchitectures(String accession, Integer pageSize) {
        String db = dbOrInterpro(accession);
        int size = (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, 100);
        return get(uri -> uri.path("/entry/{db}/{acc}/")
                .queryParam("ida", "")
                .queryParam("format", "json")
                .queryParam("page_size", size)
                .build(db, accession.trim()));
    }

    /**
     * Search domain architectures (IDAs) matching a comma-separated set of Pfam/InterPro accessions
     * via the {@code ida_search} modifier on the entry endpoint (returns the matching architectures with
     * protein counts, not the underlying proteins). {@code ordered} requires the given N->C order;
     * {@code exact} requires exactly those domains and no others. Both are presence flags, added only when set.
     */
    public String searchDomainArchitectures(String domains, boolean ordered, boolean exact, Integer pageSize) {
        int size = (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, 100);
        return get(uri -> uri.path("/entry")
                .queryParam("ida_search", domains)
                .queryParam("format", "json")
                .queryParam("page_size", size)
                .queryParamIfPresent("ordered", ordered ? Optional.of("true") : Optional.empty())
                .queryParamIfPresent("exact", exact ? Optional.of("true") : Optional.empty())
                .build());
    }

    /** InterPro entries annotated with a GO term, paginated. */
    public String entriesByGoTerm(String goTerm, Integer pageSize) {
        int size = (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, 100);
        return get(uri -> uri.path("/entry/interpro/")
                .queryParam("go_term", goTerm)
                .queryParam("format", "json")
                .queryParam("page_size", size)
                .build());
    }

    /**
     * Full taxonomic distribution tree for an entry, in one call (the {@code ?taxa} modifier).
     * Returns {@code {"taxa": {id, rank, name, proteins, species, children:[...]}}} rooted at the
     * NCBI root, already pruned to taxa where the entry occurs and structured by NCBI rank
     * (domain/superkingdom -> kingdom -> phylum -> ... -> species). {@code proteins} at a node is
     * the entry's UniProt protein count for that whole subtree; the root's count is the entry total.
     */
    public String entryTaxa(String accession) {
        String db = dbOrInterpro(accession);
        return get(uri -> uri.path("/entry/{db}/{acc}/")
                .queryParam("taxa", "")
                .queryParam("format", "json")
                .build(db, accession.trim()));
    }

    /**
     * UniProt proteins matched by an entry, paginated. {@code reviewedOnly} restricts to
     * Swiss-Prot (reviewed) proteins; an optional {@code taxId} restricts to a taxon subtree.
     */
    public String entryProteins(String accession, boolean reviewedOnly, String taxId, Integer pageSize) {
        String db = dbOrInterpro(accession);
        String proteinDb = reviewedOnly ? "reviewed" : "uniprot";
        int size = (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, 100);
        String tax = (taxId == null || taxId.isBlank()) ? null : taxId.trim();
        if (tax != null) {
            return get(uri -> uri.path("/protein/{pdb}/entry/{db}/{acc}/taxonomy/uniprot/{taxId}/")
                    .queryParam("format", "json")
                    .queryParam("page_size", size)
                    .build(proteinDb, db, accession.trim(), tax));
        }
        return get(uri -> uri.path("/protein/{pdb}/entry/{db}/{acc}/")
                .queryParam("format", "json")
                .queryParam("page_size", size)
                .build(proteinDb, db, accession.trim()));
    }

    /** PDB structures containing an entry, paginated (count in {@code count}). */
    public String entryStructures(String accession, Integer pageSize) {
        String db = dbOrInterpro(accession);
        int size = (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, 100);
        return get(uri -> uri.path("/structure/pdb/entry/{db}/{acc}/")
                .queryParam("format", "json")
                .queryParam("page_size", size)
                .build(db, accession.trim()));
    }

    private static Optional<String> opt(String s) {
        return Optional.ofNullable(s).filter(v -> !v.isBlank());
    }

    /**
     * Resolve a source database for the exploratory tools: inferred from the accession,
     * else {@code interpro} (these tools are primarily used with InterPro entries).
     */
    String dbOrInterpro(String accession) {
        String resolved = inferDatabase(accession);
        return resolved != null ? resolved : "interpro";
    }

    /**
     * Resolve an accession's source database: the prefix heuristic first (free, unambiguous for
     * every member DB except PROSITE), then an authoritative EBI Search id lookup as a fallback
     * (which disambiguates PROSITE patterns vs profiles and covers unknown prefixes). Returns
     * {@code null} only when the accession is not found in InterPro at all.
     */
    String inferDatabase(String accession) {
        String db = resolveDatabase(accession);
        return db != null ? db : ebiSearch.databaseFor(accession);
    }

    /**
     * Best-effort mapping from an accession to its InterPro source-database name by prefix.
     * Returns {@code null} when ambiguous (e.g. PROSITE PSxxxxx, which may be a 'prosite'
     * pattern or a 'profile'); {@link #inferDatabase(String)} then falls back to EBI Search.
     */
    static String resolveDatabase(String accession) {
        if (accession == null || accession.isBlank()) {
            return null;
        }
        String a = accession.trim().toUpperCase();
        if (a.startsWith("IPR")) return "interpro";
        if (a.startsWith("PTHR")) return "panther";
        if (a.startsWith("PIRSF")) return "pirsf";
        if (a.startsWith("PF")) return "pfam";
        if (a.startsWith("G3DSA")) return "cathgene3d";
        if (a.startsWith("SFLD")) return "sfld";
        if (a.startsWith("SM")) return "smart";
        if (a.startsWith("CD")) return "cdd";
        if (a.startsWith("PR")) return "prints";
        if (a.startsWith("TIGR") || a.startsWith("NF")) return "ncbifam";
        if (a.startsWith("MF_")) return "hamap";
        return null; // ambiguous or unknown prefix: inferDatabase() falls back to EBI Search
    }
}
