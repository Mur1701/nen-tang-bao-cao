package vn.pottel.reporting.report;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Official I.1 function only. No historical KIT-preview function is used.
 * All identifiers are server-side constants; SQL values use bound parameters.
 */
@Repository
public class ReportRepository {
    private static final String I1_FROM =
            " FROM reporting.fn_i1_report(:monthStart, :agencyId) ";

    private static final String I1_COLUMNS =
            "stt, agency_id, agency_name, package_code, package_price, "
          + "package_active_count, agency_total_active, achieved_tier, "
          + "incentive_total, incentive_before_vat, vat_amount, missing_price";

    private final NamedParameterJdbcTemplate jdbc;

    public ReportRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private MapSqlParameterSource args(LocalDate monthStart, Integer agencyId) {
        return new MapSqlParameterSource()
                .addValue("monthStart", monthStart)
                .addValue("agencyId", agencyId, java.sql.Types.INTEGER);
    }

    public long countI1(LocalDate monthStart, Integer agencyId) {
        Long count = jdbc.queryForObject("SELECT count(*) " + I1_FROM,
                args(monthStart, agencyId), Long.class);
        return count == null ? 0 : count;
    }

    public List<Map<String, Object>> i1(LocalDate monthStart, Integer agencyId, int limit, long offset) {
        var params = args(monthStart, agencyId)
                .addValue("limit", limit)
                .addValue("offset", offset);
        // Preserve numeric accuracy in JSON: BigDecimal is serialized as a string.
        return jdbc.query("SELECT " + I1_COLUMNS + I1_FROM
                + " ORDER BY agency_id, package_code LIMIT :limit OFFSET :offset",
                params, (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("stt", rs.getLong("stt"));
                    row.put("agency_id", rs.getInt("agency_id"));
                    row.put("agency_name", rs.getString("agency_name"));
                    row.put("package_code", rs.getString("package_code"));
                    row.put("package_price", decimal(rs.getBigDecimal("package_price")));
                    row.put("package_active_count", rs.getLong("package_active_count"));
                    row.put("agency_total_active", rs.getLong("agency_total_active"));
                    row.put("achieved_tier", rs.getString("achieved_tier"));
                    row.put("incentive_total", decimal(rs.getBigDecimal("incentive_total")));
                    row.put("incentive_before_vat", decimal(rs.getBigDecimal("incentive_before_vat")));
                    row.put("vat_amount", decimal(rs.getBigDecimal("vat_amount")));
                    row.put("missing_price", rs.getLong("missing_price"));
                    return row;
                });
    }


    private static final String REFERENCE_FROM =
            " FROM reporting.fn_phase1_reference_rows(:code, :monthStart, :agencyId) ";

    private MapSqlParameterSource referenceArgs(String code, LocalDate monthStart, Integer agencyId) {
        return args(monthStart, agencyId).addValue("code", code);
    }

    public long countReference(String code, LocalDate monthStart, Integer agencyId) {
        Long total = jdbc.queryForObject(
                "SELECT count(*)" + REFERENCE_FROM,
                referenceArgs(code, monthStart, agencyId), Long.class);
        return total == null ? 0L : total;
    }

    /**
     * The function returns JSON with dynamic actual allocation dates.
     * Financial decimals remain JSON text until rendered/exported; no Java
     * double arithmetic is performed on ledger values.
     */
    public List<String> referenceJson(String code, LocalDate monthStart, Integer agencyId,
                                      int limit, long offset) {
        var params = referenceArgs(code, monthStart, agencyId)
                .addValue("limit", limit).addValue("offset", offset);
        return jdbc.queryForList("SELECT row_data::text" + REFERENCE_FROM
                + " ORDER BY row_data->>'agency_name', row_data->>'msisdn', "
                + "row_data->>'package_code' LIMIT :limit OFFSET :offset",
                params, String.class);
    }

    // Historical technical previews. These counts are KIT, not payable subscribers.
    // Keep names explicit; financial I.1 remains a distinct stored function.
    private static final String I1_KIT_FROM =
            " FROM reporting.fn_i1_activated_kits(:monthStart, :agencyId) ";
    private static final String I1_KIT_PACKAGE_FROM =
            " FROM reporting.fn_i1_kit_by_package_preview(:monthStart, :agencyId) ";

    public long countI1KitPreview(LocalDate monthStart, Integer agencyId) {
        Long count = jdbc.queryForObject("SELECT count(*) " + I1_KIT_FROM,
                args(monthStart, agencyId), Long.class);
        return count == null ? 0 : count;
    }

    public List<Map<String, Object>> i1KitPreview(LocalDate monthStart, Integer agencyId,
                                                    int limit, long offset) {
        var params = args(monthStart, agencyId).addValue("limit", limit).addValue("offset", offset);
        return jdbc.queryForList("SELECT agency_id, agency_name, activated_kit_count "
                + I1_KIT_FROM + " ORDER BY agency_id LIMIT :limit OFFSET :offset", params);
    }

    public long countI1PackagePreview(LocalDate monthStart, Integer agencyId) {
        Long count = jdbc.queryForObject("SELECT count(*) " + I1_KIT_PACKAGE_FROM,
                args(monthStart, agencyId), Long.class);
        return count == null ? 0 : count;
    }

    public List<Map<String, Object>> i1PackagePreview(LocalDate monthStart, Integer agencyId,
                                                        int limit, long offset) {
        var params = args(monthStart, agencyId).addValue("limit", limit).addValue("offset", offset);
        return jdbc.queryForList("SELECT agency_id, agency_name, package_code, "
                + "activated_kit_count, agency_kit_total "
                + I1_KIT_PACKAGE_FROM
                + " ORDER BY agency_id NULLS LAST, package_code NULLS LAST "
                + "LIMIT :limit OFFSET :offset", params);
    }

    private static String decimal(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
}
