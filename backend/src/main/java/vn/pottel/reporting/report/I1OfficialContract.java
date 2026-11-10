package vn.pottel.reporting.report;

import java.util.List;

/**
 * I.1 target contract based on SRS-I1 v0.3 / DEC-I1-07 (11 columns).
 *
 * This is metadata ONLY, not an executable financial report. Eligibility,
 * historical pricing, fixed bonuses, VAT base, rounding and missing-price
 * handling remain pending business decisions. Do not substitute KIT preview.
 */
public final class I1OfficialContract {
    private I1OfficialContract() {}

    public static final String REPORT_CODE = "I.1";
    public static final String ROW_GRAIN = "agency_id + package_code";

    /** Ordered columns shared by the official API and XLSX layout. */
    public static List<ReportResult.Column> columns() {
        return List.of(
            new ReportResult.Column("stt", "STT", "integer"),
            new ReportResult.Column("agency_name", "Tên TĐL", "text"),
            new ReportResult.Column("package_code", "Mã gói", "text"),
            new ReportResult.Column("package_price", "Đơn giá", "decimal"),
            new ReportResult.Column("package_active_count", "Số active", "integer"),
            new ReportResult.Column("agency_total_active", "Tổng active TĐL", "integer"),
            new ReportResult.Column("achieved_tier", "Mức đạt", "text"),
            new ReportResult.Column("incentive_total", "Tổng tiền", "decimal"),
            new ReportResult.Column("incentive_before_vat", "Trước VAT", "decimal"),
            new ReportResult.Column("vat_amount", "VAT", "decimal"),
            new ReportResult.Column("missing_price", "Thiếu giá", "text")
        );
    }
}
