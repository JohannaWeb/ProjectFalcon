package app.falcon.service;

import app.falcon.atproto.XrpcClient;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

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
    public Optional<String> resolveHandle(String handle) {
        if (handle == null || handle.isBlank()) {
            return Optional.empty();
        }
        Map<String, Object> res = xrpc.get("com.atproto.identity.resolveHandle", Map.of("handle", handle.trim()), null);
        Object did = res.get("did");
        if (!(did instanceof String didValue) || didValue.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(didValue);
    }
}