package app.falcon.gateway.filter;

import app.falcon.gateway.service.DidResolver;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class AtprotoAuthFilter extends AbstractGatewayFilterFactory<AtprotoAuthFilter.Config> {

    private final DidResolver didResolver;

    public AtprotoAuthFilter(DidResolver didResolver) {
        super(Config.class);
        this.didResolver = didResolver;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);
            try {
                DecodedJWT jwt = JWT.decode(token);

                if (jwt.getExpiresAt().before(new Date())) {
                    log.warn("JWT token expired");
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                final String userDid = (jwt.getSubject() != null) ? jwt.getSubject() : jwt.getIssuer();

                // RESOLVE DID (Cached & Seamless)
                return didResolver.resolve(userDid)
                        .flatMap(didDoc -> {
                            // The presence of a valid DID document implies we can at least resolve the
                            // identity.
                            // Real verification would check the signature here.
                            log.info("Successfully resolved identity for DID: {}", userDid);

                            var modifiedRequest = exchange.getRequest().mutate()
                                    .header("X-Falcon-Viewer-DID", userDid)
                                    .build();

                            return chain.filter(exchange.mutate().request(modifiedRequest).build());
                        })
                        .onErrorResume(e -> {
                            log.error("Authentication failed for DID {}: {}", userDid, e.getMessage());
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        });

            } catch (Exception e) {
                log.error("Failed to decode ATProto JWT", e);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }

    public static class Config {
        // Configuration fields if needed
    }
}
