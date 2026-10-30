# Sơ đồ thực thể – quan hệ (ERD)

Nguồn sự thật của schema là các migration Flyway trong `backend/api/src/main/resources/db/migration`. Sơ đồ cập nhật theo từng giai đoạn; mọi bảng dưới đây đã có migration (V1–V10).

| Nhóm | Bảng | Trạng thái |
|---|---|---|
| Tài khoản & phân quyền | `users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `refresh_tokens` | ✅ M1 (`V2__auth.sql`) |
| Hệ thống | `audit_logs` | ✅ M1 · `shedlock` ✅ M0 · `system_settings` ✅ M2 |
| Danh mục | `genres`, `people`, `movies`, `movie_genres`, `movie_people` | ✅ M2 (`V3__catalog.sql`) |
| Rạp | `cinemas`, `rooms`, `seat_types`, `seats` | ✅ M2 (`V4__cinema.sql`) |
| Lịch chiếu & giá | `ticket_base_prices`, `holidays`, `showtimes`, `showtime_prices`, `showtime_seats` | ✅ M2 (`V5__showtime.sql`) |
| Bán vé | `seat_holds`, `orders`, `order_items`, `tickets`, `payments`, `order_events`, `notifications` | ✅ M4 (`V6__booking.sql`) · `refunds` ✅ M7 (`V8__refunds.sql`) |
| Nhóm | `booking_groups`, `group_members`, `group_showtime_options`, `group_votes`, `group_seat_assignments`, `group_events`, `seat_suggestion_logs` | ✅ M6 (`V7__groups.sql`) |
| Khuyến mãi | `promotions`, `promotion_usages` | ✅ M8 (`V9__promotions.sql`) |
| Mở rộng | `favorite_movies`, `movie_reviews`; cột sở thích chỗ ngồi ở `group_members`, kết quả xếp chỗ ở `booking_groups` | ✅ M10 (`V10__extensions.sql`) |

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

## 3. Toàn cảnh nghiệp vụ

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

## 4. Bán vé & thanh toán (M4, M7, M8)

```mermaid
erDiagram
    seat_holds {
        bigint id PK
        bigint user_id FK
        bigint showtime_id FK
        bigint group_id FK "null = giữ cá nhân"
        varchar status "ACTIVE | CONVERTED | RELEASED | EXPIRED"
        timestamptz expires_at
    }
    orders {
        bigint id PK
        varchar code UK "MB…"
        bigint user_id FK
        bigint showtime_id FK
        bigint hold_id FK
        bigint group_id FK
        varchar status
        bigint subtotal
        bigint discount
        bigint total
        varchar promotion_code
        timestamptz expires_at
        timestamptz paid_at
    }
    order_items {
        bigint id PK
        bigint order_id FK
        bigint seat_id FK
        varchar seat_label
        varchar seat_type_code
        bigint price
    }
    tickets {
        bigint id PK
        bigint order_id FK
        bigint showtime_id FK
        bigint seat_id FK
        varchar code UK
        bigint holder_user_id FK
        varchar holder_name
        varchar status "VALID | USED | CANCELLED"
        timestamptz checked_in_at
        bigint checked_in_by FK
    }
    payments {
        bigint id PK
        bigint order_id FK
        varchar provider
        varchar provider_txn_id "UK(provider, provider_txn_id)"
        bigint amount
        varchar status
        jsonb raw
    }
    refunds {
        bigint id PK
        bigint order_id FK
        bigint payment_id FK
        bigint amount
        varchar reason
        varchar status "PENDING | COMPLETED | FAILED"
        bigint processed_by FK
    }
    order_events {
        bigint id PK
        bigint order_id FK
        varchar type
        varchar from_status
        varchar to_status
        bigint actor_id
        jsonb data
    }
    promotions {
        bigint id PK
        varchar code UK
        varchar type "PERCENT | AMOUNT"
        bigint value
        bigint max_discount
        bigint min_order
        int min_tickets "> 1 = ưu đãi nhóm"
        int usage_limit
        int per_user_limit
        int used_count
    }
    promotion_usages {
        bigint id PK
        bigint promotion_id FK
        bigint order_id FK
        bigint user_id FK
        bigint discount
    }
    notifications {
        bigint id PK
        bigint user_id FK
        varchar type
        varchar title
        varchar link
        timestamptz read_at
    }
    seat_holds ||--o| orders : "tạo đơn"
    orders ||--|{ order_items : ""
    orders ||--o{ tickets : ""
    orders ||--o{ payments : ""
    orders ||--o{ refunds : ""
    payments ||--o{ refunds : ""
    orders ||--o{ order_events : ""
    promotions ||--o{ promotion_usages : ""
    orders ||--o| promotion_usages : ""
```

## 5. Nhóm đặt vé (M6)

```mermaid
erDiagram
    booking_groups {
        bigint id PK
        varchar code UK "mã mời"
        varchar name
        bigint leader_id FK
        bigint movie_id FK
        bigint cinema_id FK "null = rạp nào cũng được"
        date date_from
        date date_to
        timestamptz rsvp_deadline
        varchar status
        bigint locked_showtime_id FK
        int locked_ticket_count
        bigint hold_id FK
        bigint order_id FK
    }
    group_members {
        bigint id PK
        bigint group_id FK
        bigint user_id FK
        varchar role "LEADER | MEMBER"
        varchar rsvp "YES | NO | MAYBE | PENDING"
        int guest_count
        bigint max_budget
        timestamptz left_at
    }
    group_showtime_options {
        bigint id PK
        bigint group_id FK
        bigint showtime_id FK
        bigint proposed_by FK
    }
    group_votes {
        bigint option_id PK
        bigint user_id PK
    }
    group_seat_assignments {
        bigint id PK
        bigint group_id FK
        bigint ticket_id FK
        bigint user_id FK
        varchar guest_label
    }
    group_events {
        bigint id PK
        bigint group_id FK
        varchar type
        bigint actor_id
        jsonb data
    }
    seat_suggestion_logs {
        bigint id PK
        bigint group_id FK
        bigint showtime_id FK
        int party_size
        jsonb options
        varchar reason "NOT_ENOUGH_SEATS | NO_CLUSTER"
        int chosen_rank
        boolean contiguous
        int duration_ms
    }
    booking_groups ||--|{ group_members : ""
    booking_groups ||--o{ group_showtime_options : ""
    group_showtime_options ||--o{ group_votes : ""
    booking_groups ||--o{ group_seat_assignments : ""
    booking_groups ||--o{ group_events : ""
    booking_groups ||--o{ seat_suggestion_logs : ""
```

`seat_suggestion_logs` là nguồn số liệu cho thống kê nhóm (ADM02): tỷ lệ tìm được cụm liền kề, phương án được chọn.

## 6. Mở rộng (M10)

```mermaid
erDiagram
    favorite_movies {
        bigint user_id PK
        bigint movie_id PK
        timestamptz created_at
    }
    movie_reviews {
        bigint id PK
        bigint movie_id FK
        bigint user_id FK "UK(movie_id, user_id)"
        int rating "1–5"
        varchar comment
        boolean hidden "quản trị ẩn"
    }
    group_members {
        varchar seat_preference "NONE | AISLE | CENTER"
        bigint next_to_user_id FK "muốn ngồi cạnh"
    }
    users ||--o{ favorite_movies : ""
    movies ||--o{ favorite_movies : ""
    users ||--o{ movie_reviews : ""
    movies ||--o{ movie_reviews : ""
    users |o--o{ group_members : "ngồi cạnh"
```

