package vn.pottel.reporting.report;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.NoSuchElementException;

/** Phase 1 IDs intentionally follow the NEW requirements, not the old platform. */
@Component
public class ReportCatalog {
    private static final List<ReportDefinition> REPORTS = List.of(
        new ReportDefinition("I.1", "Khuyến khích theo sản lượng", "COST", "MONTH", "REFERENCE", "Có chế độ KIT kỹ thuật và tham chiếu BSS cũ; chưa nghiệm thu số tiền."),
        new ReportDefinition("I.2", "Chi tiết thuê bao khuyến khích theo sản lượng", "COST", "MONTH", "REFERENCE", "Áp dụng tháng/đại lý theo Phase 1, tính thưởng baseline BSS cũ."),
        new ReportDefinition("I.3", "Phân bổ chi phí thuê bao", "COST", "MONTH", "REFERENCE", "Có đường tính tham chiếu BSS cũ; chưa đối soát tài chính."),
        new ReportDefinition("I.4", "Chi tiết thuê bao chưa phân bổ chi phí trong kỳ", "COST", "MONTH", "REFERENCE", "Có đường tính tham chiếu BSS cũ; chưa đối soát tài chính."),
        new ReportDefinition("I.5", "Chi tiết thuê bao phân bổ chi phí trong kỳ", "COST", "MONTH", "REFERENCE", "Có đường tính tham chiếu BSS cũ; chưa đối soát tài chính."),
        new ReportDefinition("I.6", "Chi tiết thuê bao phân bổ chi phí sang kỳ sau", "COST", "MONTH", "REFERENCE", "Có đường tính tham chiếu BSS cũ; chưa đối soát tài chính."),
        new ReportDefinition("I.7", "Tổng hợp phân bổ chi phí bán gói", "COST", "MONTH", "REFERENCE", "Có đường tính tham chiếu BSS cũ; chưa đối soát tài chính."),
        new ReportDefinition("II.1", "Phân bổ doanh thu thuê bao", "REVENUE", "MONTH", "REFERENCE", "Có đường tính tham chiếu BSS cũ; chưa đối soát tài chính."),
        new ReportDefinition("II.2", "Chi tiết thuê bao chưa phân bổ doanh thu trong kỳ", "REVENUE", "MONTH", "REFERENCE", "Có đường tính tham chiếu BSS cũ; chưa đối soát tài chính."),
        new ReportDefinition("II.3", "Chi tiết thuê bao phân bổ doanh thu trong kỳ", "REVENUE", "MONTH", "REFERENCE", "Có đường tính tham chiếu BSS cũ; chưa đối soát tài chính."),
        new ReportDefinition("II.4", "Chi tiết thuê bao chờ phân bổ doanh thu sang kỳ sau", "REVENUE", "MONTH", "REFERENCE", "Có đường tính tham chiếu BSS cũ; chưa đối soát tài chính."),
        new ReportDefinition("II.5", "Tổng hợp phân bổ doanh thu bán gói", "REVENUE", "MONTH", "REFERENCE", "Có đường tính tham chiếu BSS cũ; chưa đối soát tài chính.")
    );

    public List<ReportDefinition> all() { return REPORTS; }

    public ReportDefinition get(String code) {
        return REPORTS.stream().filter(r -> r.code().equalsIgnoreCase(code))
                .findFirst().orElseThrow(() -> new NoSuchElementException("Không tìm thấy báo cáo: " + code));
    }
}
