package app.juntos.alpha.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;

/**
 * JAX-RS request filter that enforces AT Protocol JWT authentication on all XRPC endpoints.
 * Delegates all verification logic to {@link TokenVerifier}; this class only handles
 * HTTP concerns: extracting the bearer token and returning 401 on failure.
 */
@Provider
@Slf4j
public class AtprotoAuthFilter implements ContainerRequestFilter {

    public static final String VIEWER_DID_HEADER = "X-Viewer-DID";

    @Inject
    TokenVerifier tokenVerifier;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String method = ctx.getRequest().getMethod();
        String path = ctx.getUriInfo().getPath();

        if (shouldSkip(method, path)) return;

        String authHeader = ctx.getHeaderString("Authorization");
        if (authHeader == null) {
            abort(ctx, "MissingAuthHeader", "Missing Authorization header");
            return;
        }
        if (!authHeader.startsWith("Bearer ")) {
            abort(ctx, "InvalidAuthHeader", "Authorization header must use Bearer scheme");
            return;
        }

        String token = authHeader.substring(7);
        try {
            String did = tokenVerifier.verify(token);
            ctx.getHeaders().putSingle(VIEWER_DID_HEADER, did);
            log.debug("[AUTH] verified — viewer DID: {}", did);
        } catch (Exception e) {
            log.warn("[AUTH] rejected — {}", e.getMessage());
            abort(ctx, "AuthenticationFailed", "Authentication failed");
        }
    }

    private void abort(ContainerRequestContext ctx, String error, String message) {
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("error", error, "message", message))
                .build());
    }

    private boolean shouldSkip(String method, String path) {
        return "OPTIONS".equalsIgnoreCase(method)
                || path.equals("/")
                || path.equals("/ping")
                || path.startsWith("/health")
                || path.startsWith("/metrics")
                || path.startsWith("/api/auth");
    }
}
