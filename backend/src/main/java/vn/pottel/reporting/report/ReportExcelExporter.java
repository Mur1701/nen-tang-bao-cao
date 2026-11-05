package vn.pottel.reporting.report;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Common Excel export; keep large exports out of memory in a later iteration. */
@Component
public class ReportExcelExporter {
    public byte[] toXlsx(ReportResult result) throws IOException {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Bao cao");
            var header = sheet.createRow(0);
            var bold = workbook.createCellStyle();
            var font = workbook.createFont();
            font.setBold(true);
            bold.setFont(font);
            for (int i = 0; i < result.columns().size(); i++) {
                var cell = header.createCell(i);
                cell.setCellValue(result.columns().get(i).title());
                cell.setCellStyle(bold);
                sheet.setColumnWidth(i, 22 * 256);
            }
            for (int r = 0; r < result.rows().size(); r++) {
                var row = sheet.createRow(r + 1);
                var data = result.rows().get(r);
                for (int c = 0; c < result.columns().size(); c++) {
                    var key = result.columns().get(c).key();
                    var value = data.get(key);
                    var cell = row.createCell(c);
                    if (value == null) continue;
                    if (value instanceof Integer || value instanceof Long)
                        cell.setCellValue(((Number) value).doubleValue());
                    else cell.setCellValue(value.toString()); // Decimal money stays text: avoid Excel precision loss.
                }
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(1, result.rows().size()), 0, result.columns().size() - 1));
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
