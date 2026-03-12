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
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.Arrays;
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
        // Skip CORS preflight, WebSocket upgrade, and actuator endpoints
        String upgrade = request.getHeader("Upgrade");
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || "websocket".equalsIgnoreCase(upgrade)
                || path.startsWith("/actuator");
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

        String sub = unverified.getSubject();
        String iss = unverified.getIssuer();
        if (iss == null) {
            // App key — no issuer claim
            log.info("App key authentication for DID: {}", sub);
            request.setAttribute(VIEWER_DID_ATTR, sub);
            chain.doFilter(request, response);
            return;
        }
        String alg = unverified.getAlgorithm();
        String kid = unverified.getKeyId();

        log.info("JWT claims - sub: {}, iss: {}, alg: {}, kid: {}", sub, iss, alg, kid);
        log.debug("JWT header: {}", unverified.getHeader());

        try {
            unverified = JWT.decode(token);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT");
            return;
        }

        if (sub == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT missing subject");
            return;
        }

        // JWTs are signed by the PDS (iss), not the user (sub).
        // Convert HTTPS issuer URLs to did:web if needed.
        String signingDid = toSigningDid(iss, sub);

        try {
            log.debug("[VER: 2.0-ECDSASigner] Verifying token for sub: {}, iss: {} (signingDid: {})", sub, iss,
                    signingDid);
            Map<String, Object> didDoc = didResolver.resolve(signingDid);
            verifyJwtSignature(token, unverified, didDoc);
            log.info("Auth successful for DID: {}", sub);
            request.setAttribute(VIEWER_DID_ATTR, sub);
            chain.doFilter(request, response);
        } catch (Exception e) {
            log.warn("Auth failed for DID {}: {} (Algorithm: {}, Issuer: {})", signingDid, e.getMessage(),
                    unverified.getAlgorithm(), iss);
            if (log.isDebugEnabled()) {
                log.debug("Verification error detail", e);
            }
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
        }
    }

    private String toSigningDid(String iss, String sub) {
        if (iss == null)
            return sub;
        if (iss.startsWith("did:"))
            return iss;
        // HTTPS PDS URL → did:web (e.g. https://bsky.social → did:web:bsky.social)
        String host = iss.replaceFirst("https?://", "").split("/")[0];
        return "did:web:" + host;
    }

    private void verifyJwtSignature(String token, DecodedJWT jwt, Map<String, Object> didDoc) throws Exception {
        String alg = jwt.getAlgorithm();
        String kid = jwt.getKeyId();
        if ("ES256".equals(alg)) {
            ECPublicKey key = extractEcPublicKey(didDoc, "P-256", "secp256r1", kid);
            Algorithm.ECDSA256(key, null).verify(jwt);
        } else if ("ES256K".equals(alg)) {
            ECPublicKey key = extractEcPublicKey(didDoc, "secp256k1", "secp256k1", kid);
            verifyEs256k(token, key);
        } else {
            throw new JWTVerificationException("Unsupported algorithm: " + alg);
        }
    }

    private void verifyEs256k(String token, ECPublicKey key) throws Exception {
        String[] parts = token.split("\\.");
        if (parts.length != 3)
            throw new JWTVerificationException("Invalid JWT format");

        log.debug("JWT Header: {}", parts[0]);
        log.debug("JWT Payload: {}", parts[1]);

        byte[] data = (parts[0] + "." + parts[1]).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] rawSig = Base64.getUrlDecoder().decode(parts[2]);

        log.debug("Data to verify (hex): {}", bytesToHex(data));
        log.debug("Raw Sig (hex): {}", bytesToHex(rawSig));

        int half = rawSig.length / 2;
        BigInteger r = new BigInteger(1, Arrays.copyOfRange(rawSig, 0, half));
        BigInteger s = new BigInteger(1, Arrays.copyOfRange(rawSig, half, rawSig.length));

        // Use BouncyCastle's ECDSASigner directly for maximum reliability
        X9ECParameters curve = CustomNamedCurves.getByName("secp256k1");
        ECDomainParameters domainParams = new ECDomainParameters(curve.getCurve(), curve.getG(), curve.getN(),
                curve.getH());

        // Extract the BC point from the JCA key
        org.bouncycastle.math.ec.ECPoint q;
        if (key instanceof BCECPublicKey bcKey) {
            q = bcKey.getQ();
        } else {
            // Fallback for non-BC keys
            org.bouncycastle.jce.spec.ECNamedCurveParameterSpec spec = org.bouncycastle.jce.ECNamedCurveTable
                    .getParameterSpec("secp256k1");
            q = spec.getCurve().createPoint(key.getW().getAffineX(), key.getW().getAffineY());
        }

        ECPublicKeyParameters pubKeyParams = new ECPublicKeyParameters(q, domainParams);
        ECDSASigner signer = new ECDSASigner();

        log.info("Public key point Q - X: {}, Y: {}",
                q.getAffineXCoord().toBigInteger().toString(16),
                q.getAffineYCoord().toBigInteger().toString(16));
        signer.init(false, pubKeyParams);

        // Hash the data manually (SHA-256)
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);

        boolean valid = signer.verifySignature(hash, r, s);

        if (!valid) {
            // Try with Low-S normalization just in case
            BigInteger n = curve.getN();
            BigInteger halfN = n.shiftRight(1);
            if (s.compareTo(halfN) > 0) {
                log.debug("Encountered High-S signature, attempting Low-S verification");
                valid = signer.verifySignature(hash, r, n.subtract(s));
            }
        }

        if (!valid) {
            log.error("Crypto failure: ES256K verification failed. R: {}, S: {}", r.toString(16), s.toString(16));
            throw new JWTVerificationException("ES256K verification failed");
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Multicodec varint prefixes for compressed EC public keys
    private static final byte[] MULTICODEC_SECP256K1 = {(byte) 0xe7, 0x01};
    private static final byte[] MULTICODEC_P256 = {(byte) 0x80, 0x24};
    private static final String BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }
    }

    @SuppressWarnings("unchecked")
    private ECPublicKey extractEcPublicKey(Map<String, Object> didDoc, String jwtCurve, String jcaCurve, String kid)
            throws Exception {
        List<Map<String, Object>> vms = (List<Map<String, Object>>) didDoc.get("verificationMethod");
        if (vms == null || vms.isEmpty())
            throw new IllegalArgumentException("No verificationMethod in DID document");
        String did = (String) didDoc.get("id");

        if (kid != null && !kid.isBlank()) {
            for (Map<String, Object> vm : vms) {
                if (kidMatches(kid, (String) vm.get("id"), did)) {
                    ECPublicKey key = extractVmEcPublicKey(vm, jwtCurve, jcaCurve);
                    if (key != null)
                        return key;
                    throw new IllegalArgumentException("JWT kid matched DID key, but key curve was not " + jwtCurve);
                }
            }
            log.debug("JWT kid {} not found in DID document; falling back to curve-only key lookup", kid);
        }

        for (Map<String, Object> vm : vms) {
            ECPublicKey key = extractVmEcPublicKey(vm, jwtCurve, jcaCurve);
            if (key != null)
                return key;
        }
        throw new IllegalArgumentException("No " + jwtCurve + " key found in DID document");
    }

    @SuppressWarnings("unchecked")
    private ECPublicKey extractVmEcPublicKey(Map<String, Object> vm, String jwtCurve, String jcaCurve)
            throws Exception {
        // JWK format
        Map<String, Object> jwk = (Map<String, Object>) vm.get("publicKeyJwk");
        if (jwk != null && jwtCurve.equals(jwk.get("crv"))) {
            return buildEcPublicKey((String) jwk.get("x"), (String) jwk.get("y"), jcaCurve);
        }
        // Multikey format (publicKeyMultibase, base58btc 'z' prefix)
        String multibase = (String) vm.get("publicKeyMultibase");
        if (multibase != null && multibase.startsWith("z")) {
            return decodeMultikeyPublicKey(multibase, jwtCurve, jcaCurve);
        }
        return null;
    }

    private boolean kidMatches(String kid, String vmId, String did) {
        if (vmId == null || kid == null)
            return false;
        if (kid.equals(vmId))
            return true;
        if (kid.startsWith("#"))
            return vmId.endsWith(kid);
        if (vmId.startsWith("#") && did != null)
            return kid.equals(did + vmId);
        return false;
    }

    private ECPublicKey decodeMultikeyPublicKey(String multibase, String jwtCurve, String jcaCurve) throws Exception {
        byte[] decoded = base58Decode(multibase.substring(1)); // strip 'z'
        if (decoded.length < 35)
            return null;

        byte[] expected = "secp256k1".equals(jwtCurve) ? MULTICODEC_SECP256K1 : MULTICODEC_P256;
        if ((decoded[0] & 0xFF) != (expected[0] & 0xFF) || (decoded[1] & 0xFF) != (expected[1] & 0xFF)) {
            return null; // curve mismatch
        }

        byte[] compressed = Arrays.copyOfRange(decoded, 2, 35);

        // Decompress the EC point using BouncyCastle
        org.bouncycastle.jce.spec.ECNamedCurveParameterSpec spec = org.bouncycastle.jce.ECNamedCurveTable
                .getParameterSpec(jcaCurve);
        org.bouncycastle.math.ec.ECPoint bcPoint = spec.getCurve().decodePoint(compressed);

        ECPoint jcaPoint = new ECPoint(
                bcPoint.getAffineXCoord().toBigInteger(),
                bcPoint.getAffineYCoord().toBigInteger());
        org.bouncycastle.jce.spec.ECNamedCurveSpec jcaSpec = new org.bouncycastle.jce.spec.ECNamedCurveSpec(
                jcaCurve, spec.getCurve(), spec.getG(), spec.getN(), spec.getH());
        return (ECPublicKey) KeyFactory.getInstance("EC", "BC")
                .generatePublic(new ECPublicKeySpec(jcaPoint, jcaSpec));
    }

    private byte[] base58Decode(String input) {
        BigInteger value = BigInteger.ZERO;
        BigInteger base = BigInteger.valueOf(58);
        for (char c : input.toCharArray()) {
            int digit = BASE58_ALPHABET.indexOf(c);
            if (digit < 0)
                throw new IllegalArgumentException("Invalid base58 character: " + c);
            value = value.multiply(base).add(BigInteger.valueOf(digit));
        }
        int leadingZeros = 0;
        for (char c : input.toCharArray()) {
            if (c == '1')
                leadingZeros++;
            else
                break;
        }
        byte[] raw = value.toByteArray();
        int start = (raw.length > 1 && raw[0] == 0) ? 1 : 0;
        byte[] result = new byte[leadingZeros + raw.length - start];
        System.arraycopy(raw, start, result, leadingZeros, raw.length - start);
        return result;
    }

    private ECPublicKey buildEcPublicKey(String x, String y, String curve) throws Exception {
        BigInteger bx = new BigInteger(1, Base64.getUrlDecoder().decode(x));
        BigInteger by = new BigInteger(1, Base64.getUrlDecoder().decode(y));
        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec(curve));
        ECParameterSpec spec = params.getParameterSpec(ECParameterSpec.class);
        return (ECPublicKey) KeyFactory.getInstance("EC")
                .generatePublic(new ECPublicKeySpec(new ECPoint(bx, by), spec));
    }
}