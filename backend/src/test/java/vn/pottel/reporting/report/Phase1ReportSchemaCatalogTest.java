package vn.pottel.reporting.report;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class Phase1ReportSchemaCatalogTest {
    private final Phase1ReportSchemaCatalog schemas = new Phase1ReportSchemaCatalog();

    @Test
    void allTwelveCodesMatchFunctionAllowlistExactly() {
        var codes = schemas.all().stream().map(Phase1ReportSchemaCatalog.Schema::code).toList();
        assertEquals(12, codes.size());
        assertEquals(Set.copyOf(ReportFunctionRegistry.all().stream()
                        .map(ReportFunctionRegistry.Target::code).toList()), Set.copyOf(codes));
        assertEquals(codes.size(), Set.copyOf(codes).size());
        assertEquals("I.1", codes.getFirst());
        assertEquals("II.5", codes.getLast());
    }

    @Test
    void baselineReportsAreExecutableButNeverMarkedFinanciallyAccepted() {
        for (var schema : schemas.all()) {
            assertTrue(schema.executable(), schema.code());
            assertEquals("LEGACY_REFERENCE", schema.status(), schema.code());
            assertFalse(schema.blocker().isBlank(), schema.code());
            assertTrue(schema.source().contains("SRS_"), schema.code());
        }
    }

    @Test
    void onlyI1AndI2RequireAgencyFilter() {
        for (var schema : schemas.all()) {
            assertTrue(schema.filters().contains("month"), schema.code());
            assertEquals(schema.code().equals("I.1") || schema.code().equals("I.2"),
                    schema.filters().contains("agencyId"), schema.code());
        }
    }

    @Test
    void columnsAreUniqueWithIdentifiableTypes() {
        for (var schema : schemas.all()) {
            var cols = schema.columns();
            assertFalse(cols.isEmpty(), schema.code());
            assertEquals(cols.size(), cols.stream()
                    .map(Phase1ReportSchemaCatalog.Column::key).collect(Collectors.toSet()).size(), schema.code());
            for (var column : cols) {
                assertFalse(column.title().isBlank(), schema.code());
                assertTrue(Set.of("integer", "text", "decimal", "datetime").contains(column.type()),
                        schema.code() + ": " + column.key());
            }
        }
    }

    @Test
    void allocationReportsRetainSeparateDynamicDayGroups() {
        for (var code : List.of("I.3", "II.1"))
            assertEquals(2, schemas.require(code).dynamicGroups().size(), code);
        for (var code : List.of("I.5", "I.6", "II.3", "II.4"))
            assertEquals(1, schemas.require(code).dynamicGroups().size(), code);
        for (var code : List.of("I.2", "I.4", "I.7", "II.2", "II.5"))
            assertEquals(0, schemas.require(code).dynamicGroups().size(), code);
        for (var schema : schemas.all()) {
            for (var group : schema.dynamicGroups()) {
                assertEquals(3, group.measures().size(), schema.code());
                assertTrue(Set.of("CURRENT_MONTH", "NEXT_MONTH").contains(group.period()), schema.code());
            }
        }
    }

    @Test
    void stockSummariesAreNotMisrepresentedAsSubscriberDetails() {
        var costs = schemas.require("I.7");
        var revenue = schemas.require("II.5");
        assertEquals(13, costs.columns().size());
        assertEquals(20, revenue.columns().size());
        assertFalse(costs.columns().stream().anyMatch(c -> c.key().equals("msisdn")));
        assertFalse(revenue.columns().stream().anyMatch(c -> c.key().equals("msisdn")));
        assertEquals("Đại lý", revenue.columns().get(1).title());
    }

    @Test
    void unknownOrInjectedCodesDoNotResolve() {
        for (var code : List.of("III.1", "I.1;delete", "reporting.fn_ii2_report", ""))
            assertThrows(java.util.NoSuchElementException.class,
                    () -> schemas.require(code));
    }

    @Test
    void caseInsensitiveLookupKeepsCanonicalCode() {
        assertEquals("II.5", schemas.require("ii.5").code());
        assertEquals("I.2", schemas.require("i.2").code());
    }
}
