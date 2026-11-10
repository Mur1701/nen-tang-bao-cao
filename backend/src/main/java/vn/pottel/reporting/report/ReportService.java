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

    public ReportService(ReportCatalog catalog, ReportRepository repo,
                         @Value("${reporting.execution-enabled:false}") boolean executionEnabled) {
        this.catalog = catalog;
        this.repo = repo;
        this.executionEnabled = executionEnabled;
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

    private void rejectLegacyPreviewMode(ReportQuery query) {
        if (query != null && query.previewMode() != null && !query.previewMode().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chế độ KIT preview không áp dụng cho báo cáo I.1 chính thức");
    }

    public ReportResult run(String code, ReportQuery query) {
        requireOfficialI1(code);
        var q = normalize(query);
        rejectLegacyPreviewMode(query);
        try {
            long total = repo.countI1(q.monthStart(), q.agencyId());
            var rows = repo.i1(q.monthStart(), q.agencyId(), q.size(),
                    (long) (q.page() - 1) * q.size());
            return new ReportResult("I.1", catalog.get("I.1").title(), false,
                    "Báo cáo I.1 theo nguồn dữ liệu nghiệp vụ đã được kiểm chứng.",
                    I1OfficialContract.columns(), rows, q.page(), q.size(), total);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "I.1: Stored Function hoặc nguồn nghiệp vụ chưa sẵn sàng");
        }
    }

    /** Full filtered export, never just the currently displayed page. */
    public ReportResult exportData(String code, ReportQuery query) {
        requireOfficialI1(code);
        var q = normalize(query);
        rejectLegacyPreviewMode(query);
        try {
            long total = repo.countI1(q.monthStart(), q.agencyId());
            if (total > 5000)
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Tối đa 5000 dòng Excel; không xuất file thiếu dòng");
            var rows = repo.i1(q.monthStart(), q.agencyId(), (int) total, 0);
            return new ReportResult("I.1", catalog.get("I.1").title(), false,
                    "Báo cáo I.1 theo nguồn dữ liệu nghiệp vụ đã được kiểm chứng.",
                    I1OfficialContract.columns(), rows, 1, rows.size(), total);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "I.1: Stored Function hoặc nguồn nghiệp vụ chưa sẵn sàng");
        }
    }
}
