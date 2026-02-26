package app.falcon.realtime;

import app.falcon.domain.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RealtimeBroker {

    private final ConcurrentHashMap<String, SessionState> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Set<String>> subscribersByChannel = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RealtimeBroker() {
    }

    public void register(WebSocketSession session, String userDid) {
        sessions.put(session.getId(), new SessionState(session, userDid));
    }

    public void unregister(WebSocketSession session) {
        SessionState state = sessions.remove(session.getId());
        if (state == null) {
            return;
        }
        for (Long channelId : state.channelIds()) {
            Set<String> channelSubs = subscribersByChannel.get(channelId);
            if (channelSubs == null) {
                continue;
            }
            channelSubs.remove(session.getId());
            if (channelSubs.isEmpty()) {
                subscribersByChannel.remove(channelId);
            }
        }
    }

    public void subscribe(WebSocketSession session, Long channelId) {
        SessionState state = sessions.get(session.getId());
        if (state == null) {
            return;
        }
        state.channelIds().add(channelId);
        subscribersByChannel.computeIfAbsent(channelId, ignored -> ConcurrentHashMap.newKeySet()).add(session.getId());
    }

    public void unsubscribe(WebSocketSession session, Long channelId) {
        SessionState state = sessions.get(session.getId());
        if (state != null) {
            state.channelIds().remove(channelId);
        }
        Set<String> channelSubs = subscribersByChannel.get(channelId);
        if (channelSubs == null) {
            return;
        }
        channelSubs.remove(session.getId());
        if (channelSubs.isEmpty()) {
            subscribersByChannel.remove(channelId);
        }
    }

    public void publishMessageCreated(Long channelId, Message msg) {
        Set<String> subscriberIds = subscribersByChannel.get(channelId);
        if (subscriberIds == null || subscriberIds.isEmpty()) {
            return;
        }

        Map<String, Object> event = Map.of(
                "v", 1,
                "eventId", "evt_" + UUID.randomUUID(),
                "seq", msg.getId(),
                "ts", Instant.now().toString(),
                "type", "message.created",
                "channelId", channelId,
                "actorDid", msg.getAuthorDid(),
                "payload", Map.of(
                        "id", msg.getId(),
                        "content", msg.getContent(),
                        "authorDid", msg.getAuthorDid(),
                        "authorHandle", msg.getAuthorHandle() != null ? msg.getAuthorHandle() : "",
                        "createdAt", msg.getCreatedAt().toString()));
        String json = toJson(event);
        if (json == null) {
            return;
        }
        for (String sessionId : subscriberIds) {
            SessionState state = sessions.get(sessionId);
            if (state == null) {
                continue;
            }
            send(state.session(), json);
        }
    }

    public void sendAck(WebSocketSession session, String type, Map<String, Object> data) {
        Map<String, Object> event = Map.of(
                "v", 1,
                "eventId", "evt_" + UUID.randomUUID(),
                "ts", Instant.now().toString(),
                "type", type,
                "payload", data);
        String json = toJson(event);
        if (json != null) {
            send(session, json);
        }
    }

    public void sendError(WebSocketSession session, String code, String message) {
        sendAck(session, "error", Map.of("code", code, "message", message));
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return null;
        }
    }

    private void send(WebSocketSession session, String json) {
        if (!session.isOpen()) {
            unregister(session);
            return;
        }
        synchronized (session) {
            try {
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                unregister(session);
            }
        }
    }

    private record SessionState(WebSocketSession session, String userDid, Set<Long> channelIds) {
        private SessionState(WebSocketSession session, String userDid) {
            this(session, userDid, ConcurrentHashMap.newKeySet());
        }
    }
}
