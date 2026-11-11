package vn.pottel.reporting.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.pottel.reporting.auth.BssAdminPrincipal;
import vn.pottel.reporting.auth.BssAdminUserDetailsService;
import vn.pottel.reporting.report.Phase1ReportSchemaCatalog;
import vn.pottel.reporting.report.ReportCatalog;
import vn.pottel.reporting.report.ReportController;
import vn.pottel.reporting.report.ReportExcelExporter;
import vn.pottel.reporting.report.ReportResult;
import vn.pottel.reporting.report.ReportService;
import vn.pottel.reporting.report.RevenueTrendController;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Security filter integration tests on mock users and controllers; no BSS or network. */
@WebMvcTest(controllers = {ReportController.class, RevenueTrendController.class})
@Import(SecurityConfig.class)
class ReportEndpointSecurityTest {
    @Autowired MockMvc mvc;

    @MockitoBean BssAdminUserDetailsService bssUsers;
    @MockitoBean ReportCatalog catalog;
    @MockitoBean Phase1ReportSchemaCatalog schemas;
    @MockitoBean ReportService service;
    @MockitoBean ReportExcelExporter excel;
    @MockitoBean NamedParameterJdbcTemplate jdbc;
    @MockitoBean ReportQueryTimeout timeout;

    private static final String QUERY = """
            {"month":"2026-10","page":1,"pageSize":50,"previewMode":"LEGACY_REFERENCE"}
            """;

    private static Authentication admin() {
        return new UsernamePasswordAuthenticationToken(
                new BssAdminPrincipal("test-admin", "fake-hash-not-secret", 1), null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private static Authentication nonAdmin() {
        return new UsernamePasswordAuthenticationToken(
                new BssAdminPrincipal("test-viewer", "fake-hash-not-secret", 1), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test void anonymousCannotRunOrExportOrReadRevenueTrend() throws Exception {
        for (var path : List.of("/api/reports/I.1/run", "/api/reports/I.1/excel",
                "/api/reports/II.5/trend")) {
            mvc.perform(post(path).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content(QUERY)).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(service, jdbc);
    }

    @Test void missingOrInvalidCsrfIsForbiddenEvenForAdmin() throws Exception {
        mvc.perform(post("/api/reports/I.1/run")
                .with(authentication(admin())).contentType(MediaType.APPLICATION_JSON)
                .content(QUERY)).andExpect(status().isForbidden());
        mvc.perform(post("/api/reports/I.1/run")
                .with(authentication(admin())).with(csrf().useInvalidToken())
                .contentType(MediaType.APPLICATION_JSON).content(QUERY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test void bssAdminWithValidCsrfCanReachReportService() throws Exception {
        when(bssUsers.stillActiveAdmin(any(BssAdminPrincipal.class))).thenReturn(true);
        when(service.run(eq("I.1"), any())).thenReturn(new ReportResult(
                "I.1", "Test", true, "Synthetic", List.of(), List.of(), 1, 50, 0));
        mvc.perform(post("/api/reports/I.1/run")
                .with(authentication(admin())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(QUERY))
                .andExpect(status().isOk());
        verify(service).run(eq("I.1"), any());
    }

    @Test void revokedBssAdminIsDeniedBeforeReportExecution() throws Exception {
        when(bssUsers.stillActiveAdmin(any(BssAdminPrincipal.class))).thenReturn(false);
        mvc.perform(post("/api/reports/II.3/run")
                .with(authentication(admin())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(QUERY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void nonAdminRoleCannotAccessReports() throws Exception {
        when(bssUsers.stillActiveAdmin(any(BssAdminPrincipal.class))).thenReturn(true);
        mvc.perform(post("/api/reports/I.1/run")
                .with(authentication(nonAdmin())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(QUERY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
