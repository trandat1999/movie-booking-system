# Sơ đồ thực thể – quan hệ (ERD)

Nguồn sự thật của schema là các migration Flyway trong `backend/api/src/main/resources/db/migration`. Sơ đồ này cập nhật theo từng giai đoạn; bảng đánh dấu ✅ đã có migration, các bảng còn lại là thiết kế theo PLAN.md mục 7.

| Nhóm | Bảng | Trạng thái |
|---|---|---|
| Tài khoản & phân quyền | `users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `refresh_tokens` | ✅ M1 (`V2__auth.sql`) |
| Hệ thống | `audit_logs` | ✅ M1 · `shedlock` ✅ M0 · `system_settings` ✅ M2 |
| Danh mục | `genres`, `people`, `movies`, `movie_genres`, `movie_people` | ✅ M2 (`V3__catalog.sql`) |
| Rạp | `cinemas`, `rooms`, `seat_types`, `seats` | ✅ M2 (`V4__cinema.sql`) |
| Lịch chiếu & giá | `ticket_base_prices`, `holidays`, `showtimes`, `showtime_prices`, `showtime_seats` | ✅ M2 (`V5__showtime.sql`) |
| Bán vé | `seat_holds`, `orders`, `order_items`, `tickets`, `payments`, `refunds`, `order_events` | M4, M7 |
| Nhóm | `booking_groups`, `group_members`, `group_showtime_options`, `group_votes`, `group_seat_assignments`, `group_events`, `seat_suggestion_logs` | M6 |
| Khác | `promotions`, `promotion_usages`, `notifications` | M4, M8 |

## 1. Tài khoản, phân quyền, nhật ký (M1)

```mermaid
erDiagram
    users ||--o{ user_roles : "có"
    roles ||--o{ user_roles : "gán cho"
    roles ||--o{ role_permissions : "gồm"
    permissions ||--o{ role_permissions : "thuộc"
    users ||--o{ refresh_tokens : "phiên đăng nhập"
    users |o--o{ audit_logs : "thực hiện"

    users {
        bigint id PK
        varchar email UK "chữ thường"
        varchar phone UK "0xxxxxxxxx, có thể trống"
        varchar password_hash "BCrypt"
        varchar full_name
        date date_of_birth
        varchar avatar_url
        varchar status "ACTIVE | LOCKED"
        timestamptz email_verified_at
        timestamptz last_login_at
        timestamptz created_at
        timestamptz updated_at
    }
    roles {
        bigint id PK
        varchar code UK "ADMIN | STAFF | CUSTOMER"
        varchar name
        varchar description
    }
    permissions {
        bigint id PK
        varchar code UK "resource:action"
        varchar name
        varchar group_name
    }
    user_roles {
        bigint user_id PK, FK
        bigint role_id PK, FK
    }
    role_permissions {
        bigint role_id PK, FK
        bigint permission_id PK, FK
    }
    refresh_tokens {
        bigint id PK
        bigint user_id FK
        varchar token_hash UK "SHA-256, không lưu token gốc"
        uuid family_id "chuỗi token của 1 lần đăng nhập"
        timestamptz expires_at
        timestamptz revoked_at
        varchar revoke_reason "ROTATED | LOGOUT | REUSE_DETECTED | SESSIONS_INVALIDATED"
        varchar user_agent
        varchar ip_address
        timestamptz created_at
    }
    audit_logs {
        bigint id PK
        bigint actor_id "không FK: giữ log khi xóa user"
        varchar actor_email
        varchar action "LOGIN, USER_STATUS_UPDATE, ..."
        varchar resource
        varchar resource_id
        jsonb data
        varchar ip_address
        timestamptz created_at
    }
```

## 2. Phim, rạp, lịch chiếu, giá (M2)

```mermaid
erDiagram
    movies ||--o{ movie_genres : ""
    genres ||--o{ movie_genres : ""
    movies ||--o{ movie_people : "đạo diễn / diễn viên"
    people ||--o{ movie_people : ""
    cinemas ||--o{ rooms : "có"
    rooms ||--o{ seats : "sơ đồ"
    seat_types ||--o{ seats : "loại"
    movies ||--o{ showtimes : "chiếu"
    rooms ||--o{ showtimes : "tại"
    showtimes ||--o{ showtime_prices : "giá chốt"
    seat_types ||--o{ showtime_prices : ""
    showtimes ||--o{ showtime_seats : "trạng thái ghế"
    seats ||--o{ showtime_seats : ""

    movies {
        bigint id PK
        varchar title
        varchar slug UK
        int duration_min
        varchar age_rating "P | K | T13 | T16 | T18"
        date release_date
        varchar status "COMING_SOON | NOW_SHOWING | ENDED"
    }
    movie_people {
        bigint id PK
        bigint movie_id FK
        bigint person_id FK
        varchar role "DIRECTOR | ACTOR"
        int sort_order
    }
    rooms {
        bigint id PK
        bigint cinema_id FK
        varchar room_type "STANDARD | PREMIUM | IMAX"
        int grid_rows
        int grid_cols
        boolean layout_locked "true khi đã có suất chiếu"
    }
    seat_types {
        varchar code PK "STANDARD | VIP | COUPLE | ACCESSIBLE"
        int capacity "ghế đôi = 2"
        int col_span "ghế đôi = 2 ô"
        bigint surcharge
    }
    seats {
        bigint id PK
        bigint room_id FK
        varchar row_label "A, B, ..."
        int number
        int grid_row
        int grid_col
        int col_span
        varchar seat_type_code FK
        boolean active "false = ghế hỏng"
    }
    showtimes {
        bigint id PK
        bigint movie_id FK
        bigint room_id FK
        timestamptz start_at
        timestamptz end_at
        timestamptz blocked_until "end_at + dọn phòng"
        varchar format "TWO_D | THREE_D | IMAX"
        varchar day_type "WEEKDAY | WEEKEND | HOLIDAY"
        varchar status "SCHEDULED | CANCELLED"
    }
    ticket_base_prices {
        varchar format PK
        varchar day_type PK
        bigint price
    }
    showtime_seats {
        bigint showtime_id PK
        bigint seat_id PK
        varchar status "AVAILABLE | HELD | SOLD | BLOCKED"
        bigint hold_id "M4"
        timestamptz held_until "M4"
    }
```

Ràng buộc chống trùng lịch: `EXCLUDE USING gist (room_id WITH =, tstzrange(start_at, blocked_until) WITH &&) WHERE status <> 'CANCELLED'`.

## 3. Toàn cảnh nghiệp vụ (thiết kế)

```mermaid
erDiagram
    movies ||--o{ showtimes : "chiếu"
    cinemas ||--o{ rooms : "có"
    rooms ||--o{ seats : "sơ đồ"
    seat_types ||--o{ seats : "loại"
    rooms ||--o{ showtimes : "tại"
    showtimes ||--o{ showtime_seats : "trạng thái ghế"
    seats ||--o{ showtime_seats : ""
    seat_holds ||--o{ showtime_seats : "giữ"
    users ||--o{ seat_holds : ""
    users ||--o{ orders : "đặt"
    showtimes ||--o{ orders : ""
    orders ||--o{ order_items : ""
    orders ||--o{ tickets : ""
    orders ||--o{ payments : ""
    orders ||--o{ refunds : ""
    orders ||--o{ order_events : "lịch sử"
    users ||--o{ booking_groups : "trưởng nhóm"
    booking_groups ||--o{ group_members : ""
    users ||--o{ group_members : ""
    booking_groups ||--o{ group_showtime_options : "đề xuất suất"
    group_showtime_options ||--o{ group_votes : "bình chọn"
    booking_groups |o--o| orders : "đơn nhóm"
    tickets ||--o| group_seat_assignments : "gán cho thành viên"
    booking_groups ||--o{ group_events : ""
    promotions ||--o{ promotion_usages : ""
    users ||--o{ notifications : ""
```

Ràng buộc chống bán trùng ghế (BR01): khóa chính `showtime_seats(showtime_id, seat_id)` + partial unique index `tickets(showtime_id, seat_id) WHERE status <> 'CANCELLED'`.
