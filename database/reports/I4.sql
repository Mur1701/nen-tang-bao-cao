-- POTTEL REPORTING SYSTEM | CANONICAL SQL SOURCE (DRAFT / DB DISCOVERY)
-- Report: I.4 — Chưa phân bổ chi phí trong kỳ
-- Source: docs/requirements/SRS_I4.md and docs/design/LLD_REPORT_I4.md (04/11/2026, DRAFT)
-- Updated from handover 06/11 and verified local I1 progress 09/11.
-- STATUS: DISCOVERY_ONLY; NOT A FINANCIAL REPORT IMPLEMENTATION; DO NOT DEPLOY AS REPORT FUNCTION.
-- Entry point reserved [PROPOSED]: reporting.fn_i4_report
-- Proposed input [DOCUMENTED]: Tháng
-- Grain [DOCUMENTED/TBD]: Thuê bao × gói/chi trả chưa bắt đầu phân bổ (TBD)
-- Blocking decisions: DEC-ALLOC-01..03, DEC-DATA-01, DEC-DATA-02, DEC-PERIOD-01
-- Note: Phân biệt chưa active gói và thiếu bản ghi CDR; chưa được chốt.
-- Output columns/groups from SRS (not a verified physical SQL return contract):
--    1 | STT
--    2 | Số thuê bao
--    3 | Mã gói cước
--    4 | Ngày đăng ký gói (ngày active thuê bao)
--    5 | Ngày chi trả
--    6 | Số tiền chi trả đợt 1
--    7 | Số tiền chi trả đợt 2
--    8 | Số tiền hoa hồng bán gói
--    9 | Số tiền hoa hồng chưa VAT
--   10 | VAT
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
           FILTER (WHERE a.attname ~* '(msisdn|package|activ|payment|paid|amount|fee|cost|vat|cycle|period|date|status)') AS matching_columns
FROM pg_catalog.pg_class AS c
JOIN pg_catalog.pg_namespace AS n ON n.oid = c.relnamespace
JOIN pg_catalog.pg_attribute AS a ON a.attrelid = c.oid
  AND a.attnum > 0 AND NOT a.attisdropped
WHERE c.relkind IN ('r', 'p', 'v', 'm')
  AND n.nspname NOT IN ('pg_catalog','information_schema')
  AND n.nspname !~ '^pg_toast'
  AND (c.relname ~* '(subscr|msisdn|activ|cdr|pack|commission|payment|pay|alloc|cost)'
       OR a.attname ~* '(msisdn|package|activ|payment|paid|amount|fee|cost|vat|cycle|period|date|status)')
GROUP BY n.nspname, c.relname, c.relkind
ORDER BY (c.relname ~* '(subscr|msisdn|activ|cdr|pack|commission|payment|pay|alloc|cost)') DESC, n.nspname, c.relname
LIMIT 60;

-- Does this report entry point already exist? (absence is expected for unimplemented reports)
SELECT n.nspname AS function_schema, p.proname AS function_name,
       pg_catalog.pg_get_function_identity_arguments(p.oid) AS parameters
FROM pg_catalog.pg_proc AS p
JOIN pg_catalog.pg_namespace AS n ON n.oid = p.pronamespace
WHERE n.nspname = 'reporting'
  AND p.proname = 'fn_i4_report'
ORDER BY p.proname;

COMMIT;
