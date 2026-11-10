-- CI ONLY. Postgres 18 synthetic database seeded by LEGACY_REFERENCE_SMOKE_FIXTURE.sql.
-- Never run on the real BSS database.
BEGIN TRANSACTION READ ONLY;
SET LOCAL statement_timeout = '90s';

DO $test$
DECLARE
 code text;
 observed integer;
 missing integer;
 income jsonb;
 cost jsonb;
BEGIN
 FOREACH code IN ARRAY ARRAY['I.1','I.2','I.3','I.4','I.5','I.6','I.7',
                             'II.1','II.2','II.3','II.4','II.5']
 LOOP
  SELECT count(*)::integer INTO observed
  FROM reporting.fn_phase1_reference_rows(code,'2026-10-01'::date,NULL);
  IF observed < 0 THEN
    RAISE EXCEPTION 'Non-sensical row count for %',code;
  END IF;
  RAISE NOTICE 'REPORT %: % rows',code,observed;
 END LOOP;

 SELECT count(*)::integer INTO observed
 FROM reporting.fn_phase1_reference_rows('I.1','2026-10-01',NULL);
 IF observed <> 1 THEN RAISE EXCEPTION 'I.1 expected 1 agency/package group, got %', observed; END IF;

 SELECT count(*)::integer INTO observed
 FROM reporting.fn_phase1_reference_rows('II.2','2026-10-01',NULL);
 IF observed <> 1 THEN RAISE EXCEPTION 'II.2 expected 1 package not yet active, got %',observed; END IF;

 SELECT count(*)::integer INTO observed
 FROM reporting.fn_phase1_reference_rows('II.3','2026-10-01',NULL);
 IF observed <> 1 THEN RAISE EXCEPTION 'II.3 expected 1 active package, got %',observed; END IF;

 SELECT count(*)::integer INTO observed
 FROM reporting.fn_phase1_reference_rows('II.4','2026-10-01',NULL);
 IF observed <> 1 THEN RAISE EXCEPTION 'II.4 expected a remaining November allocation, got %',observed; END IF;

 SELECT row_data INTO income
 FROM reporting.fn_phase1_reference_rows('II.1','2026-10-01',NULL) LIMIT 1;
 IF income #>> '{day_groups,current,2026-10-31,net}' IS NULL
     OR income #>> '{day_groups,next,2026-11-01,net}' IS NULL THEN
   RAISE EXCEPTION 'II.1 missing real month-end allocation day values';
 END IF;

 SELECT row_data INTO cost
 FROM reporting.fn_phase1_reference_rows('I.3','2026-10-01',NULL) LIMIT 1;
 IF cost->>'payment_at' <> '2026-10-15'
    OR (cost->>'payment_amount_first')::numeric <> 24300 THEN
    RAISE EXCEPTION 'I.3 failed to read effective HHBG payment';
 END IF;

 SELECT row_data INTO income
 FROM reporting.fn_phase1_reference_rows('II.5','2026-10-01',NULL) LIMIT 1;
 IF (income->>'profile_gross')::numeric <> 13000
    OR (income->>'activation_gross')::numeric <> 50000 THEN
   RAISE EXCEPTION 'II.5 wrong profile/activation baseline';
 END IF;

 SELECT count(*)::integer INTO missing
 FROM reporting.fn_phase1_reference_rows('II.5','2026-10-01',NULL)
 WHERE (row_data->>'closing_gross')::numeric <>
       ((row_data->>'opening_gross')::numeric
        +(row_data->>'incurred_gross')::numeric
        -(row_data->>'allocated_gross')::numeric);
 IF missing <> 0 THEN RAISE EXCEPTION 'II.5 balance equation mismatch'; END IF;

 SELECT count(*)::integer INTO missing
 FROM reporting.fn_phase1_reference_rows('I.7','2026-10-01',NULL)
 WHERE (row_data->>'closing_gross')::numeric <>
       ((row_data->>'opening_gross')::numeric
        +(row_data->>'incurred_gross')::numeric
        -(row_data->>'allocated_gross')::numeric);
 IF missing <> 0 THEN RAISE EXCEPTION 'I.7 balance equation mismatch'; END IF;

 RAISE NOTICE '[PASS] 12 report SQL branches, allocation dates, HHBG payments, ledger and profile';
END
$test$;
COMMIT;
