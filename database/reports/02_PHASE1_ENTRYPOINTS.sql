-- Phase 1 fixed-entrypoint wrapper Stored Functions for the 11 remaining reports.
-- Exact function names from ReportFunctionRegistry. Calculation lives in
-- fn_phase1_reference_rows() / 00_PHASE1_REFERENCE_CORE.sql.
-- I.1 already has its distinct unaccepted finance entrypoint fn_i1_report;
-- for I.1 legacy reference, the backend uses fn_phase1_reference_rows('I.1').
-- No BSS data or source schema changes.
BEGIN;

CREATE FUNCTION reporting.fn_i2_report(p_month date,p_agency_id integer DEFAULT NULL)
RETURNS TABLE(row_data jsonb)
LANGUAGE sql STABLE SECURITY INVOKER SET search_path=pg_catalog
AS $fn$
 SELECT t.row_data FROM reporting.fn_phase1_reference_rows('I.2',p_month,p_agency_id) t
$fn$;
REVOKE ALL ON FUNCTION reporting.fn_i2_report(date,integer) FROM PUBLIC;


CREATE FUNCTION reporting.fn_i3_report(p_month date,p_agency_id integer DEFAULT NULL)
RETURNS TABLE(row_data jsonb)
LANGUAGE sql STABLE SECURITY INVOKER SET search_path=pg_catalog
AS $fn$
 SELECT t.row_data FROM reporting.fn_phase1_reference_rows('I.3',p_month,p_agency_id) t
$fn$;
REVOKE ALL ON FUNCTION reporting.fn_i3_report(date,integer) FROM PUBLIC;


CREATE FUNCTION reporting.fn_i4_report(p_month date,p_agency_id integer DEFAULT NULL)
RETURNS TABLE(row_data jsonb)
LANGUAGE sql STABLE SECURITY INVOKER SET search_path=pg_catalog
AS $fn$
 SELECT t.row_data FROM reporting.fn_phase1_reference_rows('I.4',p_month,p_agency_id) t
$fn$;
REVOKE ALL ON FUNCTION reporting.fn_i4_report(date,integer) FROM PUBLIC;


CREATE FUNCTION reporting.fn_i5_report(p_month date,p_agency_id integer DEFAULT NULL)
RETURNS TABLE(row_data jsonb)
LANGUAGE sql STABLE SECURITY INVOKER SET search_path=pg_catalog
AS $fn$
 SELECT t.row_data FROM reporting.fn_phase1_reference_rows('I.5',p_month,p_agency_id) t
$fn$;
REVOKE ALL ON FUNCTION reporting.fn_i5_report(date,integer) FROM PUBLIC;


CREATE FUNCTION reporting.fn_i6_report(p_month date,p_agency_id integer DEFAULT NULL)
RETURNS TABLE(row_data jsonb)
LANGUAGE sql STABLE SECURITY INVOKER SET search_path=pg_catalog
AS $fn$
 SELECT t.row_data FROM reporting.fn_phase1_reference_rows('I.6',p_month,p_agency_id) t
$fn$;
REVOKE ALL ON FUNCTION reporting.fn_i6_report(date,integer) FROM PUBLIC;


CREATE FUNCTION reporting.fn_i7_report(p_month date,p_agency_id integer DEFAULT NULL)
RETURNS TABLE(row_data jsonb)
LANGUAGE sql STABLE SECURITY INVOKER SET search_path=pg_catalog
AS $fn$
 SELECT t.row_data FROM reporting.fn_phase1_reference_rows('I.7',p_month,p_agency_id) t
$fn$;
REVOKE ALL ON FUNCTION reporting.fn_i7_report(date,integer) FROM PUBLIC;


CREATE FUNCTION reporting.fn_ii1_report(p_month date,p_agency_id integer DEFAULT NULL)
RETURNS TABLE(row_data jsonb)
LANGUAGE sql STABLE SECURITY INVOKER SET search_path=pg_catalog
AS $fn$
 SELECT t.row_data FROM reporting.fn_phase1_reference_rows('II.1',p_month,p_agency_id) t
$fn$;
REVOKE ALL ON FUNCTION reporting.fn_ii1_report(date,integer) FROM PUBLIC;


CREATE FUNCTION reporting.fn_ii2_report(p_month date,p_agency_id integer DEFAULT NULL)
RETURNS TABLE(row_data jsonb)
LANGUAGE sql STABLE SECURITY INVOKER SET search_path=pg_catalog
AS $fn$
 SELECT t.row_data FROM reporting.fn_phase1_reference_rows('II.2',p_month,p_agency_id) t
$fn$;
REVOKE ALL ON FUNCTION reporting.fn_ii2_report(date,integer) FROM PUBLIC;


CREATE FUNCTION reporting.fn_ii3_report(p_month date,p_agency_id integer DEFAULT NULL)
RETURNS TABLE(row_data jsonb)
LANGUAGE sql STABLE SECURITY INVOKER SET search_path=pg_catalog
AS $fn$
 SELECT t.row_data FROM reporting.fn_phase1_reference_rows('II.3',p_month,p_agency_id) t
$fn$;
REVOKE ALL ON FUNCTION reporting.fn_ii3_report(date,integer) FROM PUBLIC;


CREATE FUNCTION reporting.fn_ii4_report(p_month date,p_agency_id integer DEFAULT NULL)
RETURNS TABLE(row_data jsonb)
LANGUAGE sql STABLE SECURITY INVOKER SET search_path=pg_catalog
AS $fn$
 SELECT t.row_data FROM reporting.fn_phase1_reference_rows('II.4',p_month,p_agency_id) t
$fn$;
REVOKE ALL ON FUNCTION reporting.fn_ii4_report(date,integer) FROM PUBLIC;


CREATE FUNCTION reporting.fn_ii5_report(p_month date,p_agency_id integer DEFAULT NULL)
RETURNS TABLE(row_data jsonb)
LANGUAGE sql STABLE SECURITY INVOKER SET search_path=pg_catalog
AS $fn$
 SELECT t.row_data FROM reporting.fn_phase1_reference_rows('II.5',p_month,p_agency_id) t
$fn$;
REVOKE ALL ON FUNCTION reporting.fn_ii5_report(date,integer) FROM PUBLIC;

COMMIT;
