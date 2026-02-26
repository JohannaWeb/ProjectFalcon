package app.falcon.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AtprotoAuthFilter extends AbstractGatewayFilterFactory<AtprotoAuthFilter.Config> {

    public AtprotoAuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // In a real scenario, we would verify the JWT with ATP PDS or a public key
            // For now, we extract the "sub" (DID) and pass it as a custom header
            String mockDid = "did:plc:mockuser"; // Placeholder for actual JWT decoding logic

            var modifiedRequest = exchange.getRequest().mutate()
                    .header("X-Falcon-Viewer-DID", mockDid)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    public static class Config {
        // Configuration fields if needed
    }
}
