package vn.pottel.reporting.report;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Takes database JSON rows and produces the same typed columns for React/XLSX.
 * Monetary values retain BigDecimal-style text, never Java double precision.
 * Real per-day groups come from database data; no invented daily allocations.
 */
public final class Phase1ReferenceAssembler {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String[] MEASURES = {"net", "vat", "gross"};
    private static final String[] MEASURE_LABELS = {"Chưa VAT", "VAT", "Tổng"};

    private Phase1ReferenceAssembler() {}

    public static ReportResult build(String code, String title, Phase1ReportSchemaCatalog.Schema schema,
                                     List<String> payloads, int page, int pageSize, long total) {
        var rows = new ArrayList<Map<String, Object>>();
        Map<String, Set<String>> dayKeys = new LinkedHashMap<>();
        for (var group : schema.dynamicGroups())
            dayKeys.put(group.key(), new TreeSet<>());

        for (var payload : payloads) {
            final JsonNode tree;
            try { tree = JSON.readTree(payload); }
            catch (RuntimeException e) { throw new IllegalArgumentException("Invalid reporting JSON", e); }
            var row = new LinkedHashMap<String, Object>();
            for (var field : schema.columns()) {
                var val = tree.path(field.key());
                if (val.isMissingNode() || val.isNull()) {
                    row.put(field.key(), null);
                } else if ("integer".equals(field.type())) {
                    row.put(field.key(), val.longValue());
                } else {
                    // Monetary decimals and text both need lossless representation.
                    row.put(field.key(), val.asText());
                }
            }
            row.put("_basis", tree.path("_basis").asText("LEGACY_REFERENCE"));
            row.put("_payment_status", tree.path("_payment_status").asText(""));
            var dateGroups = tree.path("day_groups");
            for (var group : schema.dynamicGroups()) {
                var dates = dateGroups.path(group.key());
                if (!dates.isObject()) continue;
                dates.fieldNames().forEachRemaining(day -> {
                    dayKeys.get(group.key()).add(day);
                    var money = dates.path(day);
                    for (var key : MEASURES) {
                        var number = money.path(key);
                        row.put(dayKey(group.key(), day, key),
                                number.isMissingNode() || number.isNull() ? null : number.asText());
                    }
                });
            }
            rows.add(row);
        }

        var cols = new ArrayList<ReportResult.Column>();
        for (var field : schema.columns()) {
            cols.add(new ReportResult.Column(field.key(), field.title(), field.type(),
                    field.group() == null ? List.of() : List.of(field.group())));
        }
        for (var group : schema.dynamicGroups()) {
            for (var day : dayKeys.get(group.key())) {
                for (int k = 0; k < MEASURES.length; k++) {
                    cols.add(new ReportResult.Column(
                            dayKey(group.key(), day, MEASURES[k]),
                            MEASURE_LABELS[k], "decimal",
                            List.of(group.title(), day)));
                }
            }
        }
        return new ReportResult(code, title, true,
                "BẢN THAM CHIẾU BSS CŨ — chưa nghiệm thu tài chính. "
              + "Chi phí hoa hồng tính theo chính sách 25%; các khoản chi trả thực tế "
              + "chưa có chứng từ sẽ để trống, không được dùng đối soát thanh toán.",
                cols, rows, page, pageSize, total);
    }

    private static String dayKey(String group, String day, String measure) {
        return "day_" + group + "_" + day + "_" + measure;
    }
}
