# Kịch bản demo bảo vệ (~15 phút)

Mục tiêu (PLAN.md mục 12): **2 trình duyệt realtime · nhóm 5 người · tranh ghế · callback trùng**, cộng soát vé và
báo cáo.

## 0. Chuẩn bị (trước buổi demo)

```bash
cd backend && ./mvnw -pl api spring-boot:test-run      # hoặc spring-boot:run nếu có Docker
cd frontend && npm run start:web                        # http://localhost:4200
cd frontend && npm run start:admin                      # http://localhost:4300
node tools/demo/seed-demo.mjs                           # demo1…demo5@demo.local / Demo@1234, mã NHOM5
```

| Cửa sổ | Trình duyệt | Tài khoản |
|---|---|---|
| A | Chrome thường | `demo1@demo.local` (trưởng nhóm) |
| B | Chrome ẩn danh | `demo2@demo.local` |
| C, D, E | Profile Chrome khác / Edge / điện thoại | `demo3`, `demo4`, `demo5` |
| F | Tab admin | `admin@moviebooking.local` / `Password123` |
| G | Điện thoại (cùng mạng LAN) | `staff@moviebooking.local` — trang soát vé |

Nên chọn suất **ngày mai**, phòng lớn (Phòng 1, ~130 ghế) để thuật toán có nhiều phương án.

## 1. Sơ đồ ghế realtime & tranh ghế (3 phút)

1. A và B mở **cùng một suất**. A chọn E5, E6 → **Tiếp tục**. B thấy E5, E6 chuyển sang sọc "đang được giữ" ngay
   lập tức (không tải lại trang).
2. B cố chọn E5 → không chọn được. B chọn E7, E8 trong khi A vẫn đang ở trang thanh toán.
3. **Tranh ghế**: A quay lại, bỏ giữ; A và B cùng chọn F5 rồi bấm Tiếp tục gần như đồng thời → đúng một người giữ
   được, người kia nhận thông báo "ghế vừa có người giữ" và sơ đồ tự tô lại.
4. Nhắc số liệu kiểm thử tải: 500 người cùng giữ 1 ghế → đúng 1 thành công, 0 ghế trùng
   (`docs/experiments/load/README.md`).

## 2. Thanh toán & callback trùng (2 phút)

1. A ở trang thanh toán: đồng hồ đếm ngược theo giờ server; thử mã `NHOM5` → báo không đủ điều kiện (đơn 2 vé).
2. Chọn **Cổng thanh toán giả lập** → **Thành công + gửi callback 2 lần** → thông báo "lần sau được bỏ qua
   (duplicate = true)"; đơn `PAID` một lần, vé QR hiện ra; chuông thông báo của A nhảy số.
3. Tab F (admin) → Đơn hàng → mở đơn vừa rồi: lịch sử trạng thái chỉ có một lần `PENDING_PAYMENT → PAID`.

## 3. Đặt vé nhóm 5 người (6 phút) — trọng tâm

1. **A** vào trang phim → **Đặt vé nhóm** → đặt tên, hạn chốt → Tạo nhóm. Sao chép link mời.
2. **B, C, D, E** mở link → **Tham gia nhóm** → phản hồi "Tham gia" (D thêm 1 khách đi kèm, E chọn "Chưa chắc").
   Trang của A cập nhật realtime thanh tiến trình và "Chưa phản hồi: …".
3. A **Đề xuất suất** 2 suất; các thành viên bình chọn → số phiếu cập nhật realtime. Phần tổng hợp cho thấy số vé
   dự kiến, ngân sách, và trạng thái **cụm ghế còn lại** của từng suất (cùng hàng / phải chia hàng / không đủ).
4. A **Chốt suất này** (số vé mặc định = người tham gia + khách = 5) → nhóm `Đã chốt suất`.
5. A **Chọn ghế cho nhóm**: hệ thống trả 5 phương án có điểm và lý do (gần trung tâm, cùng hàng, không để ghế lẻ…).
   Bấm từng phương án → B…E thấy phương án đang xem trên sơ đồ của mình (realtime).
6. **Tranh ghế với nhóm**: trong lúc A đang xem phương án #1, tab B (dùng một suất cá nhân, hoặc tài khoản khác) giữ
   một ghế nằm trong phương án đó. A bấm **Giữ ghế & thanh toán** → bị từ chối nguyên khối (không giữ một phần) và
   nhận ngay 5 phương án mới không chứa ghế vừa mất.
7. A giữ phương án mới → áp mã `NHOM5` (đơn ≥ 4 vé) → thanh toán. Nhóm chuyển `Đã đặt vé`; bảng **chia tiền tham
   khảo** và **phân phối vé** hiện ra; A đổi chỗ vé giữa hai thành viên.
8. B…E mở **Vé của tôi** → mỗi người thấy đúng vé của mình (D có 2 vé gồm khách).

## 4. Soát vé (1 phút)

1. Điện thoại G mở `http://<ip-máy>:4300/checkin` (đăng nhập staff), cho phép camera.
2. Quét QR một vé của B → màn hình xanh "Hợp lệ". Quét lại → đỏ "đã được soát lúc …".
3. Quét QR của cả đơn nhóm (trên trang đơn của A) → soát các vé còn lại một lần.

*Lưu ý: chỉ soát được trong khung 2 giờ trước suất chiếu — với suất ngày mai, dùng một suất hôm nay cho phần này.*

## 5. Quản trị & số liệu (2 phút)

1. Dashboard: doanh thu, vé bán, tỷ lệ lấp đầy theo ngày (biểu đồ cột), top phim.
2. Thống kê nhóm: tỷ lệ chuyển đổi tạo → đặt, quy mô nhóm, **tỷ lệ tìm được cụm liền kề**, lý do thất bại.
3. Xuất CSV đơn hàng → mở bằng Excel (đúng tiếng Việt).
4. Nhật ký: các thao tác vừa làm (đăng nhập, áp mã, hoàn tiền…).

## 6. Câu hỏi thường gặp — số liệu nên mở sẵn

| Câu hỏi | Mở |
|---|---|
| Làm sao không bán trùng ghế? | `docs/sequences.md` mục 1–2, `docs/experiments/load/README.md` |
| Thuật toán so với cách đơn giản? | `docs/experiments/README.md` (2 baseline, 3 mức lấp đầy) |
| Thuật toán có nhanh không? | JMH: phòng 300 ghế, n ≤ 10, p95 < 50 ms |
| Hết hạn giữ ghế thì sao? Tiền về muộn? | `docs/sequences.md` mục 3, BR17 |
| Bảo mật? | `docs/security-review.md` |
