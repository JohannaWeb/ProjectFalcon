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
import java.security.Security;
import java.security.Signature;
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
        if (sub == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT missing subject");
            return;
        }

        // JWTs are signed by the PDS (iss), not the user (sub).
        // Convert HTTPS issuer URLs to did:web if needed.
        String signingDid = toSigningDid(iss, sub);

        try {
            Map<String, Object> didDoc = didResolver.resolve(signingDid);
            verifyJwtSignature(token, unverified, didDoc);
            request.setAttribute(VIEWER_DID_ATTR, sub);
            chain.doFilter(request, response);
        } catch (Exception e) {
            log.warn("Auth failed for DID {}: {}", signingDid, e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
        }
    }

    private String toSigningDid(String iss, String sub) {
        if (iss == null) return sub;
        if (iss.startsWith("did:")) return iss;
        // HTTPS PDS URL → did:web (e.g. https://bsky.social → did:web:bsky.social)
        String host = iss.replaceFirst("https?://", "").split("/")[0];
        return "did:web:" + host;
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
        byte[] rawSig = Base64.getUrlDecoder().decode(parts[2]);
        Signature s = Signature.getInstance("SHA256withECDSA", "BC");
        s.initVerify(key);
        s.update(data);
        if (!s.verify(derEncodeSignature(rawSig))) throw new JWTVerificationException("ES256K verification failed");
    }

    /** Convert JWT ECDSA signature (R || S, 64 bytes) to DER-encoded ASN.1 SEQUENCE. */
    private byte[] derEncodeSignature(byte[] rawSig) {
        int half = rawSig.length / 2;
        byte[] r = derInt(Arrays.copyOfRange(rawSig, 0, half));
        byte[] s = derInt(Arrays.copyOfRange(rawSig, half, rawSig.length));
        int seqLen = 2 + r.length + 2 + s.length;
        byte[] der = new byte[2 + seqLen];
        int i = 0;
        der[i++] = 0x30;
        der[i++] = (byte) seqLen;
        der[i++] = 0x02;
        der[i++] = (byte) r.length;
        System.arraycopy(r, 0, der, i, r.length); i += r.length;
        der[i++] = 0x02;
        der[i++] = (byte) s.length;
        System.arraycopy(s, 0, der, i, s.length);
        return der;
    }

    /** Trim leading zeros and prepend 0x00 if high bit is set (DER unsigned integer). */
    private byte[] derInt(byte[] bytes) {
        int start = 0;
        while (start < bytes.length - 1 && bytes[start] == 0) start++;
        bytes = Arrays.copyOfRange(bytes, start, bytes.length);
        if ((bytes[0] & 0x80) != 0) {
            byte[] padded = new byte[bytes.length + 1];
            System.arraycopy(bytes, 0, padded, 1, bytes.length);
            return padded;
        }
        return bytes;
    }

    // Multicodec varint prefixes for compressed EC public keys
    private static final byte[] MULTICODEC_SECP256K1 = {(byte) 0xe7, 0x01};
    private static final byte[] MULTICODEC_P256       = {(byte) 0x80, 0x24};
    private static final String BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }
    }

    @SuppressWarnings("unchecked")
    private ECPublicKey extractEcPublicKey(Map<String, Object> didDoc, String jwtCurve, String jcaCurve) throws Exception {
        List<Map<String, Object>> vms = (List<Map<String, Object>>) didDoc.get("verificationMethod");
        if (vms == null || vms.isEmpty()) throw new IllegalArgumentException("No verificationMethod in DID document");
        for (Map<String, Object> vm : vms) {
            // JWK format
            Map<String, Object> jwk = (Map<String, Object>) vm.get("publicKeyJwk");
            if (jwk != null && jwtCurve.equals(jwk.get("crv"))) {
                return buildEcPublicKey((String) jwk.get("x"), (String) jwk.get("y"), jcaCurve);
            }
            // Multikey format (publicKeyMultibase, base58btc 'z' prefix)
            String multibase = (String) vm.get("publicKeyMultibase");
            if (multibase != null && multibase.startsWith("z")) {
                ECPublicKey key = decodeMultikeyPublicKey(multibase, jwtCurve, jcaCurve);
                if (key != null) return key;
            }
        }
        throw new IllegalArgumentException("No " + jwtCurve + " key found in DID document");
    }

    private ECPublicKey decodeMultikeyPublicKey(String multibase, String jwtCurve, String jcaCurve) throws Exception {
        byte[] decoded = base58Decode(multibase.substring(1)); // strip 'z'
        if (decoded.length < 35) return null;

        byte[] expected = "secp256k1".equals(jwtCurve) ? MULTICODEC_SECP256K1 : MULTICODEC_P256;
        if ((decoded[0] & 0xFF) != (expected[0] & 0xFF) || (decoded[1] & 0xFF) != (expected[1] & 0xFF)) {
            return null; // curve mismatch
        }

        byte[] compressed = Arrays.copyOfRange(decoded, 2, 35);

        // Decompress the EC point using BouncyCastle
        org.bouncycastle.jce.spec.ECNamedCurveParameterSpec spec =
                org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec(jcaCurve);
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
            if (digit < 0) throw new IllegalArgumentException("Invalid base58 character: " + c);
            value = value.multiply(base).add(BigInteger.valueOf(digit));
        }
        int leadingZeros = 0;
        for (char c : input.toCharArray()) {
            if (c == '1') leadingZeros++; else break;
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
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(bx, by), spec));
    }
}
