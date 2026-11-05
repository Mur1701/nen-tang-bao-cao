package vn.pottel.reporting.report;

/** Metadata only. No SQL, business formula or user-controlled function name. */
public record ReportDefinition(
        String code,
        String title,
        String group,
        String periodFilter,
        String status,
        String description
) {}
