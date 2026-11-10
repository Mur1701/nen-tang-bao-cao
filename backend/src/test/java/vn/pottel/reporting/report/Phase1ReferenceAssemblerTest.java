package vn.pottel.reporting.report;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class Phase1ReferenceAssemblerTest {
    private final Phase1ReportSchemaCatalog catalog = new Phase1ReportSchemaCatalog();

    @Test
    void transformsActualAllocationDatesIntoGroupedExcelAndReactColumns() {
        var item = """
                {
                  "stt":1, "msisdn":"008412345678",
                  "package_code":"DL7", "revenue_gross":150000.00,
                  "revenue_net":136363.64, "vat_amount":13636.36,
                  "_basis":"LEGACY_REFERENCE",
                  "day_groups":{"current":{"2026-10-31":{
                    "net":19480.52,"vat":1948.05,"gross":21428.57
                  }},"next":{"2026-11-01":{
                    "net":19480.52,"vat":1948.05,"gross":21428.57
                  }}}
                }""";
        var result = Phase1ReferenceAssembler.build("II.1", "Doanh thu",
                catalog.require("II.1"), List.of(item), 1, 50, 1);
        assertTrue(result.preview());
        assertEquals(14, result.columns().size());
        assertEquals("008412345678", result.rows().getFirst().get("msisdn"));
        assertEquals(0, new java.math.BigDecimal(String.valueOf(result.rows().getFirst().get("revenue_gross")))
                .compareTo(new java.math.BigDecimal("150000.00")));
        assertEquals("19480.52", result.rows().getFirst().get("day_current_2026-10-31_net"));
        assertTrue(result.columns().stream().anyMatch(c ->
                c.groupPath().contains("2026-11-01")));
    }

    @Test
    void noFakeDaysForEmptyPeriods() {
        var result = Phase1ReferenceAssembler.build("II.4", "Chờ phân bổ",
                catalog.require("II.4"), List.of("{\"msisdn\":\"0900\"}"), 1, 50, 1);
        assertEquals(catalog.require("II.4").columns().size(), result.columns().size());
        assertNull(result.rows().getFirst().get("revenue_gross"));
    }

    @Test
    void allTwelveSchemasAcceptAnEmptyResultWithoutInventingAmounts() {
        for (var schema : catalog.all()) {
            var result = Phase1ReferenceAssembler.build(schema.code(), schema.code(),
                    schema, List.of(), 1, 50, 0);
            assertEquals(schema.columns().size(), result.columns().size(), schema.code());
            assertTrue(result.rows().isEmpty(), schema.code());
        }
    }
}
