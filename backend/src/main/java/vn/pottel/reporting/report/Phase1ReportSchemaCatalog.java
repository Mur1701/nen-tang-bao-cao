package vn.pottel.reporting.report;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * SRS Phase 1 DRAFT (04/11/2026): display-only field/type/group contracts.
 * A display column is not evidence of BSS source mapping or approved finance.
 * Dynamic day groups describe required UI layout, NOT fabricated dates.
 */
@Component
public final class Phase1ReportSchemaCatalog {
    public record Column(String key, String title, String type, String group) {}
    public record DynamicGroup(String key, String title, String period, List<String> measures) {}
    public record Schema(String code, String source, List<String> filters,
                         List<Column> columns, List<DynamicGroup> dynamicGroups,
                         String status, boolean executable, String blocker) {}

    private static Column c(String key, String title, String type, String group) {
        return new Column(key, title, type, group);
    }
    private static DynamicGroup d(String key, String title, String period, String measures) {
        return new DynamicGroup(key, title, period, List.of(measures.split("\\|")));
    }
    private static Schema s(String code, List<String> filters, List<Column> columns,
                            List<DynamicGroup> days, String status, String blocker) {
        return new Schema(code, "SRS_" + code.replace(".", "") + ".md v0.1 DRAFT (04/11/2026)",
                filters, columns, days, status, false, blocker);
    }

    private static final Map<String, Schema> BY_CODE = build();

