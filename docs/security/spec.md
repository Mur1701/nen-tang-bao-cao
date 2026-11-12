# SECURITY SPEC — Pottel Reporting System

**Phiên bản:** 0.1 — REVIEW REQUIRED
**Ngày:** 11/11/2026
**Audit base:** Mur1701/nen-tang-bao-cao, main @ 67523a6d5d70e0ff46af9d361816e11b3149d356
**Phạm vi:** Phase 1, 12 báo cáo I.1–I.7/II.1–II.5, ứng dụng React/Vite, Java 21/Spring Boot 4.1.2, Spring JDBC/PostgreSQL 18, REST, XLSX, xác thực admin BSS.
**Giới hạn:** Đây là **source-based security audit**. Không quét chủ động, không dùng dữ liệu thuê bao thật, không xác minh môi trường production/Render/Vercel/DB runtime. Không tuyên bố đã khai thác lỗ hổng. Chưa phê duyệt sửa code.

## 0. Phạm vi nguồn và quy tắc đánh giá

- **Code đang chạy theo Git:** nhánh main tại commit trên. Source được đọc trực tiếp: security/auth, REST/report/Excel, SQL stored functions, application YAML, Dockerfile, Vite/Vercel, npm/Maven và CI.
- **Tài liệu nghiệp vụ/thiết kế:** docs.zip / docs_without_decision_register.zip cung cấp trong Project, gồm PROJECT_OVERVIEW, PROJECT_STATUS, PROJECT_HANDOVER, SRS_COMMON, FRONTEND_ARCHITECTURE, DATA_SOURCE, API_CONTRACT và LLD. **Những bản 04/11 là snapshot lịch sử**, không mặc nhiên phản ánh main ngày 10/11. Các file docs đó không có trong Git tree của main.
- **Đối chiếu trước đây:** SPEC_AUDIT_2026-11-04.md và thiết kế an ninh 09/11 là ngữ cảnh lịch sử; quyết định hiện hữu ưu tiên source main và yêu cầu đã xác nhận.
- **Repo/tên đường dẫn cập nhật:** Mur1701/nen-tang-bao-cao, D:\Github\nen-tang-bao-cao. Tên report-management-system trong prompt gốc là đường dẫn cũ.
- **Bằng chứng CI:** workflow phase1-reference-ci.yml, run 42791814572: Java/React và PostgreSQL fixture PASS. Đây là **functional CI**, không phải penetration test hoặc security verification.
- **Phân loại:** VERIFIED = chứng minh được bằng source hoặc kết quả test tương ứng (nêu rõ static/runtime); SUSPECTED = có cơ sở nhưng thiếu kiểm chứng; PROPOSED = biện pháp; NOT VERIFIED = chưa kiểm chứng; NOT APPLICABLE = không có surface hiện hữu được tìm thấy. “VERIFIED (static)” không đồng nghĩa có bằng chứng khai thác.

## 1. Security baseline / kiến trúc thực tế

### 1.1. Luồng tin cậy và tài sản

Luồng triển khai được cấu hình: Browser → same-origin /api qua Vite dev proxy hoặc Vercel rewrite → Spring Boot API (Render URL trong frontend/vercel.json) → PostgreSQL Stored Functions / các bảng BSS. **Kết nối DB từ môi trường Render và các chính sách mạng/secret production: NOT VERIFIED.**

Tài sản: mật khẩu và phiên admin BSS; CSRF token; MSISDN và danh tính đại lý; dữ liệu doanh thu, chi phí, VAT và ngày thanh toán; file XLSX; schema BSS và quyền DB; tài nguyên CPU/RAM/DB; log/audit và secret cấu hình.

Trust boundaries: (TB1) trình duyệt/Internet ↔ API; (TB2) session admin ↔ mỗi report/export và quyền đại lý; (TB3) Java/SQL ↔ BSS PostgreSQL; (TB4) backend ↔ proxy/TLS/đơn vị lưu trữ session; (TB5) XLSX ↔ máy người mở và công cụ spreadsheet; (TB6) dependency/CI ↔ artifact triển khai.

