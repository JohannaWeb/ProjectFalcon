package app.falcon.siv.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@Slf4j
public class JetstreamHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        try {
            JsonNode node = objectMapper.readTree(payload);

            // Jetstream events have a "kind" and "commit" or "identity" etc.
            String kind = node.path("kind").asText();
            if ("commit".equals(kind)) {
                String did = node.path("did").asText();
                String collection = node.path("commit").path("collection").asText();
                String operation = node.path("commit").path("operation").asText();

                if ("app.bsky.feed.post".equals(collection) && "create".equals(operation)) {
                    log.info("🌊 Jetstream Post from {}: {}", did,
                            node.path("commit").path("record").path("text").asText());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse Jetstream message: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("✅ Jetstream connection established!");
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("❌ Jetstream transport error: {}", exception.getMessage());
    }
}
