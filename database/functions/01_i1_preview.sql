-- DESIGN ARTIFACT ONLY. DO NOT EXECUTE until explicitly approved.
-- Creates objects only in the NEW reporting schema; does not modify source BSS tables.
-- This function returns activated KIT counts, NOT distinct subscribers or incentive amounts.
-- Requires the database administrator to create/authorize schema and appropriate privileges.
-- Use a least-privileged execution role; avoid deploying under BSS superuser.

CREATE SCHEMA IF NOT EXISTS reporting;

CREATE OR REPLACE FUNCTION reporting.fn_i1_activated_kits(
    p_month_start date,
    p_agency_id integer DEFAULT NULL
)
RETURNS TABLE (
    agency_id integer,
    agency_name text,
    activated_kit_count bigint
)
LANGUAGE sql
STABLE
SECURITY INVOKER
SET search_path = pg_catalog
AS $fn$
    SELECT a.id,
           a.name::text,
           count(k.id)::bigint
    FROM crm.linhnn_kits_prod AS k
    JOIN esim_agency.agencies AS a ON a.id = k.agency_id
    WHERE k.registed_date >= (p_month_start::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh')
      AND k.registed_date < ((p_month_start + interval '1 month')::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh')
      AND (p_agency_id IS NULL OR a.id = p_agency_id)
    GROUP BY a.id, a.name
$fn$;

-- Read-only verification AFTER authorization and installation:
-- SELECT * FROM reporting.fn_i1_activated_kits(DATE '2026-10-01', NULL);
-- SELECT sum(activated_kit_count) FROM reporting.fn_i1_activated_kits(DATE '2026-10-01', NULL);
