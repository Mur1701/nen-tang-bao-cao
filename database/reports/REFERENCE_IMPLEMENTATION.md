# Phase 1 — baseline thực thi tham chiếu BSS cũ (10/11/2026)

## Thứ tự căn cứ

**Đặc tả Phase 1 → quyết định nghiệp vụ đã xác nhận → quy tắc/SQL BSS cũ khi đặc tả chưa đầy đủ.** Người phát triển đã chỉ định dùng fallback này để có dữ liệu chạy và kiểm thử, không tạo bảng trắng chỉ vì TBD.

Vẫn phân biệt:
- `REFERENCE`: mã SQL thực thi được trên dữ liệu BSS, dùng thuật toán tham chiếu lịch sử, chưa nghiệm thu tài chính.
- `PAID`: số tiền có bản ghi `vnpost_bss.chi_tra_hieu_luc` hiệu lực (nếu tồn tại).
- `POLICY_ESTIMATE`: hoa hồng 25% theo danh mục cũ, **không thay thế chứng từ thanh toán**.
- `FINAL`: chỉ sau khi đối chiếu dữ liệu, kỳ báo cáo và số tiền với bộ kỳ mẫu; chưa đạt.

## Source → Stored Function

| Phase 1 | BSS cũ | Function SQL trả kết quả |
|---|---|---|
| I.1 | II.5 khuyến khích tổng hợp | `fn_phase1_reference_rows('I.1',...)`; giữ riêng `fn_i1_report` chưa nghiệm thu |
| I.2 | II.6 chi tiết khuyến khích | `fn_i2_report` |
| I.3 | IV.1 phân bổ chi phí theo ngày | `fn_i3_report` |
| I.4 | IV.2 chi phí chưa phân bổ | `fn_i4_report` |
| I.5 | IV.3 chi phí phân bổ trong kỳ | `fn_i5_report` |
| I.6 | IV.4 chi phí chuyển kỳ sau | `fn_i6_report` |
| I.7 | IV.5 tổng hợp chi phí | `fn_i7_report` |
| II.1 | III.1 phân bổ doanh thu theo ngày | `fn_ii1_report` |
| II.2 | III.2 doanh thu chưa phân bổ | `fn_ii2_report` |
| II.3 | III.3 doanh thu phân bổ trong kỳ | `fn_ii3_report` |
| II.4 | III.4 doanh thu chuyển kỳ sau | `fn_ii4_report` |
| II.5 | III.5 tổng hợp doanh thu đại lý | `fn_ii5_report` |

## Công thức và nguồn tham chiếu đã mã hóa

- KIT, đại lý và giá gói: `crm.linhnn_kits_prod`, `esim_agency.agencies`, `inventory.products` (3 bảng/JOIN đã được audit 09/11 ở mức KIT).
- Active gói: `MIN(to_date(insert_time, 'DD-MON-YY'))` từ `cdr.vnpt_package_new` theo MSISDN (BSS cũ `nen/phan-bo.sql`).
- Chu kỳ theo `danh-muc.json`: M2M5/DL5=5, M2M7/DL7=7, M2M10/DL10=10, M2M15/DL15=15, DL30=30. Chưa có chu kỳ thì không tự tạo ngày phân bổ.
- VAT doanh thu 10%, hoa hồng 25% giá gói, VAT chi phí 8%, số thập phân 2; ngày cuối chu kỳ nhận phần dư (legacy `dong_so=true`).
- I.1/I.2 ưu tiên công thức Phase 1 cho incentive **trước VAT**, VAT=8% × incentive net; xét bậc theo tổng đại lý trong tháng 0/5/10%.
- II.5 Profile SIM theo số SIM của **cả phiếu COMPLETED**: ≤1000 = 6500đ; 1001–2000 = 3000đ; >2000 = 0đ; hòa mạng = 25000đ/SIM; VAT doanh thu 10%.
- I.7, II.5: số dư cuối kỳ = đầu kỳ + phát sinh − phân bổ, giữ hai cột net/VAT và tổng.
- Ngày và khoản HHBG thực chi đọc từ `vnpost_bss.chi_tra_hieu_luc` hiệu lực hiện tại nếu bảng tồn tại; không đọc hay ghi tệp chi trả, không cài migration BSS.
- **Cảnh báo:** số tiền hoa hồng 25% là theo chính sách, có thể khác tiền lô duyệt đã chi trả; nhóm I.3–I.7 là tham chiếu dự kiến nếu chưa đủ chứng từ. Chưa thực hiện đối chiếu theo thời điểm `chi_tra_khoa_ky`.
- Snapshot giá `inventory.products.base_price` chưa chứng minh giữ đúng lịch sử từng tháng; mọi kết quả đều phải được ghi nhãn `LEGACY_REFERENCE`.

## Mã nguồn triển khai / không viết dữ liệu BSS

1. `00_PHASE1_REFERENCE_CORE.sql` — nguồn và lịch phân bổ.
2. `00B_PHASE1_PAYMENT_SOURCE.sql` — nguồn chi trả HHBG tùy chọn, **chỉ SELECT**.
3. `01_PHASE1_LEGACY_REPORTS.sql` — 12 nhánh dữ liệu trong Stored Function.
4. `02_PHASE1_ENTRYPOINTS.sql` — 11 entry point cố định, tên theo `ReportFunctionRegistry`.
5. `ReportRepository` / `ReportService` / `Phase1ReferenceAssembler` — Spring JDBC, paging, day-group, JSON tiền dạng decimal text và Excel.
6. React SPA dùng `REFERENCE` cho toàn bộ Phase 1, giữ I.1 KIT kỹ thuật riêng.
7. `.github/workflows/phase1-reference-ci.yml` — Maven/React và PostgreSQL 18 trên database **giả lập**.

**Không tự động cài SQL lên PostgreSQL của người phát triển.** Chỉ tạo/thay đổi schema `reporting` khi người phát triển chủ động chạy các file trên local; không chạy những file `tests/*` trên BSS.

## Trạng thái kiểm chứng

- GitHub Actions (feature branch, 10/11/2026): **PASS** `java-and-react` + `postgres-18-smoke` trên database fixture.
- DB thực `pottel_bss`: **CHƯA KIỂM THỬ BẢN MỚI**; bản cũ I.1 KIT preview đã PASS (bảo lưu).
- Nghiệm thu công thức tiền, tỷ lệ khuyến khích/hoa hồng, dữ liệu chi trả theo khóa sổ, giá lịch sử và Excel sản xuất: **CHƯA HOÀN THÀNH**.

### Chặn nghiệm thu / không chặn việc chạy thử

Không sử dụng kết quả `REFERENCE` để thanh toán hoặc báo cáo pháp lý trước khi so sánh kỳ mẫu với BSS cũ và biểu mẫu Phase 1. Sai lệch phải giải quyết theo thứ tự ưu tiên đã xác nhận, không che bằng số 0.
