package vn.pottel.reporting.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.Semaphore;

/**
 * Per-process fail-fast admission for expensive authenticated report calls.
 * Install AFTER authorization and CSRF in the Spring Security filter chain.
 * A shared edge rate limiter is also required for public multi-instance use.
 */
public final class ReportAdmissionFilter extends OncePerRequestFilter {
    private final Semaphore slots;

    public ReportAdmissionFilter(int maxConcurrent) {
        if (maxConcurrent < 1 || maxConcurrent > 100)
            throw new IllegalArgumentException("Concurrent report limit must be 1..100");
        this.slots = new Semaphore(maxConcurrent, true);
    }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod())
            || !request.getRequestURI().matches("^/api/reports/[^/]+/(run|excel|trend)$");
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                               FilterChain chain) throws IOException, ServletException {
        if (!slots.tryAcquire()) {
            response.setStatus(429);
            response.setHeader("Retry-After", "3");
            response.setHeader("Cache-Control", "no-store");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Report capacity reached; please retry\"}");
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            slots.release();
        }
    }
}
