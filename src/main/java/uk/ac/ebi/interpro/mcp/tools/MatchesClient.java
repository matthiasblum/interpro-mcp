package uk.ac.ebi.interpro.mcp.tools;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Wrapper for the InterPro Matches API (https://www.ebi.ac.uk/interpro/matches/api).
 * This is a lookup of <em>precomputed</em> matches keyed by the MD5 checksum of the
 * protein sequence (UniParc convention) &mdash; it is not InterProScan.
 */
@Component
public class MatchesClient extends ApiClient {

    private static final Pattern MD5 = Pattern.compile("(?i)^[0-9a-f]{32}$");

    public MatchesClient(@Qualifier("matchesRestClient") RestClient restClient) {
        super(restClient);
    }

    /** Single MD5 lookup. */
    public String byMd5(String md5) {
        return get("/matches/{md5}", md5);
    }

    /** Batch lookup; {@code jsonBody} must be {@code {"md5":[...]}} with 1..100 entries. */
    public String byMd5Batch(String jsonBody) {
        return post("/matches", jsonBody);
    }

    public static boolean isMd5(String s) {
        return s != null && MD5.matcher(s.trim()).matches();
    }

    /** MD5 hex (lowercase) of the sequence with whitespace stripped and upper-cased. */
    public static String md5(String sequence) {
        String clean = sequence.replaceAll("\\s", "").toUpperCase();
        try {
            byte[] digest = MessageDigest.getInstance("MD5")
                    .digest(clean.getBytes(StandardCharsets.US_ASCII));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }
}
