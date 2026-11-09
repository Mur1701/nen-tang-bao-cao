-- POTTEL REPORTING SYSTEM | CANONICAL SQL SOURCE (DRAFT / DB DISCOVERY)
-- Report: II.5 — Tổng hợp phân bổ doanh thu bán gói
-- Source: docs/requirements/SRS_II5.md and docs/design/LLD_REPORT_II5.md (04/11/2026, DRAFT)
-- Updated from handover 06/11 and verified local I1 progress 09/11.
-- STATUS: DISCOVERY_ONLY; NOT A FINANCIAL REPORT IMPLEMENTATION; DO NOT DEPLOY AS REPORT FUNCTION.
-- Entry point reserved [PROPOSED]: reporting.fn_ii5_report
-- Proposed input [DOCUMENTED]: Tháng
-- Grain [DOCUMENTED/TBD]: Đại lý × kỳ (TBD nguồn/khóa)
-- Blocking decisions: DEC-REV-01..03, DEC-ALLOC-02..03, DEC-TAX-01, DEC-PERIOD-01
-- Note: Chưa chốt doanh thu profile SIM, hòa mạng, gói và mốc giá theo lô.
-- Output columns/groups from SRS (not a verified physical SQL return contract):
--    1 | STT
--    2 | Đại lý
--    3 | DT Profile SIM: DT chưa VAT / VAT / Tổng tiền
--    4 | DT hòa mạng: DT chưa VAT / VAT / Tổng tiền
--    5 | Doanh thu gói chưa PB đầu kỳ (1): DT chưa VAT / VAT / Tổng tiền
--    6 | Doanh thu gói chưa PB phát sinh kỳ (2): DT chưa VAT / VAT / Tổng tiền
--    7 | Doanh thu gói PB trong kỳ (3): DT chưa VAT / VAT / Tổng tiền
--    8 | Doanh thu gói chưa PB cuối kỳ = (1)+(2)−(3): DT chưa VAT / VAT / Tổng tiền
--
-- This file is the single canonical SQL WORK ITEM for this report.
-- After data-source verification and business approval, replace its discovery SELECT
-- with the reviewed CREATE FUNCTION entry-point and associated helpers, preserving one file/report.
-- This runnable section reads PostgreSQL metadata only (no subscriber rows, no BSS DML).
-- Candidate matches are hints, NOT evidence that a relation is the approved source.

BEGIN TRANSACTION READ ONLY;
SET LOCAL statement_timeout = '30000ms';

-- Candidate physical relations / relevant columns (read-only metadata).
SELECT n.nspname AS schema_name,
       c.relname AS relation_name,
       CASE c.relkind WHEN 'r' THEN 'table' WHEN 'p' THEN 'partitioned_table'
           WHEN 'v' THEN 'view' WHEN 'm' THEN 'materialized_view' ELSE 'other' END AS relation_type,
       STRING_AGG(DISTINCT a.attname::text, ', ' ORDER BY a.attname::text)
           FILTER (WHERE a.attname ~* '(agency|kit|profile|batch|lot|activ|price|revenue|vat|alloc|period|date|amount)') AS matching_columns
FROM pg_catalog.pg_class AS c
JOIN pg_catalog.pg_namespace AS n ON n.oid = c.relnamespace
JOIN pg_catalog.pg_attribute AS a ON a.attrelid = c.oid
  AND a.attnum > 0 AND NOT a.attisdropped
WHERE c.relkind IN ('r', 'p', 'v', 'm')
  AND n.nspname NOT IN ('pg_catalog','information_schema')
  AND n.nspname !~ '^pg_toast'
  AND (c.relname ~* '(agenc|kit|sim|profile|batch|lot|activ|product|revenue|receipt|alloc|income)'
       OR a.attname ~* '(agency|kit|profile|batch|lot|activ|price|revenue|vat|alloc|period|date|amount)')
GROUP BY n.nspname, c.relname, c.relkind
ORDER BY (c.relname ~* '(agenc|kit|sim|profile|batch|lot|activ|product|revenue|receipt|alloc|income)') DESC, n.nspname, c.relname
LIMIT 60;

-- Does this report entry point already exist? (absence is expected for unimplemented reports)
SELECT n.nspname AS function_schema, p.proname AS function_name,
       pg_catalog.pg_get_function_identity_arguments(p.oid) AS parameters
FROM pg_catalog.pg_proc AS p
JOIN pg_catalog.pg_namespace AS n ON n.oid = p.pronamespace
WHERE n.nspname = 'reporting'
  AND p.proname = 'fn_ii5_report'
ORDER BY p.proname;

COMMIT;
