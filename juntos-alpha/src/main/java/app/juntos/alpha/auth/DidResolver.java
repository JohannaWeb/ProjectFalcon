package app.juntos.alpha.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class DidResolver {

    private final RestTemplate restTemplate = new RestTemplate();
    private final Cache<String, Map<String, Object>> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .maximumSize(10_000)
            .build();

    @SuppressWarnings("unchecked")
    public Map<String, Object> resolve(String did) {
        return cache.get(did, k -> {
            String url;
            if (k.startsWith("did:plc:")) {
                url = "https://plc.directory/" + k;
            } else if (k.startsWith("did:web:")) {
                url = "https://" + k.substring(8) + "/.well-known/did.json";
            } else {
                throw new IllegalArgumentException("Unsupported DID method: " + k);
            }
            log.debug("Resolving DID document: {}", url);
            return (Map<String, Object>) restTemplate.getForObject(url, Map.class);
        });
    }
}
