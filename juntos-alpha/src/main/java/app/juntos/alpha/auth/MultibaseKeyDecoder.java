package app.juntos.alpha.auth;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;

/**
 * Decodes EC public keys from multibase/multicodec-encoded strings (the 'z' prefix base58btc format
 * used in AT Protocol DID documents under publicKeyMultibase).
 */
@ApplicationScoped
@Slf4j
public class MultibaseKeyDecoder {

    private static final byte[] PREFIX_SECP256K1 = {(byte) 0xe7, 0x01};
    private static final byte[] PREFIX_P256       = {(byte) 0x80, 0x24};
    private static final String BASE58_ALPHABET   =
            "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    /**
     * Decodes a multibase string (starting with 'z') into an EC public key for the given curve.
     * Returns null if the multicodec prefix doesn't match the requested curve.
     */
    public ECPublicKey decode(String multibase, String jwtCurve, String jcaCurve) throws Exception {
        if (!multibase.startsWith("z")) return null;

        byte[] decoded = base58Decode(multibase.substring(1));
        log.debug("[MULTIBASE] decoded {} bytes for curve={}", decoded.length, jwtCurve);

        if (decoded.length < 35) {
            log.warn("[MULTIBASE] too short: {} bytes", decoded.length);
            return null;
        }

        byte[] expected = "secp256k1".equals(jwtCurve) ? PREFIX_SECP256K1 : PREFIX_P256;
        if (decoded[0] != expected[0] || decoded[1] != expected[1]) {
            log.debug("[MULTIBASE] prefix mismatch for curve {}", jwtCurve);
            return null;
        }

        byte[] compressed = Arrays.copyOfRange(decoded, 2, 35);
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(jcaCurve);
        ECPoint bcPoint = spec.getCurve().decodePoint(compressed);

        BigInteger px = bcPoint.getAffineXCoord().toBigInteger();
        BigInteger py = bcPoint.getAffineYCoord().toBigInteger();
        ECNamedCurveSpec jcaSpec = new ECNamedCurveSpec(jcaCurve, spec.getCurve(), spec.getG(), spec.getN(), spec.getH());

        return (ECPublicKey) KeyFactory.getInstance("EC", "BC")
                .generatePublic(new ECPublicKeySpec(new java.security.spec.ECPoint(px, py), jcaSpec));
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
}
