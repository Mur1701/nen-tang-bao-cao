package vn.pottel.reporting.report;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * Fixed Phase 1 SQL entry points (DESIGN CONTRACT ONLY).
 *
 * These targets are reserved, not deployed or approved for execution.
 * No report code or SQL identifier supplied by a client may be interpolated
 * into a database query. Financial functions are enabled only after review.
 */
public final class ReportFunctionRegistry {
    private ReportFunctionRegistry() {}

    public record Target(String code, String sourceFile, String qualifiedFunction) {}

    private static final List<Target> TARGETS = List.of(
        new Target("I.1", "database/reports/I1.sql", "reporting.fn_i1_report"),
        new Target("I.2", "database/reports/I2.sql", "reporting.fn_i2_report"),
        new Target("I.3", "database/reports/I3.sql", "reporting.fn_i3_report"),
        new Target("I.4", "database/reports/I4.sql", "reporting.fn_i4_report"),
        new Target("I.5", "database/reports/I5.sql", "reporting.fn_i5_report"),
        new Target("I.6", "database/reports/I6.sql", "reporting.fn_i6_report"),
        new Target("I.7", "database/reports/I7.sql", "reporting.fn_i7_report"),
        new Target("II.1", "database/reports/II1.sql", "reporting.fn_ii1_report"),
        new Target("II.2", "database/reports/II2.sql", "reporting.fn_ii2_report"),
        new Target("II.3", "database/reports/II3.sql", "reporting.fn_ii3_report"),
        new Target("II.4", "database/reports/II4.sql", "reporting.fn_ii4_report"),
        new Target("II.5", "database/reports/II5.sql", "reporting.fn_ii5_report")
    );

    public static List<Target> all() { return TARGETS; }

    public static Target require(String code) {
        if (code == null) throw new NoSuchElementException("Report code is required");
        String normalized = code.toUpperCase(Locale.ROOT);
        return TARGETS.stream().filter(target -> target.code().equals(normalized))
                .findFirst().orElseThrow(() -> new NoSuchElementException("Unknown report code"));
    }
}
