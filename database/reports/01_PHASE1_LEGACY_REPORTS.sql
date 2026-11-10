-- Phase 1 / legacy BSS fallback for all 12 reports.
-- Source priority: SRS Phase 1 then old BSS SQL and danh-muc.json.
-- Use after 00_PHASE1_REFERENCE_CORE.sql. Creates only reporting function.
-- Cost "paid" fields are NULL when settlement file is not loaded, and
-- commission is explicitly POLICY ESTIMATE; not proof of payment.
-- Dynamic day groups are JSON for the Spring report engine to flatten.
BEGIN;
SET LOCAL statement_timeout='120s';
CREATE FUNCTION reporting.fn_phase1_reference_rows(
 p_code text,p_month date,p_agency_id integer DEFAULT NULL)
RETURNS TABLE(row_data jsonb)
LANGUAGE plpgsql STABLE SECURITY INVOKER
SET search_path=pg_catalog
SET TimeZone='Asia/Ho_Chi_Minh'
AS $fn$
BEGIN
 IF p_month IS NULL OR extract(day FROM p_month)<>1 THEN
   RAISE EXCEPTION 'month must be first day' USING ERRCODE='22023';
 END IF;
 IF p_code IS NULL OR p_code NOT IN ('I.1','I.2','I.3','I.4','I.5','I.6','I.7','II.1','II.2','II.3','II.4','II.5') THEN
   RAISE EXCEPTION 'unknown report code' USING ERRCODE='22023';
 END IF;
 IF p_code='I.1' THEN RETURN QUERY WITH s AS MATERIALIZED (
 SELECT * FROM reporting.fn_phase1_reference_source(p_month) ss
 WHERE p_agency_id IS NULL OR ss.agency_id=p_agency_id
), d AS MATERIALIZED (SELECT * FROM reporting.fn_phase1_reference_days(p_month)),
monthly AS (
 SELECT agency_id,COUNT(*) AS cnt FROM reporting.fn_phase1_reference_source(p_month) src
 WHERE src.subscriber_active_date >= p_month AND src.subscriber_active_date < (p_month + INTERVAL '1 month')::date
 GROUP BY agency_id
)
 SELECT jsonb_build_object('stt',row_number() OVER (ORDER BY s.agency_id,s.package_code), 'agency_id',s.agency_id, 'agency_name',s.agency_name, 'package_code',s.package_code, 'package_price',max(s.package_price), 'package_active_count',count(*), 'agency_total_active',max(m.cnt), 'achieved_tier',((CASE WHEN m.cnt<=1000 THEN 0::numeric WHEN m.cnt<=2000 THEN .05::numeric ELSE .1::numeric END)*100)::text||'%', 'incentive_before_vat',round(sum(s.package_price*(CASE WHEN m.cnt<=1000 THEN 0::numeric WHEN m.cnt<=2000 THEN .05::numeric ELSE .1::numeric END)),2), 'vat_amount',round(round(sum(s.package_price*(CASE WHEN m.cnt<=1000 THEN 0::numeric WHEN m.cnt<=2000 THEN .05::numeric ELSE .1::numeric END)),2)*.08::numeric,2), 'incentive_total',round(sum(s.package_price*(CASE WHEN m.cnt<=1000 THEN 0::numeric WHEN m.cnt<=2000 THEN .05::numeric ELSE .1::numeric END)),2)+round(round(sum(s.package_price*(CASE WHEN m.cnt<=1000 THEN 0::numeric WHEN m.cnt<=2000 THEN .05::numeric ELSE .1::numeric END)),2)*.08::numeric,2), 'missing_price',count(*) FILTER (WHERE s.package_price IS NULL), '_basis','LEGACY_REFERENCE')
 FROM s JOIN monthly m ON m.agency_id=s.agency_id
 WHERE s.subscriber_active_date>=p_month AND s.subscriber_active_date<(p_month + INTERVAL '1 month')::date
 GROUP BY s.agency_id,s.agency_name,s.package_code,m.cnt
 ORDER BY s.agency_id,s.package_code;
 RETURN; END IF;

