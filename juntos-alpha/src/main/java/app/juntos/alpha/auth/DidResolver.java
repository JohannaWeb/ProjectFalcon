package app.juntos.alpha.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.util.Map;

@Service
@Slf4j
public class DidResolver {

    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("unchecked")
    public Map<String, Object> resolve(String did) {
        String url;
        if (did.startsWith("did:plc:")) {
            url = "https://plc.directory/" + did;
        } else if (did.startsWith("did:web:")) {
            String host = did.substring(8).split("/")[0];
            validateHostNotInternal(host);
            url = "https://" + did.substring(8) + "/.well-known/did.json";
        } else {
            throw new IllegalArgumentException("Unsupported DID method: " + did);
        }
        log.info("[DID] Resolving {} → GET {}", did, url);
        Map<String, Object> doc = (Map<String, Object>) restTemplate.getForObject(url, Map.class);
        log.info("[DID] Resolved {} — keys: {}", did, doc != null ? doc.keySet() : "null");
        return doc;
    }

    private void validateHostNotInternal(String host) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                        || addr.isLinkLocalAddress() || addr.isAnyLocalAddress()) {
                    throw new IllegalArgumentException("DID host resolves to internal network address: " + host);
                }
            }
        } catch (java.net.UnknownHostException e) {
            throw new IllegalArgumentException("Cannot resolve DID host: " + host);
        }
    }
}
