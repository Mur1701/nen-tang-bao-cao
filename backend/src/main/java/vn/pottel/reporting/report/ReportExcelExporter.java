package vn.pottel.reporting.report;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.apache.poi.ss.util.CellRangeAddress;

/** One XLSX output contract for all reports; never recalculate financial amounts. */
@Component
public class ReportExcelExporter {
    public byte[] toXlsx(ReportResult result) throws IOException {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Bao cao");
            int headerLevels = result.columns().stream()
                    .mapToInt(col -> col.groupPath().size() + 1).max().orElse(1);
            for (int level = 0; level < headerLevels; level++) sheet.createRow(level);
            var bold = workbook.createCellStyle();
            var font = workbook.createFont();
            font.setBold(true);
            bold.setFont(font);
            var decimalStyle = workbook.createCellStyle();
            decimalStyle.setDataFormat(workbook.createDataFormat()
                    .getFormat("#,##0.################"));
            var integerStyle = workbook.createCellStyle();
            integerStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));

            // Multi-row grouped headers for allocation-day reports.
            // No synthetic dates: groupPath must come from the validated result.
            for (int i = 0; i < result.columns().size(); i++) {
                var col = result.columns().get(i);
                List<String> path = col.groupPath();
                for (int level = 0; level < path.size(); level++) {
                    if (i > 0 && sharesPrefix(result.columns().get(i - 1).groupPath(), path, level))
                        continue;
                    var cell = sheet.getRow(level).createCell(i);
                    cell.setCellValue(path.get(level));
                    cell.setCellStyle(bold);
                    int last = i;
                    while (last + 1 < result.columns().size()
                            && sharesPrefix(result.columns().get(last + 1).groupPath(), path, level))
                        last++;
                    if (last > i) sheet.addMergedRegion(new CellRangeAddress(level, level, i, last));
                }
                var cell = sheet.getRow(path.size()).createCell(i);
                cell.setCellValue(col.title());
                cell.setCellStyle(bold);
                if (path.size() < headerLevels - 1)
                    sheet.addMergedRegion(new CellRangeAddress(path.size(), headerLevels - 1, i, i));
                sheet.setColumnWidth(i, 22 * 256);
            }

            for (int r = 0; r < result.rows().size(); r++) {
                var row = sheet.createRow(r + headerLevels);
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

            sheet.createFreezePane(0, headerLevels);
            if (headerLevels == 1 && result.columns().size() > 0)
                sheet.setAutoFilter(new CellRangeAddress(
                        0, Math.max(1, result.rows().size()), 0, result.columns().size() - 1));
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static boolean sharesPrefix(List<String> previous, List<String> current, int level) {
        if (previous.size() <= level || current.size() <= level) return false;
        for (int i = 0; i <= level; i++)
            if (!previous.get(i).equals(current.get(i))) return false;
        return true;
    }
}
