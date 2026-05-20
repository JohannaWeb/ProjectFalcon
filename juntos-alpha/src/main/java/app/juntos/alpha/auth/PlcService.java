package app.juntos.alpha.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

/**
 * High-level service for Google OAuth identity management:
 * generates custodial P-256 key pairs, registers did:plc identities on plc.directory,
 * and issues JWTs that the AT Protocol auth filter can verify.
 */
@ApplicationScoped
@Slf4j
public class PlcService {

    static {
        if (Security.getProvider("BC") == null) Security.addProvider(new BouncyCastleProvider());
    }

    @Inject
    PlcGenesisEncoder encoder;

    // ── Key management ────────────────────────────────────────────────────────

    public KeyPair generateP256KeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
        kpg.initialize(new ECGenParameterSpec("P-256"));
        return kpg.generateKeyPair();
    }

    public String privateKeyToPkcs8Base64(PrivateKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public ECPrivateKey pkcs8Base64ToPrivateKey(String b64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(b64);
        return (ECPrivateKey) KeyFactory.getInstance("EC", "BC")
                .generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    // ── DID creation ──────────────────────────────────────────────────────────

    public String createPlcDid(KeyPair keyPair) throws Exception {
        String keyDid = encoder.toDidKey(keyPair);
        byte[] unsignedCbor = encoder.encodeGenesisOpCbor(keyDid);
        String did = encoder.deriveDid(unsignedCbor);
        String sig = encoder.signAndEncode(unsignedCbor, keyPair.getPrivate());

        String opJson = "{\"sig\":\"" + sig + "\",\"type\":\"plc_operation\",\"prev\":null,"
                + "\"services\":{},\"alsoKnownAs\":[],"
                + "\"rotationKeys\":[\"" + keyDid + "\"],"
                + "\"verificationMethods\":{\"atproto\":\"" + keyDid + "\"}}";

        log.info("[PLC] Creating DID {} on plc.directory", did);
        HttpResponse<String> resp = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create("https://plc.directory/" + did))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(opJson))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() >= 300)
            throw new RuntimeException("plc.directory rejected genesis op: " + resp.statusCode() + " " + resp.body());

        log.info("[PLC] Created {} successfully", did);
        return did;
    }

    // ── JWT issuance ──────────────────────────────────────────────────────────

    public String issueJwt(String did, String signingKeyPkcs8) throws Exception {
        ECPrivateKey privKey = pkcs8Base64ToPrivateKey(signingKeyPkcs8);
        Date now = new Date();
        return JWT.create()
                .withSubject(did).withIssuer(did).withAudience("https://juntos.chat")
                .withIssuedAt(now).withExpiresAt(new Date(now.getTime() + 7L * 24 * 60 * 60 * 1000))
                .withKeyId("#atproto")
                .sign(Algorithm.ECDSA256((java.security.interfaces.ECPublicKey) null, privKey));
    }
}
