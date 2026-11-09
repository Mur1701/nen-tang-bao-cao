# Pottel — SQL sources cho 12 báo cáo Phase 1 (09/11/2026)

**Trạng thái: DB SOURCE CATALOG DRAFT / DISCOVERY_ONLY. KHÔNG PHẢI 12 BÁO CÁO ĐÃ CÓ LOGIC TÀI CHÍNH.**

Quy tắc source mới: `database/reports/I1.sql` ... `database/reports/II5.sql`, **một file SQL canonical cho mỗi báo cáo**. Mỗi file chứa đầu vào, grain, các cột Phase 1, blockers nghiệp vụ, đề xuất tên Stored Function chính và truy vấn metadata **chỉ đọc** để xác minh nguồn DB. Chạy một file SQL lúc này không tạo Stored Function và không trả kết quả báo cáo tài chính.

**Tại sao không thêm ngay SQL tính toán?** Phase 1/SRS/LLD vẫn để mở nguồn dữ liệu vật lý của I.2–II.5, điều kiện thuê bao, chi trả hoa hồng, lịch phân bổ, giá lịch sử và VAT. Tự viết công thức sẽ tạo ra số tiền không có căn cứ. Cấu trúc file đã sẵn sàng để bổ sung hàm khi những quyết định đó được xác nhận.

**Nguồn:** `docs.zip` bản 04/11/2026: `SRS_I1..II5.md`, `LLD_REPORT_I1..II5.md`, `DECISION_REGISTER.md`, `REPORT_LIST.md`; handover 06/11. DB-I1 local đã PASS KIT 4.136 / 6 đại lý, 17 cặp đại lý × mã gói, API/Excel preview PASS (09/11); không tái chạy.

## Danh sách 12 báo cáo

| File canonical | Mã | Stored Function chính dự kiến [PROPOSED] | Trạng thái |
|---|---|---|---|
| `database/reports/I1.sql` | I.1 | `reporting.fn_i1_report` | Preview KIT SQL/REST đã PASS local; tài chính TBD |
| `database/reports/I2.sql` | I.2 | `reporting.fn_i2_report` | SRS/LLD DRAFT; physical mapping/finance TBD |
| `database/reports/I3.sql` | I.3 | `reporting.fn_i3_report` | SRS/LLD DRAFT; physical mapping/finance TBD |
| `database/reports/I4.sql` | I.4 | `reporting.fn_i4_report` | SRS/LLD DRAFT; physical mapping/finance TBD |
| `database/reports/I5.sql` | I.5 | `reporting.fn_i5_report` | SRS/LLD DRAFT; physical mapping/finance TBD |
| `database/reports/I6.sql` | I.6 | `reporting.fn_i6_report` | SRS/LLD DRAFT; physical mapping/finance TBD |
| `database/reports/I7.sql` | I.7 | `reporting.fn_i7_report` | SRS/LLD DRAFT; physical mapping/finance TBD |
| `database/reports/II1.sql` | II.1 | `reporting.fn_ii1_report` | SRS/LLD DRAFT; physical mapping/finance TBD |
| `database/reports/II2.sql` | II.2 | `reporting.fn_ii2_report` | SRS/LLD DRAFT; physical mapping/finance TBD |
| `database/reports/II3.sql` | II.3 | `reporting.fn_ii3_report` | SRS/LLD DRAFT; physical mapping/finance TBD |
| `database/reports/II4.sql` | II.4 | `reporting.fn_ii4_report` | SRS/LLD DRAFT; physical mapping/finance TBD |
| `database/reports/II5.sql` | II.5 | `reporting.fn_ii5_report` | SRS/LLD DRAFT; physical mapping/finance TBD |

## Lưu ý giữ nguyên I.1

GitHub snapshot `c13f2e0` có `database/functions/01_i1_preview.sql`; máy Windows đã áp dụng patch `02_i1_kit_by_package_preview.sql` và cài Stored Function preview mới. Đây là **hai SQL preview lịch sử đã PASS**, không xóa/sửa/ghi đè. `database/reports/I1.sql` là mục canonical chuẩn hóa tài liệu và nơi phát triển SQL tài chính tương lai; **chưa được phép gỡ bản SQL preview cũ**. Khi quyết định tài chính I.1 được chốt, sẽ chuyển phần entrypoint production sang I1.sql và lưu các preview như migration lịch sử, không deploy lặp.

## Cách tích hợp backend sau khi SQL thực sự hoàn chỉnh

- `ReportCatalog`: vẫn 12 mã I.1–II.5 đã có; thay status PLANNED/PREVIEW chỉ sau khi SQL và API thật PASS.
- `ReportRepository`: allowlist code → function cố định (ví dụ `I.2` → `reporting.fn_i2_report`), tham số hóa bằng Spring JDBC; tuyệt đối không nhận tên bảng/hàm từ frontend.
- `ReportService` + Excel: dữ liệu nguồn từ SQL PostgreSQL, Java không tái tính tài chính. Dùng API/React/Excel hiện có; chỉ mở endpoint từng báo cáo sau gate.
- Các group header theo ngày, số tiền, VAT và rounding phải theo quyết định đã duyệt.

## Cách áp dụng gói vào source Windows

1. Giải nén ZIP bên ngoài repository, ví dụ `D:\Dowloads`.
2. Chạy `APPLY_REPORT_SQL_CATALOG.ps1` từ thư mục đã giải nén. Script kiểm tra repository, hash từng file, preview danh sách, yêu cầu xác nhận và **chỉ copy file mới**. Nếu bất kỳ đích nào đã tồn tại script từ chối ghi đè.
3. Không chạy file `.sql` vào DB production hoặc Neon. Chỉ khi phân tích DB một report, có thể chạy file tương ứng bằng `psql -X -v ON_ERROR_STOP=1 -f ...` trên bản local được phép; nó chỉ đọc catalog.
4. Script không commit, push, `git clean`, migrate hay sửa DB.

## Tiếp theo

- **DB-I2-01:** chạy discovery của `I2.sql` để có metadata candidate, sau đó audit khóa nguồn (chỉ đọc, aggregate không có MSISDN cá nhân). Cần xác nhận eligibility đồng nhất I.1.
- Nhóm I.3–I.7 cần nguồn chi trả và quyết định DEC-ALLOC-01 trước SQL giá trị tiền.
- Nhóm II.1–II.5 cần event active gói, nguồn giá thực tế, chu kỳ và VAT; II.5 còn doanh thu Profile SIM / hòa mạng.
- Cập nhật `PROJECT_STATUS.md` theo trạng thái khác nhau: SQL catalog source CREATED; data-source mapping PENDING; runnable financial stored function PENDING; API/Excel acceptance PENDING. 
