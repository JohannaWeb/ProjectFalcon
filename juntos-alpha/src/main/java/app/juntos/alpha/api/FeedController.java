package app.juntos.alpha.api;

import app.juntos.alpha.auth.AtprotoAuthFilter;
import jakarta.ws.rs.*;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Path("/xrpc")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class FeedController {

    private static final String BSKY = "https://bsky.social";

    @GET
    @Path("/app.juntos.feed.getTimeline")
    public Response getTimeline(
            @QueryParam("limit") @DefaultValue("30") int limit,
            @QueryParam("cursor") String cursor,
            @HeaderParam("Authorization") String authHeader,
            @HeaderParam(AtprotoAuthFilter.VIEWER_DID_HEADER) String did) {

        String url = BSKY + "/xrpc/app.bsky.feed.getTimeline?limit=" + limit;
        if (cursor != null) url += "&cursor=" + cursor;

        Client client = ClientBuilder.newClient();
        try {
            Response upstream = client.target(url)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .header("Authorization", authHeader)
                    .get();

            if (upstream.getStatus() == 200) {
                byte[] body = upstream.readEntity(byte[].class);
                return Response.ok(body).type(MediaType.APPLICATION_JSON).build();
            } else {
                log.warn("Timeline proxy returned status {} for {}", upstream.getStatus(), did);
                return Response.status(Response.Status.BAD_GATEWAY).build();
            }
        } catch (Exception e) {
            log.warn("Timeline proxy failed for {}: {}", did, e.getMessage());
            return Response.status(Response.Status.BAD_GATEWAY).build();
        } finally {
            client.close();
        }
    }
}
