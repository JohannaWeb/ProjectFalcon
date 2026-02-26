package app.falcon.core.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption/decryption service for storing sensitive tokens at
 * rest.
 *
 * <p>
 * Key must be 32 bytes (256-bit), Base64-encoded, configured via
 * {@code falcon.security.token-encryption-key} in each service's
 * application.yml.
 * </p>
 */
@Service
public class TokenEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    private final SecretKeySpec secretKey;

    public TokenEncryptionService(@Value("${falcon.security.token-encryption-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "falcon.security.token-encryption-key must be a 32-byte (256-bit) Base64-encoded value");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts a plaintext token.
     *
     * @param plaintext the raw token (e.g. a GitHub PAT)
     * @return a Base64-encoded string of IV + ciphertext
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes());

            // Prepend IV to ciphertext so decrypt can extract it
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Token encryption failed", e);
        }
    }

    /**
     * Decrypts a previously encrypted token.
     *
     * @param encryptedBase64 the Base64-encoded IV + ciphertext blob
     * @return the original plaintext token
     */
    public String decrypt(String encryptedBase64) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byte[] cipherText = new byte[combined.length - IV_LENGTH_BYTE];

            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTE);
            System.arraycopy(combined, IV_LENGTH_BYTE, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            return new String(cipher.doFinal(cipherText));
        } catch (Exception e) {
            throw new IllegalStateException("Token decryption failed", e);
        }
    }
}
