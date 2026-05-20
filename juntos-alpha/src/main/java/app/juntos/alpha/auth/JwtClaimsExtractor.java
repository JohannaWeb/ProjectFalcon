package app.juntos.alpha.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Decodes a raw JWT string and extracts its claims into a {@link JwtClaims} record.
 * Validates that the minimum required claims (sub + iss or aud) are present.
 * Does not verify the signature.
 */
@ApplicationScoped
@Slf4j
public class JwtClaimsExtractor {

    public JwtClaims extract(String token) {
        DecodedJWT jwt;
        try {
            jwt = JWT.decode(token);
        } catch (Exception e) {
            throw new JWTVerificationException("Malformed JWT: " + e.getMessage());
        }

        String sub = jwt.getSubject();
        String iss = jwt.getIssuer();
        String alg = jwt.getAlgorithm();
        String kid = jwt.getKeyId();

        List<String> audList = jwt.getAudience();
        String aud = (audList != null && !audList.isEmpty()) ? audList.get(0) : null;

        log.debug("[CLAIMS] sub={} iss={} aud={} alg={} kid={}", sub, iss, aud, alg, kid);

        if (sub == null)
            throw new JWTVerificationException("JWT missing sub claim");
        if (iss == null && aud == null)
            throw new JWTVerificationException("JWT missing both iss and aud — cannot identify signing party");

        return new JwtClaims(sub, iss, aud, alg, kid, jwt.getExpiresAt(), jwt.getNotBefore());
    }
}
