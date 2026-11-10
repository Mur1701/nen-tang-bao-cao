-- Test PostgreSQL-side full-month trend semantics on synthetic fixtures.
-- DO NOT run this file on pottel_bss.
BEGIN TRANSACTION READ ONLY;
DO $verify$
DECLARE
  code text;
  function_name text;
  primary_expression text;
  secondary_expression text;
  query text;
  non_null_points bigint;
  month_count bigint;
BEGIN
  FOR code,function_name,primary_expression,secondary_expression IN
    SELECT * FROM (VALUES
      ('II.1','fn_ii1_report',
        '(SELECT sum((d.value->>''gross'')::numeric) FROM jsonb_each(coalesce(r.row_data #> ''{day_groups,current}'', ''{}''::jsonb)) d)',
        '(SELECT sum((d.value->>''gross'')::numeric) FROM jsonb_each(coalesce(r.row_data #> ''{day_groups,next}'', ''{}''::jsonb)) d)'),
      ('II.2','fn_ii2_report','(r.row_data->>''revenue_gross'')::numeric',NULL),
      ('II.3','fn_ii3_report',
        '(SELECT sum((d.value->>''gross'')::numeric) FROM jsonb_each(coalesce(r.row_data #> ''{day_groups,current}'', ''{}''::jsonb)) d)',NULL),
      ('II.4','fn_ii4_report',
        '(SELECT sum((d.value->>''gross'')::numeric) FROM jsonb_each(coalesce(r.row_data #> ''{day_groups,next}'', ''{}''::jsonb)) d)',NULL),
      ('II.5','fn_ii5_report','(r.row_data->>''incurred_gross'')::numeric',
        '(r.row_data->>''allocated_gross'')::numeric')
    ) m(code,function_name,primary_expression,secondary_expression)
  LOOP
    query := format(
      'WITH periods AS (
         SELECT g::date AS m FROM generate_series(date ''2026-05-01'',
           date ''2026-10-01'', interval ''1 month'') g
       ), values_by_month AS (
         SELECT p.m, SUM(%s) AS primary_amount, %s AS secondary_amount
         FROM periods p
         LEFT JOIN LATERAL reporting.%I(p.m, NULL::integer) r ON true
         GROUP BY p.m
       )
       SELECT COUNT(*), COUNT(primary_amount) FROM values_by_month',
      primary_expression,
      CASE WHEN secondary_expression IS NULL THEN 'NULL::numeric'
           ELSE 'SUM(' || secondary_expression || ')' END,
      function_name);
    EXECUTE query INTO month_count, non_null_points;
    IF month_count <> 6 THEN
      RAISE EXCEPTION '% returned % monthly points, expected 6',code,month_count;
    END IF;
    IF non_null_points < 1 THEN
      RAISE EXCEPTION '% did not aggregate any fixture amounts',code;
    END IF;
    RAISE NOTICE 'TREND %: % months, % non-null months',code,month_count,non_null_points;
  END LOOP;
END $verify$;
COMMIT;
