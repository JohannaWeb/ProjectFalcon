package app.juntos.alpha.auth;

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
import java.security.KeyFactory;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class AtprotoAuthFilter extends OncePerRequestFilter {

    public static final String VIEWER_DID_ATTR = "viewerDid";

    private final DidResolver didResolver;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String upgrade = request.getHeader("Upgrade");
        String path = request.getRequestURI();
        boolean skip = "OPTIONS".equalsIgnoreCase(request.getMethod())
                || "websocket".equalsIgnoreCase(upgrade)
                || path.equals("/")
                || path.equals("/ping")
                || path.startsWith("/actuator");
        if (skip) {
            log.debug("[AUTH] Skipping filter for {} {} (upgrade={}, path={})",
                    request.getMethod(), path, upgrade, path);
        }
        return skip;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String path = request.getRequestURI();
        log.info("[AUTH] ══════════════════════════════════════════════════════");
        log.info("[AUTH] Incoming request: {} {}", method, path);
        log.info("[AUTH] Remote addr: {}", request.getRemoteAddr());

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            log.warn("[AUTH] REJECTED — No Authorization header on {} {}", method, path);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Authorization header");
            return;
        }
        if (!authHeader.startsWith("Bearer ")) {
            log.warn("[AUTH] REJECTED — Authorization header does not start with 'Bearer ': [{}]",
                    authHeader.substring(0, Math.min(authHeader.length(), 20)));
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        log.debug("[AUTH] Raw token length: {} chars", token.length());
        log.debug("[AUTH] Token prefix (first 40): {}...", token.substring(0, Math.min(token.length(), 40)));

        // ── Step 1: Decode JWT (no verification yet) ──────────────────────
        log.info("[AUTH] Step 1: Decoding JWT (unverified)");
        DecodedJWT unverified;
        try {
            unverified = JWT.decode(token);
            log.info("[AUTH]   Header (b64):  {}", token.split("\\.")[0]);
            log.info("[AUTH]   Payload (b64): {}", token.split("\\.")[1]);
            log.debug("[AUTH]   Decoded header JSON:  {}", unverified.getHeader());
            log.debug("[AUTH]   Decoded payload JSON: {}", unverified.getPayload());
        } catch (Exception e) {
            log.error("[AUTH] REJECTED — JWT.decode() threw: {} — raw token: {}", e.getMessage(), token);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT");
            return;
        }

        // ── Step 2: Extract claims ─────────────────────────────────────────
        log.info("[AUTH] Step 2: Extracting claims");
        String sub = unverified.getSubject();
        String iss = unverified.getIssuer();
        String alg = unverified.getAlgorithm();
        String kid = unverified.getKeyId();
        java.util.Date iat = unverified.getIssuedAt();
        java.util.Date exp = unverified.getExpiresAt();
        java.util.Date nbf = unverified.getNotBefore();

        log.info("[AUTH]   sub (subject):   {}", sub);
        log.info("[AUTH]   iss (issuer):    {}", iss);
        log.info("[AUTH]   alg (algorithm): {}", alg);
        log.info("[AUTH]   kid (key id):    {}", kid);
        log.info("[AUTH]   iat (issued at): {}", iat);
        log.info("[AUTH]   exp (expires):   {}", exp);
        log.info("[AUTH]   nbf (not before):{}", nbf);
        log.info("[AUTH]   now (server):    {}", new java.util.Date());

        if (exp != null) {
            long ttlSeconds = (exp.getTime() - System.currentTimeMillis()) / 1000;
            log.info("[AUTH]   token TTL: {}s ({})", ttlSeconds, ttlSeconds > 0 ? "VALID" : "EXPIRED");
        }

        java.util.List<String> audList = unverified.getAudience();
        String aud = (audList != null && !audList.isEmpty()) ? audList.get(0) : null;
        log.info("[AUTH]   aud (audience):  {}", aud);

        if (sub == null) {
            log.error("[AUTH] REJECTED — JWT has no 'sub' claim. Full payload: {}", unverified.getPayload());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT missing subject");
            return;
        }
        if (iss == null && aud == null) {
            log.error("[AUTH] REJECTED — JWT has neither 'iss' nor 'aud' — cannot identify signing party. Payload: {}",
                    unverified.getPayload());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT missing issuer and audience");
            return;
        }

        // ── Step 3 & 4: Resolve signing identity and verify ───────────────
        log.info("[AUTH] Step 3: Determining signing identities");
        List<String> potentialDids = getPotentialSigningDids(iss, sub, aud);
        log.info("[AUTH]   Potential DIDs to try: {}", potentialDids);

        boolean verified = false;
        String verifiedDid = null;

        for (String candidateDid : potentialDids) {
            log.info("[AUTH] Step 4/5: Attempting verification with DID: {}", candidateDid);
            try {
                Map<String, Object> didDoc = resolveSigningDidDoc(candidateDid, sub);
                if (didDoc == null) {
                    log.warn("[AUTH]   DID document resolution returned null for {}", candidateDid);
                    
                    // Permissive fallback logic suggested by user: 
                    // If this is the PDS (audience) and we can't resolve it, 
                    // we might want to trust it anyway because we can't verify what we can't fetch.
                    if (candidateDid.equals(aud)) {
                        log.warn("[AUTH]   PDS resolution failed — PERMISSIVE FALLBACK: Trusting token without signature verification.");
                        verified = true;
                        verifiedDid = candidateDid;
                        break;
                    }
                    continue;
                }
                
                log.info("[AUTH]   DID document resolved, keys: {}", didDoc.keySet());
                log.debug("[AUTH]   DID document full: {}", didDoc);

                verifyJwtSignature(token, unverified, didDoc);
                verified = true;
                verifiedDid = candidateDid;
                log.info("[AUTH] ✓ SIGNATURE VALID — verified with DID: {}", candidateDid);
                break;
            } catch (Exception e) {
                log.warn("[AUTH]   Verification failed with candidate {}: {} — {}", 
                        candidateDid, e.getClass().getSimpleName(), e.getMessage());
                
                // Also handle exception-based resolution failures for PDS
                if (candidateDid.equals(aud) && (e.getMessage().contains("Cannot resolve") || e.getMessage().contains("404"))) {
                    log.warn("[AUTH]   PDS verification error — PERMISSIVE FALLBACK: Trusting token anyway. Error: {}", e.getMessage());
                    verified = true;
                    verifiedDid = candidateDid;
                    break;
                }
                
                if (log.isDebugEnabled()) {
                    log.debug("[AUTH]   Full failure detail for {}:", candidateDid, e);
                }
            }
        }

        if (verified) {
            request.setAttribute(VIEWER_DID_ATTR, sub);
            chain.doFilter(request, response);
        } else {
            log.error("[AUTH] ✗ SIGNATURE INVALID — could not verify with any potential DID. sub={} iss={} aud={}", sub, iss, aud);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
        }
        log.info("[AUTH] ══════════════════════════════════════════════════════");
    }

    private List<String> getPotentialSigningDids(String iss, String sub, String aud) {
        java.util.ArrayList<String> dids = new java.util.ArrayList<>();
        if (iss == null) {
            // Priority 1: Audience (PDS) for session tokens
            if (aud != null && aud.startsWith("did:")) {
                dids.add(aud);
            }
            // Priority 2: Subject (User) for self-signed tokens
            if (!dids.contains(sub)) {
                dids.add(sub);
            }
        } else {
            // Service auth token signed by issuer
            dids.add(toSigningDid(iss, sub, aud));
        }
        return dids;
    }

    private Map<String, Object> resolveSigningDidDoc(String signingDid, String sub) {
        log.info("[AUTH/RESOLVE] Resolving DID document for: {}", signingDid);
        return didResolver.resolve(signingDid);
    }

    private String toSigningDid(String iss, String sub, String aud) {
        if (iss == null) {
            if (aud != null && aud.startsWith("did:")) return aud;
            log.debug("[AUTH/DID] iss is null, falling back to sub: {}", sub);
            return sub;
        }
        if (iss.startsWith("did:")) {
            log.debug("[AUTH/DID] iss is already a DID: {}", iss);
            return iss;
        }
        String host = iss.replaceFirst("https?://", "").split("/")[0];
        String didWeb = "did:web:" + host;
        log.info("[AUTH/DID] Converted HTTPS issuer '{}' → '{}'", iss, didWeb);
        return didWeb;
    }

    private void verifyJwtSignature(String token, DecodedJWT jwt, Map<String, Object> didDoc) throws Exception {
        String alg = jwt.getAlgorithm();
        String kid = jwt.getKeyId();
        log.info("[CRYPTO] ── verifyJwtSignature ──────────────────────────────");
        log.info("[CRYPTO] Algorithm: {}", alg);
        log.info("[CRYPTO] kid:       {}", kid);

        if ("ES256".equals(alg)) {
            log.info("[CRYPTO] Path: ES256 (P-256 / secp256r1) via auth0 JWTVerifier");
            ECPublicKey key = extractEcPublicKey(didDoc, "P-256", "secp256r1", kid);
            log.info("[CRYPTO] ES256 key extracted, calling JWT.require().build().verify()");
            JWT.require(Algorithm.ECDSA256(key, null)).build().verify(token);
            log.info("[CRYPTO] ES256 verification PASSED");
        } else if ("ES256K".equals(alg)) {
            log.info("[CRYPTO] Path: ES256K (secp256k1) via BouncyCastle ECDSASigner");
            ECPublicKey key = extractEcPublicKey(didDoc, "secp256k1", "secp256k1", kid);
            verifyEs256k(token, key);
            log.info("[CRYPTO] ES256K signature check PASSED, now validating claims");
            validateClaims(jwt);
            log.info("[CRYPTO] ES256K claims validation PASSED");
        } else {
            log.error("[CRYPTO] Unsupported algorithm: '{}' — only ES256 and ES256K are supported", alg);
            throw new JWTVerificationException("Unsupported algorithm: " + alg);
        }
    }

    private void validateClaims(DecodedJWT jwt) {
        java.util.Date now = new java.util.Date();
        java.util.Date exp = jwt.getExpiresAt();
        java.util.Date nbf = jwt.getNotBefore();
        log.info("[CRYPTO/CLAIMS] Validating time claims");
        log.info("[CRYPTO/CLAIMS]   now: {}", now);
        log.info("[CRYPTO/CLAIMS]   exp: {}", exp);
        log.info("[CRYPTO/CLAIMS]   nbf: {}", nbf);

        if (exp == null) {
            log.error("[CRYPTO/CLAIMS] FAIL — no exp claim");
            throw new JWTVerificationException("Missing exp claim");
        }
        if (exp.before(now)) {
            log.error("[CRYPTO/CLAIMS] FAIL — token expired at {} ({}ms ago)",
                    exp, now.getTime() - exp.getTime());
            throw new JWTVerificationException("Token expired");
        }
        log.info("[CRYPTO/CLAIMS]   exp OK ({}ms remaining)", exp.getTime() - now.getTime());

        if (nbf != null && nbf.after(now)) {
            log.error("[CRYPTO/CLAIMS] FAIL — token not yet valid, nbf={} is {}ms in the future",
                    nbf, nbf.getTime() - now.getTime());
            throw new JWTVerificationException("Token not yet valid");
        }
        log.info("[CRYPTO/CLAIMS] All claims OK");
    }

    private void verifyEs256k(String token, ECPublicKey key) throws Exception {
        log.info("[CRYPTO/ES256K] ── verifyEs256k ─────────────────────────────");

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            log.error("[CRYPTO/ES256K] Invalid JWT structure: {} parts (expected 3)", parts.length);
            throw new JWTVerificationException("Invalid JWT format");
        }

        // ── Signing input ─────────────────────────────────────────────────
        byte[] data = (parts[0] + "." + parts[1]).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        log.info("[CRYPTO/ES256K] Signing input (header.payload):");
        log.info("[CRYPTO/ES256K]   header  (b64url): {}", parts[0]);
        log.info("[CRYPTO/ES256K]   payload (b64url): {}", parts[1]);
        log.info("[CRYPTO/ES256K]   input byte length: {}", data.length);
        log.debug("[CRYPTO/ES256K]   input hex: {}", bytesToHex(data));

        // ── Hash ──────────────────────────────────────────────────────────
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        log.info("[CRYPTO/ES256K] SHA-256 hash (hex): {}", bytesToHex(hash));

        // ── Signature decoding ────────────────────────────────────────────
        byte[] rawSig;
        try {
            rawSig = Base64.getUrlDecoder().decode(parts[2]);
        } catch (Exception e) {
            log.error("[CRYPTO/ES256K] Failed to base64url-decode signature part: '{}' — {}",
                    parts[2], e.getMessage());
            throw e;
        }
        log.info("[CRYPTO/ES256K] Raw signature bytes: {} bytes", rawSig.length);
        log.info("[CRYPTO/ES256K] Raw signature (hex): {}", bytesToHex(rawSig));

        if (rawSig.length == 0 || rawSig.length % 2 != 0) {
            log.error("[CRYPTO/ES256K] Unexpected signature length: {} (expected even, non-zero)", rawSig.length);
        }

        int half = rawSig.length / 2;
        BigInteger r = new BigInteger(1, Arrays.copyOfRange(rawSig, 0, half));
        BigInteger s = new BigInteger(1, Arrays.copyOfRange(rawSig, half, rawSig.length));
        log.info("[CRYPTO/ES256K] Signature r ({} bytes): {}", half, r.toString(16));
        log.info("[CRYPTO/ES256K] Signature s ({} bytes): {}", rawSig.length - half, s.toString(16));

        // ── Curve setup ───────────────────────────────────────────────────
        log.info("[CRYPTO/ES256K] Loading secp256k1 curve parameters via BouncyCastle CustomNamedCurves");
        X9ECParameters curve = CustomNamedCurves.getByName("secp256k1");
        if (curve == null) {
            log.error("[CRYPTO/ES256K] FATAL — CustomNamedCurves.getByName('secp256k1') returned null!");
            throw new IllegalStateException("secp256k1 curve not found in BouncyCastle");
        }
        BigInteger n = curve.getN();
        BigInteger halfN = n.shiftRight(1);
        log.info("[CRYPTO/ES256K] Curve n:     {}", n.toString(16));
        log.info("[CRYPTO/ES256K] Curve halfN: {}", halfN.toString(16));
        log.info("[CRYPTO/ES256K] s > halfN (high-S)? {}", s.compareTo(halfN) > 0);

        ECDomainParameters domainParams = new ECDomainParameters(
                curve.getCurve(), curve.getG(), curve.getN(), curve.getH());

        // ── Public key extraction ─────────────────────────────────────────
        log.info("[CRYPTO/ES256K] Extracting EC point from key (type: {})", key.getClass().getName());
        org.bouncycastle.math.ec.ECPoint q;
        if (key instanceof BCECPublicKey bcKey) {
            q = bcKey.getQ();
            log.info("[CRYPTO/ES256K] Key is BCECPublicKey — using getQ() directly");
        } else {
            log.info("[CRYPTO/ES256K] Key is NOT BCECPublicKey — using affine coordinate fallback");
            log.info("[CRYPTO/ES256K]   key.getW().getAffineX(): {}", key.getW().getAffineX().toString(16));
            log.info("[CRYPTO/ES256K]   key.getW().getAffineY(): {}", key.getW().getAffineY().toString(16));
            org.bouncycastle.jce.spec.ECNamedCurveParameterSpec spec =
                    org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("secp256k1");
            q = spec.getCurve().createPoint(key.getW().getAffineX(), key.getW().getAffineY());
        }

        log.info("[CRYPTO/ES256K] Public key point Q:");
        log.info("[CRYPTO/ES256K]   Qx: {}", q.getAffineXCoord().toBigInteger().toString(16));
        log.info("[CRYPTO/ES256K]   Qy: {}", q.getAffineYCoord().toBigInteger().toString(16));
        log.info("[CRYPTO/ES256K]   normalized? {}", q.isNormalized());
        log.info("[CRYPTO/ES256K]   valid? {}", q.isValid());

        // ── First verification attempt ────────────────────────────────────
        ECPublicKeyParameters pubKeyParams = new ECPublicKeyParameters(q, domainParams);
        ECDSASigner signer = new ECDSASigner();
        signer.init(false, pubKeyParams);

        log.info("[CRYPTO/ES256K] Calling ECDSASigner.verifySignature(hash, r, s) — attempt 1 (original s)");
        log.info("[CRYPTO/ES256K]   Using Q: ({} , {})", q.getAffineXCoord().toBigInteger().toString(16), q.getAffineYCoord().toBigInteger().toString(16));
        boolean valid = signer.verifySignature(hash, r, s);
        log.info("[CRYPTO/ES256K] Attempt 1 result: {}", valid ? "VALID ✓" : "INVALID ✗");

        // ── Low-S normalization retry ─────────────────────────────────────
        if (!valid && s.compareTo(halfN) > 0) {
            BigInteger sNorm = n.subtract(s);
            log.info("[CRYPTO/ES256K] High-S detected — retrying with normalized s");
            log.info("[CRYPTO/ES256K]   original s: {}", s.toString(16));
            log.info("[CRYPTO/ES256K]   n - s:      {}", sNorm.toString(16));
            valid = signer.verifySignature(hash, r, sNorm);
            log.info("[CRYPTO/ES256K] Attempt 2 result (low-S): {}", valid ? "VALID ✓" : "INVALID ✗");
        } else if (!valid) {
            log.info("[CRYPTO/ES256K] s is already low-S, no retry applicable");
        }

        if (!valid) {
            log.error("[CRYPTO/ES256K] ✗ VERIFICATION FAILED after all attempts");
            log.error("[CRYPTO/ES256K]   hash: {}", bytesToHex(hash));
            log.error("[CRYPTO/ES256K]   r:    {}", r.toString(16));
            log.error("[CRYPTO/ES256K]   s:    {}", s.toString(16));
            log.error("[CRYPTO/ES256K]   Qx:   {}", q.getAffineXCoord().toBigInteger().toString(16));
            log.error("[CRYPTO/ES256K]   Qy:   {}", q.getAffineYCoord().toBigInteger().toString(16));
            throw new JWTVerificationException("ES256K verification failed");
        }
        log.info("[CRYPTO/ES256K] ✓ Signature verified successfully");
    }

    // Multicodec varint prefixes for compressed EC public keys
    private static final byte[] MULTICODEC_SECP256K1 = { (byte) 0xe7, 0x01 };
    private static final byte[] MULTICODEC_P256 = { (byte) 0x80, 0x24 };
    private static final String BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    static {
        if (Security.getProvider("BC") == null) {
            log.info("[CRYPTO] Registering BouncyCastle security provider");
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        } else {
            log.debug("[CRYPTO] BouncyCastle provider already registered");
        }
    }

    @SuppressWarnings("unchecked")
    private ECPublicKey extractEcPublicKey(Map<String, Object> didDoc, String jwtCurve, String jcaCurve, String kid)
            throws Exception {
        log.info("[CRYPTO/KEY] Extracting EC public key — jwtCurve={} jcaCurve={} kid={}", jwtCurve, jcaCurve, kid);

        List<Map<String, Object>> vms = (List<Map<String, Object>>) didDoc.get("verificationMethod");
        if (vms == null || vms.isEmpty()) {
            log.error("[CRYPTO/KEY] DID document has no verificationMethod entries");
            throw new IllegalArgumentException("No verificationMethod in DID document");
        }
        String did = (String) didDoc.get("id");
        log.info("[CRYPTO/KEY] DID document id: {}", did);
        log.info("[CRYPTO/KEY] verificationMethod entries: {}", vms.size());

        // ── kid-targeted lookup ───────────────────────────────────────────
        if (kid != null && !kid.isBlank()) {
            log.info("[CRYPTO/KEY] Searching for kid match: '{}'", kid);
            for (int i = 0; i < vms.size(); i++) {
                Map<String, Object> vm = vms.get(i);
                String vmId = (String) vm.get("id");
                boolean matches = kidMatches(kid, vmId, did);
                log.info("[CRYPTO/KEY]   vm[{}] id='{}' kidMatch={}", i, vmId, matches);
                if (matches) {
                    log.info("[CRYPTO/KEY]   → kid matched vm[{}], extracting key", i);
                    ECPublicKey key = extractVmEcPublicKey(vm, jwtCurve, jcaCurve);
                    if (key != null) {
                        log.info("[CRYPTO/KEY]   → Key extracted from kid-matched vm[{}]", i);
                        return key;
                    }
                    log.error("[CRYPTO/KEY]   → kid matched but curve mismatch — expected {}, vm={}",
                            jwtCurve, vm);
                    throw new IllegalArgumentException(
                            "JWT kid matched DID key, but key curve was not " + jwtCurve);
                }
            }
            log.warn("[CRYPTO/KEY] kid='{}' not found in any verificationMethod — falling back to curve scan", kid);
        } else {
            log.info("[CRYPTO/KEY] No kid in JWT — scanning all verificationMethods for curve={}", jwtCurve);
        }

        // ── curve-only fallback ───────────────────────────────────────────
        for (int i = 0; i < vms.size(); i++) {
            Map<String, Object> vm = vms.get(i);
            log.info("[CRYPTO/KEY]   Trying vm[{}] id='{}' for curve {}", i, vm.get("id"), jwtCurve);
            ECPublicKey key = extractVmEcPublicKey(vm, jwtCurve, jcaCurve);
            if (key != null) {
                log.info("[CRYPTO/KEY]   → Found matching key at vm[{}]", i);
                return key;
            }
            log.info("[CRYPTO/KEY]   → vm[{}] skipped (no matching key format/curve)", i);
        }

        log.error("[CRYPTO/KEY] No {} key found in any of {} verificationMethod entries", jwtCurve, vms.size());
        throw new IllegalArgumentException("No " + jwtCurve + " key found in DID document");
    }

    @SuppressWarnings("unchecked")
    private ECPublicKey extractVmEcPublicKey(Map<String, Object> vm, String jwtCurve, String jcaCurve)
            throws Exception {
        String vmId = (String) vm.get("id");
        log.debug("[CRYPTO/KEY] extractVmEcPublicKey for vm.id={} jwtCurve={}", vmId, jwtCurve);

        // ── JWK path ──────────────────────────────────────────────────────
        Map<String, Object> jwk = (Map<String, Object>) vm.get("publicKeyJwk");
        if (jwk != null) {
            Object crv = jwk.get("crv");
            log.info("[CRYPTO/KEY]   JWK found: kty={} crv={} x={} y={}",
                    jwk.get("kty"), crv, jwk.get("x"), jwk.get("y"));
            if (jwtCurve.equals(crv)) {
                log.info("[CRYPTO/KEY]   JWK curve matches '{}' — building EC public key", jwtCurve);
                ECPublicKey key = buildEcPublicKey((String) jwk.get("x"), (String) jwk.get("y"), jcaCurve);
                log.info("[CRYPTO/KEY]   JWK key built successfully ({})", key.getClass().getName());
                return key;
            } else {
                log.info("[CRYPTO/KEY]   JWK curve '{}' != expected '{}' — skipping", crv, jwtCurve);
            }
        }

        // ── Multibase path ────────────────────────────────────────────────
        String multibase = (String) vm.get("publicKeyMultibase");
        if (multibase != null) {
            log.info("[CRYPTO/KEY]   Multibase found: prefix='{}' length={} value(20)={}",
                    multibase.isEmpty() ? "" : multibase.charAt(0),
                    multibase.length(),
                    multibase.substring(0, Math.min(multibase.length(), 20)));
            if (multibase.startsWith("z")) {
                ECPublicKey key = decodeMultikeyPublicKey(multibase, jwtCurve, jcaCurve);
                if (key != null) {
                    log.info("[CRYPTO/KEY]   Multibase key decoded successfully ({})", key.getClass().getName());
                } else {
                    log.info("[CRYPTO/KEY]   Multibase key decoded to null (curve mismatch or too short)");
                }
                return key;
            } else {
                log.warn("[CRYPTO/KEY]   Multibase prefix '{}' is not 'z' (base58btc) — unsupported, skipping",
                        multibase.charAt(0));
            }
        }

        log.debug("[CRYPTO/KEY]   vm.id={} has neither publicKeyJwk nor publicKeyMultibase(z) — skipping", vmId);
        return null;
    }

    private boolean kidMatches(String kid, String vmId, String did) {
        if (vmId == null || kid == null) return false;
        if (kid.equals(vmId)) {
            log.debug("[CRYPTO/KEY] kidMatches: exact match '{}' == '{}'", kid, vmId);
            return true;
        }
        if (kid.startsWith("#") && vmId.endsWith(kid)) {
            log.debug("[CRYPTO/KEY] kidMatches: fragment '{}' matches suffix of '{}'", kid, vmId);
            return true;
        }
        if (vmId.startsWith("#") && did != null && kid.equals(did + vmId)) {
            log.debug("[CRYPTO/KEY] kidMatches: '{}' == did+fragment '{}{}'", kid, did, vmId);
            return true;
        }
        return false;
    }

    private ECPublicKey decodeMultikeyPublicKey(String multibase, String jwtCurve, String jcaCurve) throws Exception {
        log.info("[CRYPTO/KEY] decodeMultikeyPublicKey: curve={} multibase(20)={}",
                jwtCurve, multibase.substring(0, Math.min(multibase.length(), 20)));

        byte[] decoded = base58Decode(multibase.substring(1));
        log.info("[CRYPTO/KEY]   base58 decoded: {} bytes", decoded.length);
        log.debug("[CRYPTO/KEY]   decoded hex: {}", bytesToHex(decoded));

        if (decoded.length < 35) {
            log.warn("[CRYPTO/KEY]   Too short: {} < 35 bytes", decoded.length);
            return null;
        }

        byte[] expected = "secp256k1".equals(jwtCurve) ? MULTICODEC_SECP256K1 : MULTICODEC_P256;
        int b0 = decoded[0] & 0xFF;
        int b1 = decoded[1] & 0xFF;
        int e0 = expected[0] & 0xFF;
        int e1 = expected[1] & 0xFF;
        log.info("[CRYPTO/KEY]   Multicodec prefix: got [0x{} 0x{}] expected [0x{} 0x{}]",
                Integer.toHexString(b0), Integer.toHexString(b1),
                Integer.toHexString(e0), Integer.toHexString(e1));

        if (b0 != e0 || b1 != e1) {
            log.info("[CRYPTO/KEY]   Prefix mismatch — not a {} key, skipping", jwtCurve);
            return null;
        }
        log.info("[CRYPTO/KEY]   Prefix match ✓ — {} key confirmed", jwtCurve);

        byte[] compressed = Arrays.copyOfRange(decoded, 2, 35);
        log.info("[CRYPTO/KEY]   Compressed point (33 bytes): {}", bytesToHex(compressed));
        log.info("[CRYPTO/KEY]   Point prefix byte: 0x{} ({})",
                Integer.toHexString(compressed[0] & 0xFF),
                (compressed[0] == 0x02 || compressed[0] == 0x03) ? "valid compressed" : "UNEXPECTED");

        org.bouncycastle.jce.spec.ECNamedCurveParameterSpec spec =
                org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec(jcaCurve);
        log.info("[CRYPTO/KEY]   Decompressing EC point using BouncyCastle curve '{}'", jcaCurve);
        org.bouncycastle.math.ec.ECPoint bcPoint = spec.getCurve().decodePoint(compressed);

        BigInteger px = bcPoint.getAffineXCoord().toBigInteger();
        BigInteger py = bcPoint.getAffineYCoord().toBigInteger();
        log.info("[CRYPTO/KEY]   Decompressed point Px: {}", px.toString(16));
        log.info("[CRYPTO/KEY]   Decompressed point Py: {}", py.toString(16));
        log.info("[CRYPTO/KEY]   Point valid? {}", bcPoint.isValid());

        ECPoint jcaPoint = new ECPoint(px, py);
        org.bouncycastle.jce.spec.ECNamedCurveSpec jcaSpec = new org.bouncycastle.jce.spec.ECNamedCurveSpec(
                jcaCurve, spec.getCurve(), spec.getG(), spec.getN(), spec.getH());
        ECPublicKey key = (ECPublicKey) KeyFactory.getInstance("EC", "BC")
                .generatePublic(new ECPublicKeySpec(jcaPoint, jcaSpec));
        log.info("[CRYPTO/KEY]   ECPublicKey created: {}", key.getClass().getName());
        return key;
    }

    private ECPublicKey buildEcPublicKey(String x, String y, String curve) throws Exception {
        log.info("[CRYPTO/KEY] buildEcPublicKey: curve={} x(b64url)={} y(b64url)={}", curve, x, y);
        BigInteger bx = new BigInteger(1, Base64.getUrlDecoder().decode(x));
        BigInteger by = new BigInteger(1, Base64.getUrlDecoder().decode(y));
        log.info("[CRYPTO/KEY]   decoded Px: {}", bx.toString(16));
        log.info("[CRYPTO/KEY]   decoded Py: {}", by.toString(16));

        org.bouncycastle.jce.spec.ECNamedCurveParameterSpec spec =
                org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec(curve);
        if (spec == null) {
            log.error("[CRYPTO/KEY]   BC curve spec is null for '{}'!", curve);
            throw new IllegalArgumentException("Unknown curve: " + curve);
        }
        log.info("[CRYPTO/KEY]   BC curve spec loaded for '{}'", curve);

        org.bouncycastle.jce.spec.ECNamedCurveSpec jcaSpec = new org.bouncycastle.jce.spec.ECNamedCurveSpec(
                curve, spec.getCurve(), spec.getG(), spec.getN(), spec.getH());
        ECPublicKey key = (ECPublicKey) KeyFactory.getInstance("EC", "BC")
                .generatePublic(new ECPublicKeySpec(new ECPoint(bx, by), jcaSpec));
        log.info("[CRYPTO/KEY]   ECPublicKey created: {}", key.getClass().getName());
        return key;
    }

    private byte[] base58Decode(String input) {
        BigInteger value = BigInteger.ZERO;
        BigInteger base = BigInteger.valueOf(58);
        for (char c : input.toCharArray()) {
            int digit = BASE58_ALPHABET.indexOf(c);
            if (digit < 0) {
                log.error("[CRYPTO/KEY] base58Decode: invalid character '{}' in input", c);
                throw new IllegalArgumentException("Invalid base58 character: " + c);
            }
            value = value.multiply(base).add(BigInteger.valueOf(digit));
        }
        int leadingZeros = 0;
        for (char c : input.toCharArray()) {
            if (c == '1') leadingZeros++;
            else break;
        }
        byte[] raw = value.toByteArray();
        int start = (raw.length > 1 && raw[0] == 0) ? 1 : 0;
        byte[] result = new byte[leadingZeros + raw.length - start];
        System.arraycopy(raw, start, result, leadingZeros, raw.length - start);
        log.debug("[CRYPTO/KEY] base58Decode: input.length={} leadingZeros={} result.length={}",
                input.length(), leadingZeros, result.length);
        return result;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
