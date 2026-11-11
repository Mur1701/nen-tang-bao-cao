package vn.pottel.reporting.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminLoginThrottleSecurityTest {
    private static HttpServletRequest login(String ip, String username) {
        var request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn(ip);
        when(request.getParameter("username")).thenReturn(username);
        return request;
    }

    @Test
    void fiveFailedAttemptsAreBlockedOnlyForTheSameKey() throws Exception {
        var clock = new AtomicLong(1000);
        var guard = new AdminLoginThrottle(clock::get, 3, 5, 900);
        var blocked = login("192.0.2.1", "admin");
        for (int i = 0; i < 5; i++) guard.fail(blocked);
        var chain = mock(FilterChain.class);
        var response = new MockHttpServletResponse();
        guard.doFilter(blocked, response, chain);
        assertEquals(429, response.getStatus());
        assertEquals("900", response.getHeader("Retry-After"));
        verifyNoInteractions(chain);

        chain = mock(FilterChain.class);
        response = new MockHttpServletResponse();
        guard.doFilter(login("192.0.2.1", "another-admin"), response, chain);
        assertEquals(200, response.getStatus());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void saturatedMapDoesNotCauseGlobalLoginDenial() throws Exception {
        var clock = new AtomicLong(5000);
        var guard = new AdminLoginThrottle(clock::get, 2, 5, 900);
        guard.fail(login("192.0.2.1", "alpha"));
        guard.fail(login("192.0.2.2", "beta"));
        // Before fix, a full map returned 429 to an unrelated clean admin.
        var chain = mock(FilterChain.class);
        var response = new MockHttpServletResponse();
        guard.doFilter(login("192.0.2.3", "clean-admin"), response, chain);
        assertEquals(200, response.getStatus());
        verify(chain).doFilter(any(), any());

        guard.fail(login("192.0.2.4", "gamma")); // evict oldest
        var nextChain = mock(FilterChain.class);
        guard.doFilter(login("192.0.2.5", "new-admin"),
                new MockHttpServletResponse(), nextChain);
        verify(nextChain).doFilter(any(), any());
    }

    @Test
    void expiryAndSuccessClearTheLocalCounter() throws Exception {
        var clock = new AtomicLong(1000);
        var guard = new AdminLoginThrottle(clock::get, 2, 2, 30);
        var admin = login("192.0.2.1", "admin");
        guard.fail(admin);
        guard.fail(admin);
        var denied = new MockHttpServletResponse();
        guard.doFilter(admin, denied, mock(FilterChain.class));
        assertEquals(429, denied.getStatus());

        clock.set(1030);
        var chain = mock(FilterChain.class);
        guard.doFilter(admin, new MockHttpServletResponse(), chain);
        verify(chain).doFilter(any(), any());

        guard.fail(admin);
        guard.succeeded(admin);
        var afterSuccess = mock(FilterChain.class);
        guard.doFilter(admin, new MockHttpServletResponse(), afterSuccess);
        verify(afterSuccess).doFilter(any(), any());
    }

    @Test
    void unknownPathsBypassLoginThrottle() throws Exception {
        var guard = new AdminLoginThrottle(() -> 0L, 1, 1, 30);
        var report = login("192.0.2.1", "admin");
        when(report.getRequestURI()).thenReturn("/api/reports/I.1/run");
        var chain = mock(FilterChain.class);
        guard.doFilter(report, new MockHttpServletResponse(), chain);
        verify(chain).doFilter(any(), any());
    }
}
