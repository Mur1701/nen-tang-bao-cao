package vn.pottel.reporting.report;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Tests the official I.1 route; no database, historical preview or production credentials. */
class ReportServiceOfficialI1Test {
    private static final LocalDate MONTH = LocalDate.of(2026, 10, 1);
    private final ReportRepository repo = mock(ReportRepository.class);
    private final ReportCatalog catalog = new ReportCatalog();

    private static ReportQuery query() {
        return new ReportQuery("2026-10", null, 1, 50, null);
    }

    @Test
    void disabledOfficialReportFailsClosedWithoutDatabaseAccess() {
        var service = new ReportService(catalog, repo, false);
        var error = assertThrows(ResponseStatusException.class,
                () -> service.run("I.1", query()));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        verifyNoInteractions(repo);
    }

    @Test
    void otherReportCodesRemainUnimplemented() {
        var service = new ReportService(catalog, repo, true);
        var error = assertThrows(ResponseStatusException.class,
                () -> service.run("I.2", query()));
        assertEquals(HttpStatus.NOT_IMPLEMENTED, error.getStatusCode());
        verifyNoInteractions(repo);
    }

    @Test
    void oldKitPreviewModeIsRejected() {
        var service = new ReportService(catalog, repo, true);
        var oldQuery = new ReportQuery("2026-10", null, 1, 50, "AGENCY_KITS");
        var error = assertThrows(ResponseStatusException.class,
                () -> service.run("I.1", oldQuery));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(repo);
    }

    @Test
    void resultUsesElevenOfficialColumnsAndNeverLabelsItPreview() {
        when(repo.countI1(MONTH, null)).thenReturn(1L);
        when(repo.i1(MONTH, null, 50, 0L)).thenReturn(List.of(Map.of(
                "agency_id", 29,
                "agency_name", "Đại lý mẫu",
                "package_code", "DL5",
                "package_active_count", 1001L
        )));
        var result = new ReportService(catalog, repo, true).run("I.1", query());
        assertFalse(result.preview());
        assertEquals(11, result.columns().size());
        assertEquals(1L, result.totalRows());
        assertEquals("DL5", result.rows().getFirst().get("package_code"));
        verify(repo).countI1(MONTH, null);
        verify(repo).i1(MONTH, null, 50, 0L);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void excelUsesAllRowsAndIgnoresCurrentPage() {
        when(repo.countI1(MONTH, null)).thenReturn(35L);
        when(repo.i1(MONTH, null, 35, 0L)).thenReturn(List.of());
        var result = new ReportService(catalog, repo, true).exportData(
                "I.1", new ReportQuery("2026-10", null, 2, 20, null));
        assertEquals(35L, result.totalRows());
        verify(repo).i1(MONTH, null, 35, 0L);
    }

    @Test
    void oversizedExportRejectsRatherThanTruncatingSilently() {
        when(repo.countI1(MONTH, null)).thenReturn(5001L);
        var error = assertThrows(ResponseStatusException.class,
                () -> new ReportService(catalog, repo, true).exportData("I.1", query()));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        verify(repo, never()).i1(any(), any(), anyInt(), anyLong());
    }
}
