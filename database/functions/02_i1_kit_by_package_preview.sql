-- POTTEL REPORTING SYSTEM | I.1 | PREVIEW AT AGENCY x PACKAGE GRAIN
-- SOURCE DESIGN ONLY -- DO NOT RUN/DEPLOY THIS FILE UNTIL EXPLICITLY REVIEWED.
-- No modification to BSS source tables. If deployed after approval, only creates
-- a function in the dedicated reporting schema; no personal subscriber data.
-- Does NOT count business-eligible subscribers; only counts KIT activation rows.
-- Does NOT calculate price, incentive, VAT, missing-price or money columns.
-- Snapshot audit of October 2026: 4,136 KIT rows, 6 agencies, 17 agency-package pairs.
-- Avoid accidentally replacing verified 3-column preview function:
-- new function has its own unique name.

CREATE OR REPLACE FUNCTION reporting.fn_i1_kit_by_package_preview(
    p_month_start date,
    p_agency_id integer DEFAULT NULL
)
RETURNS TABLE (
    agency_id integer,
    agency_name text,
    package_code text,
    activated_kit_count bigint,
    agency_kit_total bigint
)
LANGUAGE sql
STABLE
SECURITY INVOKER
SET search_path = pg_catalog
AS $fn$
    WITH grouped_kits AS (
        SELECT k.agency_id,
               k.main_package_code::text AS package_code,
               COUNT(*)::bigint AS activated_kit_count
        FROM crm.linhnn_kits_prod AS k
        WHERE k.registed_date >=
              (p_month_start::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh')
          AND k.registed_date <
              ((p_month_start + INTERVAL '1 month')::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh')
          AND (p_agency_id IS NULL OR k.agency_id = p_agency_id)
        GROUP BY k.agency_id, k.main_package_code
    ),
    agency_reference AS (
        SELECT a.id, MIN(a.name::text) AS agency_name
        FROM esim_agency.agencies AS a
        GROUP BY a.id
    )
    SELECT g.agency_id,
           a.agency_name,
           g.package_code,
           g.activated_kit_count,
           SUM(g.activated_kit_count) OVER (PARTITION BY g.agency_id)::bigint
               AS agency_kit_total
    FROM grouped_kits AS g
    LEFT JOIN agency_reference AS a ON a.id = g.agency_id
    ORDER BY g.agency_id NULLS LAST, g.package_code NULLS LAST
$fn$;

-- Once explicitly approved and installed in a reporting DB, verification only:
-- SELECT * FROM reporting.fn_i1_kit_by_package_preview(DATE '2026-10-01');
-- SELECT count(*), sum(activated_kit_count)
-- FROM reporting.fn_i1_kit_by_package_preview(DATE '2026-10-01');
-- Expected in the archived local October 2026 snapshot: 17 rows / 4,136 KITs.
-- These expectations are not financial acceptance criteria.
