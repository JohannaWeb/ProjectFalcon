package app.juntos.alpha.auth;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Discovers the DID of an AT Protocol PDS by calling com.atproto.server.describeServer.
 * Results are cached for the lifetime of the application — PDS DIDs are stable.
 *
 * This is needed because Bluesky-hosted PDSes (including EU servers) are identified by
 * a did:plc, not a did:web, and do not host a .well-known/did.json at their hostname.
 */
@ApplicationScoped
@Slf4j
public class PdsDiscoveryService {

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    /**
     * Returns the DID for the given PDS host.
     * Falls back to did:web:{host} if describeServer is unreachable.
     */
    public String discover(String host) {
        return cache.computeIfAbsent(host, this::fetch);
    }

    private String fetch(String host) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + host + "/xrpc/com.atproto.server.describeServer"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) {
                String did = extractJsonString(resp.body(), "did");
                if (did != null && did.startsWith("did:")) {
                    log.info("[PDS-DISCOVERY] {} → {}", host, did);
                    return did;
                }
            }
            log.warn("[PDS-DISCOVERY] describeServer for {} returned {}", host, resp.statusCode());
        } catch (Exception e) {
            log.warn("[PDS-DISCOVERY] describeServer unreachable for {}: {}", host, e.getMessage());
        }

        String fallback = "did:web:" + host;
        log.info("[PDS-DISCOVERY] {} → {} (fallback)", host, fallback);
        return fallback;
    }

    private String extractJsonString(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start < 0) return null;
        start += key.length();
        int end = json.indexOf('"', start);
        return end < 0 ? null : json.substring(start, end);
    }
}
