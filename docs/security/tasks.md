# Security Tasks — Pottel Reporting System

**Ngày:** 11/11/2026 | **Status:** REVIEW REQUIRED; chưa được phép code hardening.
**Traceability:** [spec.md](spec.md) → [plan.md](plan.md) → [tests.md](tests.md) → code → verify.
**Definitions:** TODO = có nhiệm vụ chưa thực hiện; BLOCKED = thiếu phê duyệt/quyền/môi trường; IN PROGRESS = đã bắt đầu; FAIL/PASS chỉ theo test thực; không đồng nhất documentation commit với security remediation.

| Task | Priority | Related finding / requirements | Prerequisites | Impact / implementation steps | Tests / acceptance | Status |
|---|---|---|---|---|---|---|
| SG-00 Audit baseline | P0 | F-01..F-10 / all | Access source main/docs | Inventory REST, auth, DB, Excel, dependencies; create spec/plan/tasks/tests; mark static evidence vs runtime. | Review traceable sources and list all NOT VERIFIED; **no code change**. | IN PROGRESS — draft ready |
| SG-01 Confirm environment / scope | P0 | F-03,F-05,F-09,F-10 / SEC-CONFIG-001, SEC-ACCESS-001 | Stakeholder/ops approval | Determine local/internal/public trust model, read/write DB role, authorized admin report/agency scope, Render direct host. Read-only metadata only. | SEC-T-01, SEC-T-02; written approval. | BLOCKED |
| SG-02 Security tests baseline | P0 | F-01,F-02,F-07,F-09 / SEC-AUTH-001, SEC-DOS-002, SEC-EXPORT-001, SEC-CSRF-001 | Spec and plan approved | Add tests to existing Spring Boot tests without changes to financial SQL; capture RED/BASELINE, no public probes. | SEC-T-03,04,05,06,07,08,09,10. | BLOCKED |
| SG-03 Fix global login throttle | P0 | F-01 / SEC-AUTH-001, SEC-DOS-001 | SG-02 tests | Change AdminLoginThrottle only; ensure bounded TTL cache without global deny; retain per-key guard and generic auth errors. | SEC-T-03 + old auth tests PASS. | BLOCKED |
| SG-04 Report cost budget | P0 | F-02 / SEC-DOS-002, SEC-SQL-001 | SG-01 budget/SG-02 tests | Runtime DB statement timeout + rate/concurrency admission. Optimize count/trend only with exact output comparison; do not change money logic. | SEC-T-04,05,11,18; report + trend + Excel regression. | BLOCKED |
| SG-05 Least privilege DB | P0 | F-03 / SEC-DB-001 | SG-01/DBA approval | Audit current_role with pg_catalog only; prepare dedicated read role/grants as proposal; DBA applies only after approval. No BSS DML. | SEC-T-12 with sanitized metadata. | BLOCKED |
| SG-06 Security integration regressions | P1 | F-09,F-10 / SEC-API-001, SEC-AUTH-002, SEC-CSRF-001, SEC-SESSION-001, SEC-ACCESS-001 | SG-02/mock identity | MockMvc login, anonymous report/trend/export, CSRF, revoked admin, access-scope decision; preserve current admin-only policy. | SEC-T-06,07,08,13. | BLOCKED |
| SG-07 Excel safe cells | P1 | F-07 / SEC-EXPORT-001, SEC-REGRESS-001 | SG-02 tests | Test POI cell type for malicious-looking text; if unsafe fix exporter minimally; preserve XLSX values/precision/format. | SEC-T-09 and existing I1ExcelExporterTest. | BLOCKED |
| SG-08 Errors and headers | P1 | F-05,F-06 / SEC-ERROR-001, SEC-HEADER-001, SEC-CONFIG-001 | SG-01,SG-02 | Sanitize API errors, no-store responses, CSP compatible with SPA after header reconnaissance. Infra changes separate. | SEC-T-10,14,15; Vite/React build and browser route smoke. | BLOCKED |
| SG-09 Security event logging | P1 | F-04 / SEC-AUDIT-001 | log retention/location approval | Add structured report/auth audit metadata only, no PII/secrets or money; correlation IDs. | SEC-T-16. | BLOCKED |
| SG-10 Supply chain checks | P1 | F-08 / SEC-DEP-001 | CI policy approved | SCA Maven/npm/container + SBOM, triage, pin external artifacts where justified; no dependency auto-upgrades. | SEC-T-17; reproducible CI. | BLOCKED |
| SG-11 Final verification | P0 | all / SEC-REGRESS-001 | SG-03..10 according to approved scope | Run tests and fixture SQL, review 12 outputs/Excel/6-month chart unchanged, local/staging check safely, write security-report.md. | SEC-T-18 and relevant tests PASS; residual risks documented. | BLOCKED |

## Working order and explicit code-touch boundaries

- **Database:** read-only role discovery first; no GRANT/REVOKE/CREATE in BSS without separate authorization.
- **Backend:** auth/throttle; resource budgets; error/log auditing; tests first.
- **Frontend:** only changes needed to satisfy security requirements (not wholesale redesign); no new login.
- **Infrastructure:** production host, HTTPS/cookies, non-root runtime and rate limiting after ops review.
- **Integration:** existing CI and security tests both PASS; source and runtime evidence separate.

## Definition of Done for every task

1. Requirement and finding linked; source/test evidence documented.
2. Plan and requested approval recorded.
3. Baseline test run captured (RED/BASELINE when safe, not compulsory to create a vulnerability).
4. Minimal code/config change reviewed with backup/rollback guidance.
5. Security test and relevant existing tests PASS **in authorized environment**.
6. No changes to BSS source and no change to confirmed financial semantics.
7. Update tasks, security-report and PROJECT_STATUS.md **after acceptance**, not on initial draft.

## Current verification evidence

- Source inspected: main commit 67523a6d; audit baseline recorded in spec.md.
- Historical CI: 42791814572 Java/React+PostgreSQL fixture PASS, **not a security test**.
- No code/config modifications; no DB runtime probes or penetration test.
- **PROJECT_STATUS.md:** available in supplied docs.zip but **absent from Git main**. Do not replace the historic status file with unrelated content; approved handover update will be prepared only with correct canonical source.
- This documentation branch must not be mistaken for task completion. All implementation tasks remain BLOCKED pending review.

**GATE:** No GREEN SECURITY TEST = NOT DONE.