IF p_code='I.2' THEN RETURN QUERY WITH s AS MATERIALIZED (
 SELECT * FROM reporting.fn_phase1_reference_source(p_month) ss
 WHERE p_agency_id IS NULL OR ss.agency_id=p_agency_id
), d AS MATERIALIZED (SELECT * FROM reporting.fn_phase1_reference_days(p_month)),
monthly AS (
 SELECT agency_id,COUNT(*) AS cnt FROM reporting.fn_phase1_reference_source(p_month) src
 WHERE src.subscriber_active_date >= p_month AND src.subscriber_active_date < (p_month + INTERVAL '1 month')::date
 GROUP BY agency_id
)
 SELECT jsonb_build_object('stt',row_number() OVER (ORDER BY s.agency_id,s.msisdn,s.kit_id), 'agency_name',s.agency_name, 'msisdn',s.msisdn, 'subscriber_active_at',s.subscriber_active_at, 'package_code',s.package_code, 'package_price',s.package_price, 'tier',((CASE WHEN m.cnt<=1000 THEN 0::numeric WHEN m.cnt<=2000 THEN .05::numeric ELSE .1::numeric END)*100)::text||'%', 'incentive_net',round(s.package_price*(CASE WHEN m.cnt<=1000 THEN 0::numeric WHEN m.cnt<=2000 THEN .05::numeric ELSE .1::numeric END),2), 'vat_amount',round(round(s.package_price*(CASE WHEN m.cnt<=1000 THEN 0::numeric WHEN m.cnt<=2000 THEN .05::numeric ELSE .1::numeric END),2)*.08::numeric,2), 'incentive_total',round(s.package_price*(CASE WHEN m.cnt<=1000 THEN 0::numeric WHEN m.cnt<=2000 THEN .05::numeric ELSE .1::numeric END),2)+round(round(s.package_price*(CASE WHEN m.cnt<=1000 THEN 0::numeric WHEN m.cnt<=2000 THEN .05::numeric ELSE .1::numeric END),2)*.08::numeric,2), '_basis','LEGACY_REFERENCE')
 FROM s JOIN monthly m ON m.agency_id=s.agency_id
 WHERE s.subscriber_active_date>=p_month AND s.subscriber_active_date<(p_month + INTERVAL '1 month')::date
 ORDER BY s.agency_id,s.subscriber_active_at,s.msisdn;
 RETURN; END IF;

IF p_code='I.3' THEN RETURN QUERY WITH s AS MATERIALIZED (
 SELECT * FROM reporting.fn_phase1_reference_source(p_month) ss
 WHERE p_agency_id IS NULL OR ss.agency_id=p_agency_id
), d AS MATERIALIZED (SELECT * FROM reporting.fn_phase1_reference_days(p_month)),
monthly AS (
 SELECT agency_id,COUNT(*) AS cnt FROM reporting.fn_phase1_reference_source(p_month) src
 WHERE src.subscriber_active_date >= p_month AND src.subscriber_active_date < (p_month + INTERVAL '1 month')::date
 GROUP BY agency_id
), daily AS (
 SELECT d.kit_id,
 coalesce(jsonb_object_agg(d.allocation_date::text,
    jsonb_build_object('net',d.cost_net, 'vat',d.cost_vat, 'gross',d.cost_net+d.cost_vat))
    FILTER (WHERE d.allocation_date>=p_month AND d.allocation_date<(p_month + INTERVAL '1 month')::date),'{}'::jsonb) AS current_days,
 coalesce(jsonb_object_agg(d.allocation_date::text,
    jsonb_build_object('net',d.cost_net, 'vat',d.cost_vat, 'gross',d.cost_net+d.cost_vat))
    FILTER (WHERE d.allocation_date>=(p_month + INTERVAL '1 month')::date AND d.allocation_date<(p_month + INTERVAL '2 month')::date),'{}'::jsonb) AS next_days,
 count(*) FILTER (WHERE d.allocation_date>=p_month AND d.allocation_date<(p_month + INTERVAL '1 month')::date) AS current_count,
 count(*) FILTER (WHERE d.allocation_date>=(p_month + INTERVAL '1 month')::date AND d.allocation_date<(p_month + INTERVAL '2 month')::date) AS next_count
 FROM d GROUP BY d.kit_id
)
 SELECT jsonb_build_object('msisdn',s.msisdn, 'package_code',s.package_code, 'package_registered_at',s.subscriber_active_date, 'payment_at',NULL::date, 'payment_amount_first',NULL::numeric, 'payment_amount_second',NULL::numeric, 'package_active_at',s.package_active_date, 'commission_gross',s.commission_gross, 'commission_net',s.commission_net, 'vat_amount',s.commission_vat, 'day_groups',jsonb_build_object('current',a.current_days,'next',a.next_days), '_basis','LEGACY_REFERENCE', '_payment_status','UNVERIFIED')
 FROM s JOIN daily a ON a.kit_id=s.kit_id
 WHERE a.current_count>0 OR a.next_count>0
 ORDER BY s.agency_id,s.msisdn,s.kit_id;
 RETURN; END IF;

