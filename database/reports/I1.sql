-- I.1 - Khuyen khich theo san luong (Phase 1).
-- Contract is sourced from SRS_I1 and LLD_REPORT_I1 (04/11/2026).
-- IMPORTANT: This is the real report entry point, NOT the historical KIT preview.
-- NOT READY FOR PRODUCTION: the approved event/price source adapter is not defined.
--
-- Required read-only relation (view) to be supplied after BSS source validation:
-- reporting.v_i1_eligible_activations(
--     event_key text,                  -- UNIQUE eligible activation identity
--     agency_id integer,
--     agency_name text,
--     package_code text,
--     activation_time timestamptz,
--     approved_incentive_unit_price numeric -- approved PRE-VAT incentive price
-- )
-- ONE row = ONE eligible, deduplicated activation event. This is NOT simply
-- crm.linhnn_kits_prod; the old KIT preview does not establish eligibility.
--
-- Thresholds 0%, 5%, 10% and VAT 8% are DOCUMENTED in SRS-I1.
-- No rounding is performed: rounding rule is not approved.
-- The >10,000 fixed-amount scheme is unresolved; fail closed for those cases.
-- The report rejects an absent adapter, duplicate event keys and inconsistent
-- per-package prices instead of silently creating misleading financial amounts.
-- Explicit DDL review/approval is required BEFORE applying to local PostgreSQL.
-- Never run against production/Neon by default; no BSS tables are modified.

BEGIN;
SET LOCAL statement_timeout = '30s';

CREATE SCHEMA IF NOT EXISTS reporting;

-- Deliberately use CREATE (not CREATE OR REPLACE) to avoid overwriting an
-- existing financial function without review.
CREATE FUNCTION reporting.fn_i1_report(
    p_month_start date,
    p_agency_id integer DEFAULT NULL
)
RETURNS TABLE (
    stt bigint,
    agency_id integer,
    agency_name text,
    package_code text,
    package_price numeric,
    package_active_count bigint,
    agency_total_active bigint,
    achieved_tier text,
    incentive_total numeric,
    incentive_before_vat numeric,
    vat_amount numeric,
    missing_price bigint
)
LANGUAGE plpgsql
STABLE
SECURITY INVOKER
SET search_path = pg_catalog
AS $fn$
DECLARE
    v_start timestamptz;
    v_end timestamptz;
BEGIN
    IF p_month_start IS NULL OR EXTRACT(DAY FROM p_month_start) <> 1 THEN
        RAISE EXCEPTION 'I1 month_start must be the first day of a month' USING ERRCODE = '22023';
    END IF;
    IF p_agency_id IS NOT NULL AND p_agency_id <= 0 THEN
        RAISE EXCEPTION 'I1 agency_id must be positive' USING ERRCODE = '22023';
    END IF;

    IF to_regclass('reporting.v_i1_eligible_activations') IS NULL THEN
        RAISE EXCEPTION 'I1 verified eligibility and pricing source is not installed'
            USING ERRCODE = 'P0001';
    END IF;

    v_start := p_month_start::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh';
    v_end := (p_month_start + INTERVAL '1 month')::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh';

    IF EXISTS (
        SELECT 1
        FROM reporting.v_i1_eligible_activations AS e
        WHERE e.activation_time >= v_start AND e.activation_time < v_end
          AND (e.event_key IS NULL OR e.agency_id IS NULL
               OR e.agency_name IS NULL OR e.package_code IS NULL)
    ) THEN
        RAISE EXCEPTION 'I1 approved input has missing identity / agency / package'
            USING ERRCODE = 'P0001';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM reporting.v_i1_eligible_activations AS e
        WHERE e.activation_time >= v_start AND e.activation_time < v_end
        GROUP BY e.event_key
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'I1 source contains duplicate eligible activation events'
            USING ERRCODE = 'P0001';
    END IF;

    -- SRS documents potential fixed allowances >10k but does NOT define
    -- add/replace/stack rules. Refuse money calculation for these agencies.
    IF EXISTS (
        SELECT 1
        FROM reporting.v_i1_eligible_activations AS e
        WHERE e.activation_time >= v_start AND e.activation_time < v_end
          AND (p_agency_id IS NULL OR e.agency_id = p_agency_id)
        GROUP BY e.agency_id
        HAVING COUNT(*) > 10000
    ) THEN
        RAISE EXCEPTION 'I1 fixed-allowance policy above 10000 is not specified'
            USING ERRCODE = 'P0001';
    END IF;

    RETURN QUERY
    WITH eligible AS (
        SELECT e.agency_id, e.agency_name, e.package_code,
               e.approved_incentive_unit_price AS unit_price
        FROM reporting.v_i1_eligible_activations AS e
        WHERE e.activation_time >= v_start AND e.activation_time < v_end
    ),
    agency_totals AS (
        SELECT e.agency_id, COUNT(*)::bigint AS total_active
        FROM eligible AS e
        GROUP BY e.agency_id
    ),
    package_totals AS (
        SELECT e.agency_id, MIN(e.agency_name) AS agency_name, e.package_code,
               COUNT(*)::bigint AS package_active,
               COUNT(*) FILTER (WHERE e.unit_price IS NULL OR e.unit_price <= 0)::bigint AS missing_price_count,
               COUNT(DISTINCT e.unit_price)::integer AS different_prices,
               MAX(e.unit_price) AS price,
               SUM(e.unit_price) AS price_sum
        FROM eligible AS e
        GROUP BY e.agency_id, e.package_code
    ),
    rated AS (
        SELECT p.*, t.total_active,
               CASE WHEN t.total_active <= 1000 THEN 0::numeric
                    WHEN t.total_active <= 2000 THEN 0.05::numeric
                    ELSE 0.10::numeric END AS rate
        FROM package_totals AS p
        JOIN agency_totals AS t ON t.agency_id = p.agency_id
    ),
    priced AS (
        SELECT r.*,
               CASE WHEN r.missing_price_count = 0 AND r.different_prices = 1
                    THEN r.price ELSE NULL::numeric END AS display_price,
               CASE WHEN r.missing_price_count = 0 AND r.different_prices = 1
                    THEN r.price_sum * r.rate ELSE NULL::numeric END AS net_amount
        FROM rated AS r
    )
    SELECT ROW_NUMBER() OVER (ORDER BY p.agency_id, p.package_code)::bigint AS stt,
           p.agency_id,
           p.agency_name,
           p.package_code,
           p.display_price AS package_price,
           p.package_active AS package_active_count,
           p.total_active AS agency_total_active,
           (p.rate * 100)::text || '%' AS achieved_tier,
           CASE WHEN p.net_amount IS NULL THEN NULL::numeric
                ELSE p.net_amount * 1.08::numeric END AS incentive_total,
           p.net_amount AS incentive_before_vat,
           CASE WHEN p.net_amount IS NULL THEN NULL::numeric
                ELSE p.net_amount * 0.08::numeric END AS vat_amount,
           p.missing_price_count AS missing_price
    FROM priced AS p
    WHERE p_agency_id IS NULL OR p.agency_id = p_agency_id
    ORDER BY p.agency_id, p.package_code;
END
$fn$;

REVOKE ALL ON FUNCTION reporting.fn_i1_report(date, integer) FROM PUBLIC;
-- DBA must explicitly GRANT EXECUTE to the dedicated read-only reporting role.
COMMIT;
