-- Phase 1 | READ-ONLY schema discovery for the 11 not-yet-executable reports.
-- Candidate tables/columns are NOT validated business sources and do NOT
-- authorize calculation. Safe metadata only: no subscriber rows, no BSS DML.
-- Run against a LOCAL READ-ONLY session after reviewing the query.
-- Each SRS_I2..II5 requires separate physical join/cardinality verification.

BEGIN TRANSACTION READ ONLY;
SET LOCAL statement_timeout = '30s';

WITH report_terms(report_code, table_regex, column_regex) AS (
    VALUES
       ('I.2',  '(subscr|kit|activ|agent|incent|package|msisdn)',
                '(msisdn|subscriber|agency|activ|package|price|amount|vat|date)'),
       ('I.3',  '(subscr|payment|commission|package|cdr|alloc|cost)',
                '(msisdn|paid|payment|commission|cost|package|activ|cycle|date|vat)'),
       ('I.4',  '(subscr|payment|commission|package|cdr|cost)',
                '(msisdn|paid|payment|commission|package|activ|date|vat)'),
       ('I.5',  '(subscr|payment|package|cdr|alloc|cost)',
                '(msisdn|commission|paid|payment|package|activ|period|date|vat)'),
       ('I.6',  '(subscr|payment|package|cdr|alloc|cost)',
                '(msisdn|commission|paid|payment|package|activ|period|date|vat)'),
       ('I.7',  '(payment|commission|package|alloc|cost)',
                '(paid|payment|commission|amount|cost|vat|date|period)'),
       ('II.1', '(subscr|product|package|cdr|alloc|revenue)',
                '(msisdn|price|amount|activ|package|cycle|date|vat|revenue)'),
       ('II.2', '(subscr|package|cdr|product|revenue)',
                '(msisdn|package|price|activ|date|vat|revenue)'),
       ('II.3', '(subscr|package|cdr|alloc|revenue)',
                '(msisdn|package|price|activ|cycle|date|vat|revenue)'),
       ('II.4', '(subscr|package|cdr|alloc|revenue)',
                '(msisdn|package|price|activ|cycle|date|vat|revenue)'),
       ('II.5', '(agent|sim|kit|batch|profile|product|activ|alloc|revenue)',
                '(agency|kit|batch|profile|price|amount|activ|date|vat|revenue)')
),
candidate_columns AS (
    SELECT r.report_code,
           n.nspname AS schema_name,
           c.relname AS relation_name,
           c.relkind AS relation_kind,
           a.attname AS column_name,
           (c.relname ~* r.table_regex) AS table_match
    FROM report_terms AS r
    JOIN pg_catalog.pg_class AS c ON c.relkind IN ('r','p','v','m')
    JOIN pg_catalog.pg_namespace AS n ON n.oid = c.relnamespace
    JOIN pg_catalog.pg_attribute AS a
      ON a.attrelid = c.oid AND a.attnum > 0 AND NOT a.attisdropped
    WHERE n.nspname NOT IN ('pg_catalog', 'information_schema')
      AND n.nspname !~ '^pg_toast'
      AND (c.relname ~* r.table_regex OR a.attname ~* r.column_regex)
      AND a.attname ~* r.column_regex
),
ranked AS (
    SELECT report_code, schema_name, relation_name,
           CASE relation_kind WHEN 'r' THEN 'table'
                              WHEN 'p' THEN 'partition'
                              WHEN 'v' THEN 'view'
                              WHEN 'm' THEN 'materialized_view' END AS relation_type,
           BOOL_OR(table_match) AS name_match,
           COUNT(DISTINCT column_name)::integer AS matching_column_count,
           STRING_AGG(DISTINCT column_name::text, ', ' ORDER BY column_name::text) AS matching_columns
    FROM candidate_columns
    GROUP BY report_code, schema_name, relation_name, relation_kind
),
top_matches AS (
    SELECT ranked.*,
           ROW_NUMBER() OVER (
               PARTITION BY report_code
               ORDER BY name_match DESC, matching_column_count DESC,
                        schema_name, relation_name
           ) AS rank_in_report
    FROM ranked
)
SELECT report_code, schema_name, relation_name, relation_type,
       matching_column_count, matching_columns
FROM top_matches
WHERE rank_in_report <= 20
ORDER BY report_code, rank_in_report;

-- Check proposed entry points without accidentally calling any function.
SELECT n.nspname AS function_schema,
       p.proname AS function_name,
       pg_catalog.pg_get_function_identity_arguments(p.oid) AS arguments
FROM pg_catalog.pg_proc AS p
JOIN pg_catalog.pg_namespace AS n ON n.oid = p.pronamespace
WHERE n.nspname = 'reporting'
  AND p.proname IN (
      'fn_i2_report','fn_i3_report','fn_i4_report','fn_i5_report',
      'fn_i6_report','fn_i7_report','fn_ii1_report','fn_ii2_report',
      'fn_ii3_report','fn_ii4_report','fn_ii5_report'
  )
ORDER BY p.proname;

COMMIT;
