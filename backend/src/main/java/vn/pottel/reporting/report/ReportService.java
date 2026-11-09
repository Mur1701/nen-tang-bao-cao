package vn.pottel.reporting.report;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class ReportService {
    private final ReportCatalog catalog;
    private final ReportRepository repo;
    private final boolean executionEnabled;
    private final boolean packagePreviewEnabled;

    public ReportService(ReportCatalog catalog, ReportRepository repo,
                         @Value("${reporting.execution-enabled:false}") boolean executionEnabled,
                         @Value("${reporting.i1-package-preview-enabled:false}") boolean packagePreviewEnabled) {
        this.catalog = catalog;
        this.repo = repo;
        this.executionEnabled = executionEnabled;
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

    private void requirePreview(String code) {
        try { catalog.get(code); }
        catch (java.util.NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy báo cáo");
        }
        if (!"I.1".equalsIgnoreCase(code))
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Báo cáo chưa triển khai Stored Function");
        if (!executionEnabled)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Truy vấn đang khóa: chỉ mở sau khi cài Stored Function và cấu hình RBAC");
    }

    private boolean packagePreviewRequested(ReportQuery query) {
        String mode = query == null ? null : query.previewMode();
        if (mode == null || mode.isBlank() || "AGENCY_KITS".equalsIgnoreCase(mode)) return false;
        if (!"PACKAGE_KITS".equalsIgnoreCase(mode))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "previewMode không hợp lệ");
        if (!packagePreviewEnabled)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Preview KIT theo gói đang khóa; cần cài function demo và cấu hình bảo vệ API");
        return true;
    }

    public ReportResult run(String code, ReportQuery query) {
        requirePreview(code);
        var q = normalize(query);
        boolean packageMode = packagePreviewRequested(query);
        final long total;
        final List<java.util.Map<String, Object>> rows;
        try {
            if (packageMode) {
                total = repo.countI1Packages(q.monthStart(), q.agencyId());
                rows = repo.i1Packages(q.monthStart(), q.agencyId(), q.size(), (long) (q.page() - 1) * q.size());
            } else {
                total = repo.countI1(q.monthStart(), q.agencyId());
                rows = repo.i1(q.monthStart(), q.agencyId(), q.size(), (long) (q.page() - 1) * q.size());
            }
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stored Function chưa sẵn sàng hoặc truy vấn thất bại");
        }
        return new ReportResult("I.1", catalog.get("I.1").title(), true,
                packageMode
                    ? "CHỈ KIT KÍCH HOẠT THEO ĐẠI LÝ × GÓI. Không phải số active đủ điều kiện; chưa tính tiền/VAT."
                    : "SỐ KIT KÍCH HOẠT (xem trước). Chưa phải số thuê bao tính thưởng; chưa tính tiền/VAT.",
                packageMode ? packageColumns() : columns(), rows, q.page(), q.size(), total);
    }
    /** Export the same filters, without page limit. Refuse silent truncation. */
    public ReportResult exportData(String code, ReportQuery query) {
        requirePreview(code);
        var q = normalize(query);
        boolean packageMode = packagePreviewRequested(query);
        final long total;
        final List<java.util.Map<String, Object>> rows;
        try {
            total = packageMode ? repo.countI1Packages(q.monthStart(), q.agencyId())
                                : repo.countI1(q.monthStart(), q.agencyId());
            if (total > 5000)
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Tối đa 5000 dòng Excel; cần cơ chế streaming cho báo cáo lớn");
            rows = packageMode ? repo.i1Packages(q.monthStart(), q.agencyId(), (int) total + 1, 0)
                               : repo.i1(q.monthStart(), q.agencyId(), (int) total + 1, 0);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stored Function chưa sẵn sàng hoặc truy vấn thất bại");
        }
        return new ReportResult("I.1", catalog.get("I.1").title(), true,
                packageMode
                    ? "Chỉ thống kê KIT theo mã gói, không phải dữ liệu khuyến khích đã nghiệm thu."
                    : "Chỉ là thống kê KIT; chưa được dùng để thanh toán/đối soát tiền.",
                packageMode ? packageColumns() : columns(), rows, 1, rows.size(), total);
    }

    private static List<ReportResult.Column> packageColumns() {
        return List.of(
                new ReportResult.Column("agency_id", "ID đại lý", "integer"),
                new ReportResult.Column("agency_name", "Tên đại lý", "text"),
                new ReportResult.Column("package_code", "Mã gói", "text"),
                new ReportResult.Column("activated_kit_count", "KIT kích hoạt theo gói", "integer"),
                new ReportResult.Column("agency_kit_total", "Tổng KIT đại lý", "integer")
        );
    }
    private static List<ReportResult.Column> columns() {
        return List.of(
                new ReportResult.Column("agency_id", "ID đại lý", "integer"),
                new ReportResult.Column("agency_name", "Tên đại lý", "text"),
                new ReportResult.Column("activated_kit_count", "KIT kích hoạt", "integer")
        );
    }
}