IF p_code='I.4' THEN RETURN QUERY WITH s AS MATERIALIZED (
 SELECT * FROM reporting.fn_phase1_reference_source(p_month) ss
 WHERE p_agency_id IS NULL OR ss.agency_id=p_agency_id
), d AS MATERIALIZED (SELECT * FROM reporting.fn_phase1_reference_days(p_month)),
monthly AS (
 SELECT agency_id,COUNT(*) AS cnt FROM reporting.fn_phase1_reference_source(p_month) src
 WHERE src.subscriber_active_date >= p_month AND src.subscriber_active_date < (p_month + INTERVAL '1 month')::date
 GROUP BY agency_id
)
 SELECT jsonb_build_object('stt',row_number() OVER (ORDER BY s.agency_id,s.msisdn,s.kit_id), 'msisdn',s.msisdn, 'package_code',s.package_code, 'package_registered_at',s.subscriber_active_date, 'payment_at',NULL::date, 'payment_amount_first',NULL::numeric, 'payment_amount_second',NULL::numeric, 'commission_gross',s.commission_gross, 'commission_net',s.commission_net, 'vat_amount',s.commission_vat, '_basis','LEGACY_REFERENCE', '_payment_status','UNVERIFIED')
 FROM s 
 WHERE s.subscriber_active_date<(p_month + INTERVAL '1 month')::date AND (s.package_active_date IS NULL OR s.package_active_date>=(p_month + INTERVAL '1 month')::date)
 ORDER BY s.agency_id,s.msisdn,s.kit_id;
 RETURN; END IF;

IF p_code='I.5' THEN RETURN QUERY WITH s AS MATERIALIZED (
 SELECT * FROM reporting.fn_phase1_reference_source(p_month) ss
 WHERE p_agency_id IS NULL OR ss.agency_id=p_agency_id
), d AS MATERIALIZED (SELECT * FROM reporting.fn_phase1_reference_days(p_month)),
monthly AS (
 SELECT agency_id,COUNT(*) AS cnt FROM reporting.fn_phase1_reference_source(p_month) src
 WHERE src.subscriber_active_date >= p_month AND src.subscriber_active_date < (p_month + INTERVAL '1 month')::date
 GROUP BY agency_id
), daily AS (
 SELECT d.kit_id,
 coalesce(jsonb_object_agg(d.allocation_date::text,
    jsonb_build_object('net',d.cost_net, 'vat',d.cost_vat, 'gross',d.cost_net+d.cost_vat))
    FILTER (WHERE d.allocation_date>=p_month AND d.allocation_date<(p_month + INTERVAL '1 month')::date),'{}'::jsonb) AS current_days,
 coalesce(jsonb_object_agg(d.allocation_date::text,
    jsonb_build_object('net',d.cost_net, 'vat',d.cost_vat, 'gross',d.cost_net+d.cost_vat))
    FILTER (WHERE d.allocation_date>=(p_month + INTERVAL '1 month')::date AND d.allocation_date<(p_month + INTERVAL '2 month')::date),'{}'::jsonb) AS next_days,
 count(*) FILTER (WHERE d.allocation_date>=p_month AND d.allocation_date<(p_month + INTERVAL '1 month')::date) AS current_count,
 count(*) FILTER (WHERE d.allocation_date>=(p_month + INTERVAL '1 month')::date AND d.allocation_date<(p_month + INTERVAL '2 month')::date) AS next_count
 FROM d GROUP BY d.kit_id
)
 SELECT jsonb_build_object('stt',row_number() OVER (ORDER BY s.agency_id,s.msisdn,s.kit_id), 'msisdn',s.msisdn, 'package_code',s.package_code, 'subscriber_active_at',s.subscriber_active_at, 'package_active_at',s.package_active_date, 'payment_at',NULL::date, 'payment_amount_first',NULL::numeric, 'payment_amount_second',NULL::numeric, 'commission_gross',s.commission_gross, 'commission_net',s.commission_net, 'vat_amount',s.commission_vat, 'allocated_days',a.current_count, 'day_groups',jsonb_build_object('current',a.current_days), '_basis','LEGACY_REFERENCE', '_payment_status','UNVERIFIED')
 FROM s JOIN daily a ON a.kit_id=s.kit_id
 WHERE a.current_count>0
 ORDER BY s.agency_id,s.msisdn,s.kit_id;
 RETURN; END IF;

