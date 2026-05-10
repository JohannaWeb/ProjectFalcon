package app.juntos.alpha.auth;

import lombok.extern.slf4j.Slf4j;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;

import java.net.InetAddress;
import java.util.Map;

@ApplicationScoped
@Slf4j
public class DidResolver {

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
        Client client = ClientBuilder.newClient();
        try {
            Map<String, Object> doc = client.target(url)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(Map.class);
            log.info("[DID] Resolved {} — keys: {}", did, doc != null ? doc.keySet() : "null");
            return doc;
        } finally {
            client.close();
        }
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
