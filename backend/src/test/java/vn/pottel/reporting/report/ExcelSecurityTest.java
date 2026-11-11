package vn.pottel.reporting.report;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExcelSecurityTest {
    @Test
    void spreadsheetLookingAgencyNamesArePlainTextNotFormulas() throws Exception {
        var names = List.of("=1+1", "+SUM(1,2)", "-1+2", "@SUM(A1:A2)",
                "\t=1+1", "\n=1+1", "001234567890");
        var columns = List.of(new ReportResult.Column("agency_name", "Tên đại lý", "text"));
        for (var name : names) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("agency_name", name);
            var result = new ReportResult("II.5", "Test", true, "",
                    columns, List.of(row), 1, 50, 1);
            try (var workbook = WorkbookFactory.create(
                    new ByteArrayInputStream(new ReportExcelExporter().toXlsx(result)))) {
                var cell = workbook.getSheetAt(0).getRow(1).getCell(0);
                assertEquals(CellType.STRING, cell.getCellType(), name);
                assertEquals(name, cell.getStringCellValue());
            }
        }
    }
}
