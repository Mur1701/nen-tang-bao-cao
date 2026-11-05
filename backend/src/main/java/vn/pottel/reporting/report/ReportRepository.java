package vn.pottel.reporting.report;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** SQL is strictly allowlisted and calls only a stored function; never interpolate a client report code. */
@Repository
public class ReportRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public ReportRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final String FROM = " FROM reporting.fn_i1_activated_kits(:monthStart, :agencyId) ";
    private MapSqlParameterSource args(LocalDate monthStart, Integer agencyId) {
        return new MapSqlParameterSource()
                .addValue("monthStart", monthStart)
                .addValue("agencyId", agencyId, java.sql.Types.INTEGER);
    }

    public long countI1(LocalDate monthStart, Integer agencyId) {
        return jdbc.queryForObject("SELECT count(*) " + FROM, args(monthStart, agencyId), Long.class);
    }

    public List<Map<String, Object>> i1(LocalDate monthStart, Integer agencyId, int limit, long offset) {
        var params = args(monthStart, agencyId).addValue("limit", limit).addValue("offset", offset);
        return jdbc.queryForList("SELECT agency_id, agency_name, activated_kit_count " + FROM
                + " ORDER BY agency_id LIMIT :limit OFFSET :offset", params);
    }
}
