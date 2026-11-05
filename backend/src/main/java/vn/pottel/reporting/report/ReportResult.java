package vn.pottel.reporting.report;

import java.util.List;
import java.util.Map;

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
    public record Column(String key, String title, String type) {}
}
