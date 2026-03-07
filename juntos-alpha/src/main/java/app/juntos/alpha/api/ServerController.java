package app.juntos.alpha.api;

import app.juntos.alpha.auth.AtprotoAuthFilter;
import app.juntos.alpha.domain.Channel;
import app.juntos.alpha.domain.Member;
import app.juntos.alpha.domain.Server;
import app.juntos.alpha.repository.MemberRepository;
import app.juntos.alpha.repository.ServerRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/xrpc")
@RequiredArgsConstructor
public class ServerController {

    private final ServerRepository serverRepo;
    private final MemberRepository memberRepo;

    @GetMapping("/app.juntos.server.list")
    public List<Map<String, Object>> listServers(HttpServletRequest req) {
        String did = (String) req.getAttribute(AtprotoAuthFilter.VIEWER_DID_ATTR);
        return serverRepo.findByMembersDid(did).stream().map(this::toSummary).toList();
    }

    @GetMapping("/app.juntos.server.get")
    public ResponseEntity<Map<String, Object>> getServer(@RequestParam Long serverId, HttpServletRequest req) {
        return serverRepo.findById(serverId)
                .map(s -> ResponseEntity.ok(toSummary(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/app.juntos.server.create")
    public Map<String, Object> createServer(@RequestBody Map<String, String> body, HttpServletRequest req) {
        String did = (String) req.getAttribute(AtprotoAuthFilter.VIEWER_DID_ATTR);
        String handle = did; // handle not available here, use DID as fallback

        Server server = new Server();
        server.setName(body.get("name"));
        server.setOwnerDid(did);
        server = serverRepo.save(server);

        // Default channel
        Channel channel = new Channel();
        channel.setName("general");
        channel.setServer(server);
        server.getChannels().add(channel);

        // Add owner as member
        Member member = new Member();
        member.setDid(did);
        member.setHandle(handle);
        member.setServer(server);
        server.getMembers().add(member);

        server = serverRepo.save(server);
        long channelId = server.getChannels().getFirst().getId();

        return Map.of(
                "id", server.getId(),
                "name", server.getName(),
                "ownerDid", server.getOwnerDid(),
                "channelId", channelId
        );
    }

    @PostMapping("/app.juntos.server.invite")
    public ResponseEntity<Map<String, Object>> inviteToServer(
            @RequestParam Long serverId,
            @RequestBody Map<String, String> body,
            HttpServletRequest req) {

        return serverRepo.findById(serverId).map(server -> {
            String handle = body.getOrDefault("handle", "");
            String inviteeDid = "did:plc:" + handle.replace(".", "-"); // placeholder

            if (!memberRepo.existsByDidAndServerId(inviteeDid, serverId)) {
                Member m = new Member();
                m.setDid(inviteeDid);
                m.setHandle(handle);
                m.setServer(server);
                memberRepo.save(m);
            }
            return ResponseEntity.ok(Map.<String, Object>of("did", inviteeDid, "handle", handle));
        }).orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toSummary(Server s) {
        List<Map<String, Object>> channels = s.getChannels().stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "name", c.getName()))
                .toList();
        return Map.of(
                "id", s.getId(),
                "name", s.getName(),
                "ownerDid", s.getOwnerDid(),
                "channels", channels
        );
    }
}
