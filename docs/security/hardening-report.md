# Pottel Security Hardening — Implementation and verification report

**Date:** 11/11/2026 | **Branch:** `security/hardening-phase1-20261111` | **Status:** REVIEW / NOT DEPLOYED
**Approved context:** The user confirmed a **public Internet deployment**, that all BSS admins may access all reports/agencies, and authorized security tests + hardening on a **separate branch**. Do not merge main automatically and do not modify BSS data.
**Source baseline:** main `67523a6d5d70e0ff46af9d361816e11b3149d356`
**Specification:** draft PR #3 (`docs/security/spec.md`, `plan.md`, `tasks.md`, `tests.md`).

## Implemented in this branch

| Task / finding | Changes | Test evidence | Status |
|---|---|---|---|
| SG-03 / SEC-F-01 | Removed the global 429 gate when login attempt-map reaches 10,000 keys. Bounded insertion-order eviction, time-window expiry, separate per-remote-address+username enforcement, success clears key. | `AdminLoginThrottleSecurityTest` — capacity, 5 failures, expiry, success. | **PASS unit**; public edge limits still required |
| SG-04 / SEC-F-02 | `ReportAdmissionFilter` after Spring Security authorization: max 4 concurrent `run/excel/trend` requests **per instance**, configurable. Excess returns HTTP 429 and Retry-After. | `ReportSecurityBudgetTest` concurrent fake request + release. | **PASS unit**; not production load-tested |
| SG-04 / SEC-F-02 | `ReportQueryTimeout`: PostgreSQL `SET LOCAL statement_timeout` default 20,000 ms, re-applied per read-query in active Spring read-only transaction. I.1 official, I.1 KIT, 11 other reports and 5 revenue-trend APIs covered. No shared pool timeout leakage. | `ReportSecurityBudgetTest` unit + `TRANSACTION_LOCAL_TIMEOUT_ASSERT.sql` on disposable PostgreSQL 18. | **PASS unit + SQL fixture**; real DB latency not measured |
| SG-06 / SEC-CSRF-001, SEC-API-001 | Spring Boot 4 MockMvc security slice and tests for anonymous report/excel/trend denial, valid/missing/invalid CSRF, revoked BSS admin and role rejection; retained existing SecurityConfig behavior. | `ReportEndpointSecurityTest` integration on mocked BSS identity. | **PASS MockMvc** |
| SG-07 / SEC-F-07 | Tested hostile-looking agency strings in XLSX using Apache POI. Existing string-cell behavior retained; no formula evaluation introduced. | `ExcelSecurityTest`. | **PASS workbook unit** |
| SG-05 / SEC-F-03 | Added `database/security/READ_ONLY_PRIVILEGE_AUDIT.sql`: read-only metadata queries to identify actual app role capabilities/grants without showing credentials/PII. | SQL source review; **NOT RUN against actual deployment role**. | **PENDING runtime DBA/infra audit** |
| SG-11 / SEC-REGRESS-001 | CI for this branch executes Maven tests, React build, 12 report PostgreSQL fixture smoke, five 6-month revenue trends, query-timeout transaction reset. | GitHub Actions `42946127462`: `java-and-react` PASS; `postgres-18-smoke` PASS. | **PASS CI synthetic**, not production acceptance |

### Configurable safety controls (no passwords)

| Spring property | Environment variable | Default | Validation |
|---|---|---|---|
| `reporting.security.max-concurrent-requests` | `REPORTING_SECURITY_MAX_CONCURRENT_REQUESTS` | 4 per JVM | 1–100 |
| `reporting.security.statement-timeout-ms` | `REPORTING_SECURITY_STATEMENT_TIMEOUT_MS` | 20,000 ms | 1,000–120,000 ms |

**Important:** A 20-second query budget **may interrupt an expensive real BSS report**. The new settings are proposed conservative defaults, **not performance numbers already validated against live PostgreSQL**. Export limit remains 5,000 rows; existing financial SQL, serializations, dates, VAT and rounding were not edited.

## Known residual public-Internet risks — do not call hardening complete

1. **Public edge rate limit / DDoS / shared backend instances — P0:** throttle state and report concurrency are local to each JVM. Rotating username/address keys can evade per-key logic. Set a trusted rate policy at CDN/edge and verify proxies before production. NEVER trust client-provided X-Forwarded-For without known proxy boundaries.
2. **Actual PostgreSQL permissions — P0:** no app role grant output has been inspected. On local BSS some sessions may use the `postgres` superuser; this must not be the production web-app account. Use dedicated minimal SELECT/EXECUTE role after DBA review. Read-only Hikari does not replace DB privileges.
3. **Production session/network — P0/P1:** TLS/Render direct host/Vercel rewrite, cookies through proxy, CSP and CORS require authorized deployment verification. The Render connector requested explicit workspace selection; no workspace has been selected, so no production metadata was inspected.
4. **Audit logs and error/header policy — P1:** structured access/export/trend audit, retention, redaction, security headers and sanitized errors require dedicated checks and decisions. Existing Spring Security response headers are not a guarantee about the public proxy.
5. **Supply chain — P1:** npm/Maven/container SCA and action/image pin review remain pending. No CVE is claimed without scan evidence.
6. **Load/performance under real data — P0:** no intrusive load/DoS test was run on public infrastructure or `pottel_bss`; run safe staged representative dataset tests before deployment.
7. **Authentication scope:** per user approval, BSS `ROLE_ADMIN` can access all 12 reports and agencies. No new RBAC or arbitrary agency restrictions were introduced.

## Verification summary

- **Source commits:** stored on `security/hardening-phase1-20261111`, created from main; `main` has not been modified by this work.
- **Functional baseline and security suite:** GitHub Actions **PASS** for run 42946127462 (Java tests + React build + PG18 synthetic fixture).
- **NOT VERIFIED:** Actual public deployment endpoint, Redis/shared rate limiting, DB grants, production report query latency and UI browser runtime.
- **No BSS DML, no schema changes applied to live database, no secrets added to Git, no automatic production deployment.**
- **Go/no-go:** this is **PR-ready for code review**, not a declaration of production-security signoff.

## Follow-up tasks and dependencies

1. Review/approve these implementation choices and performance defaults in hardening PR.
2. Select Render workspace separately before any connector inspection (the connector will not auto-select). Verify TLS/proxy behavior with authorized test endpoint.
3. Inspect actual application DB role using `READ_ONLY_PRIVILEGE_AUDIT.sql` through an authorized read-only session; do not run GRANT/REVOKE automatically.
4. Add edge/shared rate control and staging load test, then audit logging/headers/dependency scans; each with tests and separate approvals where the spec requires them.
5. After full review, update the canonical `PROJECT_STATUS.md` in Project Sources. It is not located in Git main; do not fabricate another authoritative status file.

**Merge:** NOT AUTHORIZED by the user. Keep PR draft and no deployment.
