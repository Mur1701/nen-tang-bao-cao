package vn.pottel.reporting.report;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class OfficialReportContractsTest {
    @Test
    void allPhaseOneCodesMapToDistinctFixedFunctionNames() {
        List<ReportFunctionRegistry.Target> targets = ReportFunctionRegistry.all();
        assertEquals(12, targets.size());
        assertEquals(12, targets.stream().map(ReportFunctionRegistry.Target::code)
                .collect(Collectors.toSet()).size());
        assertEquals(12, targets.stream().map(ReportFunctionRegistry.Target::qualifiedFunction)
                .collect(Collectors.toSet()).size());
        for (var target : targets) {
            assertTrue(target.qualifiedFunction().matches("reporting\\.fn_(i[1-7]|ii[1-5])_report"));
            assertTrue(target.sourceFile().startsWith("database/reports/"));
            assertEquals(target, ReportFunctionRegistry.require(target.code().toLowerCase(java.util.Locale.ROOT)));
        }
    }

    @Test
    void untrustedReportNamesCannotSelectArbitraryFunctions() {
        for (String input : new String[] { "", "I.0", "III.1", "I.1;DROP TABLE x", "pg_catalog.pg_proc" }) {
            assertThrows(java.util.NoSuchElementException.class, () -> ReportFunctionRegistry.require(input));
        }
        assertThrows(java.util.NoSuchElementException.class, () -> ReportFunctionRegistry.require(null));
    }

    @Test
    void i1OfficialColumnsMatchConfirmedBusinessTemplate() {
        assertEquals("agency_id + package_code", I1OfficialContract.ROW_GRAIN);
        assertEquals(List.of("STT", "Tên TĐL", "Mã gói", "Đơn giá", "Số active",
                        "Tổng active TĐL", "Mức đạt", "Tổng tiền", "Trước VAT", "VAT", "Thiếu giá"),
                I1OfficialContract.columns().stream().map(ReportResult.Column::title).toList());
        assertEquals(Set.of("stt", "agency_name", "package_code", "package_price",
                        "package_active_count", "agency_total_active", "achieved_tier",
                        "incentive_total", "incentive_before_vat", "vat_amount", "missing_price"),
                I1OfficialContract.columns().stream().map(ReportResult.Column::key).collect(Collectors.toSet()));
        assertEquals(11, I1OfficialContract.columns().size());
        assertEquals("decimal", I1OfficialContract.columns().get(7).type());
    }
}
