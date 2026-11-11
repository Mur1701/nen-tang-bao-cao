-- Disposable PostgreSQL 18 fixture only. No BSS data or schema changes.
-- Verify transaction-local statement_timeout does not persist after COMMIT.
BEGIN TRANSACTION READ ONLY;
SET LOCAL statement_timeout = '2000ms';
DO $assert$
DECLARE current_ms integer;
BEGIN
 SELECT setting::integer INTO current_ms
 FROM pg_catalog.pg_settings WHERE name = 'statement_timeout';
 IF current_ms <> 2000 THEN
   RAISE EXCEPTION 'Expected 2000ms statement timeout inside transaction, got %', current_ms;
 END IF;
END
$assert$;
COMMIT;

DO $assert$
DECLARE current_ms integer;
BEGIN
 SELECT setting::integer INTO current_ms
 FROM pg_catalog.pg_settings WHERE name = 'statement_timeout';
 IF current_ms <> 0 THEN
   RAISE EXCEPTION 'Transaction-local timeout leaked into connection: %', current_ms;
 END IF;
 RAISE NOTICE '[PASS] Transaction-local query timeout reset after commit';
END
$assert$;
