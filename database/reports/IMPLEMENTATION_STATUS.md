# Phase 1 — triển khai báo cáo Stored Functions (10/11/2026)

## Trạng thái và nguồn

- **Branch:** `feature/phase1-stored-reports` (tạo từ `main` merge PR #1).
- **Quyết định kiến trúc đã xác nhận:** PostgreSQL Stored Functions xử lý nghiệp vụ; Spring JDBC gọi hàm cố định. Không chuyển sang thực thi SQL tùy ý từ browser.
- **Nguồn:** `docs.zip` 04/11/2026, `SRS_I1.md` v0.3, `LLD_REPORT_I1.md`, `DATA_SOURCE.md`; audit DB-I1 09/11/2026; trạng thái thực tế source GitHub 10/11.
- `I1.sql` đã có DDL `CREATE FUNCTION reporting.fn_i1_report(date,integer)` **CHƯA CÀI VÀ CHƯA PASS SQL**. Phụ thuộc adapter chưa được xác minh `reporting.v_i1_eligible_activations`; I.2–II.5 vẫn DISCOVERY_ONLY.
- `database/functions/01_i1_preview.sql` và `02_i1_kit_by_package_preview.sql` là preview KIT lịch sử; KHÔNG là nghiệp vụ khuyến khích chính thức. Không xóa hoặc tự cài lại.
- Frontend/backend hiện có thể trả HTTP **503** (execution disabled); 503 sau sửa CSRF là tình huống bảo vệ hiện tại, KHÔNG chứng minh SQL tài chính hoạt động.

## Đã triển khai vào source (DESIGN / STATIC ONLY)

1. `ReportFunctionRegistry.java`: allowlist 12 mã I.1–II.5 với tên `reporting.fn_*_report` và tệp canonical. **Tên dự kiến, chưa có hàm DB.**
2. `I1OfficialContract.java`: 11 cột kết quả chính: STT, Tên TĐL, Mã gói, Đơn giá, Số active, Tổng active TĐL, Mức đạt, Tổng tiền, Trước VAT, VAT, Thiếu giá. Grain = đại lý × mã gói (người phát triển đã xác nhận). Chưa có số liệu điền vào.
3. `OfficialReportContractsTest.java`: kiểm tra registry đóng 12 mã, không cho tên tự do, và thứ tự 11 cột.

**ĐÃ KẾT NỐI** `ReportRepository`/`ReportService` tới `reporting.fn_i1_report`; đã xóa đường chạy preview khỏi Java, giữ function preview lịch sử. `reporting.execution-enabled=false` giữ mặc định. Không bật trước khi adapter, số liệu và SQL thật được kiểm chứng. React I.1 đã dùng status IMPLEMENTING và không gửi previewMode.

## I.1 — những điểm chặn bắt buộc (chưa tự điền)

| Vấn đề nghiệp vụ | Nội dung đối chiếu khi lập trình |
|---|---|
| Thưởng cố định | 1.500đ/3.000đ cộng hay thay %, mức cao nhất hay cộng dồn và hiệu lực |
| Điều kiện active | KIT / MSISDN / event đủ điều kiện; hủy, chuyển đại lý |
| Giá lịch sử | Giá lịch sử, giá đã/chưa VAT và thời điểm chính sách |
| Làm tròn | Quy tắc làm tròn, cấp làm tròn VAT/tiền |
| Đại lý đặc biệt | Đại lý 0 active, đại lý thử nghiệm |
| Bộ lọc gói | Khi lọc gói, mẫu số xét ngưỡng có thay đổi không |
| DB-I1 / DATA | Ngữ nghĩa cột Thiếu giá, xử lý thiếu giá, nguồn đủ điều kiện |

**Đã chốt:** I.1 11 cột và đơn vị dòng đại lý × mã gói. Có tài liệu 0/5/10% và VAT 8% trong SRS; chưa suy ra quy tắc tài chính hoàn chỉnh từ hai con số này. Dữ liệu kiểm chứng lịch sử 10/2026: 4.136 KIT, 6 đại lý, 17 nhóm; **không** mặc định đó là số thuê bao đủ điều kiện hưởng thưởng.

## Gate tiếp theo

1. Quyết định I.1 về nguồn đếm, giá, VAT, thưởng bổ sung và ngoại lệ được xác nhận; có expected outputs nghiệp vụ.
2. Review và kiểm thử tĩnh SQL `reporting.fn_i1_report(date,integer)` mới đã viết; xác định/kiểm chứng adapter đủ điều kiện và giá trước khi xin phép áp dụng DDL tại local (không viết BSS).
3. Repository và Service đã gọi hàm chính thức bằng bind params; status I.1 = IMPLEMENTING, chưa READY. Cần PASS SQL local và JSON/Excel để nâng trạng thái.
4. Đối chiếu 11 cột React/Excel, giá trị tiền, phân trang, trường hợp ngoại lệ; nghiệm thu nghiệp vụ độc lập.

## Kiểm thử / tiến độ cần cập nhật PROJECT_STATUS.md

- **SEC-ADMIN:** local POST I.1 trả HTTP 503 (không còn 403) [USER-VERIFIED], merge PR #1 lên `main` tại `7c1834a`; chưa xác nhận cloud.
- **REPORT-I1:** hợp đồng 11 cột, registry, Stored Function `I1.sql`, đường JDBC/Service/React và JUnit gate/export tests [COMMITTED; LOCAL TEST NOT YET RUN]. SQL/DB runtime CHƯA PASS; adapter eligible/giá chưa xác định/kiểm chứng; không bật thực thi, chưa nghiệm thu. Preview smoke PASS lịch sử được bảo lưu.
- **REPORT-I2–II5:** registry tĩnh [COMMITTED], SQL chỉ khám phá metadata [DISCOVERY_ONLY]; thiếu mapping và business approvals.
- **DB:** chưa chạy DDL, chưa sửa dữ liệu nguồn.
