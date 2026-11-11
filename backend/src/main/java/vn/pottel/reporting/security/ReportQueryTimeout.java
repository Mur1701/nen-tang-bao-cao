package vn.pottel.reporting.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Runtime PostgreSQL statement budget for every financial report query.
 * SET LOCAL is transaction-scoped: pooled connections cannot retain the limit
 * across requests. Config value is validated, never supplied by the client.
 */
@Component
public final class ReportQueryTimeout {
    private final int timeoutMs;

    public ReportQueryTimeout(
            @Value("${reporting.security.statement-timeout-ms:20000}") int timeoutMs) {
        if (timeoutMs < 1000 || timeoutMs > 120000)
            throw new IllegalArgumentException("report statement timeout must be between 1000 and 120000 ms");
        this.timeoutMs = timeoutMs;
    }

    public void apply(NamedParameterJdbcTemplate jdbc) {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Report query requires a managed read-only transaction");
        jdbc.getJdbcTemplate().execute("SET LOCAL statement_timeout = " + timeoutMs);
    }
}
