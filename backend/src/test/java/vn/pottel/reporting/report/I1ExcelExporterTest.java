package vn.pottel.reporting.report;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class I1ExcelExporterTest {
    @Test
    void officialExcelUsesElevenColumnsAndKeepsPreciseMoney() throws Exception {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("stt", 1L);
        row.put("agency_name", "Đại lý mẫu");
        row.put("package_code", "DL5");
        row.put("package_price", "20000.50");
        row.put("package_active_count", 1001L);
        row.put("agency_total_active", 1001L);
        row.put("achieved_tier", "5%");
        row.put("incentive_total", "1080.027");
        row.put("incentive_before_vat", "1000.025");
        row.put("vat_amount", "80.002");
        row.put("missing_price", 0L);
        var result = new ReportResult("I.1", "Test", false, "",
                I1OfficialContract.columns(), List.of(row), 1, 50, 1);
        var bytes = new ReportExcelExporter().toXlsx(result);

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals(11, sheet.getRow(0).getLastCellNum());
            assertEquals("Tổng tiền", sheet.getRow(0).getCell(7).getStringCellValue());
            assertEquals(CellType.NUMERIC, sheet.getRow(1).getCell(7).getCellType());
            assertEquals(1080.027, sheet.getRow(1).getCell(7).getNumericCellValue(), 0.0000001);
            assertEquals("DL5", sheet.getRow(1).getCell(2).getStringCellValue());
            assertEquals(1001, sheet.getRow(1).getCell(5).getNumericCellValue(), 0);
        }
    }

    @Test
    void veryPreciseAmountStaysTextInsteadOfBeingRoundedTo15Digits() throws Exception {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("incentive_total", "1234567890123456.78");
        var result = new ReportResult("I.1", "Test", false, "",
                I1OfficialContract.columns(), List.of(row), 1, 50, 1);
        var bytes = new ReportExcelExporter().toXlsx(result);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            var cell = workbook.getSheetAt(0).getRow(1).getCell(7);
            assertEquals(CellType.STRING, cell.getCellType());
            assertEquals("1234567890123456.78", cell.getStringCellValue());
        }
    }
}
