# Security Test Plan — Pottel Reporting System

**Phiên bản:** 0.1 — REVIEW REQUIRED, 11/11/2026.
**Inputs:** [spec.md](spec.md) requirements and [plan.md](plan.md); 12-report Phase 1 functional test baselines.
**Status:** All security tests below = NOT RUN, unless explicitly indicated otherwise. Existing functional CI success does not prove these security controls.

## Test safety and environments

- Use isolated Spring MockMvc and mocks to test security, a disposable PostgreSQL 18 fixture for SQL and Excel, plus a separately authorized staging environment for cookies/network headers.
- **Do not** run flood/brute-force tests on local BSS, Render production, or real user accounts.
- No real passwords, subscriber identities, revenue, hashes or CSRF tokens in test artifacts. Redact errors and logs.
- Separate static source observations (VERIFIED static) from functional automated tests (PASS/FAIL) and runtime platform observations (NOT VERIFIED).
- Do not alter financial formulas, source BSS schema/data or React report contract to satisfy security tests.

## Given / When / Then cases

| Test ID | Requirements | Given | When | Then | Environment / current status |
|---|---|---|---|---|---|
| SEC-T-01 | SEC-CONFIG-001 | Local-only backend profile and running process | Inspect bound addresses safely | Only 127.0.0.1 in local profile; any LAN exposure is separately approved | Operator metadata; NOT RUN |
| SEC-T-02 | SEC-ACCESS-001 | Admin-only scope documented and authenticated fake admin | Request I.1, I.2 and agencyId=null | Access matches **explicit approved** admin policy; no accidental implicit scope assumptions | MockMvc; BLOCKED policy review |
| SEC-T-03 | SEC-AUTH-001, SEC-DOS-001 | AdminLoginThrottle with controllable clock and 10,000 fake distinct counters (no HTTP flood) | New valid account tries once; existing key exceeds five failures; window expires | Clean account is not globally blocked; violating account gets 429; expired counters stop blocking | Pure unit/mock; NOT RUN |
| SEC-T-04 | SEC-DOS-002 | Disposable PostgreSQL query/function deliberately exceeds configured budget | Authorized mock report invokes bounded query | Query terminates within approved timeout; safe status; no DB connection leaked | PostgreSQL fixture; NOT RUN |
| SEC-T-05 | SEC-DOS-002 | Concurrency guard tested with fake held work | Submit approved capacity + 1 synthetic requests | Extra request bounded/rejected with documented status; existing requests complete; no data corruption | Mock/fake executor; NOT RUN |
| SEC-T-06 | SEC-API-001 | Anonymous HTTP client, no BSS data | POST /api/reports/I.1/run, /excel and /api/reports/II.5/trend | 401 without body containing report, SQL or stack; ensure CSRF status interpretation agreed (anonymous + missing CSRF may be 403 before auth) | MockMvc; NOT RUN |
| SEC-T-07 | SEC-CSRF-001 | Authenticated mock admin with session | POST with missing/invalid, then valid CSRF token | Missing/invalid rejected 403; valid token passes security filter into mock controller | MockMvc; NOT RUN |
| SEC-T-08 | SEC-AUTH-002, SEC-SESSION-001 | Existing admin session and identity mock | Disable/revoke admin or change securityVersion, then call report | Session invalidated; 401 on subsequent sensitive call; no queries executed | MockMvc; NOT RUN |
| SEC-T-09 | SEC-EXPORT-001 | Fake output fields with =1+1, +SUM(1), -1+2, @SUM, leading zero MSISDN, tab and newline | Export XLSX and reopen with Apache POI | Text field type remains STRING, no formula cells, all source strings preserved; numeric, date, column order & row count unchanged | JUnit + POI; NOT RUN |
| SEC-T-10 | SEC-ERROR-001 | Mock DB unavailable / invalid input, no real secret | POST malformed month/invalid code/DB failure | Stable 400/4xx/5xx, no stack trace, host, SQL, password or token in HTTP | MockMvc; NOT RUN |
| SEC-T-11 | SEC-SQL-001 | PostgreSQL isolated fixture & mocked NamedParameterJdbcTemplate | Report code containing SQL symbols, bad months, negative agency, extreme page/size | Deny/validate before dangerous query; only hardcoded allowed function identifiers; no data alteration | JUnit+fixture; NOT RUN |
| SEC-T-12 | SEC-DB-001 | Authorized *actual application DB role* on test/staging | Read current_user and pg_catalog role/grant metadata; inspect SELECT rights and SECURITY INVOKER | Role no superuser, CREATE, BYPASSRLS, BSS DML; only required EXECUTE/SELECT; output contains no credentials | Read-only psql; NOT RUN |
| SEC-T-13 | SEC-SESSION-001 | Authorized staging proxy and direct backend origin | Login/logout; inspect Set-Cookie and session IDs | Secure, HttpOnly, SameSite required, session rotated on login, invalidated on logout, session expires | Staging browser/HTTP; NOT RUN |
| SEC-T-14 | SEC-HEADER-001 | Built Vite/AntD frontend behind staging proxy | Inspect GET SPA/API/XLSX response headers, refresh deep SPA route | CSP/header policy compatible with SPA; no-store for sensitive report/Excel; frame and sniffing protections observed | Staging; NOT RUN |
| SEC-T-15 | SEC-CONFIG-001, SEC-ERROR-001 | Staging Render/Vercel endpoint where test is explicitly allowed | Call anonymous report and health endpoints; inspect TLS and errors without exposing BSS | Direct backend protected; proxy protected; TLS valid; no secret/error disclosure | Staging only; NOT RUN |
| SEC-T-16 | SEC-AUDIT-001 | Fake admin; log capture with synthetic report month | Login, run, export, trend, failure | Event has requestId, actor, action, report code and status; no password, CSRF, MSISDN, values or SQL | Unit/integration; NOT RUN |
| SEC-T-17 | SEC-DEP-001 | CI with Maven pom, frontend package lock, Dockerfile | Generate SBOM and security scan metadata | Results triaged, false positives managed, no automatic unsafe upgrade or secret disclosure | CI; NOT RUN |
| SEC-T-18 | SEC-REGRESS-001 | Before/after versions on same fixture for all 12 reports | Execute Maven tests, React build, PostgreSQL smoke, Excel compare, 6-month trends | Business values, filters, 11-column I.1, 12 report codes, Excel dates and decimals unchanged; both functional and security tests PASS | CI + approved test DB; NOT RUN |

