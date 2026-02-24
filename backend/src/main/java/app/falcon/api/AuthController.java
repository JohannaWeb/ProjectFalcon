package app.falcon.api;

import app.falcon.atproto.AtprotoException;
import app.falcon.atproto.XrpcClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Proxy/auth endpoints. Electron app can use AT Protocol directly via @atproto/api;
 * this controller is for optional server-side validation or proxy.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173}")
public class AuthController {

    private final XrpcClient xrpc;

    public AuthController(XrpcClient xrpc) {
        this.xrpc = xrpc;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "Falcon backend");
    }

    /**
     * Validate an access JWT by calling getSession (authenticated).
     * Returns 200 with profile if valid, 401 otherwise.
     */
    @GetMapping("/atproto/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        try {
            return ResponseEntity.ok(xrpc.get("com.atproto.server.getSession", Map.of(), token));
        } catch (AtprotoException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }
}
