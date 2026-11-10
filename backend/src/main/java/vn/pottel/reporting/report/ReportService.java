package vn.pottel.reporting.report;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class ReportService {
    private final ReportCatalog catalog;
    private final ReportRepository repo;
    private final boolean executionEnabled;
    private final boolean kitPreviewEnabled;
    private final boolean packagePreviewEnabled;

    public ReportService(ReportCatalog catalog, ReportRepository repo,
                         @Value("${reporting.execution-enabled:false}") boolean executionEnabled,
                         @Value("${reporting.i1-kit-preview-enabled:false}") boolean kitPreviewEnabled,
                         @Value("${reporting.i1-package-preview-enabled:false}") boolean packagePreviewEnabled) {
        this.catalog = catalog;
        this.repo = repo;
        this.executionEnabled = executionEnabled;
        this.kitPreviewEnabled = kitPreviewEnabled;
        this.packagePreviewEnabled = packagePreviewEnabled;
    }
    private record Normalized(LocalDate monthStart, Integer agencyId, int page, int size) {}

    private Normalized normalize(ReportQuery q) {
        if (q == null || q.month() == null || !q.month().matches("\\d{4}-(0[1-9]|1[0-2])"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month phải có dạng YYYY-MM");
        final LocalDate first;
        try { first = YearMonth.parse(q.month()).atDay(1); }
        catch (DateTimeException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month không hợp lệ"); }
        int page = q.page() == null ? 1 : q.page();
        int size = q.pageSize() == null ? 50 : q.pageSize();
        if (page < 1 || size < 1 || size > 500 || (long) (page - 1) * size > Integer.MAX_VALUE)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page hoặc pageSize không hợp lệ");
        if (q.agencyId() != null && q.agencyId() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "agencyId phải dương");
        return new Normalized(first, q.agencyId(), page, size);
    }

    private void requireOfficialI1(String code) {
        try { catalog.get(code); }
        catch (java.util.NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy báo cáo");
        }
        if (!"I.1".equalsIgnoreCase(code))
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Báo cáo chưa triển khai Stored Function");
        if (!executionEnabled)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Báo cáo I.1 chính thức chưa được mở: cần nguồn dữ liệu đủ điều kiện và giá đã kiểm chứng");
    }

    /** Strictly technical KIT counters, never payable subscribers or financial amounts. */
    private boolean requireKitPreviewMode(String code, ReportQuery query) {
        String mode = query == null ? null : query.previewMode();
        if (mode == null || mode.isBlank()) return false;
        try { catalog.get(code); }
        catch (java.util.NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy báo cáo");
        }
        if (!"I.1".equalsIgnoreCase(code))
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Báo cáo chưa triển khai");
        if (!"AGENCY_KITS".equalsIgnoreCase(mode) && !"PACKAGE_KITS".equalsIgnoreCase(mode))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "previewMode không hợp lệ");
        if (!kitPreviewEnabled)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Đối soát KIT kỹ thuật đang tắt; cấu hình reporting.i1-kit-preview-enabled");
        if ("PACKAGE_KITS".equalsIgnoreCase(mode) && !packagePreviewEnabled)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Đối soát KIT theo gói chưa được mở");
        return true;
    }

    private ReportResult kitPreview(String code, ReportQuery query, boolean export) {
        var q = normalize(query);
        boolean byPackage = "PACKAGE_KITS".equalsIgnoreCase(query.previewMode());
        try {
            long total = byPackage
                    ? repo.countI1PackagePreview(q.monthStart(), q.agencyId())
                    : repo.countI1KitPreview(q.monthStart(), q.agencyId());
            if (export && total > 5000)
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Tối đa 5000 dòng Excel; không xuất file thiếu dòng");
            int limit = export ? (int) total : q.size();
            long offset = export ? 0L : (long) (q.page() - 1) * q.size();
            var rows = byPackage
                    ? repo.i1PackagePreview(q.monthStart(), q.agencyId(), limit, offset)
                    : repo.i1KitPreview(q.monthStart(), q.agencyId(), limit, offset);
            return new ReportResult("I.1", catalog.get("I.1").title(), true,
                    "CHỈ ĐỐI SOÁT KIT KỸ THUẬT — không phải số active đủ điều kiện; không có tiền/VAT.",
                    byPackage ? kitPackageColumns() : kitAgencyColumns(), rows,
                    export ? 1 : q.page(), export ? rows.size() : q.size(), total);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Function KIT preview chưa cài hoặc database chưa sẵn sàng");
        }
    }

    private static List<ReportResult.Column> kitAgencyColumns() {
        return List.of(
                new ReportResult.Column("agency_id", "ID đại lý", "integer"),
                new ReportResult.Column("agency_name", "Tên đại lý", "text"),
                new ReportResult.Column("activated_kit_count", "KIT kích hoạt", "integer")
        );
    }

    private static List<ReportResult.Column> kitPackageColumns() {
        return List.of(
                new ReportResult.Column("agency_id", "ID đại lý", "integer"),
                new ReportResult.Column("agency_name", "Tên đại lý", "text"),
                new ReportResult.Column("package_code", "Mã gói", "text"),
                new ReportResult.Column("activated_kit_count", "KIT kích hoạt theo gói", "integer"),
                new ReportResult.Column("agency_kit_total", "Tổng KIT đại lý", "integer")
        );
    }


    private final Phase1ReportSchemaCatalog referenceSchemas = new Phase1ReportSchemaCatalog();

    /**
     * User-approved migration baseline: SRS takes precedence; legacy BSS
     * supplies missing implementation rules. Results are explicitly labeled
     * reference estimates until SQL/amounts are verified on local PostgreSQL.
     */
    private ReportResult reference(String code, ReportQuery input, boolean export) {
        try { catalog.get(code); }
        catch (java.util.NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy báo cáo");
        }
        var q = normalize(input);
        var schema = referenceSchemas.require(code);
        if (input.previewMode() != null
                && !"LEGACY_REFERENCE".equalsIgnoreCase(input.previewMode()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "previewMode chỉ áp dụng I.1 KIT hoặc LEGACY_REFERENCE");
        try {
            long total = repo.countReference(code.toUpperCase(java.util.Locale.ROOT),
                    q.monthStart(), q.agencyId());
            if (export && total > 5000)
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Tối đa 5000 dòng Excel; không xuất file thiếu dòng");
            int limit = export ? (int) total : q.size();
            long offset = export ? 0L : (long) (q.page() - 1) * q.size();
            var json = repo.referenceJson(code.toUpperCase(java.util.Locale.ROOT),
                    q.monthStart(), q.agencyId(), limit, offset);
            return Phase1ReferenceAssembler.build(schema.code(),
                    catalog.get(schema.code()).title(), schema, json,
                    export ? 1 : q.page(), export ? json.size() : q.size(), total);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "SQL baseline BSS chưa được cài trên PostgreSQL local; "
                    + "cần 00_PHASE1_REFERENCE_CORE.sql và 01_PHASE1_LEGACY_REPORTS.sql");
        }
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReportResult run(String code, ReportQuery query) {
        if (requireKitPreviewMode(code, query)) return kitPreview(code, query, false);
        if (!"I.1".equalsIgnoreCase(code) || (query != null
                && "LEGACY_REFERENCE".equalsIgnoreCase(query.previewMode())))
            return reference(code, query, false);
        requireOfficialI1(code);
        var q = normalize(query);
        try {
            long total = repo.countI1(q.monthStart(), q.agencyId());
            var rows = repo.i1(q.monthStart(), q.agencyId(), q.size(),
                    (long) (q.page() - 1) * q.size());
            return new ReportResult("I.1", catalog.get("I.1").title(), false,
                    "I.1 chưa nghiệm thu nghiệp vụ; không dùng số liệu để thanh toán.",
                    I1OfficialContract.columns(), rows, q.page(), q.size(), total);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "I.1: Stored Function hoặc nguồn nghiệp vụ chưa sẵn sàng");
        }
    }

    /** Full filtered export, never just the currently displayed page. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReportResult exportData(String code, ReportQuery query) {
        if (requireKitPreviewMode(code, query)) return kitPreview(code, query, true);
        if (!"I.1".equalsIgnoreCase(code) || (query != null
                && "LEGACY_REFERENCE".equalsIgnoreCase(query.previewMode())))
            return reference(code, query, true);
        requireOfficialI1(code);
        var q = normalize(query);
        try {
            long total = repo.countI1(q.monthStart(), q.agencyId());
            if (total > 5000)
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Tối đa 5000 dòng Excel; không xuất file thiếu dòng");
            var rows = repo.i1(q.monthStart(), q.agencyId(), (int) total, 0);
            return new ReportResult("I.1", catalog.get("I.1").title(), false,
                    "I.1 chưa nghiệm thu nghiệp vụ; không dùng số liệu để thanh toán.",
                    I1OfficialContract.columns(), rows, 1, rows.size(), total);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "I.1: Stored Function hoặc nguồn nghiệp vụ chưa sẵn sàng");
        }
    }
}