### Permission and CSRF nuance

Spring Security may return CSRF 403 before anonymous authentication runs; therefore test SEC-T-06 must supply a syntactically valid CSRF token/session when verifying the desired anonymous **401** behavior, and test SEC-T-07 separately proves missing/invalid CSRF **403**. Do not disable CSRF to achieve a preferred status.

### Excel formula nuance

Current ReportExcelExporter uses Apache POI setCellValue(String) for text; that already avoids writing formula cells in normal XLSX. The test is to **verify** that property even for hostile-looking strings, not to assume an exploitable formula injection. Do not prefix apostrophes or mutate MSISDN/agency names without demonstrated consumer need.

### Runtime budget nuance

A timeout inside a CREATE FUNCTION DDL transaction does not automatically constrain future executions. Measure the runtime timeout on disposable DB connections and confirm Hikari pool reuse does not leave a weakened or unintended timeout behind. Test timeout and concurrency separately.

## Test-first execution steps

1. Review and approve spec/plan/tasks; record operational environment and allowed test target.
2. Add minimal tests, run old baseline and new failing test to record **RED / BASELINE** where safe. Do not intentionally deploy an exploitable build.
3. Apply the smallest approved hardening change.
4. Run targeted test → Java/Maven regression → React build → PostgreSQL synthetic smoke.
5. Observe authorized staging/proxy if required; never extrapolate staging to production without evidence.
6. Update task status PASS only when acceptance conditions and traceability are met; write security-report.md and sync canonical PROJECT_STATUS.md.

## Evidence record template

- Test ID / requirement / finding
- Repo branch + full commit SHA
- Environment type (unit/fixture/local/staging)
- Command or CI workflow job identifier (without credentials)
- PASS / FAIL / BLOCKED / NOT VERIFIED
- Expected vs observed response/status or exact invariant
- Security logs redacted; no personal data
- Regressions performed and rollback action

## Existing results — not repurposed

GitHub Actions Phase 1 run 42791814572 previously PASS Java/React/PostgreSQL *functional* fixtures on main's predecessor. These checks are reused as regression but **none of SEC-T-01..18 has been executed as part of this audit**.

**GATE 3:** All security hardening tasks are NOT DONE pending approval, test code and execution proof.
