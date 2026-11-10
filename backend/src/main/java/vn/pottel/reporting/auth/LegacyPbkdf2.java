package vn.pottel.reporting.auth;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Wire-compatible with BSS PasswordHasher.java: UTF-8 bytes / PBKDF2-HMAC-SHA256,
 * pbkdf2-sha256.210000.<salt base64>.<hash base64>.
 * Never writes a password or password hash to BSS.
 */
public final class LegacyPbkdf2 {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ROUNDS = 210_000;
    private LegacyPbkdf2() {}

    public static String encode(String raw) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = derive((raw == null ? "" : raw).getBytes(StandardCharsets.UTF_8), salt, ROUNDS, 32);
        return "pbkdf2-sha256." + ROUNDS + "." + Base64.getEncoder().encodeToString(salt)
                + "." + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean matches(String raw, String encoded) {
        if (raw == null || encoded == null) return false;
        try {
            String[] parts = encoded.split("\\.", -1);
            if (parts.length != 4 || !"pbkdf2-sha256".equals(parts[0])) return false;
            int rounds = Integer.parseInt(parts[1]);
            // Limit attacker-controlled cost if the account store is damaged.
            if (rounds < 10_000 || rounds > 1_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(stripWhitespace(parts[2]));
            byte[] expected = Base64.getDecoder().decode(stripWhitespace(parts[3]));
            if (salt.length < 8 || salt.length > 64 || expected.length < 16 || expected.length > 64) return false;
            byte[] computed = derive(raw.getBytes(StandardCharsets.UTF_8), salt, rounds, expected.length);
            return MessageDigest.isEqual(expected, computed);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String stripWhitespace(String s) {
        return s.replace(" ", "").replace("\r", "").replace("\n", "").replace("\t", "");
    }

    private static byte[] derive(byte[] password, byte[] salt, int rounds, int length) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKey() {
                @Override public String getAlgorithm() { return "HmacSHA256"; }
                @Override public String getFormat() { return "RAW"; }
                @Override public byte[] getEncoded() { return password.clone(); }
            });
            byte[] output = new byte[length];
            int hashLength = mac.getMacLength();
            for (int block = 1; block <= (length + hashLength - 1) / hashLength; block++) {
                mac.update(salt);
                mac.update(new byte[]{ (byte)(block >>> 24), (byte)(block >>> 16), (byte)(block >>> 8), (byte)block });
                byte[] u = mac.doFinal();
                byte[] acc = u.clone();
                for (int round = 1; round < rounds; round++) {
                    u = mac.doFinal(u);
                    for (int i = 0; i < acc.length; i++) acc[i] ^= u[i];
                }
                int offset = (block - 1) * hashLength;
                System.arraycopy(acc, 0, output, offset, Math.min(hashLength, length - offset));
            }
            return output;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 provider unavailable", e);
        }
    }
}
