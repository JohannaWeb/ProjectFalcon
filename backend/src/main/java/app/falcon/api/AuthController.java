package app.falcon.api;

import app.falcon.atproto.AtprotoException;
import app.falcon.atproto.XrpcClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Proxy/auth endpoints. Electron app can use AT Protocol directly via @atproto/api;
 * this controller is for optional server-side validation or proxy.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // restrict in production
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
     * Validate an access JWT by calling getProfile (authenticated).
     * Returns 200 with profile if valid, 401 otherwise.
     */
    @GetMapping("/atproto/validate")
    public Mono<ResponseEntity<Map<String, Object>>> validate(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
        if (token == null || token.isBlank()) {
            return Mono.just(ResponseEntity.status(401).build());
        }
        return xrpc.get("com.atproto.server.getSession", Map.of(), token)
                .map(ResponseEntity::ok)
                .onErrorResume(AtprotoException.class, e -> Mono.just(ResponseEntity.status(e.getStatusCode()).build()));
    }
}
