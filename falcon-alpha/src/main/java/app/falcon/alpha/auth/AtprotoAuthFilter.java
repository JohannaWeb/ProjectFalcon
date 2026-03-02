package app.falcon.alpha.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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
@RequiredArgsConstructor
@Slf4j
public class AtprotoAuthFilter extends OncePerRequestFilter {

    public static final String VIEWER_DID_ATTR = "viewerDid";

    private final DidResolver didResolver;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip WebSocket upgrade and actuator endpoints
        String upgrade = request.getHeader("Upgrade");
        String path = request.getRequestURI();
        return "websocket".equalsIgnoreCase(upgrade) || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        DecodedJWT unverified;
        try {
            unverified = JWT.decode(token);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT");
            return;
        }

        String did = unverified.getSubject() != null ? unverified.getSubject() : unverified.getIssuer();
        if (did == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT missing subject/issuer");
            return;
        }

        try {
            Map<String, Object> didDoc = didResolver.resolve(did);
            verifyJwtSignature(token, unverified, didDoc);
            request.setAttribute(VIEWER_DID_ATTR, did);
            chain.doFilter(request, response);
        } catch (Exception e) {
            log.warn("Auth failed for DID {}: {}", did, e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
        }
    }

    private void verifyJwtSignature(String token, DecodedJWT jwt, Map<String, Object> didDoc) throws Exception {
        String alg = jwt.getAlgorithm();
        if ("ES256".equals(alg)) {
            ECPublicKey key = extractEcPublicKey(didDoc, "P-256", "secp256r1");
            Algorithm.ECDSA256(key, null).verify(jwt);
        } else if ("ES256K".equals(alg)) {
            ECPublicKey key = extractEcPublicKey(didDoc, "secp256k1", "secp256k1");
            verifyEs256k(token, key);
        } else {
            throw new JWTVerificationException("Unsupported algorithm: " + alg);
        }
    }

    private void verifyEs256k(String token, ECPublicKey key) throws Exception {
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new JWTVerificationException("Invalid JWT format");
        byte[] data = (parts[0] + "." + parts[1]).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] sig = Base64.getUrlDecoder().decode(parts[2]);
        Signature s = Signature.getInstance("SHA256withECDSA");
        s.initVerify(key);
        s.update(data);
        if (!s.verify(sig)) throw new JWTVerificationException("ES256K verification failed");
    }

    @SuppressWarnings("unchecked")
    private ECPublicKey extractEcPublicKey(Map<String, Object> didDoc, String jwtCurve, String jcaCurve) throws Exception {
        List<Map<String, Object>> vms = (List<Map<String, Object>>) didDoc.get("verificationMethod");
        if (vms == null || vms.isEmpty()) throw new IllegalArgumentException("No verificationMethod in DID document");
        for (Map<String, Object> vm : vms) {
            Map<String, Object> jwk = (Map<String, Object>) vm.get("publicKeyJwk");
            if (jwk != null && jwtCurve.equals(jwk.get("crv"))) {
                return buildEcPublicKey((String) jwk.get("x"), (String) jwk.get("y"), jcaCurve);
            }
        }
        throw new IllegalArgumentException("No " + jwtCurve + " key found in DID document");
    }

    private ECPublicKey buildEcPublicKey(String x, String y, String curve) throws Exception {
        BigInteger bx = new BigInteger(1, Base64.getUrlDecoder().decode(x));
        BigInteger by = new BigInteger(1, Base64.getUrlDecoder().decode(y));
        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec(curve));
        ECParameterSpec spec = params.getParameterSpec(ECParameterSpec.class);
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(bx, by), spec));
    }
}