    private static Map<String, Schema> build() {
        var map = new LinkedHashMap<String, Schema>();

        put(map, s("I.1", List.of("month", "agencyId"),
            List.of(
                c("stt", "STT", "integer", null),
                c("agency_name", "Tên TĐL", "text", null),
                c("package_code", "Mã gói", "text", null),
                c("package_price", "Đơn giá", "decimal", null),
                c("package_active_count", "Số active", "integer", null),
                c("agency_total_active", "Tổng active TĐL", "integer", null),
                c("achieved_tier", "Mức đạt", "text", null),
                c("incentive_total", "Tổng tiền", "decimal", null),
                c("incentive_before_vat", "Trước VAT", "decimal", null),
                c("vat_amount", "VAT", "decimal", null),
                c("missing_price", "Thiếu giá", "integer", null)
            ),
            List.of(), "IMPLEMENTING", "Cần xác minh eligible active, giá lịch sử và quyết định khuyến khích I.1."));

        put(map, s("I.2", List.of("month", "agencyId"),
            List.of(
                c("stt", "STT", "integer", null),
                c("agency_name", "Tên TĐL", "text", null),
                c("msisdn", "Số thuê bao", "text", null),
                c("subscriber_active_at", "Thời gian active thuê bao", "datetime", null),
                c("package_code", "Mã gói", "text", null),
                c("package_price", "Giá gói", "decimal", null),
                c("tier", "Mốc được hưởng trong kỳ", "text", null),
                c("incentive_total", "Tổng tiền", "decimal", null),
                c("vat_amount", "VAT", "decimal", null),
                c("incentive_net", "Tổng tiền trước VAT", "decimal", null)
            ),
            List.of(), "DRAFT_UNVERIFIED", "TBD-I2-01..03: kế thừa eligibility/chính sách I.1, khử trùng thuê bao/gói và đối soát."));

        put(map, s("I.3", List.of("month"),
            List.of(
                c("msisdn", "Số thuê bao", "text", null),
                c("package_code", "Mã gói cước", "text", null),
                c("package_registered_at", "Ngày đăng ký gói", "datetime", null),
                c("payment_at", "Ngày chi trả", "datetime", null),
                c("package_active_at", "Ngày active gói", "datetime", null),
                c("payment_amount_first", "Số tiền chi trả đợt 1", "decimal", null),
                c("payment_amount_second", "Số tiền chi trả đợt 2", "decimal", null),
                c("commission_gross", "Tiền hoa hồng bán gói", "decimal", null),
                c("commission_net", "Tiền HHBG chưa VAT", "decimal", null),
                c("vat_amount", "VAT", "decimal", null)
            ),
            List.of(d("current", "Phân bổ chi phí trong kỳ", "CURRENT_MONTH", "CP chưa VAT|VAT|Tổng CP"), d("next", "Phân bổ chi phí kỳ sau", "NEXT_MONTH", "CP chưa VAT|VAT|Tổng CP")), "DRAFT_UNVERIFIED", "TBD-I3-01..03: mâu thuẫn cơ sở phân bổ (hoa hồng/giá gói), chu kỳ, phần dư, đợt chi trả."));

        put(map, s("I.4", List.of("month"),
            List.of(
                c("stt", "STT", "integer", null),
                c("msisdn", "Số thuê bao", "text", null),
                c("package_code", "Mã gói cước", "text", null),
                c("package_registered_at", "Ngày đăng ký gói (ngày active thuê bao)", "datetime", null),
                c("payment_at", "Ngày chi trả", "datetime", null),
                c("payment_amount_first", "Số tiền chi trả đợt 1", "decimal", null),
                c("payment_amount_second", "Số tiền chi trả đợt 2", "decimal", null),
                c("commission_gross", "Số tiền hoa hồng bán gói", "decimal", null),
                c("commission_net", "Số tiền hoa hồng chưa VAT", "decimal", null),
                c("vat_amount", "VAT", "decimal", null)
            ),
            List.of(), "DRAFT_UNVERIFIED", "TBD-I4-01..02: cutoff chưa active gói và nhiều đợt chi trả/nguồn snapshot."));

        put(map, s("I.5", List.of("month"),
            List.of(
                c("stt", "STT", "integer", null),
                c("msisdn", "Số thuê bao", "text", null),
                c("package_code", "Mã gói", "text", null),
                c("subscriber_active_at", "Ngày active thuê bao", "datetime", null),
                c("payment_at", "Ngày chi trả", "datetime", null),
                c("package_active_at", "Ngày active gói", "datetime", null),
                c("payment_amount_first", "Số tiền chi trả đợt 1", "decimal", null),
                c("payment_amount_second", "Số tiền chi trả đợt 2", "decimal", null),
                c("commission_gross", "Tiền hoa hồng bán gói", "decimal", null),
                c("commission_net", "Tiền HHBG chưa VAT", "decimal", null),
                c("vat_amount", "VAT", "decimal", null),
                c("allocated_days", "Số ngày phân bổ trong kỳ", "integer", null)
            ),
            List.of(d("current", "Ngày phân bổ chi phí trong kỳ", "CURRENT_MONTH", "CP chưa VAT|VAT|Tổng tiền")), "DRAFT_UNVERIFIED", "TBD-I5-01..03: lịch phân bổ/kỳ trước, cơ sở chi phí và làm tròn."));

        put(map, s("I.6", List.of("month"),
            List.of(
                c("stt", "STT", "integer", null),
                c("msisdn", "Số thuê bao", "text", null),
                c("package_code", "Mã gói cước", "text", null),
                c("subscriber_active_at", "Ngày active thuê bao", "datetime", null),
                c("payment_first_at", "Ngày chi trả đợt 1", "datetime", null),
                c("package_active_at", "Ngày active gói", "datetime", null),
                c("payment_amount_first", "Số tiền chi trả đợt 1", "decimal", null),
                c("payment_amount_second", "Số tiền chi trả đợt 2", "decimal", null),
                c("commission_gross", "Tiền hoa hồng bán gói", "decimal", null),
                c("commission_net", "Tiền HHBG chưa VAT", "decimal", null),
                c("vat_amount", "VAT", "decimal", null),
                c("forward_days", "Số ngày phân bổ sang kỳ sau", "integer", null)
            ),
            List.of(d("next", "Ngày phân bổ chi phí kỳ sau", "NEXT_MONTH", "CP chưa VAT|VAT|Tổng tiền")), "DRAFT_UNVERIFIED", "TBD-I6-01..03: nhiều đợt chi trả, phần dư và chu kỳ kéo dài qua n+1."));

        put(map, s("I.7", List.of("month"),
            List.of(
                c("opening_net", "CP chưa VAT", "decimal", "Chi phí trả trước chờ phân bổ đầu kỳ (1)"),
                c("opening_vat", "VAT", "decimal", "Chi phí trả trước chờ phân bổ đầu kỳ (1)"),
                c("opening_gross", "Tổng tiền", "decimal", "Chi phí trả trước chờ phân bổ đầu kỳ (1)"),
                c("incurred_net", "CP chưa VAT", "decimal", "Chi phí trả trước phát sinh trong kỳ (2)"),
                c("incurred_vat", "VAT", "decimal", "Chi phí trả trước phát sinh trong kỳ (2)"),
                c("incurred_gross", "Tổng tiền", "decimal", "Chi phí trả trước phát sinh trong kỳ (2)"),
                c("allocated_net", "CP chưa VAT", "decimal", "Chi phí phân bổ trong kỳ (3)"),
                c("allocated_vat", "VAT", "decimal", "Chi phí phân bổ trong kỳ (3)"),
                c("allocated_gross", "Tổng tiền", "decimal", "Chi phí phân bổ trong kỳ (3)"),
                c("closing_net", "CP chưa VAT", "decimal", "Chi phí chưa phân bổ cuối kỳ = (1)+(2)−(3)"),
                c("closing_vat", "VAT", "decimal", "Chi phí chưa phân bổ cuối kỳ = (1)+(2)−(3)"),
                c("closing_gross", "Tổng tiền", "decimal", "Chi phí chưa phân bổ cuối kỳ = (1)+(2)−(3)")
            ),
            List.of(), "DRAFT_UNVERIFIED", "TBD-I7-01..03: xác minh khoản đầu kỳ/phát sinh/phân bổ, số dư và đối chiếu I.4–I.6."));

        put(map, s("II.1", List.of("month"),
            List.of(
                c("stt", "STT", "integer", null),
                c("msisdn", "Số thuê bao", "text", null),
                c("package_code", "Mã gói cước", "text", null),
                c("subscriber_active_at", "Ngày active thuê bao", "datetime", null),
                c("package_active_at", "Ngày active gói", "datetime", null),
                c("revenue_gross", "Doanh thu", "decimal", null),
                c("revenue_net", "Doanh thu chưa VAT", "decimal", null),
                c("vat_amount", "VAT", "decimal", null)
            ),
            List.of(d("current", "Doanh thu phân bổ trong kỳ", "CURRENT_MONTH", "DT chưa VAT|VAT|Tổng DT"), d("next", "Doanh thu phân bổ kỳ sau", "NEXT_MONTH", "DT chưa VAT|VAT|Tổng DT")), "DRAFT_UNVERIFIED", "TBD-II1-01..03: cơ sở doanh thu, tái tục/hủy gói, chu kỳ, phần dư."));

        put(map, s("II.2", List.of("month"),
            List.of(
                c("stt", "STT", "integer", null),
                c("msisdn", "Số thuê bao", "text", null),
                c("package_code", "Mã gói cước", "text", null),
                c("subscriber_active_at", "Ngày active thuê bao", "datetime", null),
                c("revenue_gross", "Doanh thu gói", "decimal", null),
                c("revenue_net", "Doanh thu chưa VAT", "decimal", null),
                c("vat_amount", "VAT", "decimal", null)
            ),
            List.of(), "DRAFT_UNVERIFIED", "TBD-II2-01..03: xác định chưa active gói theo cutoff, giá lịch sử và VAT."));

        put(map, s("II.3", List.of("month"),
            List.of(
                c("stt", "TT", "integer", null),
                c("msisdn", "Số thuê bao", "text", null),
                c("package_code", "Mã gói cước", "text", null),
                c("subscriber_active_at", "Ngày active thuê bao", "datetime", null),
                c("package_active_at", "Ngày active gói", "datetime", null),
                c("revenue_gross", "Tổng doanh thu", "decimal", null),
                c("revenue_net", "Doanh thu chưa VAT", "decimal", null),
                c("vat_amount", "VAT", "decimal", null),
                c("allocated_days", "Số ngày phân bổ trong kỳ", "integer", null)
            ),
            List.of(d("current", "Doanh thu phân bổ trong kỳ theo ngày", "CURRENT_MONTH", "DT chưa VAT|VAT|Tổng tiền")), "DRAFT_UNVERIFIED", "TBD-II3-01..03: ngày từ kỳ trước, chu kỳ/lần active và rounding."));

        put(map, s("II.4", List.of("month"),
            List.of(
                c("stt", "STT", "integer", null),
                c("msisdn", "Số thuê bao", "text", null),
                c("package_code", "Mã gói cước", "text", null),
                c("subscriber_active_at", "Ngày active thuê bao", "datetime", null),
                c("package_active_at", "Ngày active gói", "datetime", null),
                c("revenue_gross", "Tổng doanh thu", "decimal", null),
                c("revenue_net", "Doanh thu chưa VAT", "decimal", null),
                c("vat_amount", "VAT", "decimal", null),
                c("forward_days", "Số ngày phân bổ kỳ sau", "integer", null)
            ),
            List.of(d("next", "Doanh thu phân bổ sang kỳ sau (nhãn CP chưa VAT trong form gốc đang TBD)", "NEXT_MONTH", "CP chưa VAT|VAT|Tổng tiền")), "DRAFT_UNVERIFIED", "TBD-II4-01..03: nhãn CP/DT trong biểu mẫu, ngày cuối và chu kỳ >2 tháng."));

        put(map, s("II.5", List.of("month"),
            List.of(
                c("stt", "STT", "integer", null),
                c("agency_name", "Đại lý", "text", null),
                c("profile_net", "DT chưa VAT", "decimal", "DT Profile SIM"),
                c("profile_vat", "VAT", "decimal", "DT Profile SIM"),
                c("profile_gross", "Tổng tiền", "decimal", "DT Profile SIM"),
                c("activation_net", "DT chưa VAT", "decimal", "DT hòa mạng"),
                c("activation_vat", "VAT", "decimal", "DT hòa mạng"),
                c("activation_gross", "Tổng tiền", "decimal", "DT hòa mạng"),
                c("opening_net", "DT chưa VAT", "decimal", "Doanh thu gói chưa PB đầu kỳ (1)"),
                c("opening_vat", "VAT", "decimal", "Doanh thu gói chưa PB đầu kỳ (1)"),
                c("opening_gross", "Tổng tiền", "decimal", "Doanh thu gói chưa PB đầu kỳ (1)"),
                c("incurred_net", "DT chưa VAT", "decimal", "Doanh thu gói chưa PB phát sinh kỳ (2)"),
                c("incurred_vat", "VAT", "decimal", "Doanh thu gói chưa PB phát sinh kỳ (2)"),
                c("incurred_gross", "Tổng tiền", "decimal", "Doanh thu gói chưa PB phát sinh kỳ (2)"),
                c("allocated_net", "DT chưa VAT", "decimal", "Doanh thu gói PB trong kỳ (3)"),
                c("allocated_vat", "VAT", "decimal", "Doanh thu gói PB trong kỳ (3)"),
                c("allocated_gross", "Tổng tiền", "decimal", "Doanh thu gói PB trong kỳ (3)"),
                c("closing_net", "DT chưa VAT", "decimal", "Doanh thu gói chưa PB cuối kỳ = (1)+(2)−(3)"),
                c("closing_vat", "VAT", "decimal", "Doanh thu gói chưa PB cuối kỳ = (1)+(2)−(3)"),
                c("closing_gross", "Tổng tiền", "decimal", "Doanh thu gói chưa PB cuối kỳ = (1)+(2)−(3)")
            ),
            List.of(), "DRAFT_UNVERIFIED", "TBD-II5-01..03: bậc giá Profile theo lô, 25.000đ hòa mạng, VAT, nguồn phân bổ và đối chiếu."));

        return Map.copyOf(map);
    }
    private static void put(Map<String, Schema> map, Schema schema) {
        if (map.putIfAbsent(schema.code(), schema) != null)
            throw new IllegalStateException("Duplicate report schema " + schema.code());
    }
    public Schema require(String code) {
        if (code == null) throw new NoSuchElementException("Missing report code");
        var result = BY_CODE.get(code.toUpperCase(Locale.ROOT));
        if (result == null) throw new NoSuchElementException("Unknown report code");
        return result;
    }
    public List<Schema> all() {
        return List.copyOf(ReportFunctionRegistry.all().stream()
                .map(target -> require(target.code())).toList());
    }
}
