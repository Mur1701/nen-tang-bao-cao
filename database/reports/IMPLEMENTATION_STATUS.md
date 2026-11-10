> **TRẠNG THÁI MỚI NHẤT 10/11/2026 — THAM CHIẾU BSS ĐÃ TRIỂN KHAI, CI PASS:** 12 nhánh report SQL + 11 entrypoint cố định I.2–II.5 + API/JDBC/React/Excel đã được push. CI Maven, Vite và PostgreSQL 18 fixture đều PASS (run 42781888531). Đây là **báo cáo LEGACY_REFERENCE** áp dụng SRS và fallback SQL BSS cũ theo chỉ định của người phát triển; không phải đã nghiệm thu tài chính. Chưa cài/chưa đối chiếu trên DB local. SQL `I2.sql`–`II5.sql` vẫn là hồ sơ DISCOVERY_ONLY lịch sử; bộ SQL chạy thực tế nằm ở `00_PHASE1_REFERENCE_CORE.sql`, `00B_PHASE1_PAYMENT_SOURCE.sql`, `01_PHASE1_LEGACY_REPORTS.sql`, `02_PHASE1_ENTRYPOINTS.sql`.
>
> **Đọc `database/reports/REFERENCE_IMPLEMENTATION.md` để xem quy tắc đang dùng.** Các phần bên dưới là nhật ký lịch sử tại thời điểm ghi, không còn là trạng thái source mới nhất.

# Phase 1 — triển khai báo cáo Stored Functions (10/11/2026)

> **UX / Doanh thu — cập nhật 10/11/2026:** Người dùng chỉ thấy **một chế độ I.1** (frontend gửi `LEGACY_REFERENCE`), không có thanh chọn KIT/official gây 503; các đường KIT kiểm thử vẫn tồn tại ở backend. Xóa badge trạng thái BSS lặp, tập trung mô tả nghiệp vụ; ghi chú nguồn & kiểm chứng nằm trong mục mở rộng, không bị xóa. **II.1–II.5 đều có biểu đồ 6 mốc tháng làm chế độ xem ưu tiên**, dùng truy vấn PostgreSQL tổng hợp đủ dữ liệu theo từng report, bảng chi tiết và Excel vẫn được bảo lưu. PostgreSQL fixture có kiểm thử 5 xu hướng. Docs: `frontend/UX_STANDARD.md`. GitHub Actions CI: PASS code tại `69dd5a96e`, source mới nhất sau docs cần xác nhận run riêng. Local UI/DB chưa đối chiếu trực quan.
>

> **UX Phase 1 — 10/11/2026:** Đã cập nhật SPA dùng chung: locale Việt cho DatePicker/Day.js; formatter ngày `dd/MM/yyyy`, ngày giờ `dd/MM/yyyy HH:mm:ss`, số tiền/dấu ngăn cách theo tiếng Việt mà không tính lại; xuất Excel ngày giờ dạng native date; tìm kiếm chỉ khi Enter; bỏ dropdown chọn báo cáo lặp với sidebar; cố định appliedQuery khi xuất Excel. Đã có biểu đồ doanh thu II.5 6 tháng qua API tổng hợp toàn bộ kỳ (`/api/reports/II.5/trend`) chỉ tải khi người dùng chọn, không tổng hợp trên một trang bảng. Tài liệu: `frontend/UX_STANDARD.md`. Kết quả CI sẽ kiểm chứng riêng cho commit này; chưa xác nhận giao diện local qua trình duyệt.
>


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