Mục tiêu:
- **Confidentiality:** không rò MSISDN, dữ liệu tiền, mật khẩu/token, nội dung báo cáo hoặc file Excel cho người không được phép.
- **Integrity:** không ghi dữ liệu nguồn BSS, không bị injection/altered input, không đổi công thức/ngày/kỳ/vat/precision và biểu mẫu.
- **Availability:** truy vấn/đăng nhập/Excel không cho một client làm cạn DB, heap hoặc khóa login toàn hệ thống.
- **Accountability:** biết ai, lúc nào, loại báo cáo/kỳ nào được xem/xuất và kết quả, không ghi MSISDN/secret vào log.

### 1.2. REST attack surface (source-verified)

| Endpoint | Method | Bảo vệ từ SecurityConfig | Payload / tác động |
|---|---|---|---|
| /api/health | GET | Public | Health liveness; không query DB |
| /api/auth/csrf | GET | Public | Sinh/trả CSRF token thông qua session |
| /api/auth/login | POST | Public, nhưng CSRF + throttle | username/password form |
| /api/auth/logout | POST | Authenticated + CSRF | Hủy session |
| /api/auth/me | GET | ROLE_ADMIN | Danh tính admin |
| /api/health/db | GET | ROLE_ADMIN | DB SELECT 1 |
| /api/reports và /api/reports/schemas | GET | ROLE_ADMIN | Catalog/metadata |
| /api/reports/{code}, /api/reports/{code}/schema | GET | ROLE_ADMIN | Metadata theo mã |
| /api/reports/{code}/run | POST | ROLE_ADMIN + CSRF | month, agencyId, page, pageSize, previewMode → đọc BSS |
| /api/reports/{code}/excel | POST | ROLE_ADMIN + CSRF | Cùng filter → toàn bộ tập lọc XLSX, 5.000 dòng tối đa |
| /api/reports/{code}/trend (II.1–II.5) | POST | ROLE_ADMIN + CSRF | 6 tháng → nhiều lần Stored Function, tổng hợp DB |

Không thấy endpoint upload file, chạy lệnh hệ điều hành, proxy URL tùy ý, cập nhật dữ liệu BSS hoặc mở URL từ server trong các controller đang xét. Các surface này **NOT APPLICABLE cho phạm vi API đã đọc**, không phải chứng nhận toàn bộ hạ tầng.

### 1.3. Kiểm soát bảo mật hiện hữu — không xóa khi hardening

