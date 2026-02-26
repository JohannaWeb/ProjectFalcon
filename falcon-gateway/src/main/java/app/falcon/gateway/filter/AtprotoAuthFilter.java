package app.falcon.gateway.filter;

import app.falcon.gateway.service.DidResolver;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.Base64;
import java.util.List;
import java.util.Map;

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

            // Decode first to extract the DID (issuer/subject) before full verification
            final DecodedJWT unverifiedJwt;
            try {
                unverifiedJwt = JWT.decode(token);
            } catch (Exception e) {
                log.error("Failed to decode JWT structure", e);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            final String userDid = (unverifiedJwt.getSubject() != null)
                    ? unverifiedJwt.getSubject()
                    : unverifiedJwt.getIssuer();

            if (userDid == null) {
                log.warn("JWT has no subject or issuer claim");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Resolve DID document then verify the JWT signature against the public key
            return didResolver.resolve(userDid)
                    .flatMap(didDoc -> {
                        try {
                            verifyJwtSignature(token, unverifiedJwt, didDoc);
                        } catch (Exception e) {
                            throw new RuntimeException("JWT signature verification failed: " + e.getMessage(), e);
                        }

                        log.info("JWT signature verified for DID: {}", userDid);

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
        };
    }

    /**
     * Verifies the JWT signature against the verificationMethod keys in the DID
     * document.
     * Supports P-256 (ES256) via java-jwt, and secp256k1 (ES256K) via JCA directly.
     */
    private void verifyJwtSignature(String token, DecodedJWT jwt, Map<String, Object> didDoc) throws Exception {
        String alg = jwt.getAlgorithm(); // "ES256" or "ES256K"

        if ("ES256".equals(alg)) {
            ECPublicKey publicKey = extractEcPublicKey(didDoc, "P-256", "secp256r1");
            Algorithm algorithm = Algorithm.ECDSA256(publicKey, null);
            algorithm.verify(jwt);
        } else if ("ES256K".equals(alg)) {
            // java-jwt doesn't expose ECDSA256K, verify via JCA
            ECPublicKey publicKey = extractEcPublicKey(didDoc, "secp256k1", "secp256k1");
            verifyEs256k(token, publicKey);
        } else {
            throw new JWTVerificationException("Unsupported JWT algorithm: " + alg);
        }
    }

    /** Manual ES256K verification using JCA Signature. */
    private void verifyEs256k(String token, ECPublicKey publicKey) throws Exception {
        String[] parts = token.split("\\.");
        if (parts.length != 3)
            throw new JWTVerificationException("Invalid JWT format");

        byte[] headerAndPayload = (parts[0] + "." + parts[1]).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);

        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initVerify(publicKey);
        sig.update(headerAndPayload);

        if (!sig.verify(signatureBytes)) {
            throw new JWTVerificationException("ES256K signature verification failed");
        }
    }

    /**
     * Extracts an EC public key from a DID document's verificationMethod array.
     *
     * @param didDoc       the parsed DID document
     * @param jwtCurveName the "crv" value to match in the JWK (e.g. "P-256" or
     *                     "secp256k1")
     * @param jcaCurveName the JCA standard curve name (e.g. "secp256r1" or
     *                     "secp256k1")
     */
    @SuppressWarnings("unchecked")
    private ECPublicKey extractEcPublicKey(Map<String, Object> didDoc,
            String jwtCurveName,
            String jcaCurveName) throws Exception {
        List<Map<String, Object>> verificationMethods = (List<Map<String, Object>>) didDoc.get("verificationMethod");

        if (verificationMethods == null || verificationMethods.isEmpty()) {
            throw new IllegalArgumentException("No verificationMethod found in DID document");
        }

        for (Map<String, Object> vm : verificationMethods) {
            Map<String, Object> jwk = (Map<String, Object>) vm.get("publicKeyJwk");
            if (jwk != null && jwtCurveName.equals(jwk.get("crv"))) {
                return buildEcPublicKey((String) jwk.get("x"), (String) jwk.get("y"), jcaCurveName);
            }
        }

        throw new IllegalArgumentException(
                "No %s verificationMethod found in DID document for DID: %s"
                        .formatted(jwtCurveName, didDoc.get("id")));
    }

    private ECPublicKey buildEcPublicKey(String xBase64Url, String yBase64Url, String jcaCurveName) throws Exception {
        byte[] xBytes = Base64.getUrlDecoder().decode(xBase64Url);
        byte[] yBytes = Base64.getUrlDecoder().decode(yBase64Url);

        BigInteger x = new BigInteger(1, xBytes);
        BigInteger y = new BigInteger(1, yBytes);

        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec(jcaCurveName));
        ECParameterSpec ecParameterSpec = parameters.getParameterSpec(ECParameterSpec.class);

        ECPublicKeySpec keySpec = new ECPublicKeySpec(new ECPoint(x, y), ecParameterSpec);
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(keySpec);
    }

    public static class Config {
        // Configuration fields if needed
    }
}
