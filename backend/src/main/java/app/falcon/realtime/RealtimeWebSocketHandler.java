package app.falcon.realtime;

import app.falcon.domain.Channel;
import app.falcon.repository.ChannelRepository;
import app.falcon.repository.MemberRepository;
import org.springframework.boot.json.JsonParseException;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Optional;

@Component
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private static final JsonParser JSON_PARSER = JsonParserFactory.getJsonParser();
    private final RealtimeBroker realtimeBroker;
    private final ChannelRepository channelRepository;
    private final MemberRepository memberRepository;

    public RealtimeWebSocketHandler(
            RealtimeBroker realtimeBroker,
            ChannelRepository channelRepository,
            MemberRepository memberRepository) {
        this.realtimeBroker = realtimeBroker;
        this.channelRepository = channelRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String did = userDid(session);
        if (did == null) {
            close(session, CloseStatus.NOT_ACCEPTABLE.withReason("Missing authenticated DID"));
            return;
        }
        realtimeBroker.register(session, did);
        realtimeBroker.sendAck(session, "connected", Map.of("did", did));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Map<String, Object> body;
        try {
            body = JSON_PARSER.parseMap(message.getPayload());
        } catch (JsonParseException e) {
            realtimeBroker.sendError(session, "bad_request", "Invalid JSON payload");
            return;
        }

        String type = asString(body.get("type"));
        if (type == null || type.isBlank()) {
            realtimeBroker.sendError(session, "bad_request", "Missing event type");
            return;
        }

        if ("ping".equals(type)) {
            realtimeBroker.sendAck(session, "pong", Map.of());
            return;
        }

        Long channelId = asLong(body.get("channelId"));
        if (channelId == null) {
            realtimeBroker.sendError(session, "bad_request", "Missing channelId");
            return;
        }

        Optional<Channel> channel = channelRepository.findById(channelId);
        String did = userDid(session);
        if (channel.isEmpty() || did == null || !memberRepository.existsByServerIdAndDid(channel.get().getServer().getId(), did)) {
            realtimeBroker.sendError(session, "forbidden", "Not authorized for channel");
            return;
        }

        switch (type) {
            case "subscribe" -> {
                realtimeBroker.subscribe(session, channelId);
                realtimeBroker.sendAck(session, "subscribed", Map.of("channelId", channelId));
            }
            case "unsubscribe" -> {
                realtimeBroker.unsubscribe(session, channelId);
                realtimeBroker.sendAck(session, "unsubscribed", Map.of("channelId", channelId));
            }
            default -> realtimeBroker.sendError(session, "bad_request", "Unsupported event type");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        realtimeBroker.unregister(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        realtimeBroker.unregister(session);
        close(session, CloseStatus.SERVER_ERROR);
    }

    private String userDid(WebSocketSession session) {
        Object did = session.getAttributes().get(WsAuthHandshakeInterceptor.ATTR_USER_DID);
        if (did instanceof String didValue && !didValue.isBlank()) {
            return didValue;
        }
        return null;
    }

    private String asString(Object value) {
        return value instanceof String s ? s : null;
    }

    private Long asLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private void close(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (Exception ignored) {
            // no-op
        }
    }
}