| ID | Bằng chứng | Kết luận |
|---|---|---|
| BL-01 | SecurityConfig.securityFilterChain: denyAll, ROLE_ADMIN cho /api/reports/** và /api/health/db | VERIFIED (static): authorization ở backend; không chỉ ẩn UI |
| BL-02 | SecurityConfig: HttpSessionCsrfTokenRepository và CsrfTokenRequestAttributeHandler; authApi.ts gọi CSRF cho POST | VERIFIED (static): có cơ chế CSRF |
| BL-03 | SecurityConfig: changeSessionId(), httpBasic disabled; application.yml: HttpOnly, Secure=true mặc định, SameSite=Lax, idle timeout 20m | VERIFIED (static): có session controls; cookie HTTPS runtime NOT VERIFIED |
| BL-04 | BssAdminUserDetailsService: kiểm tra role admin, khóa, type, securityVersion; AdminSessionValidationFilter.stillActiveAdmin | VERIFIED (static): fail-closed revocation path |
| BL-05 | ReportRepository.referenceFrom / ReportFunctionRegistry.require: function identifier từ allowlist; các giá trị SQL được bind | VERIFIED (static): không thấy SQL identifier trực tiếp từ user |
| BL-06 | PostgreSQL report SQL: SECURITY INVOKER, fixed search_path, REVOKE EXECUTE FROM PUBLIC; .gitignore loại trừ .env, local.yml, key/dump | VERIFIED (static): có defense-in-depth; quyền thực tế NOT VERIFIED |
| BL-07 | ReportService: pageSize 1..500, export <=5000; @Transactional(readOnly=true); XLSX POI dùng setCellValue(String) cho text | VERIFIED (static): có giới hạn output; hiệu năng/security của Excel cần test riêng |
| BL-08 | React: hiển thị dữ liệu bằng JSX và Ant Design; không thấy dangerouslySetInnerHTML trong các component report/auth đã đọc | VERIFIED (static): mặc định escape text; XSS end-to-end NOT VERIFIED |

## 2. Threat model theo STRIDE

| Threat | Điều kiện | Tác động | Surface / trạng thái |
|---|---|---|---|
| Spoofing | Sai cấu hình hoặc đánh cắp session admin | Đọc/xuất báo cáo | Auth/session — kiểm soát có sẵn, runtime NOT VERIFIED |
| Tampering | Input report/SQL ngoài allowlist; XLSX mở bởi công cụ khác | Sai kết quả hay dữ liệu xuất | JDBC/Excel — SQL allowlist VERIFIED, hành vi Excel mở file NOT VERIFIED |
| Repudiation | Không có business audit sự kiện export/run | Không truy dấu truy cập dữ liệu | ReportController/Service — thiếu audit app chuyên biệt trong source |
| Information disclosure | Chạy report không đúng phạm vi hoặc lỗi trả chi tiết | Rò MSISDN/doanh thu/DB info | Authorization/error/DB — production NOT VERIFIED |
| DoS | Truy vấn rất nặng, tạo nhiều phiên, khóa login toàn cục | Gián đoạn backend/DB | AdminLoginThrottle, report functions, trend — rủi ro static được chỉ ra |
| Elevation of privilege | DB role có quyền thừa, role admin quá rộng khi mở rộng người dùng | Thay đổi BSS hoặc vượt phạm vi đại lý | Datasource/GRANT/scopes — cần DB permission check |

Không suy đoán danh tính threat actor: Internet client chưa xác thực; admin hợp lệ dùng report nặng; tài khoản admin bị chiếm; người có thể thay đổi dữ liệu nhập vào BSS; CI/dependency bị xâm phạm.

## 3. Vulnerability assessment — findings có bằng chứng

### SEC-F-01 — Khóa login do bộ đếm toàn cục đầy (HIGH nếu API public)

- **Trạng thái:** VERIFIED (static logic); runtime/exploit NOT VERIFIED.
- **Thành phần:** backend/src/main/java/vn/pottel/reporting/auth/AdminLoginThrottle.java, methods fail(), doFilterInternal().
- **Root cause:** fail() giới hạn MAX_KEYS=10000; doFilterInternal() trả 429 cho **mọi** login khi attempts.size() >= MAX_KEYS, không chỉ khóa đã vi phạm.
- **Điều kiện:** số khóa username/IP khác nhau tích lũy đạt MAX_KEYS trong một instance. Source dùng getRemoteAddr (khác biệt proxy deployment chưa xác minh). Không thực hiện request flood để kiểm chứng.
- **Tác động:** từ chối đăng nhập admin hợp lệ; khả năng DoS ứng dụng. Mức HIGH khi public, MEDIUM trên máy dev cô lập.
- **Yêu cầu:** SEC-AUTH-001, SEC-DOS-001. **Acceptance:** full counter capacity không làm chặn user hợp lệ khác; 5 lần sai bị giới hạn theo policy; không có khóa vĩnh viễn; test giả lập không mạng.

### SEC-F-02 — Chi phí tính toán không bị chặn bởi giới hạn số dòng (HIGH nếu truy cập Internet)

- **Trạng thái:** VERIFIED (static call graph), impact/DB latency NOT VERIFIED.
- **Thành phần:** ReportService.reference()/exportData(); ReportRepository.countReference()/referenceJson(); RevenueTrendController.trend(); SQL 00_PHASE1_REFERENCE_CORE.sql (CTE first_package_event nhóm dữ liệu CDR trước lọc tháng), 01_PHASE1_LEGACY_REPORTS.sql.
- **Root cause:** count(*) thực thi report function rồi trang kết quả lại thực thi lần nữa; trend chạy 6 tháng từ stored function; không thấy per-request statement_timeout hay concurrency/rate limit cho /run, /excel, /trend; SET LOCAL statement_timeout=120s ở script **cài DDL**, không tự áp dụng khi gọi function.
- **Điều kiện:** admin/session hợp lệ hoặc bị chiếm gửi truy vấn liên tiếp; DB nhiều dữ liệu. Query chưa benchmark.
- **Tác động:** DB/CPU/RAM bị chiếm; ảnh hưởng người khác, timeout/lỗi hệ thống.
- **Yêu cầu:** SEC-DOS-001, SEC-DOS-002, SEC-SQL-001. **Acceptance:** query timeout thật được chứng minh qua test DB giả lập; đồng thời/nhịp gọi giới hạn; không trả dữ liệu cắt âm thầm.

### SEC-F-03 — Quyền DB ứng dụng và kết nối local (HIGH nếu DB writer/superuser)

- **Trạng thái:** SUSPECTED đối với triển khai; NOT VERIFIED quyền thực tế của DB/Render.
- **Thành phần:** application-local.example.yml, security/project handover; SQL SECURITY INVOKER và REVOKE FROM PUBLIC.
- **Root cause / evidence:** cấu hình mẫu chỉ đặt Hikari read-only=true, không áp đặt least privilege DB; thiết lập local trước đây có user postgres; không có kết quả \du / role grants của runtime. Connection readOnly không thay quyền SQL của DB.
- **Điều kiện:** credential có quyền ghi hoặc superuser bị lạm dụng trong runtime.
- **Tác động:** đọc vượt phạm vi hay thay đổi dữ liệu BSS dù mục tiêu app là chỉ đọc.
- **Yêu cầu:** SEC-DB-001. **Acceptance:** có output role privilege metadata an toàn, role không superuser/BYPASSRLS/CREATE DML, chỉ EXECUTE/SELECT cần thiết; giao dịch BSS chỉ đọc.

### SEC-F-04 — Thiếu business audit truy cập và xuất báo cáo (MEDIUM)

- **Trạng thái:** VERIFIED (static: chưa thấy audit app trong controller/service hiện tại); central audit hạ tầng NOT VERIFIED.
- **Thành phần:** ReportController, RevenueTrendController, ReportService, Auth success/failure handlers.
- **Root cause:** không có structured event cho actor/reportCode/kỳ/export/kết quả/requestId; HealthController log lỗi DB nhưng không thay audit truy cập.
- **Điều kiện:** sử dụng báo cáo bình thường hoặc account bị chiếm.
- **Tác động:** khó điều tra access bất thường/tải Excel nhạy cảm.
- **Yêu cầu:** SEC-AUDIT-001. **Acceptance:** phát sinh event không chứa MSISDN/hash/password/CSRF; có requestId, hành động, actor, report, status và số dòng.

### SEC-F-05 — Kiểm soát môi trường & public deployment chưa chốt (MEDIUM/HIGH tùy triển khai)

- **Trạng thái:** NOT VERIFIED runtime; VERIFIED (static config evidence).
- **Thành phần:** backend application.yml chỉ có server.port=8081 (không ghi server.address loopback); frontend/vercel.json rewrite /api tới URL Render; backend/Dockerfile chạy mặc định không định danh non-root; cấu hình TLS/host header/policy tại Render/Vercel chưa kiểm tra.
- **Root cause:** một số chính sách chỉ có thể kiểm chứng ở hạ tầng.
- **Điều kiện:** máy local bind rộng ngoài firewall, Render direct API, thiếu TLS hoặc secret policy.
- **Tác động:** tăng attack surface / quyền container.
- **Yêu cầu:** SEC-CONFIG-001, SEC-SESSION-001, SEC-DB-001. **Acceptance:** cấu hình local loopback (trừ khi cố ý), production HTTPS/cookie, non-root process khi khả thi, Render direct host cũng yêu cầu auth; chứng cứ runtime theo từng môi trường.

### SEC-F-06 — Phản hồi lỗi và HTTP security headers chưa test đầy đủ (MEDIUM)

- **Trạng thái:** NOT VERIFIED runtime; static cho thấy không khai báo CSP chuyên biệt, không có global API error contract.
- **Thành phần:** SecurityConfig, ReportController.get(), HealthController, frontend/index.html, Vercel/Render config.
- **Root cause:** thiếu test response header/problem JSON và tránh lộ chi tiết ngoại lệ.
- **Điều kiện:** lỗi validation/DB/timing hoặc phản hồi trực tiếp qua proxy.
- **Tác động:** lộ cấu hình hoặc giảm bảo vệ chống script injection nếu các lớp khác có lỗi.
- **Yêu cầu:** SEC-ERROR-001, SEC-HEADER-001. **Acceptance:** 400/401/403/422/500 không trả stack/SQL/credentials; CSP compatible React/AntD; header xác minh trên host thực.

### SEC-F-07 — Formula handling trong XLSX cần test file thực (MEDIUM nếu downstream tái diễn giải)

- **Trạng thái:** SUSPECTED/NOT VERIFIED, **chưa chứng minh Excel Formula Injection**.
- **Thành phần:** ReportExcelExporter.toXlsx().
- **Evidence:** các ô text dùng POI setCellValue(String), không dùng setCellFormula trong exporter, đây là hành vi phòng vệ có lợi. Chưa có test độc hại cho MSISDN/name bắt đầu =, +, @ hoặc khi file được chuyển thành CSV.
- **Điều kiện:** nội dung BSS chưa tin cậy được mở/chuyển đổi trong spreadsheet.
- **Tác động:** đánh giá nguy cơ spreadsheet formula tùy consumer.
- **Yêu cầu:** SEC-EXPORT-001. **Acceptance:** giải nén/đọc XLSX kiểm tra cell type STRING, không có formula cell, text giữ nguyên, định dạng và tiền không đổi.

### SEC-F-08 — Kiểm chứng độc lập dependency / supply chain còn thiếu (MEDIUM)

- **Trạng thái:** VERIFIED (static CI absence), CVE cụ thể NOT VERIFIED.
- **Thành phần:** backend/pom.xml; frontend/package-lock.json; .github/workflows/phase1-reference-ci.yml; backend/Dockerfile.
- **Evidence:** CI chạy Maven tests, React build và PG fixture; chưa có SCA/SBOM/vulnerability gate. GitHub Actions dùng tag @v4, base images dạng tag, không digest (đây là rủi ro pinning, chưa chứng minh bị thay thế).
- **Yêu cầu:** SEC-DEP-001. **Acceptance:** report scan SCA cho npm/Maven và container, policy triage phát hiện, không log secrets.

### SEC-F-09 — CSRF/session/resource behavior ở proxy chưa xác minh (MEDIUM)

- **Trạng thái:** NOT VERIFIED runtime.
- **Thành phần:** SecurityConfig, AuthController.csrf, authApi.ts, Vercel rewrite, Render session.
- **Evidence:** HttpSessionCsrfTokenRepository cấp token qua GET public; CSRF protection được cấu hình; chưa có test cookie Secure/SameSite/HttpOnly qua **cả** Vercel proxy và Render hostname, cũng chưa kiểm tra tạo session đồng thời giới hạn.
- **Yêu cầu:** SEC-CSRF-001, SEC-SESSION-001, SEC-DOS-002. **Acceptance:** request POST không/CSRF sai bị 403, token đúng + session đúng thành công; anonymous GET csrf không phát triển session ngoài mức chấp nhận.

### SEC-F-10 — Phạm vi quyền xem/xuất theo đại lý chưa có policy chi tiết (MEDIUM khi mở cho non-admin)

- **Trạng thái:** NOT APPLICABLE như IDOR hiện tại nếu mọi ROLE_ADMIN được phép đọc toàn BSS; policy “admin được đọc tất cả đại lý” cần xác nhận trước production.
- **Thành phần:** SecurityConfig (ROLE_ADMIN), ReportService normalize(agencyId), Stored Function p_agency_id.
- **Evidence:** không có per-agency grants và split VIEW/EXPORT; hệ thống hiện chỉ cho admin. **Không đủ căn cứ kết luận IDOR đã tồn tại**.
- **Yêu cầu:** SEC-ACCESS-001. **Acceptance:** mô hình quyền môi trường xác nhận; nếu yêu cầu scope, API luôn kiểm quyền phía backend và export độc lập, kể cả agencyId=null.

## 4. Ma trận 15 nhóm rủi ro theo yêu cầu audit

| Nhóm | Kết quả |
|---|---|
| SQL Injection | Không thấy user-controlled identifier; bind/allowlist hiện hữu; runtime fuzz NOT VERIFIED |
| XSS | JSX escape mặc định; chưa test hostile BSS data, CSP chưa xác minh |
| CSRF | Token/session hiện hữu; negative/runtime tests cần bổ sung |
| Broken Access / IDOR | Admin-only hiện hữu; agency-permission policy TBD |
| Authentication / Session | Fail closed cho admin, nhưng throttle có rủi ro F-01 |
| Misconfiguration | Local bind/proxy/TLS/runtime role cần xác minh F-03/F-05 |
| Sensitive Data Exposure | Auth protection hiện hữu; export scope/log/caching NOT VERIFIED |
| SSRF | NOT APPLICABLE trong server controllers được đọc (không có arbitrary URL fetch) |
| File Handling | Không thấy upload API; export XLSX có, kiểm tra workbook F-07 |
| Command Injection | NOT APPLICABLE ở API handlers được đọc (không exec command) |
| Supply Chain | Thiếu SCA gate F-08 |
| DoS / Resource | F-01/F-02/F-09 |
| Business Logic Abuse | Không sửa công thức; test authorization scope, price/data consistency riêng |
| Excel/CSV Formula | F-07: chưa chứng minh lỗ hổng; export XLSX chứ không có CSV endpoint |
| Unsafe Errors/Logging | F-04/F-06 |

## 5. Security requirements — SHALL + tiêu chí nghiệm thu

| ID | Yêu cầu kiểm chứng được | Acceptance criteria |
|---|---|---|
| SEC-AUTH-001 | Login throttle theo actor/key phải fail-safe khi đạt dung lượng | Không khóa admin hợp lệ không vi phạm; 429 đúng cho khóa bị giới hạn, hết hạn theo window |
| SEC-AUTH-002 | Admin bị khóa/xóa quyền/bump version phải mất truy cập báo cáo | 401 cho phiên bị revoke; không mất dữ liệu/không đổi BSS |
| SEC-API-001 | Tất cả endpoints báo cáo kể cả trend/export bắt buộc auth backend | Anonymous 401, non-admin 403; public health và CSRF theo policy |
| SEC-ACCESS-001 | Quyền report/export/agency phải được định nghĩa rõ theo môi trường | Matrix được review; nếu role chỉ admin thì hành vi phản ánh policy; nếu mở rộng phải test negative agency scope |
| SEC-CSRF-001 | POST dựa trên session cookie cần CSRF valid | Missing/invalid 403; valid token + valid session PASS |
| SEC-SQL-001 | Chỉ function code allowlist; mọi giá trị query tham số hóa | Input bất thường 4xx; không thay cấu trúc SQL/DB; test không destructive |
| SEC-DOS-001 | Không client nào làm khóa login toàn hệ thống | Unit test tái hiện full-map và quá ngưỡng; không có permanent global deny |
| SEC-DOS-002 | Run/export/trend có ngân sách tài nguyên/thời gian và hạn concurrency | Bounded timeout thực tế; giới hạn song song và export hợp lý; test fixture/isolated DB, tránh stress production |
| SEC-DB-001 | Role DB least privilege, BSS read-only, secret không hardcode | Runtime DB role/GRANT chứng minh no superuser/DML; không ghi nguồn BSS |
| SEC-EXPORT-001 | XLSX không cho text thành formula và không sai nghiệp vụ | Công thức trong file = 0; MSISDN số 0 đầu, tiền, ngày và số dòng đúng |
| SEC-ERROR-001 | HTTP error không rò SQL, stack, internal host, secret | Kiểm tra 4xx/5xx JSON/headers với dữ liệu giả lập; server log đủ diagnostic đã redacted |
| SEC-HEADER-001 | Browser response headers phù hợp SPA và proxy | CSP/X-Content-Type-Options/frame-policy/cache policy được kiểm chứng không phá SPA |
| SEC-SESSION-001 | Cookie/sessions an toàn qua proxy và direct host | Secure/HttpOnly/SameSite, session rotation, revoke/logout/timeout PASS trong môi trường kiểm thử |
| SEC-AUDIT-001 | Run/export/trend/login có dấu vết cần thiết, không chứa PII | Audit event theo actor/report/action/resultId/status; không log raw MSISDN/password/CSRF |
| SEC-CONFIG-001 | Tách local/internal/production, mạng và secrets được kiểm chứng | Local không tự expose LAN; production TLS + deny-by-default, host direct cũng protected |
| SEC-DEP-001 | Dependency và build chain có cơ chế phát hiện, đánh giá lỗ hổng | Maven/npm/container checks với SBOM/review policy, không auto-upgrade gây phá nghiệp vụ |
| SEC-REGRESS-001 | Hardening không thay đổi kết quả kế toán/contract | Functional CI hiện hữu PASS; so sánh JSON, Excel, filter, VAT/rounding fixture |

## 6. GATE 1 / phạm vi phê duyệt

- **GATE 1:** Security requirements trên cần người phát triển xác nhận trước khi thay đổi code/cấu hình. Mức khẩn cấp production của F-01/F-02 phụ thuộc xác nhận môi trường truy cập.
- **Không** triển khai login/RBAC mới trong local; không nâng ROLE_ADMIN sang quyền tự suy đoán.
- **Không** tắt CSRF, bỏ mã hóa, mở port DB, thử DoS trên production hoặc đưa dữ liệu thuê bao thật vào CI.
- **Ưu tiên review:** F-01 login availability, F-02 report resource consumption, F-03 DB privileges/deployment, rồi security regression và audit.
- **Ngoài phạm vi source audit:** live infra headers, phiên thật, quyền GRANT thực tế, dữ liệu BSS chính thức, dependency CVE scan và browser workbook evaluation: NOT VERIFIED.

## 7. Tham chiếu chuẩn

- OWASP ASVS 5.0.0: https://asvs.owasp.org/
- OWASP ASVS security logging, session, access controls.
- OWASP WSTG spreadsheet injection: https://owasp.github.io/www-project-web-security-testing-guide/latest/4-Web_Application_Security_Testing/07-Injection/21-CSV_Injection
- Tài liệu Phase 1 và Project Sources đã mô tả ở §0.

**Approval:** PENDING REVIEW. Không hàm ý đang tồn tại exploit cụ thể ngoài những điều kiện static đã nêu.
