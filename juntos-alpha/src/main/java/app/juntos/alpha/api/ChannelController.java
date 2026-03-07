package app.juntos.alpha.api;

import app.juntos.alpha.auth.AtprotoAuthFilter;
import app.juntos.alpha.domain.Channel;
import app.juntos.alpha.domain.Message;
import app.juntos.alpha.repository.ChannelRepository;
import app.juntos.alpha.repository.MessageRepository;
import app.juntos.alpha.repository.ServerRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/xrpc")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelRepository channelRepo;
    private final MessageRepository messageRepo;
    private final ServerRepository serverRepo;

    @GetMapping("/app.juntos.channel.list")
    public List<Map<String, Object>> listChannels(@RequestParam Long serverId) {
        return channelRepo.findByServerId(serverId).stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "name", c.getName(), "serverId", serverId))
                .toList();
    }

    @PostMapping("/app.juntos.channel.create")
    public ResponseEntity<Map<String, Object>> createChannel(
            @RequestParam Long serverId,
            @RequestBody Map<String, String> body) {

        return serverRepo.findById(serverId).map(server -> {
            Channel channel = new Channel();
            channel.setName(body.get("name"));
            channel.setServer(server);
            Channel saved = channelRepo.save(channel);
            return ResponseEntity.ok(Map.<String, Object>of(
                    "id", saved.getId(),
                    "name", saved.getName(),
                    "serverId", serverId
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/app.juntos.channel.getMessages")
    public ResponseEntity<List<Map<String, Object>>> getMessages(
            @RequestParam Long channelId,
            @RequestParam(defaultValue = "50") int limit) {

        if (!channelRepo.existsById(channelId)) return ResponseEntity.notFound().build();
        List<Map<String, Object>> messages = messageRepo
                .findByChannelIdOrderByCreatedAtAsc(channelId, PageRequest.of(0, limit))
                .stream().map(this::toSummary).toList();
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/app.juntos.channel.postMessage")
    public ResponseEntity<Map<String, Object>> postMessage(
            @RequestParam Long channelId,
            @RequestBody Map<String, String> body,
            HttpServletRequest req) {

        return channelRepo.findById(channelId).map(channel -> {
            String did = (String) req.getAttribute(AtprotoAuthFilter.VIEWER_DID_ATTR);
            Message msg = new Message();
            msg.setContent(body.get("content"));
            msg.setAuthorDid(did);
            msg.setAuthorHandle(did);
            msg.setChannel(channel);
            Message saved = messageRepo.save(msg);
            return ResponseEntity.ok(toSummary(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toSummary(Message m) {
        return Map.of(
                "id", m.getId(),
                "content", m.getContent(),
                "authorDid", m.getAuthorDid(),
                "authorHandle", m.getAuthorHandle(),
                "createdAt", m.getCreatedAt().toString()
        );
    }
}
