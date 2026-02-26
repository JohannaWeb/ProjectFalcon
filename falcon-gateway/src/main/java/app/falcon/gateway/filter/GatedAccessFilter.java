package app.falcon.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class GatedAccessFilter extends AbstractGatewayFilterFactory<GatedAccessFilter.Config> {

    private final WebClient.Builder webClientBuilder;

    public GatedAccessFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String userDid = exchange.getRequest().getHeaders().getFirst("X-Falcon-Viewer-DID");
            String channelIdStr = exchange.getRequest().getQueryParams().getFirst("channelId");
            String serverIdStr = exchange.getRequest().getQueryParams().getFirst("serverId");

            if (userDid == null || channelIdStr == null || serverIdStr == null) {
                // If missing required info for gating, we could either block or let through
                // For a prototype, we'll let through to avoid breaking things, but in
                // production, we should block.
                return chain.filter(exchange);
            }

            return webClientBuilder.build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("http")
                            .host("localhost")
                            .port(8081)
                            .path("/api/trust/membership/verify")
                            .queryParam("userDid", userDid)
                            .queryParam("serverId", serverIdStr)
                            .queryParam("channelId", channelIdStr)
                            .build())
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .flatMap(isAuthorized -> {
                        if (Boolean.TRUE.equals(isAuthorized)) {
                            return chain.filter(exchange);
                        } else {
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return exchange.getResponse().setComplete();
                        }
                    })
                    .onErrorResume(e -> {
                        // In case of error calling trust-service, we fail-safe (block access)
                        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                        return exchange.getResponse().setComplete();
                    });
        };
    }

    public static class Config {
        // Configuration fields if needed
    }
}
