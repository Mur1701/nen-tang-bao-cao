package vn.pottel.reporting.report;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.NoSuchElementException;

/** Phase 1 IDs intentionally follow the NEW requirements, not the old platform. */
@Component
public class ReportCatalog {
    private static final List<ReportDefinition> REPORTS = List.of(
        new ReportDefinition("I.1", "Khuyến khích theo sản lượng", "COST", "MONTH", "REFERENCE", "Tổng hợp khuyến khích theo sản lượng theo đại lý và mã gói."),
        new ReportDefinition("I.2", "Chi tiết thuê bao khuyến khích theo sản lượng", "COST", "MONTH", "REFERENCE", "Chi tiết thuê bao thuộc kỳ tính khuyến khích theo sản lượng."),
        new ReportDefinition("I.3", "Phân bổ chi phí thuê bao", "COST", "MONTH", "REFERENCE", "Lịch phân bổ chi phí hoa hồng bán gói theo thuê bao."),
        new ReportDefinition("I.4", "Chi tiết thuê bao chưa phân bổ chi phí trong kỳ", "COST", "MONTH", "REFERENCE", "Các thuê bao có chi phí chưa phân bổ tại thời điểm kết thúc kỳ."),
        new ReportDefinition("I.5", "Chi tiết thuê bao phân bổ chi phí trong kỳ", "COST", "MONTH", "REFERENCE", "Chi phí hoa hồng đã phân bổ trong kỳ."),
        new ReportDefinition("I.6", "Chi tiết thuê bao phân bổ chi phí sang kỳ sau", "COST", "MONTH", "REFERENCE", "Chi phí hoa hồng dự kiến phân bổ sang kỳ tiếp theo."),
        new ReportDefinition("I.7", "Tổng hợp phân bổ chi phí bán gói", "COST", "MONTH", "REFERENCE", "Số dư chi phí đầu kỳ, phát sinh, phân bổ và cuối kỳ."),
        new ReportDefinition("II.1", "Phân bổ doanh thu thuê bao", "REVENUE", "MONTH", "REFERENCE", "Lịch phân bổ doanh thu gói theo thuê bao và ngày."),
        new ReportDefinition("II.2", "Chi tiết thuê bao chưa phân bổ doanh thu trong kỳ", "REVENUE", "MONTH", "REFERENCE", "Doanh thu gói chưa được phân bổ tại thời điểm kết thúc kỳ."),
        new ReportDefinition("II.3", "Chi tiết thuê bao phân bổ doanh thu trong kỳ", "REVENUE", "MONTH", "REFERENCE", "Doanh thu gói được phân bổ trong kỳ."),
        new ReportDefinition("II.4", "Chi tiết thuê bao chờ phân bổ doanh thu sang kỳ sau", "REVENUE", "MONTH", "REFERENCE", "Doanh thu gói dự kiến phân bổ sang kỳ tiếp theo."),
        new ReportDefinition("II.5", "Tổng hợp phân bổ doanh thu bán gói", "REVENUE", "MONTH", "REFERENCE", "Tổng hợp doanh thu theo đại lý, phát sinh, phân bổ và số dư.")
    );

    public List<ReportDefinition> all() { return REPORTS; }

    public ReportDefinition get(String code) {
        return REPORTS.stream().filter(r -> r.code().equalsIgnoreCase(code))
                .findFirst().orElseThrow(() -> new NoSuchElementException("Không tìm thấy báo cáo: " + code));
    }
}
