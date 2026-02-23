package app.falcon.service;

import app.falcon.atproto.AtprotoException;
import app.falcon.atproto.XrpcClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Resolve Bluesky/AT Protocol handle to DID via XRPC.
 */
@Service
public class IdentityService {

    private final XrpcClient xrpc;

    public IdentityService(XrpcClient xrpc) {
        this.xrpc = xrpc;
    }

    /**
     * Resolve a handle (e.g. user.bsky.social) to a DID.
     * No auth required for resolveHandle.
     */
    public Mono<String> resolveHandle(String handle) {
        if (handle == null || handle.isBlank()) return Mono.empty();
        return xrpc.get("com.atproto.identity.resolveHandle", Map.of("handle", handle.trim()), null)
                .map(res -> (String) res.get("did"))
                .filter(did -> did != null && !did.isBlank());
    }
}