IF p_code='I.6' THEN RETURN QUERY WITH s AS MATERIALIZED (
 SELECT * FROM reporting.fn_phase1_reference_source(p_month) ss
 WHERE p_agency_id IS NULL OR ss.agency_id=p_agency_id
), d AS MATERIALIZED (SELECT * FROM reporting.fn_phase1_reference_days(p_month)),
monthly AS (
 SELECT agency_id,COUNT(*) AS cnt FROM reporting.fn_phase1_reference_source(p_month) src
 WHERE src.subscriber_active_date >= p_month AND src.subscriber_active_date < (p_month + INTERVAL '1 month')::date
 GROUP BY agency_id
), daily AS (
 SELECT d.kit_id,
 coalesce(jsonb_object_agg(d.allocation_date::text,
    jsonb_build_object('net',d.cost_net, 'vat',d.cost_vat, 'gross',d.cost_net+d.cost_vat))
    FILTER (WHERE d.allocation_date>=p_month AND d.allocation_date<(p_month + INTERVAL '1 month')::date),'{}'::jsonb) AS current_days,
 coalesce(jsonb_object_agg(d.allocation_date::text,
    jsonb_build_object('net',d.cost_net, 'vat',d.cost_vat, 'gross',d.cost_net+d.cost_vat))
    FILTER (WHERE d.allocation_date>=(p_month + INTERVAL '1 month')::date AND d.allocation_date<(p_month + INTERVAL '2 month')::date),'{}'::jsonb) AS next_days,
 count(*) FILTER (WHERE d.allocation_date>=p_month AND d.allocation_date<(p_month + INTERVAL '1 month')::date) AS current_count,
 count(*) FILTER (WHERE d.allocation_date>=(p_month + INTERVAL '1 month')::date AND d.allocation_date<(p_month + INTERVAL '2 month')::date) AS next_count
 FROM d GROUP BY d.kit_id
)
 SELECT jsonb_build_object('stt',row_number() OVER (ORDER BY s.agency_id,s.msisdn,s.kit_id), 'msisdn',s.msisdn, 'package_code',s.package_code, 'subscriber_active_at',s.subscriber_active_at, 'package_active_at',s.package_active_date, 'payment_first_at',NULL::date, 'payment_amount_first',NULL::numeric, 'payment_amount_second',NULL::numeric, 'commission_gross',s.commission_gross, 'commission_net',s.commission_net, 'vat_amount',s.commission_vat, 'forward_days',a.next_count, 'day_groups',jsonb_build_object('next',a.next_days), '_basis','LEGACY_REFERENCE', '_payment_status','UNVERIFIED')
 FROM s JOIN daily a ON a.kit_id=s.kit_id
 WHERE a.next_count>0
 ORDER BY s.agency_id,s.msisdn,s.kit_id;
 RETURN; END IF;

IF p_code='II.1' THEN RETURN QUERY WITH s AS MATERIALIZED (
 SELECT * FROM reporting.fn_phase1_reference_source(p_month) ss
 WHERE p_agency_id IS NULL OR ss.agency_id=p_agency_id
), d AS MATERIALIZED (SELECT * FROM reporting.fn_phase1_reference_days(p_month)),
monthly AS (
 SELECT agency_id,COUNT(*) AS cnt FROM reporting.fn_phase1_reference_source(p_month) src
 WHERE src.subscriber_active_date >= p_month AND src.subscriber_active_date < (p_month + INTERVAL '1 month')::date
 GROUP BY agency_id
), daily AS (
 SELECT d.kit_id,
 coalesce(jsonb_object_agg(d.allocation_date::text,
    jsonb_build_object('net',d.revenue_net, 'vat',d.revenue_vat, 'gross',d.revenue_net+d.revenue_vat))
    FILTER (WHERE d.allocation_date>=p_month AND d.allocation_date<(p_month + INTERVAL '1 month')::date),'{}'::jsonb) AS current_days,
 coalesce(jsonb_object_agg(d.allocation_date::text,
    jsonb_build_object('net',d.revenue_net, 'vat',d.revenue_vat, 'gross',d.revenue_net+d.revenue_vat))
    FILTER (WHERE d.allocation_date>=(p_month + INTERVAL '1 month')::date AND d.allocation_date<(p_month + INTERVAL '2 month')::date),'{}'::jsonb) AS next_days,
 count(*) FILTER (WHERE d.allocation_date>=p_month AND d.allocation_date<(p_month + INTERVAL '1 month')::date) AS current_count,
 count(*) FILTER (WHERE d.allocation_date>=(p_month + INTERVAL '1 month')::date AND d.allocation_date<(p_month + INTERVAL '2 month')::date) AS next_count
 FROM d GROUP BY d.kit_id
)
 SELECT jsonb_build_object('stt',row_number() OVER (ORDER BY s.agency_id,s.msisdn,s.kit_id), 'msisdn',s.msisdn, 'package_code',s.package_code, 'subscriber_active_at',s.subscriber_active_at, 'package_active_at',s.package_active_date, 'revenue_gross',s.revenue_gross, 'revenue_net',s.revenue_net, 'vat_amount',s.revenue_vat, 'day_groups',jsonb_build_object('current',a.current_days,'next',a.next_days), '_basis','LEGACY_REFERENCE')
 FROM s JOIN daily a ON a.kit_id=s.kit_id
 WHERE a.current_count>0 OR a.next_count>0
 ORDER BY s.agency_id,s.msisdn,s.kit_id;
 RETURN; END IF;

