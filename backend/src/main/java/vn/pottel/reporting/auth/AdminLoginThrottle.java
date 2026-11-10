package vn.pottel.reporting.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** Per-instance guard; production still requires edge rate limits / a shared backing store. */
public final class AdminLoginThrottle extends OncePerRequestFilter {
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_KEYS = 10_000;
    private static final long WINDOW_SECONDS = 900;
    private record Counter(long since, int failures) {}
    private final ConcurrentHashMap<String, Counter> attempts = new ConcurrentHashMap<>();

    private static String key(String address, String username) {
        String user = username == null ? "" : username.trim().toLowerCase(java.util.Locale.ROOT);
        if (user.length() > 50) user = user.substring(0, 50);
        return address + ":" + user;
    }
    public void fail(HttpServletRequest request) {
        String key = key(request.getRemoteAddr(), request.getParameter("username"));
        long now = Instant.now().getEpochSecond();
        if (attempts.size() >= MAX_KEYS && !attempts.containsKey(key)) return;
        attempts.compute(key, (k, old) -> old == null || now - old.since > WINDOW_SECONDS
                ? new Counter(now, 1) : new Counter(old.since, old.failures + 1));
    }
    public void succeeded(HttpServletRequest request) {
        attempts.remove(key(request.getRemoteAddr(), request.getParameter("username")));
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !"/api/auth/login".equals(request.getRequestURI());
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                               FilterChain chain) throws IOException, ServletException {
        long now = Instant.now().getEpochSecond();
        Counter c = attempts.get(key(request.getRemoteAddr(), request.getParameter("username")));
        if (attempts.size() >= MAX_KEYS ||
            (c != null && now - c.since <= WINDOW_SECONDS && c.failures >= MAX_ATTEMPTS)) {
            response.setStatus(429);
            response.setHeader("Retry-After", "900");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Too many login attempts\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
