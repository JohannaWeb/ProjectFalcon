package app.falcon.api;

import app.falcon.domain.Channel;
import app.falcon.domain.Message;
import app.falcon.realtime.RealtimeBroker;
import app.falcon.repository.ChannelRepository;
import app.falcon.repository.MemberRepository;
import app.falcon.repository.MessageRepository;
import app.falcon.repository.ServerRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173}")
public class ChannelController {

    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository;
    private final ServerRepository serverRepository;
    private final MemberRepository memberRepository;
    private final RealtimeBroker realtimeBroker;

    @GetMapping("/server/{serverId}")
    public ResponseEntity<List<Map<String, Object>>> listChannels(@PathVariable Long serverId, HttpServletRequest request) {
        String userDid = userDid(request);
        if (!memberRepository.existsByServerIdAndDid(serverId, userDid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<Map<String, Object>> channels = channelRepository.findByServerIdOrderById(serverId).stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "name", c.getName(), "serverId", serverId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(channels);
    }

    @PostMapping("/server/{serverId}")
    public ResponseEntity<Map<String, Object>> createChannel(
            @PathVariable Long serverId,
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        String userDid = userDid(request);
        String name = body != null ? body.get("name") : null;
        if (name == null || name.isBlank()) {
            name = "general";
        }
        String finalName = name;
        return serverRepository.findById(serverId)
                .filter(s -> memberRepository.findByServerIdAndDid(serverId, userDid).isPresent())
                .map(server -> {
                    Channel ch = Channel.builder().name(finalName).server(server).build();
                    ch = channelRepository.save(ch);
                    return ResponseEntity.status(HttpStatus.CREATED)
                            .body(Map.<String, Object>of("id", ch.getId(), "name", ch.getName(), "serverId", serverId));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{channelId}/messages")
    public ResponseEntity<List<Map<String, Object>>> getMessages(
            @PathVariable Long channelId,
            HttpServletRequest request,
            @RequestParam(defaultValue = "50") int limit) {
        String userDid = userDid(request);
        return channelRepository.findById(channelId)
                .filter(c -> memberRepository.existsByServerIdAndDid(c.getServer().getId(), userDid))
                .map(ch -> {
                    List<Map<String, Object>> list = messageRepository
                            .findByChannelIdOrderByCreatedAtDesc(channelId, PageRequest.of(0, Math.min(limit, 100)))
                            .stream()
                            .map(m -> Map.<String, Object>of(
                                    "id", m.getId(),
                                    "content", m.getContent(),
                                    "authorDid", m.getAuthorDid(),
                                    "authorHandle", m.getAuthorHandle() != null ? m.getAuthorHandle() : "",
                                    "createdAt", m.getCreatedAt().toString()))
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(list);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{channelId}/messages")
    public ResponseEntity<Map<String, Object>> postMessage(
            @PathVariable Long channelId,
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        String userDid = userDid(request);
        String userHandle = userHandle(request).orElse(null);

        String content = body != null ? body.get("content") : null;
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "content required"));
        }

        return channelRepository.findById(channelId)
                .filter(c -> memberRepository.existsByServerIdAndDid(c.getServer().getId(), userDid))
                .map(ch -> {
                    Message msg = Message.builder()
                            .content(content)
                            .authorDid(userDid)
                            .authorHandle(userHandle)
                            .channel(ch)
                            .build();
                    msg = messageRepository.save(msg);
                    realtimeBroker.publishMessageCreated(channelId, msg);
                    return ResponseEntity.status(HttpStatus.CREATED).body(Map.<String, Object>of(
                            "id", msg.getId(),
                            "content", msg.getContent(),
                            "authorDid", msg.getAuthorDid(),
                            "authorHandle", msg.getAuthorHandle() != null ? msg.getAuthorHandle() : "",
                            "createdAt", msg.getCreatedAt().toString()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private String userDid(HttpServletRequest request) {
        Object did = request.getAttribute(AtprotoAuthFilter.ATTR_USER_DID);
        if (did instanceof String didValue && !didValue.isBlank()) {
            return didValue;
        }
        throw new IllegalStateException("Missing authenticated DID");
    }

    private Optional<String> userHandle(HttpServletRequest request) {
        Object handle = request.getAttribute(AtprotoAuthFilter.ATTR_USER_HANDLE);
        if (handle instanceof String handleValue && !handleValue.isBlank()) {
            return Optional.of(handleValue);
        }
        return Optional.empty();
    }
}
