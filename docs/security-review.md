# Rà soát bảo mật (M9)

Phạm vi: API backend (`/api/v1/**`), kênh realtime STOMP, cổng thanh toán, vé QR, hai ứng dụng web. Kiểm chứng tự
động bằng `AccessControlIntegrationTest`, `ClientInfoTest`, `AuthIntegrationTest`, `ArchitectureTest` và các test
tích hợp nghiệp vụ.

## 1. Xác thực & phiên

| Hạng mục | Hiện trạng | Kiểm chứng |
|---|---|---|
| Mật khẩu | BCrypt; chính sách độ dài + chữ hoa/thường/số; so sánh với hash giả khi email không tồn tại (thời gian phản hồi như nhau, không lộ tài khoản qua đăng nhập) | `AuthIntegrationTest` |
| Access token | JWT HS256, hạn ngắn; khóa bắt buộc đặt qua `JWT_SECRET` ngoài dev/test (không có giá trị mặc định) | token giả → 401 (`AccessControlIntegrationTest`) |
| Refresh token | Cookie `HttpOnly`, `SameSite=Strict`, `Secure` (cấu hình), path giới hạn; xoay vòng mỗi lần dùng; dùng lại token cũ ngoài thời gian ân hạn → thu hồi cả family + ghi nhật ký | `AuthIntegrationTest` |
| CSRF | API dùng Bearer token trong header, không dựa cookie → tắt CSRF; cookie refresh `SameSite=Strict` | — |
| CORS | Chỉ origin cấu hình (`APP_CORS_ALLOWED_ORIGINS`) | — |
| Header bảo mật | Mặc định Spring Security: `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Cache-Control: no-store` | — |

## 2. Phân quyền (kiểm ở backend, giao diện chỉ ẩn/hiện)

- API quản trị `/api/v1/admin/**`: yêu cầu đăng nhập ở `SecurityConfig` và `@PreAuthorize` theo quyền ở **cấp lớp**
  của mọi `Admin*Controller` — luật kiến trúc L8 trong `ArchitectureTest` bảo đảm không controller nào bị sót.
  Quyền chi tiết: `order:read`, `order:refund`, `ticket:checkin`, `promotion:write`, `stats:read`, `audit:read`… (STAFF chỉ soát vé).
- Khách hàng gọi 8 API quản trị + soát vé → 403 (`customerCannotCallAdminOrStaffApis`).
- API riêng tư khi chưa đăng nhập → 401 (`privateApisRequireAuthentication`).

## 3. IDOR — truy cập tài nguyên của người khác

Mọi truy vấn tài nguyên cá nhân đều lọc theo `userId` lấy từ token (không lấy từ request). Không thuộc về mình →
**404** (không phải 403) để không lộ sự tồn tại của mã đơn / nhóm.

| Tài nguyên | Định danh | Bảo vệ | Test |
|---|---|---|---|
| Đơn hàng | mã `MB…` (đoán được) | `OrderService` lọc `order.userId == user` cho xem/hủy/thanh toán/mã giảm giá | `ordersAndHoldsOfAnotherUserAreInvisible` |
| Lượt giữ ghế | id tăng dần | trả ghế / tạo đơn chỉ với lượt giữ của mình | như trên |
| Nhóm | id tăng dần | `requireMember` / `requireLeader` ở mọi thao tác; xem trước bằng mã mời chỉ trả thông tin công khai | `groupIsVisibleToMembersOnly` |
| Kênh realtime nhóm | `/topic/groups/{id}` | `StompAuthChannelInterceptor` chỉ cho thành viên SUBSCRIBE | `GroupIntegrationTest` |
| Kênh cá nhân | `/user/queue/**` | gắn principal từ JWT lúc CONNECT | — |
| Thông báo | id tăng dần | `findByIdAndUserId` | `notificationsOfAnotherUserCannotBeMarkedRead` |
| Vé | mã vé | chỉ xem qua `/me/tickets` hoặc đơn của mình; QR ký HMAC | `RefundCheckinIntegrationTest` |

## 4. Thanh toán & vé

- IPN cổng giả lập: chữ ký HMAC-SHA256 trên (mã đơn, mã giao dịch, số tiền, kết quả); VNPay: HMAC-SHA512 theo đặc
  tả. Số tiền lệch tổng đơn → ghi `AMOUNT_MISMATCH`, không xác nhận đơn. Xử lý idempotent theo mã giao dịch (callback trùng không đổi trạng thái lần 2).
- Số tiền luôn tính ở server từ giá trong DB; client không gửi giá.
- QR vé = mã + 128 bit đầu HMAC-SHA256 (`TICKET_SECRET`); soát vé kiểm chữ ký, đúng suất/khung giờ, chống soát
  lặp bằng `UPDATE ... WHERE status = 'VALID'`.

## 5. Chống lạm dụng (BR15)

| Hành động | Giới hạn mặc định |
|---|---|
| Đăng nhập | 10 / phút / IP |
| Giữ ghế (cá nhân, nhóm), tạo nhóm | 20 / phút / người dùng |

Bộ đếm cửa sổ trượt trong bộ nhớ (một instance); khi chạy nhiều instance cần chuyển sang Redis (PLAN.md mục 2).

## 6. Vấn đề phát hiện trong đợt rà soát và đã sửa

1. **Giả IP vượt giới hạn đăng nhập** (mức cao): `ClientInfo` đọc thẳng `X-Forwarded-For` do máy khách gửi → đổi
   header mỗi lần là vượt giới hạn 10 lần/phút/IP (dò mật khẩu) và ghi IP giả vào nhật ký. Sửa: chỉ dùng
   `getRemoteAddr()`; khi chạy sau reverse proxy bật `server.forward-headers-strategy=native` để Tomcat
   `RemoteIpValve` chỉ tin header từ proxy nội bộ. Test: `ClientInfoTest`.
2. **Cạn pool kết nối DB dưới tải** (từ chối dịch vụ): nhật ký kiểm toán và thông báo dùng transaction lồng
   `REQUIRES_NEW` → mỗi request giữ 2 kết nối; ≥ 10 request đăng ký đồng thời làm treo toàn hệ thống 30 s. Sửa và số
   liệu: `docs/experiments/load/README.md` mục 5.
3. **`@Async` tạo luồng không giới hạn**: chuyển sang pool cấu hình được (`AsyncConfig`).

## 7. Rủi ro còn lại / ngoài phạm vi

- Đăng ký trả lỗi "email đã tồn tại" → có thể dò email đã đăng ký (chấp nhận được cho đồ án; giảm thiểu bằng rate
  limit đăng ký theo IP khi triển khai thật).
- Rate limit và kênh STOMP đơn instance (bộ nhớ); triển khai nhiều instance cần Redis + broker ngoài.
- Chưa có xác minh email / 2FA cho tài khoản quản trị.
- Khóa bí mật dev nằm trong `application-dev.yml` (chỉ dùng cho môi trường dev; production bắt buộc biến môi trường).
