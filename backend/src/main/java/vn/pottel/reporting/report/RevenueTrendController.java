package vn.pottel.reporting.report;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.pottel.reporting.security.ReportQueryTimeout;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.List;

/**
 * Vietnamese monthly trend for five revenue reports (II.1–II.5).
 * Each point is aggregated in PostgreSQL from the FULL report result, never a
 * paginated subset. Only hardcoded server-side Stored Function identifiers are
 * interpolated into SQL, not browser input.
 */
@RestController
@RequestMapping("/api/reports")
public class RevenueTrendController {
    public record Point(String month, String primary, String secondary) {}
    public record Trend(String code, String primaryLabel, String secondaryLabel, List<Point> points) {}
    record Metric(String function, String primaryExpression, String secondaryExpression,
                  String primaryLabel, String secondaryLabel) {}

    private static final String CURRENT_DAYS =
            "(SELECT sum((day.value->>'gross')::numeric) " +
            "FROM jsonb_each(coalesce(r.row_data #> '{day_groups,current}', '{}'::jsonb)) AS day)";
    private static final String NEXT_DAYS =
            "(SELECT sum((day.value->>'gross')::numeric) " +
            "FROM jsonb_each(coalesce(r.row_data #> '{day_groups,next}', '{}'::jsonb)) AS day)";

    static Metric metricFor(String code) {
        return switch (code) {
            case "II.1" -> new Metric("fn_ii1_report", CURRENT_DAYS, NEXT_DAYS,
                    "Doanh thu phân bổ trong kỳ", "Doanh thu phân bổ kỳ sau");
            case "II.2" -> new Metric("fn_ii2_report",
                    "(r.row_data->>'revenue_gross')::numeric", null,
                    "Doanh thu chưa phân bổ", null);
            case "II.3" -> new Metric("fn_ii3_report", CURRENT_DAYS, null,
                    "Doanh thu phân bổ trong kỳ", null);
            case "II.4" -> new Metric("fn_ii4_report", NEXT_DAYS, null,
                    "Doanh thu chờ phân bổ kỳ sau", null);
            case "II.5" -> new Metric("fn_ii5_report",
                    "(r.row_data->>'incurred_gross')::numeric",
                    "(r.row_data->>'allocated_gross')::numeric",
                    "Doanh thu gói phát sinh", "Doanh thu gói đã phân bổ");
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Không có biểu đồ doanh thu cho báo cáo này");
        };
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final ReportQueryTimeout timeout;

    public RevenueTrendController(NamedParameterJdbcTemplate jdbc, ReportQueryTimeout timeout) {
        this.jdbc = jdbc;
        this.timeout = timeout;
    }

    @PostMapping("/{code}/trend")
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Trend trend(@PathVariable String code, @RequestBody ReportQuery query) {
        var metric = metricFor(code == null ? "" : code.toUpperCase(java.util.Locale.ROOT));
        if (query == null || query.month() == null
                || !query.month().matches("\\d{4}-(0[1-9]|1[0-2])"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tháng phải có dạng YYYY-MM");

        final YearMonth end;
        try { end = YearMonth.parse(query.month()); }
        catch (DateTimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tháng không hợp lệ");
        }

        // Fixed SQL expressions selected above; missing month values stay NULL,
        // not fabricated zeros. All five endpoints use the same six-month engine.
        var sql = """
            WITH periods AS (
              SELECT month_start::date AS month_start
              FROM generate_series(CAST(:firstMonth AS date),
                                   CAST(:lastMonth AS date),
                                   interval '1 month') AS t(month_start)
            )
            SELECT to_char(p.month_start, 'YYYY-MM') AS report_month,
                   sum(PRIMARY_EXPRESSION) AS primary_amount,
                   SECONDARY_EXPRESSION AS secondary_amount
            FROM periods p
            LEFT JOIN LATERAL reporting.FUNCTION_NAME(
                p.month_start, CAST(NULL AS integer)) r ON true
            GROUP BY p.month_start
            ORDER BY p.month_start
            """.replace("PRIMARY_EXPRESSION", metric.primaryExpression())
                .replace("SECONDARY_EXPRESSION",
                        metric.secondaryExpression() == null
                                ? "NULL::numeric" : "sum(" + metric.secondaryExpression() + ")")
                .replace("FUNCTION_NAME", metric.function());

        var params = new MapSqlParameterSource()
                .addValue("firstMonth", end.minusMonths(5).atDay(1))
                .addValue("lastMonth", end.atDay(1));
        // Primary always aggregates. Keep the SUM at SQL level for all rows.
        try {
            timeout.apply(jdbc);
            var points = jdbc.query(sql, params, (rs, i) -> new Point(
                    rs.getString("report_month"),
                    decimal(rs.getBigDecimal("primary_amount")),
                    decimal(rs.getBigDecimal("secondary_amount"))));
            return new Trend(code.toUpperCase(java.util.Locale.ROOT),
                    metric.primaryLabel(), metric.secondaryLabel(), points);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Không thể tổng hợp xu hướng doanh thu từ PostgreSQL");
        }
    }

    private static String decimal(BigDecimal number) {
        return number == null ? null : number.toPlainString();
    }
}
