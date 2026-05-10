package app.juntos.alpha.api;

import app.juntos.alpha.auth.AtprotoAuthFilter;
import app.juntos.alpha.domain.Channel;
import app.juntos.alpha.domain.Member;
import app.juntos.alpha.domain.Server;
import app.juntos.alpha.repository.MemberRepository;
import app.juntos.alpha.repository.ServerRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/xrpc")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServerController {

    @Inject
    ServerRepository serverRepo;

    @Inject
    MemberRepository memberRepo;

    @GET
    @Path("/app.juntos.server.list")
    public List<Map<String, Object>> listServers(@HeaderParam(AtprotoAuthFilter.VIEWER_DID_HEADER) String did) {
        return serverRepo.findByMembersDid(did).stream().map(this::toSummary).toList();
    }

    @GET
    @Path("/app.juntos.server.get")
    public Response getServer(@QueryParam("serverId") Long serverId) {
        return serverRepo.findByIdWithChannels(serverId)
                .map(s -> Response.ok(toSummary(s)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path("/app.juntos.server.create")
    public Map<String, Object> createServer(
            Map<String, String> body,
            @HeaderParam(AtprotoAuthFilter.VIEWER_DID_HEADER) String did) {
        String handle = did;

        Server server = new Server();
        server.setName(body.get("name"));
        server.setOwnerDid(did);
        serverRepo.persist(server);

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

        serverRepo.flush();
        long channelId = server.getChannels().getFirst().getId();

        return Map.of(
                "id", server.getId(),
                "name", server.getName(),
                "ownerDid", server.getOwnerDid(),
                "channelId", channelId
        );
    }

    @POST
    @Path("/app.juntos.server.invite")
    public Response inviteToServer(
            @QueryParam("serverId") Long serverId,
            Map<String, String> body,
            @HeaderParam(AtprotoAuthFilter.VIEWER_DID_HEADER) String did) {

        var serverOpt = serverRepo.find("id", serverId).firstResultOptional();
        if (serverOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Server server = serverOpt.get();
        String handle = body.getOrDefault("handle", "");
        String inviteeDid = "did:plc:" + handle.replace(".", "-");

        if (!memberRepo.existsByDidAndServerId(inviteeDid, serverId)) {
            Member m = new Member();
            m.setDid(inviteeDid);
            m.setHandle(handle);
            m.setServer(server);
            memberRepo.persist(m);
        }
        return Response.ok(Map.<String, Object>of("did", inviteeDid, "handle", handle)).build();
    }

    private Map<String, Object> toSummary(Server s) {
        List<Map<String, Object>> channels = s.getChannels().stream()
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getId());
                    m.put("name", c.getName());
                    if (c.getAtUri() != null) m.put("atUri", c.getAtUri());
                    return m;
                })
                .toList();
        return Map.of(
                "id", s.getId(),
                "name", s.getName(),
                "ownerDid", s.getOwnerDid(),
                "channels", channels
        );
    }
}
