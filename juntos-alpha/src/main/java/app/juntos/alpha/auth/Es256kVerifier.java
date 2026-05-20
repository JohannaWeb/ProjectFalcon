package app.juntos.alpha.auth;

import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.Base64;

/**
 * Verifies ES256K (secp256k1) JWT signatures using BouncyCastle's raw ECDSA signer.
 * Handles both low-S and high-S (normalised) signatures for maximum compatibility.
 */
@ApplicationScoped
@Slf4j
public class Es256kVerifier {

    public void verify(String token, ECPublicKey publicKey) throws Exception {
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new JWTVerificationException("Invalid JWT format");

        byte[] data = (parts[0] + "." + parts[1]).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);

        byte[] rawSig = Base64.getUrlDecoder().decode(parts[2]);
        if (rawSig.length == 0 || rawSig.length % 2 != 0)
            throw new JWTVerificationException("Invalid ES256K signature length: " + rawSig.length);

        int half = rawSig.length / 2;
        BigInteger r = new BigInteger(1, Arrays.copyOfRange(rawSig, 0, half));
        BigInteger s = new BigInteger(1, Arrays.copyOfRange(rawSig, half, rawSig.length));

        X9ECParameters curve = CustomNamedCurves.getByName("secp256k1");
        if (curve == null) throw new IllegalStateException("secp256k1 not found in BouncyCastle");

        ECDomainParameters domain = new ECDomainParameters(
                curve.getCurve(), curve.getG(), curve.getN(), curve.getH());

        org.bouncycastle.math.ec.ECPoint q = extractPoint(publicKey, domain);

        ECPublicKeyParameters params = new ECPublicKeyParameters(q, domain);
        ECDSASigner signer = new ECDSASigner();
        signer.init(false, params);

        log.debug("[ES256K] verifying r={} s={}", r.toString(16).substring(0, 8), s.toString(16).substring(0, 8));
        boolean valid = signer.verifySignature(hash, r, s);

        // Low-S normalisation: some signers produce high-S; both are valid per ECDSA spec
        if (!valid) {
            BigInteger halfN = curve.getN().shiftRight(1);
            if (s.compareTo(halfN) > 0) {
                BigInteger sNorm = curve.getN().subtract(s);
                log.debug("[ES256K] high-S detected — retrying with normalised s");
                valid = signer.verifySignature(hash, r, sNorm);
            }
        }

        if (!valid) throw new JWTVerificationException("ES256K signature invalid");
        log.debug("[ES256K] signature valid");
    }

    private org.bouncycastle.math.ec.ECPoint extractPoint(ECPublicKey key, ECDomainParameters domain) {
        if (key instanceof BCECPublicKey bc) return bc.getQ();
        org.bouncycastle.jce.spec.ECNamedCurveParameterSpec spec =
                org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("secp256k1");
        return spec.getCurve().createPoint(key.getW().getAffineX(), key.getW().getAffineY());
    }
}
