package app.falcon.api;

import app.falcon.atproto.AtprotoException;
import app.falcon.atproto.XrpcClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
public class AtprotoAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_USER_DID = "auth.userDid";
    public static final String ATTR_USER_HANDLE = "auth.userHandle";

    private final XrpcClient xrpcClient;

    public AtprotoAuthFilter(XrpcClient xrpcClient) {
        this.xrpcClient = xrpcClient;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/servers") || path.startsWith("/api/channels")) {
            return false;
        }
        if (path.startsWith("/xrpc/app.falcon.")) {
            return false;
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        String token = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;

        if (token == null || token.isBlank()) {
            unauthorized(response, "Missing bearer token");
            return;
        }

        try {
            Map<String, Object> session = xrpcClient.get("com.atproto.server.getSession", Map.of(), token);
            Object did = session.get("did");
            if (!(did instanceof String didValue) || didValue.isBlank()) {
                unauthorized(response, "Invalid token session");
                return;
            }

            request.setAttribute(ATTR_USER_DID, didValue);
            Object handle = session.get("handle");
            if (handle instanceof String handleValue && !handleValue.isBlank()) {
                request.setAttribute(ATTR_USER_HANDLE, handleValue);
            }

            filterChain.doFilter(request, response);
        } catch (AtprotoException e) {
            int status = e.getStatusCode() > 0 ? e.getStatusCode() : HttpServletResponse.SC_UNAUTHORIZED;
            response.setStatus(status);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
        }
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