IF p_code='II.2' THEN RETURN QUERY WITH s AS MATERIALIZED (
 SELECT * FROM reporting.fn_phase1_reference_source(p_month) ss
 WHERE p_agency_id IS NULL OR ss.agency_id=p_agency_id
), d AS MATERIALIZED (SELECT * FROM reporting.fn_phase1_reference_days(p_month)),
monthly AS (
 SELECT agency_id,COUNT(*) AS cnt FROM reporting.fn_phase1_reference_source(p_month) src
 WHERE src.subscriber_active_date >= p_month AND src.subscriber_active_date < (p_month + INTERVAL '1 month')::date
 GROUP BY agency_id
)
 SELECT jsonb_build_object('stt',row_number() OVER (ORDER BY s.agency_id,s.msisdn,s.kit_id), 'msisdn',s.msisdn, 'package_code',s.package_code, 'subscriber_active_at',s.subscriber_active_at, 'revenue_gross',s.revenue_gross, 'revenue_net',s.revenue_net, 'vat_amount',s.revenue_vat, '_basis','LEGACY_REFERENCE')
 FROM s 
 WHERE s.subscriber_active_date<(p_month + INTERVAL '1 month')::date AND (s.package_active_date IS NULL OR s.package_active_date>=(p_month + INTERVAL '1 month')::date)
 ORDER BY s.agency_id,s.msisdn,s.kit_id;
 RETURN; END IF;

IF p_code='II.3' THEN RETURN QUERY WITH s AS MATERIALIZED (
 SELECT * FROM reporting.fn_phase1_reference_source(p_month) ss
 WHERE p_agency_id IS NULL OR ss.agency_id=p_agency_id
), d AS MATERIALIZED (SELECT * FROM reporting.fn_phase1_reference_days(p_month)),
monthly AS (
 SELECT agency_id,COUNT(*) AS cnt FROM reporting.fn_phase1_reference_source(p_month) src
 WHERE src.subscriber_active_date >= p_month AND src.subscriber_active_date < (p_month + INTERVAL '1 month')::date
 GROUP BY agency_id
), daily AS (
 SELECT d.kit_id,
 coalesce(jsonb_object_agg(d.allocation_date::text,
    jsonb_build_object('net',d.revenue_net, 'vat',d.revenue_vat, 'gross',d.revenue_net+d.revenue_vat))
    FILTER (WHERE d.allocation_date>=p_month AND d.allocation_date<(p_month + INTERVAL '1 month')::date),'{}'::jsonb) AS current_days,
 coalesce(jsonb_object_agg(d.allocation_date::text,
    jsonb_build_object('net',d.revenue_net, 'vat',d.revenue_vat, 'gross',d.revenue_net+d.revenue_vat))
    FILTER (WHERE d.allocation_date>=(p_month + INTERVAL '1 month')::date AND d.allocation_date<(p_month + INTERVAL '2 month')::date),'{}'::jsonb) AS next_days,
 count(*) FILTER (WHERE d.allocation_date>=p_month AND d.allocation_date<(p_month + INTERVAL '1 month')::date) AS current_count,
 count(*) FILTER (WHERE d.allocation_date>=(p_month + INTERVAL '1 month')::date AND d.allocation_date<(p_month + INTERVAL '2 month')::date) AS next_count
 FROM d GROUP BY d.kit_id
)
 SELECT jsonb_build_object('stt',row_number() OVER (ORDER BY s.agency_id,s.msisdn,s.kit_id), 'msisdn',s.msisdn, 'package_code',s.package_code, 'subscriber_active_at',s.subscriber_active_at, 'package_active_at',s.package_active_date, 'revenue_gross',s.revenue_gross, 'revenue_net',s.revenue_net, 'vat_amount',s.revenue_vat, 'allocated_days',a.current_count, 'day_groups',jsonb_build_object('current',a.current_days), '_basis','LEGACY_REFERENCE')
 FROM s JOIN daily a ON a.kit_id=s.kit_id
 WHERE a.current_count>0
 ORDER BY s.agency_id,s.msisdn,s.kit_id;
 RETURN; END IF;

