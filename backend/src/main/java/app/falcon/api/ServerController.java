package app.falcon.api;

import app.falcon.domain.Channel;
import app.falcon.domain.Member;
import app.falcon.domain.Server;
import app.falcon.repository.ChannelRepository;
import app.falcon.repository.MemberRepository;
import app.falcon.repository.ServerRepository;
import app.falcon.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ServerController {

    private final ServerRepository serverRepository;
    private final ChannelRepository channelRepository;
    private final MemberRepository memberRepository;
    private final IdentityService identityService;

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getServer(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Did", required = false) String userDid) {
        if (userDid == null || !memberRepository.existsByServerIdAndDid(id, userDid))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return serverRepository.findById(id)
                .map(s -> ResponseEntity.ok(Map.<String, Object>of(
                        "id", s.getId(),
                        "name", s.getName(),
                        "ownerDid", s.getOwnerDid(),
                        "channels", channelRepository.findByServerIdOrderById(s.getId()).stream()
                                .map(c -> Map.<String, Object>of("id", c.getId(), "name", c.getName()))
                                .collect(Collectors.toList()))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Map<String, Object>> listServers(@RequestHeader(value = "X-User-Did", required = false) String userDid) {
        if (userDid == null || userDid.isBlank()) return List.of();
        return serverRepository.findByMembersDid(userDid).stream()
                .map(s -> Map.<String, Object>of(
                        "id", s.getId(),
                        "name", s.getName(),
                        "ownerDid", s.getOwnerDid(),
                        "channels", channelRepository.findByServerIdOrderById(s.getId()).stream()
                                .map(c -> Map.<String, Object>of("id", c.getId(), "name", c.getName()))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createServer(
            @RequestHeader("X-User-Did") String userDid,
            @RequestHeader(value = "X-User-Handle", required = false) String userHandle,
            @RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) name = "New Server";
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
        channelRepository.save(general);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", server.getId(),
                "name", server.getName(),
                "ownerDid", server.getOwnerDid(),
                "channelId", general.getId()
        ));
    }

    @PostMapping("/{serverId}/invite")
    public ResponseEntity<?> inviteByHandle(
            @PathVariable Long serverId,
            @RequestHeader("X-User-Did") String inviterDid,
            @RequestBody Map<String, String> body) {
        String handle = body.get("handle");
        if (handle == null || handle.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "handle required"));
        handle = handle.trim();
        String finalHandle = handle;
        return serverRepository.findById(serverId)
                .filter(s -> s.getOwnerDid().equals(inviterDid) || memberRepository.findByServerIdAndDid(serverId, inviterDid)
                        .map(m -> m.getRole() == Member.MemberRole.MODERATOR || m.getRole() == Member.MemberRole.OWNER).orElse(false))
                .map(server -> {
                    String resolvedDid = identityService.resolveHandle(finalHandle).block();
                    String did = resolvedDid != null ? resolvedDid : ("did:placeholder:" + finalHandle);
                    if (memberRepository.existsByServerIdAndDid(serverId, did))
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Already a member"));
                    Member m = Member.builder().server(server).did(did).handle(finalHandle).role(Member.MemberRole.MEMBER).build();
                    memberRepository.save(m);
                    return ResponseEntity.ok(Map.of("did", did, "handle", finalHandle));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
