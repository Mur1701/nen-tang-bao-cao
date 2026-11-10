package vn.pottel.reporting.auth;

import org.springframework.security.crypto.password.PasswordEncoder;

/** Compatibility adapter only. Does NOT provision or update accounts. */
public final class BssPasswordEncoder implements PasswordEncoder {
    @Override public String encode(CharSequence rawPassword) {
        // Spring's DaoAuthenticationProvider uses encode() internally for the
        // unknown-user timing mitigation; it does not persist this value.
        return LegacyPbkdf2.encode(rawPassword == null ? "" : rawPassword.toString());
    }
    @Override public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return LegacyPbkdf2.matches(rawPassword == null ? null : rawPassword.toString(), encodedPassword);
    }
}
