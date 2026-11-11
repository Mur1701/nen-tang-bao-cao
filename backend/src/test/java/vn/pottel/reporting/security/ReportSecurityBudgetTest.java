package vn.pottel.reporting.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportSecurityBudgetTest {
    private static HttpServletRequest request(String path) {
        var r = mock(HttpServletRequest.class);
        when(r.getMethod()).thenReturn("POST");
        when(r.getRequestURI()).thenReturn(path);
        return r;
    }

    @Test
    void secondReportIsRejectedWhileOneLongRequestHoldsCapacity() throws Exception {
        var filter = new ReportAdmissionFilter(1);
        var occupied = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var error = new AtomicReference<Throwable>();
        FilterChain slow = (req, resp) -> {
            occupied.countDown();
            try {
                if (!release.await(3, TimeUnit.SECONDS))
                    throw new IllegalStateException("Synthetic test release timed out");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        };

        var worker = new Thread(() -> {
            try {
                filter.doFilter(request("/api/reports/I.1/run"),
                        new MockHttpServletResponse(), slow);
            } catch (Throwable t) { error.set(t); }
        });
        worker.start();
        try {
            assertTrue(occupied.await(2, TimeUnit.SECONDS));
            var response = new MockHttpServletResponse();
            var rejected = mock(FilterChain.class);
            filter.doFilter(request("/api/reports/II.5/trend"), response, rejected);
            assertEquals(429, response.getStatus());
            assertEquals("3", response.getHeader("Retry-After"));
            verifyNoInteractions(rejected);
        } finally {
            release.countDown();
            worker.join(3000);
        }
        assertNull(error.get());
        assertFalse(worker.isAlive());
        var after = mock(FilterChain.class);
        filter.doFilter(request("/api/reports/I.2/excel"),
                new MockHttpServletResponse(), after);
        verify(after).doFilter(any(), any());
    }

    @Test
    void reportBudgetIsTransactionLocalAndFailsClosedOutsideTransactions() {
        var budget = new ReportQueryTimeout(20000);
        var template = mock(NamedParameterJdbcTemplate.class);
        var sql = mock(JdbcTemplate.class);
        when(template.getJdbcTemplate()).thenReturn(sql);

        assertThrows(IllegalStateException.class, () -> budget.apply(template));
        verifyNoInteractions(sql);

        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            budget.apply(template);
            verify(sql).execute("SET LOCAL statement_timeout = 20000");
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void unsafeConfigurationValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ReportQueryTimeout(0));
        assertThrows(IllegalArgumentException.class, () -> new ReportQueryTimeout(120001));
        assertThrows(IllegalArgumentException.class, () -> new ReportAdmissionFilter(0));
        assertThrows(IllegalArgumentException.class, () -> new ReportAdmissionFilter(101));
    }
}
