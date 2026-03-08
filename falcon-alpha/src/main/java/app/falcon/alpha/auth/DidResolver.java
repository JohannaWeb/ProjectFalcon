package app.falcon.alpha.auth;

import app.falcon.alpha.domain.DidDocument;
import app.falcon.alpha.repository.DidDocumentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class DidResolver {

    private final DidDocumentRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    
    private final Cache<String, Map<String, Object>> memoryCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .maximumSize(10_000)
            .build();

    @SuppressWarnings("unchecked")
    public Map<String, Object> resolve(String did) {
        // 1. Try Memory Cache
        Map<String, Object> cached = memoryCache.getIfPresent(did);
        if (cached != null) return cached;

        // 2. Try DB
        Optional<DidDocument> dbDoc = repository.findById(did);
        if (dbDoc.isPresent()) {
            try {
                Map<String, Object> doc = objectMapper.readValue(dbDoc.get().getDocumentJson(), Map.class);
                memoryCache.put(did, doc);
                
                // Trigger async refresh if older than 1 hour (Stale-While-Revalidate)
                if (dbDoc.get().getLastUpdated().isBefore(LocalDateTime.now().minusHours(1))) {
                    refreshAsync(did);
                }
                return doc;
            } catch (JsonProcessingException e) {
                log.error("Failed to parse persisted DID document for {}: {}", did, e.getMessage());
            }
        }

        // 3. Fallback to Network (blocking for first time)
        Map<String, Object> fresh = fetchFromNetwork(did);
        persistAndCache(did, fresh);
        return fresh;
    }

    @Async
    public void refreshAsync(String did) {
        try {
            log.debug("Async refresh for DID: {}", did);
            Map<String, Object> fresh = fetchFromNetwork(did);
            persistAndCache(did, fresh);
        } catch (Exception e) {
            log.error("Background refresh failed for {}: {}", did, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchFromNetwork(String did) {
        String url;
        if (did.startsWith("did:plc:")) {
            url = "https://plc.directory/" + did;
        } else if (did.startsWith("did:web:")) {
            url = "https://" + did.substring(8) + "/.well-known/did.json";
        } else {
            throw new IllegalArgumentException("Unsupported DID method: " + did);
        }
        log.info("Fetching DID document from network: {}", url);
        return (Map<String, Object>) restTemplate.getForObject(url, Map.class);
    }

    private void persistAndCache(String did, Map<String, Object> doc) {
        try {
            String json = objectMapper.writeValueAsString(doc);
            DidDocument entity = DidDocument.builder()
                    .did(did)
                    .documentJson(json)
                    .lastUpdated(LocalDateTime.now())
                    .build();
            repository.save(entity);
            memoryCache.put(did, doc);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize DID document for storage: {}", e.getMessage());
        }
    }
}
