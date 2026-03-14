package app.juntos.alpha.api;

import app.juntos.alpha.auth.AtprotoAuthFilter;
import app.juntos.alpha.auth.DidResolver;
import app.juntos.alpha.domain.Channel;
import app.juntos.alpha.domain.Message;
import app.juntos.alpha.repository.ChannelRepository;
import app.juntos.alpha.repository.MemberRepository;
import app.juntos.alpha.repository.MessageRepository;
import app.juntos.alpha.repository.ServerRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/xrpc")
@RequiredArgsConstructor
@Slf4j
public class ChannelController {

    // 2-32 chars, lowercase alphanumeric + hyphens, no leading/trailing hyphens
    private static final Pattern CHANNEL_NAME = Pattern.compile("^[a-z0-9]([a-z0-9-]{0,30}[a-z0-9])?$");

    private final ChannelRepository channelRepo;
    private final MessageRepository messageRepo;
    private final ServerRepository serverRepo;
    private final MemberRepository memberRepo;
    private final DidResolver didResolver;
    private final RestTemplate http = new RestTemplate();

    @GetMapping("/app.juntos.channel.list")
    public List<Map<String, Object>> listChannels(@RequestParam Long serverId) {
        return channelRepo.findByServerId(serverId).stream()
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getId());
                    m.put("name", c.getName());
                    m.put("serverId", serverId);
                    if (c.getAtUri() != null) m.put("atUri", c.getAtUri());
                    return m;
                })
                .toList();
    }

    @PostMapping("/app.juntos.channel.create")
    public ResponseEntity<Map<String, Object>> createChannel(
            @RequestParam Long serverId,
            @RequestBody Map<String, String> body,
            HttpServletRequest req) {

        String did = (String) req.getAttribute(AtprotoAuthFilter.VIEWER_DID_ATTR);

        // Validate name before any DB/network calls
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "InvalidRequest",
                    "message", "Channel name is required"));
        }
        name = name.trim().toLowerCase();
        if (!CHANNEL_NAME.matcher(name).matches()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "InvalidRequest",
                    "message", "Channel name must be 2-32 characters, lowercase alphanumeric and hyphens only, no leading or trailing hyphens"));
        }

        // Check server exists (404) before membership (403)
        var serverOpt = serverRepo.findById(serverId);
        if (serverOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!memberRepo.existsByDidAndServerId(did, serverId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Write to AT Protocol repo first — this is the source of truth.
        // If the PDS write fails the channel is not created locally either.
        String atUri;
        try {
            atUri = writeAtRecord(did, name, serverId, req.getHeader("Authorization"));
        } catch (Exception e) {
            log.error("AT Protocol write failed for channel '{}' by {}: {}", name, did, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "error", "ATProtoWriteFailed",
                    "message", "Could not write channel record to AT Protocol repository"));
        }

        Channel channel = new Channel();
        channel.setName(name);
        channel.setServer(serverOpt.get());
        channel.setAtUri(atUri);
        // Extract rkey from at://did/collection/rkey
        String[] uriParts = atUri.split("/");
        if (uriParts.length >= 1) {
            channel.setAtRkey(uriParts[uriParts.length - 1]);
        }
        Channel saved = channelRepo.save(channel);

        log.info("Channel '{}' created by {} — atUri: {}", name, did, atUri);
        Map<String, Object> result = new HashMap<>();
        result.put("id", saved.getId());
        result.put("name", saved.getName());
        result.put("serverId", serverId);
        result.put("atUri", atUri);
        return ResponseEntity.ok(result);
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

    /**
     * Writes an app.juntos.channel record to the user's PDS via com.atproto.repo.createRecord.
     *
     * @return the AT URI of the created record (at://did/app.juntos.channel/rkey)
     * @throws Exception if the PDS is unreachable, rejects the record, or the response is malformed
     */
    @SuppressWarnings("unchecked")
    private String writeAtRecord(String did, String name, Long serverId, String authHeader) throws Exception {
        Map<String, Object> didDoc = didResolver.resolve(did);
        String pdsUrl = extractPdsUrl(didDoc);
        if (pdsUrl == null) {
            throw new IllegalStateException("No AtprotoPersonalDataServer service found in DID document for " + did);
        }

        Map<String, Object> record = Map.of(
                "$type", "app.juntos.channel",
                "name", name,
                "serverId", serverId,
                "createdAt", Instant.now().toString()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = http.exchange(
                pdsUrl + "/xrpc/com.atproto.repo.createRecord",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "repo", did,
                        "collection", "app.juntos.channel",
                        "record", record
                ), headers),
                Map.class
        );

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !responseBody.containsKey("uri")) {
            throw new IllegalStateException("createRecord response missing 'uri' field");
        }
        return (String) responseBody.get("uri");
    }

    @SuppressWarnings("unchecked")
    private String extractPdsUrl(Map<String, Object> didDoc) {
        List<Map<String, Object>> services = (List<Map<String, Object>>) didDoc.get("service");
        if (services == null) return null;
        for (Map<String, Object> service : services) {
            if ("AtprotoPersonalDataServer".equals(service.get("type"))) {
                return (String) service.get("serviceEndpoint");
            }
        }
        return null;
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
