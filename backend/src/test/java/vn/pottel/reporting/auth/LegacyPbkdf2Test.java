package vn.pottel.reporting.auth;

import org.junit.jupiter.api.Test;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import static org.junit.jupiter.api.Assertions.*;

class LegacyPbkdf2Test {
    @Test void matchesExistingBssFormat() throws Exception {
        // ASCII vector cross-checked against JCE, no real accounts/hashes.
        var salt = "abcdefghijklmnop".getBytes(StandardCharsets.UTF_8);
        var pbkdf2 = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        var key = pbkdf2.generateSecret(new PBEKeySpec("sample-password".toCharArray(), salt, 10_000, 256)).getEncoded();
        var encoded = "pbkdf2-sha256.10000." + Base64.getEncoder().encodeToString(salt) + "."
                 + Base64.getEncoder().encodeToString(key);
        assertTrue(LegacyPbkdf2.matches("sample-password", encoded));
        assertFalse(LegacyPbkdf2.matches("wrong", encoded));
        assertFalse(LegacyPbkdf2.matches("anything", "not-a-hash"));
        assertFalse(LegacyPbkdf2.matches(null, encoded));
    }
}
