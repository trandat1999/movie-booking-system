# Máy trạng thái

Mọi chuyển trạng thái được thực hiện bằng câu lệnh có điều kiện `UPDATE ... WHERE status = <trạng thái cũ>` và kiểm tra số dòng bị ảnh hưởng, để hai request đồng thời không cùng chuyển một bản ghi (PLAN.md mục 6).

## 1. Ghế theo suất chiếu — `showtime_seats.status` (M4)

```mermaid
stateDiagram-v2
    [*] --> AVAILABLE : tạo suất chiếu
    AVAILABLE --> HELD : giữ ghế (cả cụm trong 1 transaction)
    HELD --> AVAILABLE : hết hạn giữ / người dùng bỏ chọn
    HELD --> SOLD : thanh toán thành công
    SOLD --> AVAILABLE : hủy vé / hoàn tiền
    AVAILABLE --> BLOCKED : admin khóa ghế
    BLOCKED --> AVAILABLE : admin mở lại
```

`HELD` mà `held_until < now()` được coi là trống ngay cả khi job dọn dẹp chưa chạy (BR02).

## 2. Đơn hàng — `orders.status` (M4, M7)

```mermaid
stateDiagram-v2
    [*] --> PENDING_PAYMENT : tạo đơn từ hold
    PENDING_PAYMENT --> PAID : IPN thành công
    PENDING_PAYMENT --> EXPIRED : hết hạn giữ ghế
    PENDING_PAYMENT --> CANCELLED : người dùng hủy
    PENDING_PAYMENT --> REFUND_PENDING : tiền về sau khi hết hạn và ghế đã mất (BR17)
    PAID --> COMPLETED : suất chiếu kết thúc
    PAID --> CANCELLED : hủy theo chính sách
    CANCELLED --> REFUNDED : hoàn tiền xong
    REFUND_PENDING --> REFUNDED : hoàn tiền xong
```

## 3. Nhóm đặt vé — `booking_groups.status` (M6)

```mermaid
stateDiagram-v2
    [*] --> OPEN : tạo nhóm
    OPEN --> LOCKED : trưởng nhóm chốt suất & số vé
    LOCKED --> OPEN : mở chốt (đổi suất) — thông báo thành viên
    LOCKED --> HOLDING : giữ cụm ghế thành công
    HOLDING --> LOCKED : hết hạn giữ ghế
    HOLDING --> BOOKED : thanh toán thành công
    BOOKED --> COMPLETED : suất chiếu kết thúc
    BOOKED --> CANCELLED : hủy theo chính sách
    OPEN --> EXPIRED : quá hạn chốt
    OPEN --> CANCELLED : trưởng nhóm hủy
    LOCKED --> CANCELLED : trưởng nhóm hủy
```

## 4. Refresh token — `refresh_tokens` (M1, đã triển khai)

Mỗi lần đăng nhập tạo một *family*. Mỗi lần refresh, token cũ bị thu hồi (`ROTATED`) và token mới cùng family được cấp.

```mermaid
stateDiagram-v2
    [*] --> ACTIVE : đăng nhập / refresh
    ACTIVE --> ROTATED : refresh (cấp token mới cùng family)
    ACTIVE --> LOGOUT : đăng xuất (thu hồi cả family)
    ACTIVE --> SESSIONS_INVALIDATED : đổi mật khẩu / bị khóa (thu hồi mọi family của user)
    ACTIVE --> EXPIRED : quá refresh-token-ttl (7 ngày)
    ROTATED --> ROTATED : dùng lại trong 30s ân hạn và family còn token hoạt động → cấp thêm token
    ROTATED --> REUSE_DETECTED : dùng lại sau ân hạn → thu hồi cả family + ghi audit
```

Thời gian ân hạn (`app.security.refresh-token-reuse-grace`) tránh đăng xuất nhầm khi nhiều tab cùng refresh một lúc; ngoài khoảng đó, token đã xoay vòng bị dùng lại được coi là bị đánh cắp.
