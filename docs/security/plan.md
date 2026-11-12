# Security Plan — Pottel Reporting System

**Version:** 0.1, 11/11/2026 — REVIEW REQUIRED. Based on [spec.md](spec.md) and main commit 67523a6d.
**Scope:** design only; no source/config/DB mutation is authorized.

## Design invariants

Preserve BSS-admin Spring Security login, CSRF and session validation, all 12 report contracts, numerical exactness, Excel content and existing smoke tests. Do not change financial calculations, source BSS data, login/RBAC model or expose a local database. Stage all security code in a separate branch only after confirmation. Prefer tests first and fail-closed changes. Roll back by reverting only the approved hardening commit and verifying previous functional behavior.

## Plan by requirement

| Requirements / findings | Proposed control and files | Verification | Rollback |
|---|---|---|---|
| SEC-AUTH-001, SEC-DOS-001 / F-01 | Replace global saturated-counter deny in AdminLoginThrottle with bounded TTL eviction plus **per-key** enforcement; define safe full-capacity handling; avoid spoofable proxy IP trust. No new login system. | Mock time, 5 failures, capacity 10,000, fresh valid login still passes, expired counters evicted; no external flood. | Revert throttle class; record known risk if emergency rollback. |
| SEC-AUTH-002, SEC-API-001 / F-09 | Keep SecurityConfig deny-by-default ROLE_ADMIN, AdminSessionValidationFilter, account revocation check. Add MockMvc tests for anonymous, non-admin, disabled account and admin report/trend/export. | 401/403/2xx matrix with mock BSS identity; unchanged session behavior. | Revert tests/config changes only. |
| SEC-CSRF-001, SEC-SESSION-001 / F-09 | Preserve HttpSessionCsrfTokenRepository, Secure/HttpOnly/SameSite cookie defaults, session fixation rotation and 20-minute idle timeout. Test across authorized local/staging proxy and direct host; cap session creation if measured necessary. | Missing/invalid CSRF 403; valid session & token pass; Secure/HttpOnly/SameSite and logout/revocation verified. | Revert only newly added settings, never disable existing CSRF to make tests green. |
| SEC-ACCESS-001 / F-10 | Document admin-only scope and explicitly decide whether admin is allowed all agencies. Add future per-report view/export and agency authorization only after separately approved policy; server-side enforce before SQL if adopted. | Tests for approved policy, including agencyId null/no implicit widening if agency scoping added. | Revert new authorization policy, not BSS identity bridge. |
| SEC-SQL-001 / F-02 | Preserve ReportFunctionRegistry allowlist and NamedParameterJdbcTemplate bind parameters; validate month, agency, page, previewMode and report code. Add negative tests for special characters and unknown function names. | Invalid input -> safe 4xx; no dynamic user SQL fragments; fixture unchanged. | Revert validator change after full regression. |
| SEC-DOS-002 / F-02 | Add **runtime** PostgreSQL statement timeout in read-only report transactions; limit simultaneous run/excel/trend admissions with bounded queue, suitable per-admin/endpoint rate; preserve Excel 5,000 row cap and exact output. Profile count+page and six-month computation on fake/staging DB; propose aggregate optimization only after exact result comparisons. | Isolated timeout and concurrency tests, 429/503 when exhausted, predictable error, no partial export; financial and React regression PASS. | Revert budget tuning if functional regression; keep admission/safety monitoring. |
| SEC-DB-001 / F-03 | Audit actual runtime role using read-only pg_catalog role/grant metadata. Request DBA to create dedicated reporting read role with only CONNECT/USAGE/EXECUTE and minimum SELECT for SECURITY INVOKER functions; never use postgres superuser as web-app identity in deployment. Hikari readOnly is not sufficient. | Actual DB role not superuser/CREATE/DML; access to approved views/functions only; no BSS changes. | DBA-controlled revoke/grant rollback recorded separately; no automatic privilege escalation. |
| SEC-CONFIG-001 / F-05 | Environment matrix: loopback bind for local-only profile; internal network with authorized source; public HTTPS via Vercel/Render and protected Render direct URL. Run container as non-root and verify permissions; do not copy local secrets into Docker. | Bind address and port metadata; TLS, HTTP redirects, direct-backend 401, non-root container verified on staging. | Restore last secure deployment/config; do not expose DB. |
| SEC-EXPORT-001 / F-07 | Apache POI must retain text cell type for arbitrary strings, not formula; exact decimal/leading-zero MSISDN and date formats. Test strings beginning equals/plus/minus/at-sign, tabs/newlines and suspicious formula text, **without** changing original values. | Open produced XLSX with POI; zero FORMULA cells; correct strings, columns, row cap; existing Excel tests PASS. | Revert exporter change only, retain tests. |
| SEC-ERROR-001 / F-06 | Consistent sanitized API errors using safe Spring exception mapping and stable JSON; no SQL/stack/cookie/credential disclosure. Log correlation ID and redacted server-side diagnostics. Add no-store for sensitive report JSON/XLSX. | 400,401,403,422,500 tests contain no stack or secrets; original status preserved. | Revert error mapper only after review. |
| SEC-HEADER-001 / F-06 | First verify Spring Security default headers and proxy response headers; design CSP separately for Vite dev/production React/AntD without unsafe-eval; cache-control, referrer, frame policy. | Authenticated SPA/route refresh/Excel under CSP; browser and proxy header check. | Roll back only offending new policy; retain default framework protections. |
| SEC-AUDIT-001 / F-04 | Emit structured server-side report run/export/trend/login events: requestId, actor, report code, month, outcome, duration and count; **exclude MSISDN/PII, password, hash, CSRF and raw SQL**. Decide storage, rotation and retention before adding logging. | Unit tests check correct attributes and absence of secrets; allow operator correlation. | Disable new sink, preserve necessary error diagnostics. |
| SEC-DEP-001 / F-08 | Add Maven/npm/container SCA reports and SBOM; review CVE relevance before block; consider pinning actions/images; no mass dependency upgrades. | CI outputs scans without secrets; triaged vulnerabilities recorded; regression PASS. | Temporarily downgrade scanner to advisory on false positives, not erase findings. |
| SEC-REGRESS-001 / all | Compare report JSON & Excel byte-independent canonical values from 12-report fixture, MSISDN/date/VAT and chart totals; run existing CI baseline. | java-and-react + postgres-18-smoke PASS; security integration tests PASS. | Revert offending security change, never modify finance logic to satisfy a security test. |

## Proposed staged implementation

1. **Stage A — Baseline and negative tests:** approve spec and threat model. Write tests demonstrating F-01 and missing query budgets without stressing a live environment.
2. **Stage B — Availability:** patch throttle; design and test admission/timeout for report, export, chart; no business SQL modifications.
3. **Stage C — Access and data:** verify existing auth/CSRF; review DB least privilege and production topology; implement any config only with ops approval.
4. **Stage D — Boundary and operations:** Excel, errors, security headers, audit, dependency scanning; use small atomic PRs.
5. **Stage E — Security acceptance:** repeat functional CI and production-safe staging checks; record residual issues in security-report.md.

## Review-required decisions

- Is currently active environment only local admin, or is a public Render/ Vercel deployment serving real BSS data? **NOT VERIFIED**.
- Does each BSS admin have access to all 12 reports and agencies? Current source says yes; this is not a confirmed future RBAC policy.
- What query time budget, concurrent request budget and Excel row cap are operationally acceptable? Must be measured on authorized test data.
- Who owns production DB grants, deployment settings, and audit logs/retention?
- Security tests must not create attack traffic to public endpoints.

**GATE 2:** proposed; approval required before source/config fixes.
