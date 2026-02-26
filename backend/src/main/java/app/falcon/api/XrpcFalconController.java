package app.falcon.api;

import app.falcon.atproto.AtprotoException;
import app.falcon.domain.Channel;
import app.falcon.domain.Member;
import app.falcon.domain.Message;
import app.falcon.domain.Server;
import app.falcon.realtime.RealtimeBroker;
import app.falcon.repository.ChannelRepository;
import app.falcon.repository.MemberRepository;
import app.falcon.repository.MessageRepository;
import app.falcon.repository.ServerRepository;
import app.falcon.service.IdentityService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * XRPC endpoint for Falcon lexicons (app.falcon.*).
 * All requests require Authorization: Bearer &lt;AT access JWT&gt;.
 */
@RestController
@RequestMapping("/xrpc")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173}")
public class XrpcFalconController {

    private final ServerRepository serverRepository;
    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;
    private final IdentityService identityService;
    private final RealtimeBroker realtimeBroker;

    @GetMapping(value = "/{nsid:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> query(
            @PathVariable String nsid,
            @RequestParam Map<String, String> params,
            HttpServletRequest request) {
        String userDid = userDid(request);

        return switch (nsid) {
            case "app.falcon.server.list" -> listServers(userDid);
            case "app.falcon.server.get" -> getServer(paramLong(params, "serverId"), userDid);
            case "app.falcon.channel.list" -> listChannels(paramLong(params, "serverId"), userDid);
            case "app.falcon.channel.getMessages" -> getMessages(
                    paramLong(params, "channelId"),
                    paramInt(params, "limit", 50),
                    userDid);
            default -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "UnknownMethod", "message", "Unknown XRPC: " + nsid));
        };
    }

    @PostMapping(value = "/{nsid:.+}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> procedure(
            @PathVariable String nsid,
            @RequestParam Map<String, String> params,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        String userDid = userDid(request);
        String userHandle = userHandle(request).orElse(null);
        if (body == null)
            body = Map.of();

        return switch (nsid) {
            case "app.falcon.server.create" -> createServer(userDid, userHandle, body);
            case "app.falcon.server.invite" -> inviteToServer(paramLong(params, "serverId"), userDid, body);
            case "app.falcon.channel.create" -> createChannel(paramLong(params, "serverId"), userDid, body);
            case "app.falcon.channel.postMessage" ->
                postMessage(paramLong(params, "channelId"), userDid, userHandle, body);
            default -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "UnknownMethod", "message", "Unknown XRPC: " + nsid));
        };
    }

    private ResponseEntity<?> listServers(String userDid) {
        List<Map<String, Object>> list = serverRepository.findByMembersDid(userDid).stream()
                .map(this::serverView)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    private ResponseEntity<?> getServer(Long serverId, String userDid) {
        if (serverId == null)
            return err(HttpStatus.BAD_REQUEST, "InvalidRequest", "serverId required");
        if (!memberRepository.existsByServerIdAndDid(serverId, userDid)) {
            return err(HttpStatus.FORBIDDEN, "Forbidden", "Not a member");
        }
        return serverRepository.findById(serverId)
                .map(s -> ResponseEntity.ok(serverView(s)))
                .orElseGet(() -> err(HttpStatus.NOT_FOUND, "NotFound", "Server not found"));
    }

    private ResponseEntity<?> createServer(String userDid, String userHandle, Map<String, Object> body) {
        String name = body.containsKey("name") && body.get("name") != null
                ? body.get("name").toString()
                : "New Server";
        if (name.isBlank())
            name = "New Server";

        Server server = Server.builder()
                .name(name)
                .ownerDid(userDid)
                .build();
        server = serverRepository.save(server);

        Member owner = Member.builder()
                .server(server)
                .did(userDid)
                .handle(userHandle)
                .role(Member.MemberRole.OWNER)
                .build();
        memberRepository.save(owner);

        Channel general = Channel.builder()
                .name("general")
                .server(server)
                .build();
        general = channelRepository.save(general);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.<String, Object>of(
                "id", server.getId(),
                "name", server.getName(),
                "ownerDid", server.getOwnerDid(),
                "channelId", general.getId()));
    }

    private ResponseEntity<?> inviteToServer(Long serverId, String inviterDid, Map<String, Object> body) {
        if (serverId == null)
            return err(HttpStatus.BAD_REQUEST, "InvalidRequest", "serverId required");
        Object h = body.get("handle");
        if (h == null || h.toString().isBlank())
            return err(HttpStatus.BAD_REQUEST, "InvalidRequest", "handle required");
        String handle = h.toString().trim();

        Optional<Server> serverOpt = serverRepository.findById(serverId);
        if (serverOpt.isEmpty())
            return err(HttpStatus.NOT_FOUND, "NotFound", "Server not found");
        Server server = serverOpt.get();

        boolean canInvite = server.getOwnerDid().equals(inviterDid)
                || memberRepository.findByServerIdAndDid(serverId, inviterDid)
                        .map(m -> m.getRole() == Member.MemberRole.MODERATOR || m.getRole() == Member.MemberRole.OWNER)
                        .orElse(false);
        if (!canInvite)
            return err(HttpStatus.FORBIDDEN, "Forbidden", "Cannot invite");

        String did;
        try {
            did = identityService.resolveHandle(handle).orElse(null);
        } catch (AtprotoException e) {
            did = null;
        }
        if (did == null)
            return err(HttpStatus.UNPROCESSABLE_ENTITY, "CouldNotResolveHandle", "Could not resolve handle to DID");
        if (memberRepository.existsByServerIdAndDid(serverId, did)) {
            return err(HttpStatus.CONFLICT, "AlreadyMember", "Already a member");
        }

        Member m = Member.builder()
                .server(server)
                .did(did)
                .handle(handle)
                .role(Member.MemberRole.MEMBER)
                .build();
        memberRepository.save(m);
        return ResponseEntity.ok(Map.of("did", did, "handle", handle));
    }

    private ResponseEntity<?> listChannels(Long serverId, String userDid) {
        if (serverId == null)
            return err(HttpStatus.BAD_REQUEST, "InvalidRequest", "serverId required");
        if (!memberRepository.existsByServerIdAndDid(serverId, userDid)) {
            return err(HttpStatus.FORBIDDEN, "Forbidden", "Not a member");
        }
        List<Map<String, Object>> list = channelRepository.findByServerIdOrderById(serverId).stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "name", c.getName(), "serverId", serverId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    private ResponseEntity<?> createChannel(Long serverId, String userDid, Map<String, Object> body) {
        if (serverId == null)
            return err(HttpStatus.BAD_REQUEST, "InvalidRequest", "serverId required");
        if (memberRepository.findByServerIdAndDid(serverId, userDid).isEmpty()) {
            return err(HttpStatus.NOT_FOUND, "NotFound", "Server not found or not a member");
        }
        String rawName = body.containsKey("name") && body.get("name") != null
                ? body.get("name").toString()
                : "general";
        final String channelName = rawName.isBlank() ? "general" : rawName;

        return serverRepository.findById(serverId)
                .map(server -> {
                    Channel ch = Channel.builder().name(channelName).server(server).build();
                    ch = channelRepository.save(ch);
                    return ResponseEntity.status(HttpStatus.CREATED)
                            .body(Map.<String, Object>of("id", ch.getId(), "name", ch.getName(), "serverId", serverId));
                })
                .orElseGet(() -> err(HttpStatus.NOT_FOUND, "NotFound", "Server not found"));
    }

    private ResponseEntity<?> getMessages(Long channelId, int limit, String userDid) {
        if (channelId == null)
            return err(HttpStatus.BAD_REQUEST, "InvalidRequest", "channelId required");
        int safeLimit = Math.min(Math.max(1, limit), 100);

        return channelRepository.findById(channelId)
                .filter(c -> memberRepository.existsByServerIdAndDid(c.getServer().getId(), userDid))
                .map(ch -> {
                    List<Map<String, Object>> list = messageRepository
                            .findByChannelIdOrderByCreatedAtDesc(channelId, PageRequest.of(0, safeLimit))
                            .stream()
                            .map(this::messageView)
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(list);
                })
                .orElseGet(() -> err(HttpStatus.NOT_FOUND, "NotFound", "Channel not found"));
    }

    private ResponseEntity<?> postMessage(Long channelId, String userDid, String userHandle, Map<String, Object> body) {
        if (channelId == null)
            return err(HttpStatus.BAD_REQUEST, "InvalidRequest", "channelId required");
        Object c = body.get("content");
        if (c == null || c.toString().isBlank())
            return err(HttpStatus.BAD_REQUEST, "InvalidRequest", "content required");
        String content = c.toString();

        return channelRepository.findById(channelId)
                .filter(ch -> memberRepository.existsByServerIdAndDid(ch.getServer().getId(), userDid))
                .map(ch -> {
                    Message msg = Message.builder()
                            .content(content)
                            .authorDid(userDid)
                            .authorHandle(userHandle)
                            .channel(ch)
                            .build();
                    msg = messageRepository.save(msg);
                    realtimeBroker.publishMessageCreated(channelId, msg);
                    return ResponseEntity.status(HttpStatus.CREATED).body(messageView(msg));
                })
                .orElseGet(() -> err(HttpStatus.NOT_FOUND, "NotFound", "Channel not found"));
    }

    private Map<String, Object> serverView(Server s) {
        List<Map<String, Object>> channels = channelRepository.findByServerIdOrderById(s.getId()).stream()
                .map(ch -> Map.<String, Object>of("id", ch.getId(), "name", ch.getName()))
                .collect(Collectors.toList());
        return Map.of(
                "id", s.getId(),
                "name", s.getName(),
                "ownerDid", s.getOwnerDid(),
                "channels", channels);
    }

    private Map<String, Object> messageView(Message m) {
        return Map.<String, Object>of(
                "id", m.getId(),
                "content", m.getContent(),
                "authorDid", m.getAuthorDid(),
                "authorHandle", m.getAuthorHandle() != null ? m.getAuthorHandle() : "",
                "createdAt", m.getCreatedAt().toString());
    }

    private <T> ResponseEntity<T> err(HttpStatus status, String errorCode, String message) {
        return (ResponseEntity<T>) ResponseEntity.status(status).body(Map.of("error", errorCode, "message", message));
    }

    private static Long paramLong(Map<String, String> params, String key) {
        String v = params.get(key);
        if (v == null || v.isBlank())
            return null;
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int paramInt(Map<String, String> params, String key, int defaultValue) {
        String v = params.get(key);
        if (v == null || v.isBlank())
            return defaultValue;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String userDid(HttpServletRequest request) {
        Object did = request.getAttribute(AtprotoAuthFilter.ATTR_USER_DID);
        if (did instanceof String didValue && !didValue.isBlank())
            return didValue;
        throw new IllegalStateException("Missing authenticated DID");
    }

    private Optional<String> userHandle(HttpServletRequest request) {
        Object handle = request.getAttribute(AtprotoAuthFilter.ATTR_USER_HANDLE);
        if (handle instanceof String handleValue && !handleValue.isBlank())
            return Optional.of(handleValue);
        return Optional.empty();
    }
}
