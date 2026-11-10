package vn.pottel.reporting.report;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.List;

/**
 * Six-month comparison from ALL agencies and ALL rows of each monthly II.5
 * Stored Function. Never aggregate just the currently paginated React table.
 * The baseline data is reference-only, not financially accepted.
 */
@RestController
@RequestMapping("/api/reports/II.5")
public class RevenueTrendController {
    public record Point(String month, String incurredGross, String allocatedGross) {}

    private static final String SQL = """
        WITH periods AS (
          SELECT month_start::date AS month_start
          FROM generate_series(CAST(:firstMonth AS date),
                               CAST(:lastMonth AS date),
                               interval '1 month') AS t(month_start)
        )
        SELECT to_char(p.month_start, 'YYYY-MM') AS report_month,
               sum((r.row_data->>'incurred_gross')::numeric) AS incurred,
               sum((r.row_data->>'allocated_gross')::numeric) AS allocated
        FROM periods p
        LEFT JOIN LATERAL reporting.fn_ii5_report(p.month_start, CAST(NULL AS integer)) r ON true
        GROUP BY p.month_start
        ORDER BY p.month_start
        """;

    private final NamedParameterJdbcTemplate jdbc;

    public RevenueTrendController(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping("/trend")
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<Point> trend(@RequestBody ReportQuery query) {
        if (query == null || query.month() == null
                || !query.month().matches("\\d{4}-(0[1-9]|1[0-2])"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tháng phải có dạng YYYY-MM");
        final YearMonth end;
        try { end = YearMonth.parse(query.month()); }
        catch (DateTimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tháng không hợp lệ");
        }
        var params = new MapSqlParameterSource()
                .addValue("firstMonth", end.minusMonths(5).atDay(1))
                .addValue("lastMonth", end.atDay(1));
        try {
            return jdbc.query(SQL, params, (rs, i) -> new Point(
                    rs.getString("report_month"),
                    asString(rs.getBigDecimal("incurred")),
                    asString(rs.getBigDecimal("allocated"))));
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Chưa truy vấn được xu hướng doanh thu từ Stored Function II.5");
        }
    }

    private static String asString(BigDecimal n) {
        return n == null ? null : n.toPlainString();
    }
}
