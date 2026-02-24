package app.falcon.api;

import app.falcon.atproto.AtprotoException;
import app.falcon.domain.Channel;
import app.falcon.domain.Member;
import app.falcon.domain.Server;
import app.falcon.repository.ChannelRepository;
import app.falcon.repository.MemberRepository;
import app.falcon.repository.ServerRepository;
import app.falcon.service.IdentityService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173}")
public class ServerController {

    private final ServerRepository serverRepository;
    private final ChannelRepository channelRepository;
    private final MemberRepository memberRepository;
    private final IdentityService identityService;

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getServer(@PathVariable Long id, HttpServletRequest request) {
        String userDid = userDid(request);
        if (!memberRepository.existsByServerIdAndDid(id, userDid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
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
    public List<Map<String, Object>> listServers(HttpServletRequest request) {
        String userDid = userDid(request);
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
    public ResponseEntity<Map<String, Object>> createServer(HttpServletRequest request, @RequestBody Map<String, String> body) {
        String userDid = userDid(request);
        String userHandle = userHandle(request).orElse(null);

        String name = body.get("name");
        if (name == null || name.isBlank()) {
            name = "New Server";
        }

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
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        String inviterDid = userDid(request);
        String handle = body.get("handle");
        if (handle == null || handle.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "handle required"));
        }
        String finalHandle = handle.trim();

        return serverRepository.findById(serverId)
                .filter(s -> s.getOwnerDid().equals(inviterDid) || memberRepository.findByServerIdAndDid(serverId, inviterDid)
                        .map(m -> m.getRole() == Member.MemberRole.MODERATOR || m.getRole() == Member.MemberRole.OWNER).orElse(false))
                .map(server -> {
                    String did;
                    try {
                        did = identityService.resolveHandle(finalHandle).orElse(null);
                    } catch (AtprotoException e) {
                        did = null;
                    }
                    if (did == null) {
                        return ResponseEntity.unprocessableEntity().body(Map.of("error", "Could not resolve handle to DID"));
                    }
                    if (memberRepository.existsByServerIdAndDid(serverId, did)) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Already a member"));
                    }
                    Member m = Member.builder()
                            .server(server)
                            .did(did)
                            .handle(finalHandle)
                            .role(Member.MemberRole.MEMBER)
                            .build();
                    memberRepository.save(m);
                    return ResponseEntity.ok(Map.of("did", did, "handle", finalHandle));
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
