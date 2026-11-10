# Phase 1 — triển khai báo cáo Stored Functions (10/11/2026)

## Trạng thái và nguồn

- **Branch:** `feature/phase1-stored-reports` (tạo từ `main` merge PR #1).
- **Quyết định kiến trúc đã xác nhận:** PostgreSQL Stored Functions xử lý nghiệp vụ; Spring JDBC gọi hàm cố định. Không chuyển sang thực thi SQL tùy ý từ browser.
- **Nguồn:** `docs.zip` 04/11/2026, `SRS_I1.md` v0.3, `LLD_REPORT_I1.md`, `DECISION_REGISTER.md`, `DATA_SOURCE.md`; audit DB-I1 09/11/2026; trạng thái thực tế source GitHub 10/11.
- Các file `I1.sql` … `II5.sql` hiện là **DISCOVERY_ONLY**, chưa chứa định nghĩa report chính thức. Không chạy chúng như function báo cáo.
- `database/functions/01_i1_preview.sql` và `02_i1_kit_by_package_preview.sql` là preview KIT lịch sử; KHÔNG là nghiệp vụ khuyến khích chính thức. Không xóa hoặc tự cài lại.
- Frontend/backend hiện có thể trả HTTP **503** (execution disabled); 503 sau sửa CSRF là tình huống bảo vệ hiện tại, KHÔNG chứng minh SQL tài chính hoạt động.

## Đã triển khai vào source (DESIGN / STATIC ONLY)

1. `ReportFunctionRegistry.java`: allowlist 12 mã I.1–II.5 với tên `reporting.fn_*_report` và tệp canonical. **Tên dự kiến, chưa có hàm DB.**
2. `I1OfficialContract.java`: 11 cột kết quả chính: STT, Tên TĐL, Mã gói, Đơn giá, Số active, Tổng active TĐL, Mức đạt, Tổng tiền, Trước VAT, VAT, Thiếu giá. Grain = đại lý × mã gói (**DEC-I1-07 USER_CONFIRMED**). Chưa có số liệu điền vào.
3. `OfficialReportContractsTest.java`: kiểm tra registry đóng 12 mã, không cho tên tự do, và thứ tự 11 cột.

**Không kết nối** registry/contract vào `ReportService` để tạo kết quả tài chính giả. **Không bật thực thi preview**. Khi chính thức triển khai, thay đường gọi I.1 preview trong `ReportRepository` và `ReportService` bằng `reporting.fn_i1_report` sau khi business gate PASS.

## I.1 — những điểm chặn bắt buộc (chưa tự điền)

| Quyết định | Cần được xác nhận để mã hóa tiền |
|---|---|
| DEC-I1-01, DEC-I1-02 | 1.500đ/3.000đ cộng hay thay %, mức cao nhất hay cộng dồn và hiệu lực |
| DEC-I1-03 | KIT / MSISDN / event đủ điều kiện; hủy, chuyển đại lý |
| DEC-I1-04 | Giá lịch sử, giá đã/chưa VAT và thời điểm chính sách |
| DEC-I1-05 | Quy tắc làm tròn, cấp làm tròn VAT/tiền |
| DEC-I1-06 | Đại lý 0 active, đại lý thử nghiệm |
| DEC-I1-08 | Khi lọc gói, mẫu số xét ngưỡng có thay đổi không |
| DB-I1 / DATA | Ngữ nghĩa cột Thiếu giá, xử lý thiếu giá, nguồn đủ điều kiện |

**Đã chốt:** I.1 11 cột và đơn vị dòng đại lý × mã gói. Có tài liệu 0/5/10% và VAT 8% trong SRS; chưa suy ra quy tắc tài chính hoàn chỉnh từ hai con số này. Dữ liệu kiểm chứng lịch sử 10/2026: 4.136 KIT, 6 đại lý, 17 nhóm; **không** mặc định đó là số thuê bao đủ điều kiện hưởng thưởng.

## Gate tiếp theo

1. Quyết định I.1 về nguồn đếm, giá, VAT, thưởng bổ sung và ngoại lệ được xác nhận; có expected outputs nghiệp vụ.
2. Chuyển `database/reports/I1.sql` sang `CREATE FUNCTION reporting.fn_i1_report(...)` có chữ ký, kiểu cột, biến thời gian rõ ràng. Review trước khi áp dụng DDL ở local (chỉ tạo/thay đổi schema `reporting`, không viết BSS).
3. Đưa `ReportRepository` + `ReportService` gọi hàm chính thức bằng bind params; đổi status I.1 chỉ khi function và API thực sự PASS.
4. Đối chiếu 11 cột React/Excel, giá trị tiền, phân trang, trường hợp ngoại lệ; nghiệm thu nghiệp vụ độc lập.

## Kiểm thử / tiến độ cần cập nhật PROJECT_STATUS.md

- **SEC-ADMIN:** local POST I.1 trả HTTP 503 (không còn 403) [USER-VERIFIED], merge PR #1 lên `main` tại `7c1834a`; chưa xác nhận cloud.
- **REPORT-I1:** contract 11 cột + registry Stored Functions + JUnit tests đã thêm vào source [COMMITTED; LOCAL TEST NOT YET RUN]. Không có function tài chính chính thức [BLOCKED BY BUSINESS RULES]; preview smoke đã PASS trước đây, không thay thế nghiệm thu.
- **REPORT-I2–II5:** registry tĩnh [COMMITTED], SQL chỉ khám phá metadata [DISCOVERY_ONLY]; thiếu mapping và business approvals.
- **DB:** chưa chạy DDL, chưa sửa dữ liệu nguồn.
