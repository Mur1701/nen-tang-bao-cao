package vn.pottel.reporting.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Read-only adapter to the EXISTING vnpost_bss identity schema.
 * Missing schema/DB, disabled account, service account, or non-admin => fail closed.
 * Does not call BSS's UPDATE dang_nhap_cuoi.
 */
@Service
public class BssAdminUserDetailsService implements UserDetailsService {
    private static final String ACCOUNT_SQL = """
        SELECT tk.ten_dang_nhap, tk.hash, tk.khoa, tk.loai, tk.phien_ban_bao_mat,
               EXISTS (SELECT 1 FROM vnpost_bss.user_tai_khoan_vai_tro r
                       WHERE r.tai_khoan_id = tk.id AND r.vai_tro = 'admin') AS is_admin
          FROM vnpost_bss.user_tai_khoan tk
         WHERE lower(tk.ten_dang_nhap) = lower(?)
        """;
    private static final String RECHECK_SQL = """
        SELECT tk.khoa, tk.loai, tk.phien_ban_bao_mat,
               EXISTS (SELECT 1 FROM vnpost_bss.user_tai_khoan_vai_tro r
                       WHERE r.tai_khoan_id = tk.id AND r.vai_tro = 'admin') AS is_admin
          FROM vnpost_bss.user_tai_khoan tk
         WHERE lower(tk.ten_dang_nhap) = lower(?)
        """;

    private record Account(String username, String hash, boolean locked,
                           String type, int securityVersion, boolean admin) {}
    private record State(boolean locked, String type, int securityVersion, boolean admin) {}

    private final JdbcTemplate jdbc;
    private final boolean bssAuthEnabled;

    public BssAdminUserDetailsService(JdbcTemplate jdbc,
             @Value("${reporting.auth.bss-enabled:false}") boolean bssAuthEnabled) {
        this.jdbc = jdbc;
        this.bssAuthEnabled = bssAuthEnabled;
    }

    private static boolean validUsername(String username) {
        return username != null && username.matches("[A-Za-z0-9._-]{2,50}");
    }

    @Override public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!bssAuthEnabled || !validUsername(username)) throw new UsernameNotFoundException("Login denied");
        try {
            List<Account> rows = jdbc.query(ACCOUNT_SQL, (rs, i) -> new Account(
                    rs.getString("ten_dang_nhap"), rs.getString("hash"), rs.getBoolean("khoa"),
                    rs.getString("loai"), rs.getInt("phien_ban_bao_mat"), rs.getBoolean("is_admin")), username);
            if (rows.size() != 1) throw new UsernameNotFoundException("Login denied");
            Account a = rows.getFirst();
            if (a.locked || !a.admin || !"nguoi".equals(a.type) || a.hash == null)
                throw new UsernameNotFoundException("Login denied");
            return new BssAdminPrincipal(a.username, a.hash, a.securityVersion);
        } catch (DataAccessException e) {
            // Deliberately no SQL, connection string, username or stack in HTTP response.
            throw new AuthenticationServiceException("Identity store unavailable");
        }
    }

    /** Called on every protected request: lockdown/revocation takes effect immediately. */
    public boolean stillActiveAdmin(BssAdminPrincipal principal) {
        if (!bssAuthEnabled || principal == null || !validUsername(principal.username())) return false;
        try {
            List<State> rows = jdbc.query(RECHECK_SQL, (rs, i) -> new State(
                rs.getBoolean("khoa"), rs.getString("loai"),
                rs.getInt("phien_ban_bao_mat"), rs.getBoolean("is_admin")), principal.username());
            if (rows.size() != 1) return false;
            State s = rows.getFirst();
            return !s.locked && s.admin && "nguoi".equals(s.type)
                    && s.securityVersion == principal.securityVersion();
        } catch (DataAccessException e) {
            return false;
        }
    }
}
