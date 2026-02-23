package app.falcon.api;

import app.falcon.domain.Channel;
import app.falcon.domain.Member;
import app.falcon.domain.Message;
import app.falcon.repository.ChannelRepository;
import app.falcon.repository.MemberRepository;
import app.falcon.repository.MessageRepository;
import app.falcon.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChannelController {

    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository;
    private final ServerRepository serverRepository;
    private final MemberRepository memberRepository;

    @GetMapping("/server/{serverId}")
    public ResponseEntity<List<Map<String, Object>>> listChannels(
            @PathVariable Long serverId,
            @RequestHeader(value = "X-User-Did", required = false) String userDid) {
        if (userDid == null || !memberRepository.existsByServerIdAndDid(serverId, userDid))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        List<Map<String, Object>> channels = channelRepository.findByServerIdOrderById(serverId).stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "name", c.getName(), "serverId", serverId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(channels);
    }

    @PostMapping("/server/{serverId}")
    public ResponseEntity<Map<String, Object>> createChannel(
            @PathVariable Long serverId,
            @RequestHeader("X-User-Did") String userDid,
            @RequestBody Map<String, String> body) {
        String name = body != null ? body.get("name") : null;
        if (name == null || name.isBlank()) name = "general";
        String finalName = name;
        return serverRepository.findById(serverId)
                .filter(s -> memberRepository.findByServerIdAndDid(serverId, userDid).isPresent())
                .map(server -> {
                    Channel ch = Channel.builder().name(finalName).server(server).build();
                    ch = channelRepository.save(ch);
                    return ResponseEntity.status(HttpStatus.CREATED).body(Map.<String, Object>of("id", ch.getId(), "name", ch.getName(), "serverId", serverId));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{channelId}/messages")
    public ResponseEntity<List<Map<String, Object>>> getMessages(
            @PathVariable Long channelId,
            @RequestHeader(value = "X-User-Did", required = false) String userDid,
            @RequestParam(defaultValue = "50") int limit) {
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
            @RequestHeader("X-User-Did") String userDid,
            @RequestHeader(value = "X-User-Handle", required = false) String userHandle,
            @RequestBody Map<String, String> body) {
        String content = body != null ? body.get("content") : null;
        if (content == null || content.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "content required"));
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
                    return ResponseEntity.status(HttpStatus.CREATED).body(Map.<String, Object>of(
                            "id", msg.getId(),
                            "content", msg.getContent(),
                            "authorDid", msg.getAuthorDid(),
                            "authorHandle", msg.getAuthorHandle() != null ? msg.getAuthorHandle() : "",
                            "createdAt", msg.getCreatedAt().toString()));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
