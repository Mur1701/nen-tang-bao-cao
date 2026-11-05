package vn.pottel.reporting.report;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.NoSuchElementException;

/** Phase 1 IDs intentionally follow the NEW requirements, not the old platform. */
@Component
public class ReportCatalog {
    private static final List<ReportDefinition> REPORTS = List.of(
        new ReportDefinition("I.1", "Khuyến khích theo sản lượng", "COST", "MONTH", "PREVIEW", "Mới hỗ trợ xem trước số KIT kích hoạt; chưa tính tiền khuyến khích hoặc VAT."),
        new ReportDefinition("I.2", "Chi tiết thuê bao khuyến khích theo sản lượng", "COST", "TBD", "PLANNED", "Bộ lọc chi tiết chưa xác nhận."),
        new ReportDefinition("I.3", "Phân bổ chi phí thuê bao", "COST", "MONTH", "PLANNED", ""),
        new ReportDefinition("I.4", "Chi tiết thuê bao chưa phân bổ chi phí trong kỳ", "COST", "MONTH", "PLANNED", ""),
        new ReportDefinition("I.5", "Chi tiết thuê bao phân bổ chi phí trong kỳ", "COST", "MONTH", "PLANNED", ""),
        new ReportDefinition("I.6", "Chi tiết thuê bao phân bổ chi phí sang kỳ sau", "COST", "MONTH", "PLANNED", ""),
        new ReportDefinition("I.7", "Tổng hợp phân bổ chi phí bán gói", "COST", "MONTH", "PLANNED", ""),
        new ReportDefinition("II.1", "Phân bổ doanh thu thuê bao", "REVENUE", "MONTH", "PLANNED", ""),
        new ReportDefinition("II.2", "Chi tiết thuê bao chưa phân bổ doanh thu trong kỳ", "REVENUE", "MONTH", "PLANNED", ""),
        new ReportDefinition("II.3", "Chi tiết thuê bao phân bổ doanh thu trong kỳ", "REVENUE", "MONTH", "PLANNED", ""),
        new ReportDefinition("II.4", "Chi tiết thuê bao chờ phân bổ doanh thu sang kỳ sau", "REVENUE", "MONTH", "PLANNED", ""),
        new ReportDefinition("II.5", "Tổng hợp phân bổ doanh thu bán gói", "REVENUE", "MONTH", "PLANNED", "")
    );

    public List<ReportDefinition> all() { return REPORTS; }

    public ReportDefinition get(String code) {
        return REPORTS.stream().filter(r -> r.code().equalsIgnoreCase(code))
                .findFirst().orElseThrow(() -> new NoSuchElementException("Không tìm thấy báo cáo: " + code));
    }
}
