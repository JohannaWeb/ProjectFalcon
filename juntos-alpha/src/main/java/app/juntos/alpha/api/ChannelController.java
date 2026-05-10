package app.juntos.alpha.api;

import app.juntos.alpha.auth.AtprotoAuthFilter;
import app.juntos.alpha.auth.DidResolver;
import app.juntos.alpha.domain.Channel;
import app.juntos.alpha.domain.Message;
import app.juntos.alpha.repository.ChannelRepository;
import app.juntos.alpha.repository.MemberRepository;
import app.juntos.alpha.repository.MessageRepository;
import app.juntos.alpha.repository.ServerRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Path("/xrpc")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class ChannelController {

    private static final Pattern CHANNEL_NAME = Pattern.compile("^[a-z0-9]([a-z0-9-]{0,30}[a-z0-9])?$");

    @Inject
    ChannelRepository channelRepo;

    @Inject
    MessageRepository messageRepo;

    @Inject
    ServerRepository serverRepo;

    @Inject
    MemberRepository memberRepo;

    @Inject
    DidResolver didResolver;

    @GET
    @Path("/app.juntos.channel.list")
    public List<Map<String, Object>> listChannels(@QueryParam("serverId") Long serverId) {
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

    @POST
    @Path("/app.juntos.channel.create")
    public Response createChannel(
            @QueryParam("serverId") Long serverId,
            Map<String, String> body,
            @HeaderParam("Authorization") String authHeader,
            @HeaderParam(AtprotoAuthFilter.VIEWER_DID_HEADER) String did) {

        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return Response.status(400).entity(Map.of(
                    "error", "InvalidRequest",
                    "message", "Channel name is required")).build();
        }
        name = name.trim().toLowerCase();
        if (!CHANNEL_NAME.matcher(name).matches()) {
            return Response.status(400).entity(Map.of(
                    "error", "InvalidRequest",
                    "message", "Channel name must be 2-32 characters, lowercase alphanumeric and hyphens only")).build();
        }

        var serverOpt = serverRepo.findByIdOptional(serverId);
        if (serverOpt.isEmpty()) {
            return Response.status(404).build();
        }
        if (!memberRepo.existsByDidAndServerId(did, serverId)) {
            return Response.status(403).build();
        }

        String atUri;
        try {
            atUri = writeAtRecord(did, name, serverId, authHeader);
        } catch (Exception e) {
            log.error("AT Protocol write failed for channel '{}' by {}: {}", name, did, e.getMessage());
            return Response.status(502).entity(Map.of(
                    "error", "ATProtoWriteFailed",
                    "message", "Could not write channel record to AT Protocol repository")).build();
        }

        Channel channel = new Channel();
        channel.setName(name);
        channel.setServer(serverOpt.get());
        channel.setAtUri(atUri);
        String[] uriParts = atUri.split("/");
        if (uriParts.length >= 1) {
            channel.setAtRkey(uriParts[uriParts.length - 1]);
        }
        channelRepo.persist(channel);
        Channel saved = channel;

        log.info("Channel '{}' created by {} — atUri: {}", name, did, atUri);
        Map<String, Object> result = new HashMap<>();
        result.put("id", saved.getId());
        result.put("name", saved.getName());
        result.put("serverId", serverId);
        result.put("atUri", atUri);
        return Response.ok(result).build();
    }

    @GET
    @Path("/app.juntos.channel.getMessages")
    public Response getMessages(
            @QueryParam("channelId") Long channelId,
            @QueryParam("limit") @DefaultValue("50") int limit) {

        if (!channelRepo.existsById(channelId)) return Response.status(404).build();
        List<Map<String, Object>> messages = messageRepo
                .findByChannelIdOrderByCreatedAtAsc(channelId)
                .stream().limit(limit).map(this::toSummary).toList();
        return Response.ok(messages).build();
    }

    @POST
    @Path("/app.juntos.channel.postMessage")
    public Response postMessage(
            @QueryParam("channelId") Long channelId,
            Map<String, String> body,
            @HeaderParam(AtprotoAuthFilter.VIEWER_DID_HEADER) String did) {

        var channelOpt = channelRepo.findByIdOptional(channelId);
        if (channelOpt.isEmpty()) return Response.status(404).build();

        Channel channel = channelOpt.get();
        Message msg = new Message();
        msg.setContent(body.get("content"));
        msg.setAuthorDid(did);
        msg.setAuthorHandle(did);
        msg.setChannel(channel);
        messageRepo.persist(msg);
        Message saved = msg;
        return Response.ok(toSummary(saved)).build();
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

        Client client = ClientBuilder.newBuilder().build();
        Response response = client.target(pdsUrl)
                .path("/xrpc/com.atproto.repo.createRecord")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", authHeader)
                .post(Entity.json(Map.of(
                        "repo", did,
                        "collection", "app.juntos.channel",
                        "record", record
                )));

        if (response.getStatus() != 200) {
            throw new IllegalStateException("PDS returned status " + response.getStatus());
        }

        Map<String, Object> responseBody = response.readEntity(Map.class);
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
