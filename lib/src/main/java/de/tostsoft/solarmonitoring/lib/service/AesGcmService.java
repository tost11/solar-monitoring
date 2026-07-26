package de.tostsoft.solarmonitoring.lib.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class AesGcmService {

    private static final Logger LOG = LoggerFactory.getLogger(AesGcmService.class);

    private static final int TAG_SIZE_BITS = 128;
    private static final int TAG_SIZE_BYTES = 16;
    private static final int NONCE_SIZE_BYTES = 12;
    private static final int KEY_SIZE_BYTES = 32;

    /**
     * Decrypts an AES-256-GCM AEAD ciphertext.
     *
     * @param body         ciphertext || 16-byte GCM auth tag (as received from ESP32)
     * @param nonceHex     24 hex characters (12 bytes nonce/IV)
     * @param systemId     used as AAD (UTF-8 encoded)
     * @param keySha256Hex 64 hex characters (32 bytes) - the stored SHA-256 hash of the clientToken
     * @return decrypted plaintext bytes (JSON)
     * @throws DecryptionException on any decryption/auth failure
     */
    public byte[] decrypt(byte[] body, String nonceHex, String systemId, String keySha256Hex) {
        if (body == null || body.length <= TAG_SIZE_BYTES) {
            throw new DecryptionException("Body too short - must contain ciphertext + 16-byte tag");
        }

        byte[] nonce = hexToBytes(nonceHex);
        if (nonce.length != NONCE_SIZE_BYTES) {
            throw new DecryptionException("Nonce must be exactly 12 bytes (24 hex chars), got " + nonce.length);
        }

        byte[] key = hexToBytes(keySha256Hex);
        if (key.length != KEY_SIZE_BYTES) {
            throw new DecryptionException("Key must be exactly 32 bytes (64 hex chars), got " + key.length);
        }

        LOG.debug("AES-GCM decrypt: bodyLen={}, nonceHex='{}', systemId='{}' (bytes={}), keyHex='{}'",
                body.length, nonceHex, systemId, bytesToHex(systemId.getBytes(StandardCharsets.UTF_8)), keySha256Hex);

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_SIZE_BITS, nonce);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            // Add systemId as AAD (Additional Authenticated Data)
            cipher.updateAAD(systemId.getBytes(StandardCharsets.UTF_8));

            // JDK AES/GCM expects ciphertext+tag concatenated as input to doFinal
            return cipher.doFinal(body);
        } catch (GeneralSecurityException e) {
            LOG.debug("AES-GCM decrypt failed: {}", e.getMessage());
            throw new DecryptionException("Decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Computes SHA-256 hash of a plaintext token, returned as hex string.
     * Used when creating a DATA_PUSH_ENCRYPTED token.
     */
    public static String sha256Hex(String plainToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainToken.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private static byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            throw new DecryptionException("Invalid hex string");
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static class DecryptionException extends RuntimeException {
        public DecryptionException(String message) {
            super(message);
        }

        public DecryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
