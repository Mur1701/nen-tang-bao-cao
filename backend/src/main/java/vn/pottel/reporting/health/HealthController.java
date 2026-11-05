package vn.pottel.reporting.health;

import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "service", "pottel-reporting-backend");
    }

    @GetMapping("/api/health/db")
    public ResponseEntity<Map<String, String>> databaseHealth() {
        try {
            Integer result = jdbcTemplate.queryForObject(
                    "SELECT 1",
                    Integer.class);

            if (Integer.valueOf(1).equals(result)) {
                return ResponseEntity.ok(
                        Map.of("status", "UP", "database", "UP"));
            }
        } catch (DataAccessException ex) {
            org.slf4j.LoggerFactory.getLogger(HealthController.class)
                    .error("Database health check failed", ex);
        }

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "DOWN",
                        "database", "DOWN"));
    }
}
