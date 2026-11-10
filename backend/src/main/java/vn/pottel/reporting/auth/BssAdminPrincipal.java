package vn.pottel.reporting.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.io.Serial;
import java.util.Collection;
import java.util.List;

/** Only an active BSS human account with the existing admin role reaches this principal. */
public record BssAdminPrincipal(String username, String password, int securityVersion) implements UserDetails {
    @Serial private static final long serialVersionUID = 1L;
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return password; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
    /** Prevent accidental disclosure of the BSS password hash in logs. */
    @Override public String toString() { return "BssAdminPrincipal[role=ADMIN]"; }
}
