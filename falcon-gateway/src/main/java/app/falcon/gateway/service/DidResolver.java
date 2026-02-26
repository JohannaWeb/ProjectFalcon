package app.falcon.gateway.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class DidResolver {

    private final WebClient webClient;
    private final Cache<String, Map<String, Object>> documentCache;

    public DidResolver(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
        // Cache DID documents for 1 hour to ensure "instantaneous" subsequent logins
        this.documentCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(1))
                .maximumSize(10_000)
                .build();
    }

    public Mono<Map<String, Object>> resolve(String did) {
        Map<String, Object> cached = documentCache.getIfPresent(did);
        if (cached != null) {
            return Mono.just(cached);
        }

        String url;
        if (did.startsWith("did:plc:")) {
            url = "https://plc.directory/" + did;
        } else if (did.startsWith("did:web:")) {
            url = "https://" + did.substring(8) + "/.well-known/did.json";
        } else {
            return Mono.error(new IllegalArgumentException("Unsupported DID method: " + did));
        }

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .map(map -> (Map<String, Object>) map)
                .doOnNext(doc -> documentCache.put(did, doc))
                .doOnError(e -> log.error("Failed to resolve DID {}: {}", did, e.getMessage()))
                .cache(); // Prevent multiple concurrent inflight requests for same DID
    }
}
