# Pottel Reporting System — Chuẩn hiển thị & trải nghiệm Phase 1

**Ngày:** 10/11/2026 — **Phạm vi:** SPA React/TypeScript/Ant Design dùng chung cho 12 báo cáo.

## Căn cứ

- `docs/requirements/SRS_COMMON.md`: danh mục đúng 12 báo cáo; định dạng tháng `MM/YYYY` ở hợp đồng cũ; không suy diễn số tiền `null` thành 0; xuất Excel cùng bộ lọc đã chạy.
- `docs/design/FRONTEND_ARCHITECTURE.md`: route quyết định reportCode; không đặt thêm chọn báo cáo trùng route; format Việt Nam; giữ nguồn chính xác.
- Phản hồi UX ngày 10/11/2026 của người phát triển: thao tác tìm kiếm chỉ chạy khi Enter; toàn bộ ứng dụng hiển thị tiếng Việt; giảm chọn báo cáo trùng lặp; dùng biểu đồ khi có dữ liệu xu hướng.
- BSS cũ là **tham chiếu tư duy vận hành**, không sao chép UI hoặc thêm bộ lọc không có căn cứ.

## Quy ước hiển thị đã triển khai

| Trường | API / dữ liệu gốc | Hiển thị ứng dụng | Excel |
|---|---|---|---|
| Kỳ tháng | `YYYY-MM` | `Tháng M năm YYYY` | Dùng bộ lọc kỳ đã chạy |
| Ngày `date` | `YYYY-MM-DD` | `dd/MM/yyyy` | Ô ngày thật, format `dd/mm/yyyy` |
| Ngày giờ `datetime` | ISO/PostgreSQL timestamp | `dd/MM/yyyy HH:mm:ss` | Ô thời gian thật, format `dd/mm/yyyy hh:mm:ss` |
| Timestamp có offset | ISO 8601 và offset | Chuyển về `Asia/Ho_Chi_Minh` | Cùng múi giờ Việt Nam |
| Timestamp không offset | Không xác định timezone | Giữ giờ nguồn; không tự dịch | Cùng giá trị nguồn |
| Số nguyên / tiền | Numeric từ DB, decimal JSON dạng text | Dấu chấm nhóm nghìn; dấu phẩy thập phân, **không làm tròn lại** | Ô số khi đủ 15 chữ số có nghĩa, nếu không thì giữ text để tránh sai |
| Giá trị thiếu | `null` | `—` | Để trống, khác số 0 |
| Số thuê bao/MSISDN | text | Giữ chuỗi và số 0 đầu | Text, không tự ép số |
| Nhóm cột ngày | `YYYY-MM-DD` | `dd/MM/yyyy` | Tiêu đề ngày `dd/MM/yyyy` |

## Điều hướng, tìm kiếm, bộ lọc

1. **Sidebar là menu báo cáo chính.** Header có thể tìm mã/tên báo cáo **sau khi nhấn Enter**. Không còn dropdown chọn báo cáo trong bộ lọc của từng report.
2. Ô tìm **trong trang dữ liệu hiện tại** chỉ áp dụng sau Enter. Không có truy vấn server mỗi ký tự. Tìm nhanh chỉ tác động trang hiện tại; không tuyên bố là tìm toàn bộ BSS.
3. `appliedQuery` giữ bộ lọc của lần chạy thành công, dùng khi xuất Excel; thay bộ lọc mới sẽ vô hiệu hóa kết quả cũ và số liệu biểu đồ.
4. DatePicker Ant Design và Day.js dùng locale `vi-VN`/ `vi`; tháng hiển thị thành cụm từ tiếng Việt trong bộ lọc. Giá trị gửi API vẫn giữ hợp đồng `YYYY-MM`.
5. Các mẫu biểu/column được tái sử dụng; không nhân bản UI cho 12 báo cáo.

## Biểu đồ xu hướng

- **Đã triển khai bước đầu** cho II.5: đường biểu diễn *doanh thu gói phát sinh* và *doanh thu gói đã phân bổ* trong **6 tháng liên tiếp** đến tháng người dùng chọn.
- Lấy tổng hợp từng tháng từ `reporting.fn_ii5_report` qua `POST /api/reports/II.5/trend`; **không** cộng trang dữ liệu trên React để giả là tổng toàn kỳ.
- Truy vấn biểu đồ **chỉ chạy khi bấm xem** (6 lần tính tháng là truy vấn tương đối nặng); nếu nguồn thiếu thì để trống thay vì vẽ điểm 0.
- Biểu đồ dùng `number` chỉ để vẽ tọa độ; giá trị tiền chính xác tiếp tục được giữ ở API dưới dạng chuỗi thập phân. Có mục đối chiếu số gốc từng tháng.
- Tiếp tục nghiên cứu thêm đồ thị/so sánh phù hợp từng báo cáo theo dữ liệu thực tế; không áp dụng một kiểu biểu đồ cho mọi báo cáo.

## Kiểm thử / trạng thái

- CI tự động: Maven, build React, PostgreSQL synthetic. Phần kiểm thử React hiện là **build**; chưa có visual regression/cross-browser test.
- Dữ liệu BSS local: người phát triển đã xác nhận 12 báo cáo chạy; **chưa có chứng cứ kiểm tra giao diện mới trên local**.
- Excel: đã có test giữ MSISDN, ngày giờ và nhóm cột; cần kiểm tra nội dung thực trên vài kỳ và đối chiếu với mẫu nghiệp vụ.
- Những thuật ngữ nghiệp vụ từ SRS như VAT/KIT/MSISDN được bảo lưu. Không dịch thuật ngữ bừa bãi làm mất nghĩa.
