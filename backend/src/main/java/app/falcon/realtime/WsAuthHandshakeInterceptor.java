package app.falcon.realtime;

import app.falcon.atproto.AtprotoException;
import app.falcon.atproto.XrpcClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Component
public class WsAuthHandshakeInterceptor implements HandshakeInterceptor {

    static final String ATTR_USER_DID = "auth.userDid";
    static final String ATTR_USER_HANDLE = "auth.userHandle";

    private final XrpcClient xrpcClient;

    public WsAuthHandshakeInterceptor(XrpcClient xrpcClient) {
        this.xrpcClient = xrpcClient;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            Map<String, Object> session = xrpcClient.get("com.atproto.server.getSession", Map.of(), token);
            Object did = session.get("did");
            if (!(did instanceof String didValue) || didValue.isBlank()) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            attributes.put(ATTR_USER_DID, didValue);
            Object handle = session.get("handle");
            if (handle instanceof String handleValue && !handleValue.isBlank()) {
                attributes.put(ATTR_USER_HANDLE, handleValue);
            }
            return true;
        } catch (AtprotoException e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String extractToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String fromParam = servletRequest.getServletRequest().getParameter("token");
            if (fromParam != null && !fromParam.isBlank()) {
                return fromParam;
            }
        }
        List<String> authHeaders = request.getHeaders().get("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }
        String auth = authHeaders.get(0);
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }
}
