package uk.ac.ebi.interpro.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** Sequence -> precomputed InterPro matches via the Matches API (MD5 lookup). */
@Component
public class SequenceTool {

    private static final int MAX_BATCH = 100;

    private final MatchesClient client;

    public SequenceTool(MatchesClient client) {
        this.client = client;
    }

    @Tool(name = "matchSequences",
          description = """
                  Look up precomputed InterPro matches for one or more protein sequences (up to 100) via the
                  InterPro Matches API. Provide one amino-acid sequence or 32-character MD5 hex string per line;
                  each raw sequence has its MD5 computed automatically. The response is keyed by MD5 and reports,
                  for each input, whether it was found and its matches (member-database signatures and integrated
                  InterPro entries with locations). NOTE: this is a lookup of precomputed results (NOT
                  InterProScan); only sequences already known to InterPro/UniParc return matches. Use
                  analyzeSequence to analyse a novel sequence.""")
    public String matchSequences(
            @ToolParam(description = "One or more protein sequences or MD5 hex strings, one per line (max 100).")
            String sequencesOrMd5s) {
        if (sequencesOrMd5s == null || sequencesOrMd5s.isBlank()) {
            return "{\"error\":\"Provide one protein sequence or MD5 per line (max 100).\"}";
        }
        Set<String> md5s = new LinkedHashSet<>();
        for (String line : sequencesOrMd5s.split("\\R")) {
            String item = line.trim();
            if (item.isEmpty()) {
                continue;
            }
            md5s.add(MatchesClient.isMd5(item) ? item.toLowerCase() : MatchesClient.md5(item));
            if (md5s.size() >= MAX_BATCH) {
                break;
            }
        }
        if (md5s.isEmpty()) {
            return "{\"error\":\"No sequences or MD5s provided.\"}";
        }
        return client.byMd5Batch(toMd5ArrayJson(md5s));
    }

    /** Build {@code {"md5":["..",".."]}}. MD5 values are [0-9a-f]{32}, so no escaping is needed. */
    private static String toMd5ArrayJson(Set<String> md5s) {
        List<String> quoted = new ArrayList<>(md5s.size());
        for (String m : md5s) {
            quoted.add("\"" + m + "\"");
        }
        return "{\"md5\":[" + String.join(",", quoted) + "]}";
    }
}