IF p_code='II.4' THEN RETURN QUERY WITH s AS MATERIALIZED (
 SELECT * FROM reporting.fn_phase1_reference_source(p_month) ss
 WHERE p_agency_id IS NULL OR ss.agency_id=p_agency_id
), d AS MATERIALIZED (SELECT * FROM reporting.fn_phase1_reference_days(p_month)),
monthly AS (
 SELECT agency_id,COUNT(*) AS cnt FROM reporting.fn_phase1_reference_source(p_month) src
 WHERE src.subscriber_active_date >= p_month AND src.subscriber_active_date < (p_month + INTERVAL '1 month')::date
 GROUP BY agency_id
), daily AS (
 SELECT d.kit_id,
 coalesce(jsonb_object_agg(d.allocation_date::text,
    jsonb_build_object('net',d.revenue_net, 'vat',d.revenue_vat, 'gross',d.revenue_net+d.revenue_vat))
    FILTER (WHERE d.allocation_date>=p_month AND d.allocation_date<(p_month + INTERVAL '1 month')::date),'{}'::jsonb) AS current_days,
 coalesce(jsonb_object_agg(d.allocation_date::text,
    jsonb_build_object('net',d.revenue_net, 'vat',d.revenue_vat, 'gross',d.revenue_net+d.revenue_vat))
    FILTER (WHERE d.allocation_date>=(p_month + INTERVAL '1 month')::date AND d.allocation_date<(p_month + INTERVAL '2 month')::date),'{}'::jsonb) AS next_days,
 count(*) FILTER (WHERE d.allocation_date>=p_month AND d.allocation_date<(p_month + INTERVAL '1 month')::date) AS current_count,
 count(*) FILTER (WHERE d.allocation_date>=(p_month + INTERVAL '1 month')::date AND d.allocation_date<(p_month + INTERVAL '2 month')::date) AS next_count
 FROM d GROUP BY d.kit_id
)
 SELECT jsonb_build_object('stt',row_number() OVER (ORDER BY s.agency_id,s.msisdn,s.kit_id), 'msisdn',s.msisdn, 'package_code',s.package_code, 'subscriber_active_at',s.subscriber_active_at, 'package_active_at',s.package_active_date, 'revenue_gross',s.revenue_gross, 'revenue_net',s.revenue_net, 'vat_amount',s.revenue_vat, 'forward_days',a.next_count, 'day_groups',jsonb_build_object('next',a.next_days), '_basis','LEGACY_REFERENCE')
 FROM s JOIN daily a ON a.kit_id=s.kit_id
 WHERE a.next_count>0
 ORDER BY s.agency_id,s.msisdn,s.kit_id;
 RETURN; END IF;

