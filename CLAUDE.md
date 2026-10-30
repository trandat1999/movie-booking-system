# CLAUDE.md

Đề tài: **Hệ thống bán vé xem phim và hỗ trợ đặt vị trí theo nhóm**.

- **Đọc `PLAN.md` trước khi làm bất cứ việc gì.** Đó là nguồn sự thật cho phạm vi, quy tắc nghiệp vụ (BR01–BR18), mô hình dữ liệu, thiết kế giữ ghế/thanh toán, thuật toán cụm ghế và lộ trình M0–M10.
- Làm theo thứ tự giai đoạn trong PLAN.md mục 11; xong việc nào thì tick checklist và ghi vào mục 14 (Nhật ký tiến độ).
- Thay đổi phạm vi/thiết kế → cập nhật PLAN.md trước rồi mới viết code.

## Stack
- Backend: Java 17, Spring Boot 3, Maven multi-module (`backend/seat-engine`, `backend/seat-bench`, `backend/api`), PostgreSQL + Flyway, Redis.
- Frontend: Angular 19 (standalone, signals) + NG-ZORRO, workspace `frontend/` gồm `web`, `admin`, thư viện `shared`.

## Quy ước
- Tuân thủ quy tắc phân tầng L1–L9 (backend) và F1–F5 (frontend) ở PLAN.md mục 2A. ArchUnit test phải luôn xanh.
- Giao diện tiếng Việt; code, tên bảng, API, commit message bằng tiếng Anh.
- Tiền: VND số nguyên (`Long`/`BIGINT`). Thời gian: lưu UTC (`Instant`/`timestamptz`), hiển thị `Asia/Ho_Chi_Minh`.
- Trạng thái ghế/đơn/nhóm chỉ chuyển bằng `UPDATE ... WHERE status = <cũ>`; giữ ghế luôn trong một transaction (tất cả hoặc không).
- Kiểm tra quyền ở backend; giao diện chỉ ẩn/hiện.
- Thuật toán cụm ghế nằm trong `backend/seat-engine` (Java thuần, không phụ thuộc Spring).
