-- POTTEL REPORTING SYSTEM | CANONICAL SQL SOURCE (DRAFT / DB DISCOVERY)
-- Report: I.7 — Tổng hợp phân bổ chi phí bán gói
-- Source: docs/requirements/SRS_I7.md and docs/design/LLD_REPORT_I7.md (04/11/2026, DRAFT)
-- Updated from handover 06/11 and verified local I1 progress 09/11.
-- STATUS: DISCOVERY_ONLY; NOT A FINANCIAL REPORT IMPLEMENTATION; DO NOT DEPLOY AS REPORT FUNCTION.
-- Entry point reserved [PROPOSED]: reporting.fn_i7_report
-- Proposed input [DOCUMENTED]: Tháng
-- Grain [DOCUMENTED/TBD]: Tổng hợp một kỳ (không tự chia đại lý)
-- Blocking decisions: DEC-ALLOC-01..03, DEC-DATA-01, DEC-TAX-01, DEC-PERIOD-01
-- Note: Cân đối tồn đầu + phát sinh - phân bổ = tồn cuối; các thành phần tiền vẫn TBD.
-- Output columns/groups from SRS (not a verified physical SQL return contract):
--    1 | Chi phí trả trước chờ phân bổ đầu kỳ (1): CP chưa VAT / VAT / Tổng tiền
--    2 | Chi phí trả trước phát sinh trong kỳ (2): CP chưa VAT / VAT / Tổng tiền
--    3 | Chi phí phân bổ trong kỳ (3): CP chưa VAT / VAT / Tổng tiền
--    4 | Chi phí chưa phân bổ cuối kỳ: CP chưa VAT / VAT / Tổng tiền
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
           FILTER (WHERE a.attname ~* '(amount|payment|paid|cost|vat|alloc|period|balance|date)') AS matching_columns
FROM pg_catalog.pg_class AS c
JOIN pg_catalog.pg_namespace AS n ON n.oid = c.relnamespace
JOIN pg_catalog.pg_attribute AS a ON a.attrelid = c.oid
  AND a.attnum > 0 AND NOT a.attisdropped
WHERE c.relkind IN ('r', 'p', 'v', 'm')
  AND n.nspname NOT IN ('pg_catalog','information_schema')
  AND n.nspname !~ '^pg_toast'
  AND (c.relname ~* '(commission|payment|pay|alloc|cost|expense|period|balance)'
       OR a.attname ~* '(amount|payment|paid|cost|vat|alloc|period|balance|date)')
GROUP BY n.nspname, c.relname, c.relkind
ORDER BY (c.relname ~* '(commission|payment|pay|alloc|cost|expense|period|balance)') DESC, n.nspname, c.relname
LIMIT 60;

-- Does this report entry point already exist? (absence is expected for unimplemented reports)
SELECT n.nspname AS function_schema, p.proname AS function_name,
       pg_catalog.pg_get_function_identity_arguments(p.oid) AS parameters
FROM pg_catalog.pg_proc AS p
JOIN pg_catalog.pg_namespace AS n ON n.oid = p.pronamespace
WHERE n.nspname = 'reporting'
  AND p.proname = 'fn_i7_report'
ORDER BY p.proname;

COMMIT;