IF p_code='I.7' THEN RETURN QUERY WITH s AS MATERIALIZED (
 SELECT * FROM reporting.fn_phase1_reference_source(p_month) ss
 WHERE p_agency_id IS NULL OR ss.agency_id=p_agency_id
), d AS MATERIALIZED (SELECT * FROM reporting.fn_phase1_reference_days(p_month)),
monthly AS (
 SELECT agency_id,COUNT(*) AS cnt FROM reporting.fn_phase1_reference_source(p_month) src
 WHERE src.subscriber_active_date >= p_month AND src.subscriber_active_date < (p_month + INTERVAL '1 month')::date
 GROUP BY agency_id
), issuance AS (
 SELECT agency_id, min(agency_name)::text AS agency_name,
 sum(commission_net) FILTER (WHERE package_active_date>=p_month AND package_active_date<(p_month + INTERVAL '1 month')::date) AS incurred_net,
 sum(commission_vat) FILTER (WHERE package_active_date>=p_month AND package_active_date<(p_month + INTERVAL '1 month')::date) AS incurred_vat,
 count(*) FILTER (WHERE package_price IS NULL) AS missing_price
 FROM s WHERE package_active_date<(p_month + INTERVAL '1 month')::date GROUP BY agency_id
), movements AS (
 SELECT s.agency_id,
 sum(d.cost_net) FILTER (WHERE s.package_active_date<p_month AND d.allocation_date>=p_month) AS opening_net,
 sum(d.cost_vat) FILTER (WHERE s.package_active_date<p_month AND d.allocation_date>=p_month) AS opening_vat,
 sum(d.cost_net) FILTER (WHERE d.allocation_date>=p_month AND d.allocation_date<(p_month + INTERVAL '1 month')::date) AS allocated_net,
 sum(d.cost_vat) FILTER (WHERE d.allocation_date>=p_month AND d.allocation_date<(p_month + INTERVAL '1 month')::date) AS allocated_vat
 FROM s JOIN d ON d.kit_id=s.kit_id GROUP BY s.agency_id
), ledger AS (
 SELECT i.agency_id,i.agency_name,i.missing_price,
 coalesce(m.opening_net,0) AS opening_net,coalesce(m.opening_vat,0) AS opening_vat,
 coalesce(i.incurred_net,0) AS incurred_net,coalesce(i.incurred_vat,0) AS incurred_vat,
 coalesce(m.allocated_net,0) AS allocated_net,coalesce(m.allocated_vat,0) AS allocated_vat
 FROM issuance i LEFT JOIN movements m ON m.agency_id=i.agency_id
)
 SELECT jsonb_build_object('stt',row_number() OVER (ORDER BY l.agency_id), 'agency_name',l.agency_name, 'agency_id',l.agency_id, 'opening_net',l.opening_net, 'opening_vat',l.opening_vat, 'opening_gross',l.opening_net+l.opening_vat, 'incurred_net',l.incurred_net, 'incurred_vat',l.incurred_vat, 'incurred_gross',l.incurred_net+l.incurred_vat, 'allocated_net',l.allocated_net, 'allocated_vat',l.allocated_vat, 'allocated_gross',l.allocated_net+l.allocated_vat, 'closing_net',(l.opening_net+l.incurred_net-l.allocated_net), 'closing_vat',(l.opening_vat+l.incurred_vat-l.allocated_vat), 'closing_gross',(l.opening_net+l.incurred_net-l.allocated_net)+(l.opening_vat+l.incurred_vat-l.allocated_vat), 'missing_price',l.missing_price, '_basis','LEGACY_REFERENCE', '_payment_status','UNVERIFIED') FROM ledger l ORDER BY l.agency_id;
 RETURN; END IF;

