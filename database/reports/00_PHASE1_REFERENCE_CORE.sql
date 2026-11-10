-- Phase 1 reference execution core (10/11/2026)
-- PRECEDENCE: SRS Phase 1 > confirmed decisions > old BSS module-bao-cao/sql/nen.
-- Baseline provenance:
--   Dev.rar/nen/kich-hoat.sql, phan-bo.sql, chu-ky.sql, danh-muc.json.
--   Verified 09/11 local: crm.linhnn_kits_prod JOIN esim_agency.agencies
--   and inventory.products for KIT/agency/product. GOI_MOI is LEGACY-ASSUMED.
--   Revenue VAT=10%; agency commission VAT=8%; commission=25% of package;
--   closing adjustment on last allocation day, rounding to 2 decimals.
-- This baseline is NOT payment settlement evidence or financial sign-off.
-- REPORTING SCHEMA DDL ONLY; NO BSS DML. Apply explicitly on LOCAL DB.
BEGIN;
SET LOCAL statement_timeout = '120s';
CREATE SCHEMA IF NOT EXISTS reporting;

CREATE FUNCTION reporting.fn_phase1_reference_source(p_month date)
RETURNS TABLE (
    kit_id bigint,
    agency_id integer,
    agency_name text,
    msisdn text,
    package_code text,
    subscriber_active_at timestamp,
    subscriber_active_date date,
    package_active_date date,
    cycle_days integer,
    package_price numeric,
    revenue_gross numeric,
    revenue_net numeric,
    revenue_vat numeric,
    commission_gross numeric,
    commission_net numeric,
    commission_vat numeric,
    payment_scheduled_first numeric,
    payment_scheduled_second numeric,
    allocation_end date
)
LANGUAGE sql STABLE SECURITY INVOKER SET search_path = pg_catalog
SET TimeZone = 'Asia/Ho_Chi_Minh'
AS $fn$
WITH first_package_event AS (
    SELECT e.msisdn::text AS msisdn,
           min(to_date(e.insert_time, 'DD-MON-YY')) AS active_date
    FROM cdr.vnpt_package_new e
    GROUP BY e.msisdn
),
source AS (
    SELECT k.id::bigint AS kit_id,
           a.id::integer AS agency_id,
           a.name::text AS agency_name,
           k.msisdn_snapshot::text AS msisdn,
           k.main_package_code::text AS package_code,
           k.registed_date::timestamp AS subscriber_active_at,
           k.registed_date::date AS subscriber_active_date,
           g.active_date AS package_active_date,
           CASE k.main_package_code
               WHEN 'M2M5' THEN 5 WHEN 'DL5' THEN 5
               WHEN 'M2M7' THEN 7 WHEN 'DL7' THEN 7
               WHEN 'M2M10' THEN 10 WHEN 'DL10' THEN 10
               WHEN 'M2M15' THEN 15 WHEN 'DL15' THEN 15
               WHEN 'DL30' THEN 30
               ELSE NULL::integer END AS cycle_days,
           p.base_price::numeric AS package_price
    FROM crm.linhnn_kits_prod k
    JOIN esim_agency.agencies a ON a.id = k.agency_id
    LEFT JOIN inventory.products p ON p.code = k.main_package_code
    LEFT JOIN first_package_event g ON g.msisdn = k.msisdn_snapshot
    WHERE k.registed_date IS NOT NULL
      AND k.registed_date::date < (p_month + interval '1 month')::date
), totals AS (
    SELECT s.*,
           s.package_price AS rev_gross,
           round(s.package_price / 1.10::numeric, 2) AS rev_net,
           round(s.package_price * 0.25::numeric, 2) AS com_gross
    FROM source s
), money AS (
    SELECT t.*,
           t.rev_gross - t.rev_net AS rev_vat,
           round(t.com_gross / 1.08::numeric, 2) AS com_net
    FROM totals t
)
SELECT m.kit_id, m.agency_id, m.agency_name, m.msisdn, m.package_code,
       m.subscriber_active_at, m.subscriber_active_date, m.package_active_date,
       m.cycle_days, m.package_price, m.rev_gross, m.rev_net, m.rev_vat,
       m.com_gross, m.com_net, m.com_gross - m.com_net,
       round(m.com_gross * 0.90::numeric, 2) AS payment_scheduled_first,
       m.com_gross - round(m.com_gross * 0.90::numeric, 2) AS payment_scheduled_second,
       m.package_active_date + (m.cycle_days - 1) AS allocation_end
FROM money m
$fn$;

-- Source yields the full allocation cycle; callers explicitly filter month.
-- Last day gets rounding residual (legacy "dong_so" mode), avoiding drift.
CREATE FUNCTION reporting.fn_phase1_reference_days(p_month date)
RETURNS TABLE (
    kit_id bigint, allocation_date date,
    revenue_net numeric, revenue_vat numeric,
    cost_net numeric, cost_vat numeric
)
LANGUAGE sql STABLE SECURITY INVOKER SET search_path = pg_catalog
SET TimeZone = 'Asia/Ho_Chi_Minh'
AS $fn$
SELECT s.kit_id, day.day_date::date,
       CASE WHEN day.day_date::date = s.allocation_end
            THEN s.revenue_net - (s.cycle_days - 1) * part.rev_net
            ELSE part.rev_net END,
       CASE WHEN day.day_date::date = s.allocation_end
            THEN (s.revenue_gross - (s.cycle_days - 1) * part.rev_gross)
               - (s.revenue_net - (s.cycle_days - 1) * part.rev_net)
            ELSE part.rev_gross - part.rev_net END,
       CASE WHEN day.day_date::date = s.allocation_end
            THEN s.commission_net - (s.cycle_days - 1) * part.cost_net
            ELSE part.cost_net END,
       CASE WHEN day.day_date::date = s.allocation_end
            THEN (s.commission_gross - (s.cycle_days - 1) * part.cost_gross)
               - (s.commission_net - (s.cycle_days - 1) * part.cost_net)
            ELSE part.cost_gross - part.cost_net END
FROM reporting.fn_phase1_reference_source(p_month) s
CROSS JOIN LATERAL (
    SELECT round(s.revenue_gross / s.cycle_days, 2) AS rev_gross,
           round(round(s.revenue_gross / s.cycle_days, 2) / 1.10::numeric, 2) AS rev_net,
           round(s.commission_gross / s.cycle_days, 2) AS cost_gross,
           round(s.commission_net / s.cycle_days, 2) AS cost_net
) part
CROSS JOIN LATERAL generate_series(
    s.package_active_date, s.allocation_end, interval '1 day'
) AS day(day_date)
WHERE s.package_active_date IS NOT NULL
  AND s.cycle_days > 0 AND s.package_price IS NOT NULL
  AND s.package_active_date < (p_month + interval '1 month')::date
  AND s.allocation_end >= p_month;

$fn$;

REVOKE ALL ON FUNCTION reporting.fn_phase1_reference_source(date) FROM PUBLIC;
REVOKE ALL ON FUNCTION reporting.fn_phase1_reference_days(date) FROM PUBLIC;
-- Explicitly GRANT EXECUTE to reporting application's DB role after review.
COMMIT;
