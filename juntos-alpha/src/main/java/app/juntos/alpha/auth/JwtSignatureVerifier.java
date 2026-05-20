package app.juntos.alpha.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.security.interfaces.ECPublicKey;
import java.util.Date;
import java.util.Map;

/**
 * Dispatches JWT signature verification to the correct algorithm implementation
 * and validates time claims. Knows about algorithms but nothing about DIDs.
 */
@ApplicationScoped
@Slf4j
public class JwtSignatureVerifier {

    @Inject EcKeyExtractor keyExtractor;
    @Inject Es256kVerifier es256kVerifier;

    public void verify(String token, JwtClaims claims, Map<String, Object> didDoc) throws Exception {
        String alg = claims.alg();
        String kid = claims.kid();
        log.debug("[SIGVERIFY] alg={} kid={}", alg, kid);

        switch (alg) {
            case "ES256" -> verifyEs256(token, didDoc, kid);
            case "ES256K" -> {
                ECPublicKey key = keyExtractor.extract(didDoc, "secp256k1", "secp256k1", kid);
                es256kVerifier.verify(token, key);
                validateTimeClaims(claims);
            }
            default -> throw new JWTVerificationException("Unsupported algorithm: " + alg);
        }
    }

    private void verifyEs256(String token, Map<String, Object> didDoc, String kid) throws Exception {
        ECPublicKey key = keyExtractor.extract(didDoc, "P-256", "secp256r1", kid);
        // auth0's ECDSA256 verifier handles ES256 time claims internally
        JWT.require(Algorithm.ECDSA256(key, null)).build().verify(token);
        log.debug("[SIGVERIFY] ES256 passed");
    }

    private void validateTimeClaims(JwtClaims claims) {
        Date now = new Date();
        Date exp = claims.exp();
        Date nbf = claims.nbf();

        if (exp == null) throw new JWTVerificationException("Missing exp claim");
        if (exp.before(now))
            throw new JWTVerificationException("Token expired at " + exp);
        if (nbf != null && nbf.after(now))
            throw new JWTVerificationException("Token not yet valid (nbf=" + nbf + ")");

        log.debug("[SIGVERIFY] time claims OK — {}ms remaining", exp.getTime() - now.getTime());
    }
}
