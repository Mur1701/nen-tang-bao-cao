-- POTTEL REPORTING SYSTEM | CANONICAL SQL SOURCE (DRAFT / DB DISCOVERY)
-- Report: I.3 — Phân bổ chi phí thuê bao
-- Source: docs/requirements/SRS_I3.md and docs/design/LLD_REPORT_I3.md (04/11/2026, DRAFT)
-- Updated from handover 06/11 and verified local I1 progress 09/11.
-- STATUS: DISCOVERY_ONLY; NOT A FINANCIAL REPORT IMPLEMENTATION; DO NOT DEPLOY AS REPORT FUNCTION.
-- Entry point reserved [PROPOSED]: reporting.fn_i3_report
-- Proposed input [DOCUMENTED]: Tháng
-- Grain [DOCUMENTED/TBD]: Thuê bao × gói/đợt chi trả (khóa dòng TBD)
-- Blocking decisions: DEC-ALLOC-01..03, DEC-DATA-01, DEC-TAX-01, DEC-PERIOD-01
-- Note: Mâu thuẫn căn cứ phân bổ: hoa hồng đã chi trả hay giá gói; chưa tính tiền.
-- Output columns/groups from SRS (not a verified physical SQL return contract):
--    1 | Số thuê bao
--    2 | Mã gói cước
--    3 | Ngày đăng ký gói
--    4 | Ngày chi trả
--    5 | Ngày active gói
--    6 | Số tiền chi trả đợt 1
--    7 | Số tiền chi trả đợt 2
--    8 | Tiền hoa hồng bán gói
--    9 | Tiền HHBG chưa VAT
--   10 | VAT
--   11 | Nhóm ngày phân bổ chi phí trong kỳ (mỗi ngày: CP chưa VAT / VAT / Tổng CP)
--   12 | Nhóm ngày phân bổ chi phí kỳ sau (mỗi ngày: CP chưa VAT / VAT / Tổng CP)
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
           FILTER (WHERE a.attname ~* '(msisdn|package|activ|payment|paid|amount|fee|commission|cost|vat|cycle|period|date|duration)') AS matching_columns
FROM pg_catalog.pg_class AS c
JOIN pg_catalog.pg_namespace AS n ON n.oid = c.relnamespace
JOIN pg_catalog.pg_attribute AS a ON a.attrelid = c.oid
  AND a.attnum > 0 AND NOT a.attisdropped
WHERE c.relkind IN ('r', 'p', 'v', 'm')
  AND n.nspname NOT IN ('pg_catalog','information_schema')
  AND n.nspname !~ '^pg_toast'
  AND (c.relname ~* '(subscr|msisdn|activ|cdr|pack|fee|commission|incentiv|payment|pay|alloc|cost)'
       OR a.attname ~* '(msisdn|package|activ|payment|paid|amount|fee|commission|cost|vat|cycle|period|date|duration)')
GROUP BY n.nspname, c.relname, c.relkind
ORDER BY (c.relname ~* '(subscr|msisdn|activ|cdr|pack|fee|commission|incentiv|payment|pay|alloc|cost)') DESC, n.nspname, c.relname
LIMIT 60;

-- Does this report entry point already exist? (absence is expected for unimplemented reports)
SELECT n.nspname AS function_schema, p.proname AS function_name,
       pg_catalog.pg_get_function_identity_arguments(p.oid) AS parameters
FROM pg_catalog.pg_proc AS p
JOIN pg_catalog.pg_namespace AS n ON n.oid = p.pronamespace
WHERE n.nspname = 'reporting'
  AND p.proname = 'fn_i3_report'
ORDER BY p.proname;

COMMIT;
