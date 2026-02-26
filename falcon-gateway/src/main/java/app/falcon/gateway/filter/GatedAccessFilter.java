package app.falcon.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class GatedAccessFilter extends AbstractGatewayFilterFactory<GatedAccessFilter.Config> {

    private final WebClient.Builder webClientBuilder;
    private final String trustServiceUrl;

    public GatedAccessFilter(WebClient.Builder webClientBuilder,
            @Value("${falcon.services.trust-url}") String trustServiceUrl) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
        this.trustServiceUrl = trustServiceUrl;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String userDid = exchange.getRequest().getHeaders().getFirst("X-Falcon-Viewer-DID");
            String channelIdStr = exchange.getRequest().getQueryParams().getFirst("channelId");
            String serverIdStr = exchange.getRequest().getQueryParams().getFirst("serverId");

            if (userDid == null || channelIdStr == null || serverIdStr == null) {
                // TODO: SECURITY — block requests missing gating params before production
                // launch
                return chain.filter(exchange);
            }

            return webClientBuilder.build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme(parseScheme(trustServiceUrl))
                            .host(parseHost(trustServiceUrl))
                            .port(parsePort(trustServiceUrl))
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
                        // Fail closed — if trust-service is unreachable, deny access
                        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                        return exchange.getResponse().setComplete();
                    });
        };
    }

    // Simple URL parsing helpers to avoid the URI-builder limitation with full URL
    // strings
    private String parseScheme(String url) {
        return url.startsWith("https") ? "https" : "http";
    }

    private String parseHost(String url) {
        String withoutScheme = url.replaceFirst("https?://", "");
        int colonIdx = withoutScheme.indexOf(':');
        return colonIdx >= 0 ? withoutScheme.substring(0, colonIdx) : withoutScheme;
    }

    private int parsePort(String url) {
        String withoutScheme = url.replaceFirst("https?://", "");
        int colonIdx = withoutScheme.indexOf(':');
        if (colonIdx >= 0) {
            String portStr = withoutScheme.substring(colonIdx + 1).replaceAll("/.*", "");
            return Integer.parseInt(portStr);
        }
        return url.startsWith("https") ? 443 : 80;
    }

    public static class Config {
        // Configuration fields if needed
    }
}