IF p_code='II.5' THEN RETURN QUERY WITH s AS MATERIALIZED (
 SELECT * FROM reporting.fn_phase1_reference_source(p_month) ss
 WHERE p_agency_id IS NULL OR ss.agency_id=p_agency_id
), d AS MATERIALIZED (SELECT * FROM reporting.fn_phase1_reference_days(p_month)),
monthly AS (
 SELECT agency_id,COUNT(*) AS cnt FROM reporting.fn_phase1_reference_source(p_month) src
 WHERE src.subscriber_active_date >= p_month AND src.subscriber_active_date < (p_month + INTERVAL '1 month')::date
 GROUP BY agency_id
), issuance AS (
 SELECT agency_id, min(agency_name)::text AS agency_name,
 sum(revenue_net) FILTER (WHERE package_active_date>=p_month AND package_active_date<(p_month + INTERVAL '1 month')::date) AS incurred_net,
 sum(revenue_vat) FILTER (WHERE package_active_date>=p_month AND package_active_date<(p_month + INTERVAL '1 month')::date) AS incurred_vat,
 count(*) FILTER (WHERE package_price IS NULL) AS missing_price
 FROM s WHERE package_active_date<(p_month + INTERVAL '1 month')::date GROUP BY agency_id
), movements AS (
 SELECT s.agency_id,
 sum(d.revenue_net) FILTER (WHERE s.package_active_date<p_month AND d.allocation_date>=p_month) AS opening_net,
 sum(d.revenue_vat) FILTER (WHERE s.package_active_date<p_month AND d.allocation_date>=p_month) AS opening_vat,
 sum(d.revenue_net) FILTER (WHERE d.allocation_date>=p_month AND d.allocation_date<(p_month + INTERVAL '1 month')::date) AS allocated_net,
 sum(d.revenue_vat) FILTER (WHERE d.allocation_date>=p_month AND d.allocation_date<(p_month + INTERVAL '1 month')::date) AS allocated_vat
 FROM s JOIN d ON d.kit_id=s.kit_id GROUP BY s.agency_id
), ledger AS (
 SELECT i.agency_id,i.agency_name,i.missing_price,
 coalesce(m.opening_net,0) AS opening_net,coalesce(m.opening_vat,0) AS opening_vat,
 coalesce(i.incurred_net,0) AS incurred_net,coalesce(i.incurred_vat,0) AS incurred_vat,
 coalesce(m.allocated_net,0) AS allocated_net,coalesce(m.allocated_vat,0) AS allocated_vat
 FROM issuance i LEFT JOIN movements m ON m.agency_id=i.agency_id
), slips AS (
 SELECT r.id AS slip_id,a.id AS agency_id,count(DISTINCT i.kit_id)::numeric AS qty
 FROM crm.pottel_kit_warehouse_transfer_requests r
 JOIN crm.pottel_kit_warehouse_transfer_request_items i ON i.request_id=r.id
 JOIN esim_agency.agencies a ON a.warehouse_id=r.to_warehouse_id
 WHERE r.status='COMPLETED'
   AND r.created_at::date>=p_month AND r.created_at::date<(p_month + INTERVAL '1 month')::date
   AND (p_agency_id IS NULL OR a.id=p_agency_id)
 GROUP BY r.id,a.id
), profile AS (
 SELECT agency_id,
 sum(qty * CASE WHEN qty<=1000 THEN 6500::numeric WHEN qty<=2000 THEN 3000::numeric ELSE 0::numeric END) AS gross,
 sum(qty*25000::numeric) AS active_gross
 FROM slips GROUP BY agency_id
), all_agencies AS (
 SELECT id AS agency_id,name::text AS agency_name
 FROM esim_agency.agencies
 WHERE id IN (SELECT agency_id FROM ledger UNION SELECT agency_id FROM profile)
), l AS (
 SELECT a.agency_id,a.agency_name,
 coalesce(t.opening_net,0) AS opening_net,coalesce(t.opening_vat,0) AS opening_vat,
 coalesce(t.incurred_net,0) AS incurred_net,coalesce(t.incurred_vat,0) AS incurred_vat,
 coalesce(t.allocated_net,0) AS allocated_net,coalesce(t.allocated_vat,0) AS allocated_vat,
 coalesce(t.missing_price,0) AS missing_price,
 coalesce(p.gross,0) AS profile_gross,coalesce(p.active_gross,0) AS activation_gross
 FROM all_agencies a LEFT JOIN ledger t ON t.agency_id=a.agency_id
 LEFT JOIN profile p ON p.agency_id=a.agency_id
)
 SELECT jsonb_build_object('stt',row_number() OVER (ORDER BY l.agency_id), 'agency_name',l.agency_name, 'agency_id',l.agency_id, 'opening_net',l.opening_net, 'opening_vat',l.opening_vat, 'opening_gross',l.opening_net+l.opening_vat, 'incurred_net',l.incurred_net, 'incurred_vat',l.incurred_vat, 'incurred_gross',l.incurred_net+l.incurred_vat, 'allocated_net',l.allocated_net, 'allocated_vat',l.allocated_vat, 'allocated_gross',l.allocated_net+l.allocated_vat, 'closing_net',(l.opening_net+l.incurred_net-l.allocated_net), 'closing_vat',(l.opening_vat+l.incurred_vat-l.allocated_vat), 'closing_gross',(l.opening_net+l.incurred_net-l.allocated_net)+(l.opening_vat+l.incurred_vat-l.allocated_vat), 'missing_price',l.missing_price, '_basis','LEGACY_REFERENCE', 'profile_gross',l.profile_gross, 'profile_net',round(l.profile_gross/1.1::numeric,2), 'profile_vat',l.profile_gross-round(l.profile_gross/1.1::numeric,2), 'activation_gross',l.activation_gross, 'activation_net',round(l.activation_gross/1.1::numeric,2), 'activation_vat',l.activation_gross-round(l.activation_gross/1.1::numeric,2)) FROM l ORDER BY l.agency_id;
 RETURN; END IF;
END
$fn$;
REVOKE ALL ON FUNCTION reporting.fn_phase1_reference_rows(text,date,integer) FROM PUBLIC;
COMMIT;
