package vn.pottel.reporting.report;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportCatalog catalog;
    private final ReportService service;
    private final ReportExcelExporter excel;

    public ReportController(ReportCatalog catalog, ReportService service, ReportExcelExporter excel) {
        this.catalog = catalog;
        this.service = service;
        this.excel = excel;
    }

    @GetMapping
    public List<ReportDefinition> all() { return catalog.all(); }

    @GetMapping("/{code}")
    public ReportDefinition get(@PathVariable String code) {
        try { return catalog.get(code); }
        catch (NoSuchElementException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage()); }
    }

    @PostMapping("/{code}/run")
    public ReportResult run(@PathVariable String code, @RequestBody ReportQuery query) {
        return service.run(code, query);
    }

    @PostMapping("/{code}/excel")
    public ResponseEntity<byte[]> excel(@PathVariable String code, @RequestBody ReportQuery query) throws IOException {
        var data = service.exportData(code, query);
        var bytes = excel.toXlsx(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("pottel-" + code.replace('.', '-') + (data.preview() ? "-KIT-PREVIEW" : "-CHUA-NGHIEM-THU") + ".xlsx", StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(bytes.length).body(bytes);
    }
}
