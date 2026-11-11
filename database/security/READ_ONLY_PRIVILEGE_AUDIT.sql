-- Pottel Security Hardening — read-only privilege discovery.
-- RUN ONLY under the application's actual DB user in authorized local/staging.
-- DO NOT run fixture creation, GRANT/REVOKE or BSS DML.
-- Output contains no credentials, subscriber rows or finance amounts.
BEGIN TRANSACTION READ ONLY;
SET LOCAL statement_timeout = '10s';

SELECT current_user AS active_role, current_database() AS database_name,
       r.rolsuper AS is_superuser, r.rolcreaterole AS can_manage_roles,
       r.rolcreatedb AS can_create_databases,
       r.rolbypassrls AS bypasses_row_security,
       has_database_privilege(current_user, current_database(), 'CREATE') AS can_create_objects
FROM pg_catalog.pg_roles r
WHERE r.rolname = current_user;

SELECT n.nspname AS schema_name,
       has_schema_privilege(current_user, n.oid, 'USAGE') AS can_use_schema,
       has_schema_privilege(current_user, n.oid, 'CREATE') AS can_create_in_schema
FROM pg_catalog.pg_namespace n
WHERE n.nspname IN ('reporting','crm','esim_agency','inventory','cdr','vnpost_bss')
ORDER BY n.nspname;

-- Aggregate privilege exposure by schema; do not read subscriber data.
SELECT n.nspname AS schema_name,
       count(*) FILTER (WHERE has_table_privilege(current_user, c.oid, 'SELECT')) AS selectable_relations,
       count(*) FILTER (WHERE has_table_privilege(current_user, c.oid, 'INSERT')
                           OR has_table_privilege(current_user, c.oid, 'UPDATE')
                           OR has_table_privilege(current_user, c.oid, 'DELETE')
                           OR has_table_privilege(current_user, c.oid, 'TRUNCATE')) AS writable_relations
FROM pg_catalog.pg_class c
JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace
WHERE n.nspname IN ('reporting','crm','esim_agency','inventory','cdr','vnpost_bss')
  AND c.relkind IN ('r','p','v','m')
GROUP BY n.nspname ORDER BY n.nspname;

SELECT p.proname AS report_function,
       pg_catalog.pg_get_function_identity_arguments(p.oid) AS argument_types,
       has_function_privilege(current_user,p.oid,'EXECUTE') AS can_execute
FROM pg_catalog.pg_proc p
JOIN pg_catalog.pg_namespace n ON n.oid=p.pronamespace
WHERE n.nspname='reporting' AND p.proname LIKE 'fn_%'
ORDER BY p.proname;

COMMIT;
