package vn.pottel.reporting.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * In-process bounded guard for BSS admin login attempts.
 * IMPORTANT: Internet deployments also require shared edge rate limiting.
 * Full map capacity must never block ALL previously clean admin logins.
 */
public final class AdminLoginThrottle extends OncePerRequestFilter {
    private static final int DEFAULT_MAX_ATTEMPTS = 5;
    private static final int DEFAULT_MAX_KEYS = 10_000;
    private static final long DEFAULT_WINDOW_SECONDS = 900;
    private record Counter(long since, int failures) {}

    private final Map<String, Counter> attempts = new LinkedHashMap<>();
    private final LongSupplier clock;
    private final int maxKeys;
    private final int maxAttempts;
    private final long windowSeconds;

    public AdminLoginThrottle() {
        this(() -> Instant.now().getEpochSecond(),
                DEFAULT_MAX_KEYS, DEFAULT_MAX_ATTEMPTS, DEFAULT_WINDOW_SECONDS);
    }

    // Package-private deterministic test seam: no HTTP floods or sleeping.
    AdminLoginThrottle(LongSupplier clock, int maxKeys, int maxAttempts, long windowSeconds) {
        if (maxKeys < 1 || maxAttempts < 1 || windowSeconds < 1)
            throw new IllegalArgumentException("Invalid throttle limits");
        this.clock = clock;
        this.maxKeys = maxKeys;
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
    }

    private static String key(String address, String username) {
        String user = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (user.length() > 50) user = user.substring(0, 50);
        return (address == null ? "" : address) + ":" + user;
    }

    public void fail(HttpServletRequest request) {
        String key = key(request.getRemoteAddr(), request.getParameter("username"));
        long now = clock.getAsLong();
        synchronized (attempts) {
            Counter old = attempts.get(key);
            if (old != null && now - old.since >= windowSeconds) {
                attempts.remove(key);
                old = null;
            }
            if (old == null) {
                // Bounded O(1) insertion-order eviction. Not a global deny.
                // Edge limits are still required to protect against rotating keys.
                if (attempts.size() >= maxKeys) {
                    Iterator<String> iterator = attempts.keySet().iterator();
                    if (iterator.hasNext()) {
                        iterator.next();
                        iterator.remove();
                    }
                }
                attempts.put(key, new Counter(now, 1));
            } else {
                attempts.put(key, new Counter(old.since,
                        Math.min(maxAttempts, old.failures + 1)));
            }
        }
    }

    public void succeeded(HttpServletRequest request) {
        synchronized (attempts) {
            attempts.remove(key(request.getRemoteAddr(), request.getParameter("username")));
        }
    }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod())
                || !"/api/auth/login".equals(request.getRequestURI());
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                               FilterChain chain) throws IOException, ServletException {
        long now = clock.getAsLong();
        Counter active;
        String requestKey = key(request.getRemoteAddr(), request.getParameter("username"));
        synchronized (attempts) {
            active = attempts.get(requestKey);
            if (active != null && now - active.since >= windowSeconds) {
                attempts.remove(requestKey);
                active = null;
            }
        }
        if (active != null && active.failures >= maxAttempts) {
            long seconds = Math.max(1L, windowSeconds - Math.max(0L, now - active.since));
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.setHeader("Retry-After", Long.toString(seconds));
            response.setHeader("Cache-Control", "no-store");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Too many login attempts\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
