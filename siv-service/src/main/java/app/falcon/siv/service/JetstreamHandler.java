package app.falcon.siv.service;

import app.falcon.siv.ai.AiContextService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Optional;

/**
 * Processes raw AT Protocol Jetstream WebSocket events.
 *
 * <p>
 * On each {@code app.bsky.feed.post} commit, the text is dispatched to
 * {@link AiContextService#processPost} which runs on a virtual thread — this
 * handler
 * never blocks regardless of AI response time.
 * </p>
 */
@Component
@Slf4j
public class JetstreamHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Optional — AI processing is only active when {@code falcon.ai.enabled=true}.
     * If the bean is absent, the handler simply logs events as before.
     */
    @Autowired(required = false)
    private AiContextService aiContextService;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        try {
            JsonNode node = objectMapper.readTree(payload);

            String kind = node.path("kind").asText();
            if ("commit".equals(kind)) {
                String did = node.path("did").asText();
                String collection = node.path("commit").path("collection").asText();
                String operation = node.path("commit").path("operation").asText();

                if ("app.bsky.feed.post".equals(collection) && "create".equals(operation)) {
                    String text = node.path("commit").path("record").path("text").asText();
                    String seq = node.path("seq").asText(null);
                    log.info("🌊 Jetstream Post from {}: {}", did, text);

                    // Dispatch to AI context engine asynchronously (virtual thread, non-blocking)
                    Optional.ofNullable(aiContextService)
                            .ifPresent(ai -> ai.processPost(did, text, seq));
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
