package vn.pottel.reporting.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Backend checks admin state on every sensitive request, not only at login time. */
public final class AdminSessionValidationFilter extends OncePerRequestFilter {
    private final BssAdminUserDetailsService users;
    public AdminSessionValidationFilter(BssAdminUserDetailsService users) { this.users = users; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/api/reports") || path.equals("/api/auth/me")
                || path.equals("/api/health/db"));
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                               FilterChain chain) throws IOException, ServletException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && (!(auth.getPrincipal() instanceof BssAdminPrincipal principal)
                    || !users.stillActiveAdmin(principal))) {
            SecurityContextHolder.clearContext();
            var session = request.getSession(false);
            if (session != null) session.invalidate();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Session expired or admin access revoked\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
