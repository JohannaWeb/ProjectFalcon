package app.juntos.alpha.auth;

import jakarta.enterprise.context.ApplicationScoped;
import org.bouncycastle.jce.interfaces.ECPublicKey;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;

/**
 * Handles the low-level cryptographic and encoding operations needed to create a did:plc:
 *   - did:key encoding of a P-256 public key (multibase + multicodec)
 *   - DAG-CBOR encoding of the genesis operation (canonical key order required by plc.directory)
 *   - DID derivation: SHA-256 → base32lower[0:24]
 *   - ECDSA-SHA256 signing for the genesis operation signature
 */
@ApplicationScoped
public class PlcGenesisEncoder {

    private static final String BASE58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final String BASE32 = "abcdefghijklmnopqrstuvwxyz234567";

    // ── did:key ───────────────────────────────────────────────────────────────

    public String toDidKey(KeyPair keyPair) {
        ECPublicKey pub = (ECPublicKey) keyPair.getPublic();
        byte[] compressed = pub.getQ().getEncoded(true);
        byte[] prefixed = new byte[2 + compressed.length];
        prefixed[0] = (byte) 0x80; // P-256 multicodec varint: 0x1200 → [0x80, 0x24]
        prefixed[1] = (byte) 0x24;
        System.arraycopy(compressed, 0, prefixed, 2, compressed.length);
        return "did:key:z" + base58Encode(prefixed);
    }

    private String base58Encode(byte[] input) {
        BigInteger n = new BigInteger(1, input);
        StringBuilder sb = new StringBuilder();
        while (n.signum() > 0) {
            BigInteger[] qr = n.divideAndRemainder(BigInteger.valueOf(58));
            sb.insert(0, BASE58.charAt(qr[1].intValue()));
            n = qr[0];
        }
        for (byte b : input) { if (b == 0) sb.insert(0, '1'); else break; }
        return sb.toString();
    }

    // ── DAG-CBOR genesis op ───────────────────────────────────────────────────
    // Key order: prev(4) < type(4) < services(8) < alsoKnownAs(11) < rotationKeys(12) < verificationMethods(19)

    public byte[] encodeGenesisOpCbor(String keyDid) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0xa6);                                   // map(6)
        writeText(out, "prev");             out.write(0xf6); // null
        writeText(out, "type");             writeText(out, "plc_operation");
        writeText(out, "services");         out.write(0xa0); // {}
        writeText(out, "alsoKnownAs");      out.write(0x80); // []
        writeText(out, "rotationKeys");     out.write(0x81); writeText(out, keyDid);
        writeText(out, "verificationMethods"); out.write(0xa1); writeText(out, "atproto"); writeText(out, keyDid);
        return out.toByteArray();
    }

    private void writeText(ByteArrayOutputStream out, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        int len = bytes.length;
        if (len < 24)       { out.write(0x60 | len); }
        else if (len < 256) { out.write(0x78); out.write(len); }
        else                { out.write(0x79); out.write((len >> 8) & 0xff); out.write(len & 0xff); }
        out.writeBytes(bytes);
    }

    // ── DID derivation ────────────────────────────────────────────────────────

    public String deriveDid(byte[] genesisOpCbor) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(genesisOpCbor);
        return "did:plc:" + base32Lower(hash).substring(0, 24);
    }

    private String base32Lower(byte[] input) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte b : input) {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) { bitsLeft -= 5; sb.append(BASE32.charAt((buffer >> bitsLeft) & 0x1f)); }
        }
        if (bitsLeft > 0) sb.append(BASE32.charAt((buffer << (5 - bitsLeft)) & 0x1f));
        return sb.toString();
    }

    // ── Signing ───────────────────────────────────────────────────────────────

    public String signAndEncode(byte[] cbor, PrivateKey key) throws Exception {
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(key);
        signer.update(cbor);
        byte[] raw = derToRaw64(signer.sign());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private byte[] derToRaw64(byte[] der) {
        int rLen = der[3] & 0xff;
        byte[] r = Arrays.copyOfRange(der, 4, 4 + rLen);
        int sLen = der[4 + rLen + 1] & 0xff;
        byte[] s = Arrays.copyOfRange(der, 4 + rLen + 2, 4 + rLen + 2 + sLen);
        byte[] raw = new byte[64];
        copyRightAligned(r, raw, 0, 32);
        copyRightAligned(s, raw, 32, 32);
        return raw;
    }

    private void copyRightAligned(byte[] src, byte[] dst, int off, int width) {
        if (src.length <= width) System.arraycopy(src, 0, dst, off + (width - src.length), src.length);
        else                     System.arraycopy(src, src.length - width, dst, off, width);
    }
}
