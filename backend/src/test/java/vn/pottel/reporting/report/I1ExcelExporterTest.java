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
    @Test
    void groupedAllocationHeadersRemainMergedAndDoNotChangeSourceValues() throws Exception {
        var cols = List.of(
                new ReportResult.Column("msisdn", "Số thuê bao", "text"),
                new ReportResult.Column("net", "CP chưa VAT", "decimal",
                        List.of("Chi phí phân bổ trong kỳ", "01/11/2026")),
                new ReportResult.Column("vat", "VAT", "decimal",
                        List.of("Chi phí phân bổ trong kỳ", "01/11/2026")),
                new ReportResult.Column("gross", "Tổng CP", "decimal",
                        List.of("Chi phí phân bổ trong kỳ", "01/11/2026"))
        );
        var row = Map.<String, Object>of("msisdn", "0084123456",
                "net", "100.25", "vat", "8.02", "gross", "108.27");
        var result = new ReportResult("I.3", "Sample", false, "",
                cols, List.of(row), 1, 50, 1);
        var bytes = new ReportExcelExporter().toXlsx(result);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals("Chi phí phân bổ trong kỳ", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("01/11/2026", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("VAT", sheet.getRow(2).getCell(2).getStringCellValue());
            assertEquals("0084123456", sheet.getRow(3).getCell(0).getStringCellValue());
            assertEquals(108.27, sheet.getRow(3).getCell(3).getNumericCellValue(), 0.000001);
            assertTrue(sheet.getNumMergedRegions() >= 2);
        }
    }


    @Test
    void excelDatesUseNativeDateCellsWithoutLosingTime() throws Exception {
        var cols = List.of(
                new ReportResult.Column("registered", "Ngày đăng ký", "date"),
                new ReportResult.Column("activated", "Thời gian kích hoạt", "datetime"),
                new ReportResult.Column("msisdn", "Số thuê bao", "text")
        );
        var row = Map.<String, Object>of(
                "registered", "2026-11-10",
                "activated", "2026-11-10T09:30:45+07:00",
                "msisdn", "008400001234"
        );
        var result = new ReportResult("II.1", "Kiểm thử", true, "",
                cols, List.of(row), 1, 50, 1);
        try (var wb = WorkbookFactory.create(new ByteArrayInputStream(new ReportExcelExporter().toXlsx(result)))) {
            var sheet = wb.getSheetAt(0);
            assertEquals(CellType.NUMERIC, sheet.getRow(1).getCell(0).getCellType());
            assertEquals("dd/mm/yyyy", sheet.getRow(1).getCell(0).getCellStyle().getDataFormatString());
            assertEquals(java.time.LocalDateTime.of(2026, 11, 10, 9, 30, 45),
                    sheet.getRow(1).getCell(1).getLocalDateTimeCellValue());
            assertEquals("008400001234", sheet.getRow(1).getCell(2).getStringCellValue());
        }
    }
}
