package app.juntos.alpha.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
@Produces(MediaType.TEXT_PLAIN)
public class HealthController {

    @GET
    public String home() {
        return "ok";
    }

    @GET
    @Path("/ping")
    public String ping() {
        return "pong";
    }
}
