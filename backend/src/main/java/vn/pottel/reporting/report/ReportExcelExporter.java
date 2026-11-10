package vn.pottel.reporting.report;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

/** One XLSX output contract for all reports; never recalculate financial amounts. */
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
            var decimalStyle = workbook.createCellStyle();
            decimalStyle.setDataFormat(workbook.createDataFormat()
                    .getFormat("#,##0.################"));
            var integerStyle = workbook.createCellStyle();
            integerStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));

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
                    var col = result.columns().get(c);
                    var value = data.get(col.key());
                    var cell = row.createCell(c);
                    if (value == null) continue;
                    if ("decimal".equals(col.type()) || "integer".equals(col.type())) {
                        try {
                            var n = new BigDecimal(value.toString());
                            // Excel supports up to 15 significant decimal digits.
                            // Large/precise amounts remain text rather than losing digits.
                            if (n.precision() <= 15 && Double.isFinite(n.doubleValue())) {
                                cell.setCellValue(n.doubleValue());
                                cell.setCellStyle("integer".equals(col.type()) ? integerStyle : decimalStyle);
                                continue;
                            }
                        } catch (NumberFormatException ignored) {
                            // Unexpected source data remains text; never coerce to zero.
                        }
                    }
                    cell.setCellValue(value.toString());
                }
            }

            sheet.createFreezePane(0, 1);
            if (result.columns().size() > 0)
                sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                        0, Math.max(1, result.rows().size()), 0, result.columns().size() - 1));
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
