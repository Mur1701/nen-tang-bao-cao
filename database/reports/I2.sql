-- POTTEL REPORTING SYSTEM | CANONICAL SQL SOURCE (DRAFT / DB DISCOVERY)
-- Report: I.2 — Chi tiết thuê bao khuyến khích theo sản lượng
-- Source: docs/requirements/SRS_I2.md and docs/design/LLD_REPORT_I2.md (04/11/2026, DRAFT)
-- Updated from handover 06/11 and verified local I1 progress 09/11.
-- STATUS: DISCOVERY_ONLY; NOT A FINANCIAL REPORT IMPLEMENTATION; DO NOT DEPLOY AS REPORT FUNCTION.
-- Entry point reserved [PROPOSED]: reporting.fn_i2_report
-- Proposed input [DOCUMENTED]: Tháng, đại lý
-- Grain [DOCUMENTED/TBD]: Thuê bao × gói/sự kiện đủ điều kiện (khóa dòng TBD)
-- Blocking decisions: TBD-I2-01..03, TBD-I1-01..08, DEC-DATA-02
-- Note: I.2 phải đối soát eligibility và tiền với I.1; nguồn KIT đơn thuần chưa đủ.
-- Output columns/groups from SRS (not a verified physical SQL return contract):
--    1 | STT
--    2 | Tên TĐL
--    3 | Số thuê bao
--    4 | Thời gian active thuê bao
--    5 | Mã gói
--    6 | Giá gói
--    7 | Mốc được hưởng trong kỳ
--    8 | Tổng tiền
--    9 | VAT
--   10 | Tổng tiền trước VAT
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
           FILTER (WHERE a.attname ~* '(msisdn|subscriber|agency|package|product|regist|activ|status|price|policy|effective|date)') AS matching_columns
FROM pg_catalog.pg_class AS c
JOIN pg_catalog.pg_namespace AS n ON n.oid = c.relnamespace
JOIN pg_catalog.pg_attribute AS a ON a.attrelid = c.oid
  AND a.attnum > 0 AND NOT a.attisdropped
WHERE c.relkind IN ('r', 'p', 'v', 'm')
  AND n.nspname NOT IN ('pg_catalog','information_schema')
  AND n.nspname !~ '^pg_toast'
  AND (c.relname ~* '(kit|subscr|msisdn|activ|cdr|pack|prod|agenc|policy|incentiv|reward)'
       OR a.attname ~* '(msisdn|subscriber|agency|package|product|regist|activ|status|price|policy|effective|date)')
GROUP BY n.nspname, c.relname, c.relkind
ORDER BY (c.relname ~* '(kit|subscr|msisdn|activ|cdr|pack|prod|agenc|policy|incentiv|reward)') DESC, n.nspname, c.relname
LIMIT 60;

-- Does this report entry point already exist? (absence is expected for unimplemented reports)
SELECT n.nspname AS function_schema, p.proname AS function_name,
       pg_catalog.pg_get_function_identity_arguments(p.oid) AS parameters
FROM pg_catalog.pg_proc AS p
JOIN pg_catalog.pg_namespace AS n ON n.oid = p.pronamespace
WHERE n.nspname = 'reporting'
  AND p.proname = 'fn_i2_report'
ORDER BY p.proname;

COMMIT;
