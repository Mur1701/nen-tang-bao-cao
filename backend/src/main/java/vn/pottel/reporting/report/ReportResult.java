package vn.pottel.reporting.report;

import java.util.List;
import java.util.Map;

/** Shared JSON and XLSX result shape for all Phase 1 reports. */
public record ReportResult(
        String code,
        String title,
        boolean preview,
        String notice,
        List<Column> columns,
        List<Map<String, Object>> rows,
        int page,
        int pageSize,
        long totalRows
) {
    /**
     * Optional grouped table header path, e.g. ["Phân bổ trong kỳ", "01/11/2026"].
     * Fixed columns use the legacy 3-arg constructor with an empty groupPath.
     * Dates must be supplied by an approved allocation source, never fabricated.
     */
    public record Column(String key, String title, String type, List<String> groupPath) {
        public Column(String key, String title, String type) {
            this(key, title, type, List.of());
        }

        public Column {
            groupPath = groupPath == null ? List.of() : List.copyOf(groupPath);
        }
    }
}
