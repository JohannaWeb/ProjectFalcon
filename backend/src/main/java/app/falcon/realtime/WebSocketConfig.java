package app.falcon.realtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RealtimeWebSocketHandler realtimeWebSocketHandler;
    private final WsAuthHandshakeInterceptor wsAuthHandshakeInterceptor;
    private final String[] allowedOrigins;

    public WebSocketConfig(
            RealtimeWebSocketHandler realtimeWebSocketHandler,
            WsAuthHandshakeInterceptor wsAuthHandshakeInterceptor,
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        this.realtimeWebSocketHandler = realtimeWebSocketHandler;
        this.wsAuthHandshakeInterceptor = wsAuthHandshakeInterceptor;
        this.allowedOrigins = splitOrigins(allowedOrigins);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeWebSocketHandler, "/ws")
                .addInterceptors(wsAuthHandshakeInterceptor)
                .setAllowedOriginPatterns(allowedOrigins);
    }

    private String[] splitOrigins(String originsCsv) {
        return originsCsv == null ? new String[0] :
                java.util.Arrays.stream(originsCsv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toArray(String[]::new);
    }
}