**ĐÃ KẾT NỐI** `ReportRepository`/`ReportService` tới `reporting.fn_i1_report`, với `reporting.execution-enabled=false` mặc định. Đã **khôi phục đường preview KIT kỹ thuật riêng** bằng `reporting.i1-kit-preview-enabled=false` mặc định và `reporting.i1-package-preview-enabled=false`; không yêu cầu mở khóa finance. React I.1 hiển thị lựa chọn rõ ràng giữa KIT preview và tài chính chưa nghiệm thu. Chỉ bật preview local trên môi trường admin tin cậy sau khi xác minh functions cũ còn tồn tại. Không bật tài chính trước khi adapter, số liệu và SQL thật được kiểm chứng.

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
- **REPORT-I1:** hợp đồng 11 cột, registry, Stored Function `I1.sql`, đường JDBC/Service/React/Excel và JUnit gate/export tests [COMMITTED; LOCAL TEST NOT YET RUN]. SQL/DB runtime CHƯA PASS; adapter eligible/giá chưa xác định/kiểm chứng; finance không bật, chưa nghiệm thu. 10/11 UI ảnh chụp hiển thị 503 do execution disabled; đã tách preview khỏi finance để không làm mất smoke PASS lịch sử, nhưng bản sửa chưa PASS runtime. Không coi preview KIT là report chính thức.
- **REPORT-I2–II5:** registry tĩnh [COMMITTED], SQL chỉ khám phá metadata [DISCOVERY_ONLY]; thiếu mapping và business approvals.
- **DB:** chưa chạy DDL, chưa sửa dữ liệu nguồn.


## 10/11/2026 — Phase 1: xây dựng schema/layout dùng chung cho I.2–II.5

**Code đã commit (chưa chạy kiểm thử local cho bản mới):**

- `Phase1ReportSchemaCatalog.java`: metadata cố định cho đủ 12 báo cáo: tháng, đại lý chỉ I.1/I.2; cột cụ thể theo 12 SRS; nhóm cột theo ngày cho I.3/I.5/I.6/II.1/II.3/II.4; cảnh báo blockers SRS; `executable=false` để **không giả trạng thái đã hoàn thành**.
- `GET /api/reports/schemas` và `GET /api/reports/{code}/schema`: API trả schema/nhóm cột và blockers, chỉ cho admin, không gọi BSS, không thực thi báo cáo.
- React `ReportBlueprintView` lấy schema qua API cho các báo cáo chưa sẵn và giữ fallback cục bộ để hiển thị cấu trúc; không tạo dữ liệu mẫu hoặc ngày phân bổ giả.
- `ReportResult.Column.groupPath` là metadata tùy chọn cho nhóm theo ngày ở kết quả khi dữ liệu thật sẵn sàng; `ReportsWorkspace` render nhóm lồng nhau; `ReportExcelExporter` tạo header nhiều tầng/merge khi có `groupPath`; luồng 3 tham số I.1/KIT vẫn tương thích.
- `Phase1ReportSchemaCatalogTest`: 8 tests cấu trúc metadata (12 mã, filter, cột, nhóm động, gate).
- `I1ExcelExporterTest`: thêm test header 3 tầng và MSISDN giữ số 0 đầu.
- `database/reports/PHASE1_SOURCE_DISCOVERY.sql`: metadata audit **READ ONLY** để tìm nguồn ứng viên cho 11 báo cáo, KHÔNG xác nhận nguồn/khóa JOIN và KHÔNG tạo hàm.

**Bảo lưu chưa hoàn thành:**

- SQL I.2–II.5 vẫn `DISCOVERY_ONLY`. Chưa có Stored Functions tính toán đã kiểm chứng, chưa có report run/excel thật; backend tiếp tục 501 và UI tiếp tục `PLANNED`/schema preview.
- Giá/khuyến khích, nguồn chi trả, chu kỳ, phần dư, VAT và các trường hợp thiếu/trùng còn theo SRS DRAFT. Không được dùng công thức BSS cũ để tự chốt.
- Không viết database hoặc thay quyền. KIT preview lịch sử I.1 vẫn là **kỹ thuật**; I.1 financial còn chặn.
- Không có Maven/Vite/PostgreSQL runtime ở môi trường viết code; **TEST CHƯA CHẠY** cho thay đổi mới. Không tự nâng trạng thái PASS.
- `PROJECT_STATUS.md` trong tài liệu Phase 1: cần đồng bộ tiến độ này khi tài liệu được đưa vào repository/chỉnh sửa nguồn.

**Gate tiếp theo:** local `mvn test` và `npm run build` PASS; chạy discovery metadata SQL ở DB local rồi xác định nguồn vật lý từng report bằng SELECT chỉ đọc; khóa quyết định nghiệp vụ; mới cài Stored Functions và đối chiếu API/Excel trên tập dữ liệu thật.
