# KẾ HOẠCH DỰ ÁN — HỆ THỐNG BÁN VÉ XEM PHIM & ĐẶT VỊ TRÍ THEO NHÓM

> Tài liệu gốc để phát triển dự án. Mọi thay đổi phạm vi, thiết kế hoặc quyết định kỹ thuật cần cập nhật vào file này trước khi viết code.
>
> Phiên bản: 1.1 — 2026-10-30 (stack: Java 17 + Spring Boot 3, Angular 19 + NG-ZORRO)

---

## 0. Tóm tắt

Hệ thống gồm **Web người dùng** và **Web quản trị**. Ngoài bán vé trực tuyến, hệ thống hỗ trợ **đặt vé theo nhóm**: tạo nhóm, mời thành viên, đăng ký nhu cầu, bình chọn suất chiếu, **tự động đề xuất cụm ghế ngồi gần nhau** và giữ ghế nguyên khối cho cả nhóm.

**Ba trọng tâm kỹ thuật** (phần trình bày chính khi bảo vệ):

1. **Thuật toán đề xuất cụm ghế**: hàm chấm điểm rõ ràng, có thực nghiệm đánh giá.
2. **Xử lý đồng thời**: không bán trùng ghế, giữ ghế cả nhóm theo kiểu "tất cả hoặc không", thanh toán idempotent.
3. **Thời gian thực**: sơ đồ ghế, trạng thái nhóm và bình chọn cập nhật tức thì.

---

## 1. Quyết định đã chốt (mặc định — có thể đổi ở mục 13)

| # | Quyết định | Giá trị |
|---|---|---|
| D1 | Mô hình thanh toán nhóm | **Mô hình A**: trưởng nhóm thanh toán toàn bộ. Mô hình B để ở phần mở rộng. |
| D2 | Thành viên nhóm | **Bắt buộc có tài khoản**. Link mời → đăng nhập/đăng ký → vào nhóm. Thành viên có thể đăng ký thêm "khách đi kèm" (không cần tài khoản). |
| D3 | Nguồn sự thật của trạng thái ghế | **PostgreSQL** (bảng `showtime_seats`). Redis chỉ dùng cho rate limit và cache. |
| D4 | Cổng thanh toán | **Mock provider** cho dev/test, **VNPay sandbox** cho demo. MoMo là mở rộng. |
| D5 | Tiền tệ | VND, lưu số nguyên (`BIGINT`), không dùng số thực. |
| D6 | Thời gian | Lưu UTC (`timestamptz`), hiển thị theo `Asia/Ho_Chi_Minh`. |
| D7 | Ngôn ngữ | Giao diện tiếng Việt. Code, tên bảng, API bằng tiếng Anh. |
| D8 | Vai trò | `ADMIN`, `STAFF`, `CUSTOMER` (RBAC theo permission, dễ mở rộng thêm `MANAGER`, `SUPPORT`). |
| D9 | Hủy một phần đơn nhóm | Không hỗ trợ trong MVP. Chỉ hủy toàn bộ đơn theo chính sách. |

---

## 2. Công nghệ

### 2.1 Backend

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ / framework | **Java 17**, **Spring Boot 3.5.x** |
| Build | Maven (multi-module) |
| Web / validate | Spring Web MVC, Jakarta Bean Validation |
| Truy cập dữ liệu | Spring Data JPA (Hibernate 6); `NamedParameterJdbcTemplate` / native query cho thao tác ghế |
| CSDL & migration | PostgreSQL 16, **Flyway** |
| Bảo mật | Spring Security 6, JWT (jjwt), BCrypt, `@PreAuthorize` |
| Mapping / boilerplate | MapStruct, Lombok |
| Thời gian thực | Spring WebSocket + **STOMP** (simple broker; có thể đổi sang broker relay khi chạy nhiều instance) |
| Job định kỳ | `@Scheduled` + **ShedLock** (khóa qua JDBC) |
| Cache / rate limit | Redis 7 (Spring Data Redis) + **Bucket4j** |
| Xử lý sau commit | Spring `ApplicationEvent` + `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` |
| Email | Spring Mail + Thymeleaf template; Mailpit khi dev |
| QR | ZXing (tạo ảnh QR), nội dung ký HMAC-SHA256 |
| Xuất báo cáo | Apache POI (Excel), OpenCSV |
| Tài liệu API | springdoc-openapi (Swagger UI) |
| Kiểm thử | JUnit 5, Mockito, Spring Boot Test, **PostgreSQL nhúng (Zonky)** cho test tích hợp — không cần Docker, **ArchUnit** (ép quy tắc tầng), JaCoCo |
| Benchmark | JMH (cho `seat-engine`) |

### 2.2 Frontend

| Thành phần | Công nghệ |
|---|---|
| Framework | **Angular 19** (standalone components, signals, control flow `@if/@for`) |
| UI | **NG-ZORRO (Ant Design for Angular) 19**, locale `vi_VN`, theme Less |
| Workspace | Angular CLI multi-project: `web`, `admin`, thư viện `shared` |
| HTTP / state | `HttpClient` + interceptor; state bằng **signals** trong service store (facade) |
| Realtime | `@stomp/rx-stomp` |
| Biểu đồ | `ngx-echarts` + ECharts |
| Quét QR | `@zxing/ngx-scanner` |
| Kiểm thử | Jasmine/Karma (unit, mặc định Angular CLI), **Playwright** (E2E) |

### 2.3 Khác

| Thành phần | Công nghệ |
|---|---|
| Kiểm thử tải & đồng thời | **k6** |
| Hạ tầng dev | Docker Compose: postgres, redis, mailpit |

---

## 2A. Cấu trúc dự án & kiến trúc phân tầng

### 2A.1 Cấu trúc thư mục gốc

```
movie-booking-system/
├── backend/                         # Maven multi-module
├── frontend/                        # Angular workspace
├── tools/k6/                        # kịch bản kiểm thử đồng thời
├── docs/                            # ERD, state machine, sequence, kết quả thực nghiệm
├── docker-compose.yml
├── PLAN.md
└── CLAUDE.md
```

### 2A.2 Backend — kiến trúc phân tầng theo module

Package gốc: `com.moviebooking`.

```
backend/
├── pom.xml                                   # parent POM: Java 17, Spring Boot 3, quản lý version
├── seat-engine/                              # thuật toán cụm ghế — Java thuần, KHÔNG phụ thuộc Spring/JPA
│   └── src/main/java/com/moviebooking/seatengine/
│       ├── model/                            # SeatCell, RoomLayout, SuggestionRequest, SeatOption, ScoreWeights
│       ├── RunFinder.java                    # tìm đoạn ghế liền kề
│       ├── SingleRowCandidateGenerator.java
│       ├── MultiRowCandidateGenerator.java   # beam search
│       ├── OptionScorer.java
│       ├── DiversitySelector.java            # top-K đa dạng
│       └── SeatSuggestionEngine.java         # điểm vào duy nhất
├── seat-bench/                               # thực nghiệm: JMH + bộ sinh dữ liệu + baseline, xuất CSV
└── api/                                      # ứng dụng Spring Boot
    └── src/
        ├── main/java/com/moviebooking/
        │   ├── MovieBookingApplication.java
        │   ├── common/                       # BaseEntity (id, createdAt, updatedAt), ApiResponse, PageResponse, utils
        │   ├── config/                       # Jackson, OpenAPI, Async, Scheduling/ShedLock, Redis, WebSocket, CORS
        │   ├── security/                     # SecurityConfig, JwtService, JwtAuthenticationFilter,
        │   │                                 # CustomUserDetails, CurrentUser (annotation), RestAuthEntryPoint
        │   ├── exception/                    # ErrorCode (enum), BusinessException, GlobalExceptionHandler
        │   ├── infrastructure/               # adapter ra bên ngoài, không chứa nghiệp vụ
        │   │   ├── payment/                  # PaymentProvider (interface), MockPaymentProvider, VnpayPaymentProvider
        │   │   ├── mail/                     # MailSender
        │   │   ├── qr/                       # QrCodeGenerator, TicketSigner (HMAC)
        │   │   ├── realtime/                 # RealtimePublisher (SimpMessagingTemplate)
        │   │   └── ratelimit/                # Bucket4j + Redis
        │   ├── scheduler/                    # HoldExpiryJob, GroupDeadlineJob, OrderCompletionJob
        │   └── modules/
        │       ├── auth/        ├── user/         ├── movie/
        │       ├── cinema/      (rạp, phòng, ghế, sơ đồ)
        │       ├── showtime/    (suất chiếu, giá, showtime_seats)
        │       ├── booking/     (hold, order, ticket)
        │       ├── payment/     ├── group/        ├── checkin/
        │       ├── promotion/   ├── notification/ ├── statistics/
        │       ├── setting/     └── audit/
        └── main/resources/
            ├── application.yml, application-dev.yml, application-test.yml
            ├── db/migration/                 # Flyway: V1__init_auth.sql, V2__catalog.sql, ...
            ├── db/seed/                      # dữ liệu demo (chỉ chạy profile dev)
            └── templates/mail/               # Thymeleaf
```

**Cấu trúc bên trong mỗi module** (ví dụ `group`):

```
modules/group/
├── controller/
│   ├── GroupController.java                  # /api/v1/groups/**
│   └── AdminGroupController.java             # /api/v1/admin/groups/**
├── dto/
│   ├── request/   CreateGroupRequest, RsvpRequest, LockGroupRequest, ...
│   └── response/  GroupResponse, GroupSummaryResponse, SeatOptionResponse, ...
├── mapper/        GroupMapper.java           # MapStruct entity ↔ DTO
├── service/
│   ├── GroupService.java                     # interface
│   ├── GroupSeatService.java
│   └── impl/      GroupServiceImpl.java, GroupSeatServiceImpl.java
├── repository/    BookingGroupRepository, GroupMemberRepository, ...
├── entity/        BookingGroup, GroupMember, GroupShowtimeOption, GroupVote, ...
├── enums/         GroupStatus, GroupRole, RsvpStatus
└── event/         GroupLockedEvent, GroupSeatsHeldEvent, ... (+ listener)
```

**Quy tắc phân tầng (bắt buộc, kiểm tra bằng ArchUnit)**

| # | Quy tắc |
|---|---|
| L1 | Luồng gọi một chiều: `controller → service → repository → entity`. Controller **không** gọi repository. |
| L2 | Controller chỉ nhận/trả **DTO**; không bao giờ trả entity ra ngoài. Validate cú pháp bằng annotation trên DTO (`@Valid`). |
| L3 | Nghiệp vụ và `@Transactional` chỉ nằm ở `service/impl`. Kiểm tra nghiệp vụ lỗi → ném `BusinessException(ErrorCode.X)`. |
| L4 | Module A cần dữ liệu của module B → gọi **service** của B, không dùng repository/entity của B trực tiếp (trừ quan hệ JPA đọc). |
| L5 | `infrastructure` không phụ thuộc `modules`; `modules` dùng infrastructure qua interface. |
| L6 | Tác dụng phụ (email, realtime, thông báo, audit) phát qua `ApplicationEvent`, xử lý **sau commit**. |
| L7 | `seat-engine` không import gì từ Spring/JPA; service `GroupSeatServiceImpl` chuyển dữ liệu DB sang model của engine. |
| L8 | Endpoint quản trị nằm trong `AdminXxxController` cùng module, path `/api/v1/admin/**`, có `@PreAuthorize`. |
| L9 | Mọi thay đổi schema qua Flyway; `spring.jpa.hibernate.ddl-auto=validate`. |

### 2A.3 Frontend — Angular workspace

```
frontend/
├── angular.json
├── package.json
└── projects/
    ├── shared/                               # thư viện dùng chung (ng generate library)
    │   └── src/lib/
    │       ├── core/
    │       │   ├── auth/                     # AuthService, TokenStorage, auth.guard, permission.guard
    │       │   ├── interceptors/             # auth (gắn JWT), refresh-token, error (thông báo nz-message)
    │       │   └── config/                   # API_BASE_URL (InjectionToken)
    │       ├── models/                       # interface DTO + enum, khớp với backend
    │       ├── api/                          # MovieApi, ShowtimeApi, BookingApi, GroupApi, ... (chỉ gọi HTTP)
    │       ├── realtime/                     # StompService (RxStomp)
    │       ├── components/                   # seat-map, countdown, movie-card, status-tag, empty-state
    │       ├── pipes/                        # vnd, viDate, ageRating
    │       └── utils/
    ├── web/                                  # Web người dùng
    │   └── src/app/
    │       ├── app.config.ts, app.routes.ts
    │       ├── layout/                       # main-layout, header, footer
    │       └── features/                     # mỗi feature lazy-load
    │           ├── auth/  home/  movies/  cinemas/
    │           ├── booking/                  # chọn ghế, xác nhận, thanh toán, vé
    │           ├── group/                    # tạo, tham gia, chi tiết, bình chọn, chọn ghế nhóm, thanh toán
    │           ├── account/                  # hồ sơ, đơn hàng, vé của tôi, nhóm của tôi
    │           └── notifications/
    └── admin/                                # Web quản trị
        └── src/app/
            ├── app.config.ts, app.routes.ts
            ├── layout/                       # nz-layout + sider menu theo quyền
            └── features/
                ├── dashboard/  movies/  cinemas/ (seat-layout-designer)  showtimes/
                ├── orders/  groups/  users/  promotions/  checkin/  settings/  audit/
```

**Cấu trúc bên trong mỗi feature** (ví dụ `web/features/group`):

```
group/
├── group.routes.ts
├── pages/                    # component "smart": gắn route, inject store
│   ├── group-create.page.ts
│   ├── group-detail.page.ts
│   └── group-seat-selection.page.ts
├── components/               # component "presentational": chỉ @Input/@Output (input()/output())
│   ├── member-list/  vote-panel/  seat-option-card/  group-progress/
└── group.store.ts            # facade: signals + gọi GroupApi + nghe StompService
```

**Quy tắc phân tầng frontend**

| # | Quy tắc |
|---|---|
| F1 | `page → store (facade) → api service → HttpClient`. Component không inject `HttpClient` trực tiếp. |
| F2 | Component presentational không inject service nghiệp vụ; chỉ nhận input và phát output. |
| F3 | Model/DTO và API service chỉ định nghĩa ở `shared`, hai app không tự định nghĩa lại. |
| F4 | Mọi feature lazy-load; guard theo đăng nhập và theo permission. |
| F5 | Đồng hồ đếm ngược luôn tính từ `expiresAt` do server trả về. |

---

## 3. Tác nhân & phân quyền

| Vai trò | Phạm vi |
|---|---|
| Khách (chưa đăng nhập) | Xem phim, rạp, lịch chiếu, sơ đồ ghế |
| CUSTOMER | Đặt vé, tạo/tham gia nhóm, xem vé, hồ sơ |
| — Trưởng nhóm | Vai trò **trong nhóm** (không phải vai trò hệ thống): chốt phương án, chọn ghế, thanh toán |
| STAFF | Quét/kiểm tra vé, tra cứu đơn tại rạp |
| ADMIN | Toàn quyền quản trị, cấu hình, báo cáo |

Permission dạng `resource:action` (ví dụ `movie:write`, `order:refund`, `ticket:checkin`). **Kiểm tra quyền ở backend** (`@PreAuthorize("hasAuthority('order:refund')")`), giao diện chỉ ẩn/hiện nút.

---

## 4. Danh sách chức năng (đã rút gọn & gộp)

Ưu tiên: **P0** = bắt buộc (MVP), **P1** = nên có, **P2** = mở rộng / hướng phát triển.

### 4.1 Web người dùng

**Tài khoản (USR)**
| Mã | Chức năng | Ưu tiên |
|---|---|---|
| USR01 | Đăng ký (họ tên, email, SĐT, mật khẩu, ngày sinh) | P0 |
| USR02 | Đăng nhập / đăng xuất / refresh token | P0 |
| USR03 | Xác thực email (OTP/link) | P1 |
| USR04 | Quên mật khẩu qua email | P1 |
| USR05 | Xem / cập nhật hồ sơ, ảnh đại diện | P0 |
| USR06 | Đổi mật khẩu | P0 |

**Phim (MOV)**
| Mã | Chức năng | Ưu tiên |
|---|---|---|
| MOV01 | Trang chủ: đang chiếu, sắp chiếu, nổi bật | P0 |
| MOV02 | Danh sách phim + tìm kiếm + lọc (thể loại, độ tuổi, ngôn ngữ, định dạng) | P0 |
| MOV03 | Chi tiết phim, trailer (YouTube embed) | P0 |
| MOV04 | Phim yêu thích | P2 |
| MOV05 | Phim tương tự (theo thể loại) | P2 |

**Rạp & suất chiếu (SCH)**
| Mã | Chức năng | Ưu tiên |
|---|---|---|
| SCH01 | Danh sách & chi tiết rạp | P0 |
| SCH02 | Lịch chiếu theo phim / rạp / ngày, lọc giờ, định dạng | P0 |
| SCH03 | Thông tin suất chiếu + bảng giá theo loại ghế | P0 |

**Đặt vé cá nhân (BKG)**
| Mã | Chức năng | Ưu tiên |
|---|---|---|
| BKG01 | Sơ đồ ghế theo suất chiếu (lối đi, loại ghế, trạng thái) — **realtime** | P0 |
| BKG02 | Chọn ghế (kiểm tra ghế đôi, quy tắc ghế lẻ) | P0 |
| BKG03 | Giữ ghế có thời hạn (backend), đồng hồ đếm ngược lấy theo `held_until` của server | P0 |
| BKG04 | Tính tiền: giá ghế × loại vé + phụ thu ngày − giảm giá | P0 |
| BKG05 | Mã giảm giá | P1 |
| BKG06 | Xác nhận đơn → thanh toán | P0 |
| BKG07 | Vé điện tử + QR (ký HMAC) | P0 |
| BKG08 | Email xác nhận | P1 |
| BKG09 | **Lịch sử đơn hàng** + **Vé của tôi** (gộp USR09/USR10/BKG15/PAY04 cũ) | P0 |
| BKG10 | Hủy vé theo chính sách → hoàn tiền | P1 |
| BKG11 | Loại vé (người lớn / HSSV / trẻ em) | P1 |
| BKG12 | Combo bắp nước | P2 |

**Đặt vé theo nhóm (GRP)** — trọng tâm
| Mã | Chức năng | Ưu tiên |
|---|---|---|
| GRP01 | Tạo nhóm (tên, phim, rạp, khoảng ngày dự kiến, hạn chốt) | P0 |
| GRP02 | Mời bằng link / mã mời; tham gia nhóm | P0 |
| GRP03 | Danh sách thành viên, loại thành viên, rời nhóm | P0 |
| GRP04 | Chuyển quyền trưởng nhóm | P1 |
| GRP05 | Thành viên phản hồi tham gia (Có / Không / Chưa chắc) + số khách đi kèm + ngân sách tối đa | P0 |
| GRP06 | Đề xuất & **bình chọn suất chiếu** (gộp GRP12/GRP14 cũ), realtime | P0 |
| GRP07 | Tổng hợp nhu cầu: số người, số vé, ngân sách, suất được bình chọn nhiều nhất, **cảnh báo số ghế liền kề còn lại** của từng suất | P0 |
| GRP08 | Cảnh báo giới hạn độ tuổi (thành viên chưa đủ tuổi xem phim T13/T16/T18) | P1 |
| GRP09 | Trưởng nhóm **chốt** suất chiếu & số vé → nhóm khóa danh sách | P0 |
| GRP10 | **Đề xuất cụm ghế tự động** (top 3–5 phương án, có điểm số & lý do) | P0 |
| GRP11 | Tùy chọn đề xuất: cho phép chia hàng, loại ghế, giá tối đa | P0 |
| GRP12 | Xem trước phương án trên sơ đồ; so sánh phương án | P0 |
| GRP13 | Chọn / chỉnh ghế thủ công | P0 |
| GRP14 | **Giữ ghế cả nhóm nguyên khối** (tất cả hoặc không); nếu thất bại → đề xuất lại ngay | P0 |
| GRP15 | Thành viên xem realtime phương án ghế trưởng nhóm đang chọn / đã giữ | P1 |
| GRP16 | Trưởng nhóm thanh toán toàn bộ; hiển thị số tiền chia cho từng người (tham khảo) | P0 |
| GRP17 | Phân phối vé: gán ghế cho thành viên (tự động theo thứ tự, trưởng nhóm sửa được); thành viên thấy vé của mình | P0 |
| GRP18 | Thông báo: được mời, đổi suất, đã chốt, đã giữ ghế, đã thanh toán, sắp hết hạn | P0 (in-app), P1 (email) |
| GRP19 | Theo dõi tiến trình nhóm (thanh trạng thái, ai chưa phản hồi) | P0 |
| GRP20 | Nhóm của tôi / lịch sử nhóm | P0 |
| GRP21 | Hủy đơn nhóm theo chính sách | P1 |
| GRP22 | Sở thích chỗ ngồi của thành viên ("ngồi cạnh X", "đầu hàng") | P2 |
| GRP23 | Mô hình B: mỗi người tự thanh toán | P2 |

**Khác**
| Mã | Chức năng | Ưu tiên |
|---|---|---|
| NOT01 | Trung tâm thông báo in-app (realtime) | P0 |
| FBK01 | Đánh giá phim (chỉ khi đã có vé đã check-in) | P2 |
| FBK02 | Gửi phản hồi / báo lỗi | P2 |
| LOY01 | Điểm thưởng, ví voucher | P2 |

### 4.2 Web quản trị

| Mã | Chức năng | Ưu tiên |
|---|---|---|
| ADM01 | Dashboard: doanh thu, vé bán, tỷ lệ lấp đầy, tỷ lệ thanh toán thất bại, đơn trung bình | P0 |
| ADM02 | Thống kê nhóm: số nhóm, tỷ lệ chuyển đổi tạo → đặt thành công, quy mô nhóm, **tỷ lệ tìm được cụm liền kề**, thời gian trung bình tạo → đặt, lý do thất bại | P0 |
| ADM03 | Lọc theo thời gian/rạp/phim; xuất CSV/Excel | P1 |
| MOVADM | Phim, thể loại, đạo diễn/diễn viên, poster/trailer, độ tuổi, trạng thái | P0 |
| CIN01 | Rạp, phòng chiếu | P0 |
| CIN02 | **Trình thiết kế sơ đồ ghế** dạng lưới: đặt ghế/lối đi, loại ghế (thường/VIP/đôi/khuyết tật), ghế hỏng/khóa, xem trước | P0 |
| SCHADM01 | Tạo/sửa suất chiếu; **kiểm tra trùng phòng + thời gian dọn phòng** | P0 |
| SCHADM02 | Giá vé theo loại ghế, ngày thường/cuối tuần/lễ, định dạng | P0 |
| SCHADM03 | Hủy suất chiếu → tự động hoàn tiền + thông báo cho khách & nhóm | P1 |
| SCHADM04 | Theo dõi ghế của suất: trống / đang giữ / đã bán | P0 |
| ORDADM | Danh sách, tìm kiếm, lọc, chi tiết đơn, lịch sử thay đổi đơn | P0 |
| ORDADM-R | Hoàn tiền theo quyền; đối soát giao dịch | P1 |
| CHK01 | **Trang check-in cho STAFF**: quét QR vé hoặc QR đơn (check-in cả nhóm) | P0 |
| GRPADM | Danh sách nhóm, chi tiết, sơ đồ ghế của nhóm, nhóm hết hạn, nhật ký thao tác | P0 |
| USRADM | Người dùng: tìm kiếm, khóa/mở; vai trò & quyền | P0 |
| PROADM | Mã giảm giá: thời gian, giá trị đơn tối thiểu, lượt dùng, **giảm theo số lượng vé nhóm** | P1 |
| SYS01 | **Cấu hình hệ thống tập trung** (xem mục 9) — chỉ cấu hình ở đây, không lặp ở module khác | P0 |
| SYS02 | Nhật ký hệ thống (audit log) | P0 |
| SYS03 | Mẫu email | P2 |

---

## 5. Quy tắc nghiệp vụ

| Mã | Quy tắc |
|---|---|
| BR01 | Một ghế trong một suất chiếu chỉ thuộc **tối đa một** vé hợp lệ (ràng buộc ở CSDL). |
| BR02 | Giữ ghế có hạn; hạn do server quản lý. Ghế hết hạn được coi là trống ngay cả khi job dọn chưa chạy. |
| BR03 | Giữ ghế nhiều ghế (đặc biệt nhóm) là **nguyên tử**: giữ được tất cả hoặc không giữ ghế nào. |
| BR04 | Kiểm tra lại hold và trạng thái đơn trước khi tạo phiên thanh toán và khi nhận kết quả. |
| BR05 | Xử lý kết quả thanh toán **idempotent** theo `(provider, provider_txn_id)`. |
| BR06 | Đơn hàng có trạng thái rõ ràng, chuyển trạng thái bằng câu lệnh có điều kiện (xem mục 6). |
| BR07 | Mỗi vé có mã duy nhất; QR chứa `ticketCode` + chữ ký HMAC. |
| BR08 | Chỉ trưởng nhóm được chốt, chọn ghế, giữ ghế, thanh toán, loại thành viên. |
| BR09 | Đơn đã thanh toán không sửa tùy tiện; mọi thay đổi ghi `order_events`. |
| BR10 | Hoàn tiền theo chính sách (ví dụ: hủy trước giờ chiếu ≥ `cancel_before_hours`) và theo quyền. |
| BR11 | Kiểm tra quyền ở backend. |
| BR12 | Ghi audit log cho thao tác quản trị, thanh toán, hoàn tiền, đổi ghế. |
| BR13 | **Ghế đôi** bán nguyên cặp, tính 2 chỗ ngồi. |
| BR14 | **Không để ghế lẻ**: lựa chọn tạo ra đúng 1 ghế trống bị kẹt giữa (ghế đã bán/giữ hoặc tường/lối đi) bị chặn (đặt vé cá nhân, cấu hình bật/tắt) hoặc bị trừ điểm (thuật toán nhóm). |
| BR15 | **Chống giữ ghế tràn lan**: giới hạn số ghế đang giữ / người, số nhóm đang hoạt động / người, rate limit API giữ ghế. |
| BR16 | Không bán vé cho suất đã bắt đầu quá `sale_close_after_start_min` phút. |
| BR17 | Thanh toán đến sau khi hold hết hạn: nếu ghế còn trống → giữ lại & xác nhận; nếu không → đơn `REFUND_PENDING` và hoàn tiền tự động. |
| BR18 | Kích thước nhóm ≤ `group_max_size`; số vé nhóm = số thành viên "Có" + khách đi kèm. |

---

## 6. Máy trạng thái

### 6.1 Ghế theo suất chiếu (`showtime_seats.status`)
```
AVAILABLE ──hold──▶ HELD ──payment ok──▶ SOLD
    ▲                │
    └──expire/release┘
BLOCKED (admin khóa / ghế hỏng) — không thể giữ
SOLD ──refund/cancel──▶ AVAILABLE
```

### 6.2 Đơn hàng (`orders.status`)
```
PENDING_PAYMENT ──paid──▶ PAID ──showtime end──▶ COMPLETED
      │                    │
      ├──hold expired──▶ EXPIRED      ├──cancel──▶ CANCELLED ──▶ REFUNDED
      └──user cancel──▶ CANCELLED     
PENDING_PAYMENT ──paid after expiry & seats lost──▶ REFUND_PENDING ──▶ REFUNDED
```

### 6.3 Nhóm (`booking_groups.status`)
```
OPEN (mời + phản hồi + bình chọn)
  ├──leader lock──▶ LOCKED (đã chốt suất & số vé, đang chọn ghế)
  │                   └──hold ok──▶ HOLDING ──paid──▶ BOOKED ──showtime end──▶ COMPLETED
  │                                   └──hold expired──▶ LOCKED (chọn lại)
  ├──deadline passed──▶ EXPIRED
  └──leader cancel──▶ CANCELLED
LOCKED ──leader unlock (đổi suất)──▶ OPEN   (thông báo cho thành viên)
BOOKED ──cancel theo chính sách──▶ CANCELLED
```

Mọi chuyển trạng thái thực hiện bằng `UPDATE ... WHERE status = <trạng thái cũ>` và kiểm tra số dòng bị ảnh hưởng.

---

## 7. Mô hình dữ liệu (tóm tắt)

```
users(id, email UQ, phone UQ, password_hash, full_name, dob, avatar_url, email_verified_at, status, created_at)
roles(id, code) · permissions(id, code) · role_permissions · user_roles
refresh_tokens(id, user_id, token_hash, expires_at, revoked_at)

genres(id, name UQ, slug UQ) · people(id, full_name, avatar_url)
movies(id, title, original_title, slug UQ, description, duration_min, age_rating[P,K,T13,T16,T18],
        language, release_date, poster_url, banner_url, trailer_url, status[COMING_SOON,NOW_SHOWING,ENDED])
movie_genres(movie_id, genre_id) · movie_people(id, movie_id, person_id, role[DIRECTOR,ACTOR], sort_order)
      UQ(movie_id, person_id, role)

cinemas(id, name UQ, address, city, phone, description, image_url, amenities jsonb, status)
rooms(id, cinema_id, name, room_type[STANDARD,PREMIUM,IMAX], grid_rows, grid_cols, layout_locked, status)
      -- layout_locked = true từ suất chiếu đầu tiên: không vẽ lại sơ đồ được nữa (mã ghế ổn định)
seat_types(code PK[STANDARD,VIP,COUPLE,ACCESSIBLE], name, capacity, col_span, surcharge, color)   -- COUPLE: 2 chỗ, 2 ô
seats(id, room_id, row_label, number, grid_row, grid_col, col_span, seat_type_code, active)
      UQ(room_id, row_label, number), UQ(room_id, grid_row, grid_col)
      -- ô lưới không có ghế = lối đi/khoảng trống; nhãn hàng/số ghế do backend tính

ticket_base_prices(format[TWO_D,THREE_D,IMAX], day_type[WEEKDAY,WEEKEND,HOLIDAY], price)   PK(format, day_type)
holidays(holiday_date PK, name)
showtimes(id, movie_id, room_id, start_at, end_at, blocked_until, format, audio_type[SUBTITLED,DUBBED],
          day_type, status[SCHEDULED,CANCELLED])
      EXCLUDE USING gist (room_id WITH =, tstzrange(start_at, blocked_until) WITH &&) WHERE status <> 'CANCELLED'
showtime_prices(showtime_id, seat_type_code, price)   -- giá chốt lúc tạo suất (8.6)
showtime_seats(showtime_id, seat_id, status, hold_id NULL, held_until NULL, order_id NULL, version)
      PK(showtime_id, seat_id)  -- sinh sẵn khi tạo suất chiếu

seat_holds(id, user_id, showtime_id, group_id NULL, expires_at, status[ACTIVE,CONVERTED,EXPIRED,RELEASED])
orders(id, code UQ, user_id, showtime_id, group_id NULL, hold_id, status, subtotal, discount, total,
       promotion_id NULL, expires_at, paid_at, created_at)
order_items(id, order_id, seat_id, ticket_type, price)
tickets(id, order_id, seat_id, showtime_id, code UQ, holder_user_id NULL, holder_name, status[VALID,USED,CANCELLED], checked_in_at, checked_in_by)
      partial UQ(showtime_id, seat_id) WHERE status <> 'CANCELLED'      -- chốt chặn cuối cho BR01
payments(id, order_id, provider, provider_txn_id, amount, status, raw jsonb, created_at)
      UQ(provider, provider_txn_id)
refunds(id, order_id, payment_id, amount, reason, status, processed_by)
order_events(id, order_id, type, from_status, to_status, actor_id, data jsonb, created_at)

booking_groups(id, code UQ (mã mời), name, leader_id, movie_id, cinema_id NULL, date_from, date_to,
               rsvp_deadline, status, locked_showtime_id NULL, locked_ticket_count NULL, order_id NULL, created_at)
group_members(group_id, user_id, role[LEADER,MEMBER], rsvp[PENDING,YES,NO,MAYBE], guest_count, max_budget NULL, joined_at, left_at)
group_showtime_options(id, group_id, showtime_id, proposed_by)
group_votes(option_id, user_id)  PK(option_id, user_id)
group_seat_assignments(group_id, ticket_id, user_id NULL, guest_label NULL)
group_events(id, group_id, type, actor_id, data jsonb, created_at)    -- lịch sử + thống kê
seat_suggestion_logs(id, group_id, showtime_id, n, options jsonb, chosen_rank NULL, contiguous bool, duration_ms, created_at)  -- phục vụ ADM02 & thực nghiệm

promotions(id, code UQ, type[PERCENT,AMOUNT], value, max_discount, min_order, min_tickets, starts_at, ends_at,
           usage_limit, per_user_limit, used_count, status)
promotion_usages(promotion_id, user_id, order_id)

notifications(id, user_id, type, title, body, data jsonb, read_at, created_at)
system_settings(key PK, value jsonb, updated_by, updated_at)
audit_logs(id, actor_id, action, resource, resource_id, data jsonb, ip, created_at)
```

ERD chi tiết vẽ trong `docs/erd.md` (Mermaid) ở giai đoạn M1.

---

## 8. Thiết kế kỹ thuật cốt lõi

### 8.1 Giữ ghế an toàn khi đồng thời (BR01–BR03)

**Giữ ghế** — một transaction duy nhất cho mọi số lượng ghế (cá nhân hay nhóm):

```sql
-- 1. kiểm tra giới hạn của user (BR15)
-- 2. tạo seat_holds (status ACTIVE, expires_at = now() + ttl)
UPDATE showtime_seats
SET status = 'HELD', hold_id = $holdId, held_until = $expiresAt, version = version + 1
WHERE showtime_id = $showtimeId
  AND seat_id = ANY($seatIds)
  AND (status = 'AVAILABLE' OR (status = 'HELD' AND held_until < now()))
RETURNING seat_id;
-- 3. nếu số dòng trả về != số ghế yêu cầu → ROLLBACK, trả 409 kèm danh sách ghế bị mất
-- 4. kiểm tra BR13/BR14 trên kết quả; COMMIT
-- 5. sau commit: phát sự kiện realtime seat:held
```

- Câu `UPDATE` có điều kiện là atomic ở mức dòng trong PostgreSQL → hai request tranh một ghế thì chỉ một request cập nhật được.
- **Nguyên tử cho cả nhóm** vì toàn bộ nằm trong một transaction.
- Chốt chặn cuối: partial unique index trên `tickets(showtime_id, seat_id)`.

**Giải phóng ghế hết hạn**:
- *Lazy*: mọi truy vấn coi `HELD AND held_until < now()` là trống.
- *Chủ động*: `HoldExpiryJob` (`@Scheduled` + ShedLock) chạy mỗi 15–30 giây, đặt về `AVAILABLE`, chuyển `seat_holds` → `EXPIRED`, đơn → `EXPIRED`, nhóm `HOLDING` → `LOCKED`, phát sự kiện realtime.

**Xác nhận thanh toán** (một transaction):
1. `INSERT payments ... ON CONFLICT (provider, provider_txn_id) DO NOTHING` → nếu đã có thì trả kết quả cũ (BR05).
2. `UPDATE orders SET status='PAID' WHERE id=$1 AND status='PENDING_PAYMENT'`.
3. `UPDATE showtime_seats SET status='SOLD', order_id=... WHERE hold_id=$holdId AND status='HELD'`. Nếu thiếu ghế (hold đã hết hạn) → thử giữ lại theo BR17, nếu không được → `REFUND_PENDING`.
4. Tạo `tickets`, `group_seat_assignments`, `order_events`; phát `OrderPaidEvent` → listener `AFTER_COMMIT` + `@Async` gửi email, thông báo, realtime.

Câu `UPDATE` giữ ghế viết bằng native query (`@Modifying @Query(nativeQuery = true)`) hoặc `NamedParameterJdbcTemplate` trong `ShowtimeSeatRepository`, gọi từ `HoldServiceImpl` có `@Transactional`.

### 8.2 Thuật toán đề xuất cụm ghế (module `backend/seat-engine`)

Backend là nguồn kết quả chính thức. Frontend chỉ hiển thị phương án do API trả về và kiểm tra nhẹ (ghế đôi, ghế lẻ) khi chọn thủ công.

**Đầu vào**
- Lưới phòng: danh sách ghế `(id, gridRow, gridCol, colSpan, capacity, typeCode, price)`; ô không có ghế = lối đi.
- Tập ghế khả dụng của suất chiếu.
- `n`: số người cần chỗ ngồi.
- Tùy chọn: `allowSplit`, `maxRows` (mặc định 2–3), `allowedSeatTypes`, `maxPricePerSeat`, `topK` (mặc định 5), trọng số `w`.

**Định nghĩa**
- Hai ghế **liền kề ngang** khi cùng hàng và `colB = colA + colSpanA` (không có ô trống ở giữa).
- **Đoạn (run)**: dãy ghế khả dụng liền kề ngang tối đa trong một hàng.
- Ghế đôi không được tách, tính `capacity = 2`.

**Các bước**
1. **Tìm đoạn**: duyệt từng hàng → danh sách đoạn. Độ phức tạp O(S) với S là số ghế.
2. **Ứng viên một hàng**: trượt cửa sổ trên mỗi đoạn có tổng capacity ≥ n, lấy các cửa sổ có capacity đúng bằng n (cho phép n+1 nếu ghế đôi buộc phải thừa — trừ điểm).
3. **Ứng viên nhiều hàng** (khi `allowSplit` hoặc bước 2 không đủ `topK`): với mỗi bộ k hàng **liên tiếp** (k = 2..maxRows), chia n thành k phần cân bằng (ví dụ 5 → 3+2), mỗi phần lấy một cửa sổ trong một đoạn của hàng tương ứng, yêu cầu các cửa sổ **chồng cột** với nhau. Cắt tỉa bằng beam search (giữ B = 50 ứng viên tốt nhất ở mỗi hàng) để tránh bùng nổ tổ hợp.
4. **Chấm điểm** (điểm càng cao càng tốt, các thành phần chuẩn hóa về [0, 1]):

```
score = 100
      − w_center · d_center        // khoảng cách tâm cụm tới "điểm xem đẹp" (giữa ngang, ~2/3 chiều sâu phòng)
      − w_rows   · (rows − 1)      // số hàng phải chia
      − w_align  · misalign        // độ lệch cột giữa các hàng (nhiều hàng)
      − w_orphan · orphans         // số ghế lẻ tạo ra (BR14)
      − w_price  · overBudget      // phần vượt ngân sách / giá trung bình
      − w_waste  · wastedSeats     // chỗ thừa do ghế đôi
```

   Trọng số mặc định lưu trong `system_settings.seat_weights`, admin chỉnh được.
5. **Chọn top-K đa dạng**: sắp xếp theo điểm, loại phương án trùng > 50% ghế với phương án đã chọn, trả về K phương án kèm **giải thích** (ví dụ "Cùng hàng E, gần trung tâm, 0 ghế lẻ").
6. **Không có phương án**: trả về `reason` (không đủ ghế trống / không đủ ghế gần nhau) + gợi ý suất chiếu khác cùng ngày còn cụm phù hợp.

**Thực nghiệm (module `backend/seat-bench`, đo thời gian bằng JMH)**
- Sinh ngẫu nhiên phòng 100 / 200 / 300 ghế, tỷ lệ lấp đầy 30 / 60 / 85 %, kiểu lấp đầy ngẫu nhiên và kiểu tập trung ở giữa, n = 2..10.
- So sánh với 2 baseline: **tham lam trái → phải** và **chọn n ghế trống bất kỳ gần trung tâm nhất**.
- Chỉ số: tỷ lệ tìm được cụm cùng hàng, điểm trung bình, số ghế lẻ tạo ra, thời gian chạy (p50/p95).
- Xuất bảng CSV + biểu đồ cho báo cáo.

### 8.3 Thời gian thực (Spring WebSocket + STOMP)

Endpoint kết nối: `/ws` (STOMP over WebSocket). JWT gửi trong header `Authorization` của frame `CONNECT`, xác thực bằng `ChannelInterceptor`.

| Destination | Sự kiện (server → client), trường `type` trong payload |
|---|---|
| `/topic/showtimes/{id}/seats` | `SEAT_HELD`, `SEAT_RELEASED`, `SEAT_SOLD` (payload: danh sách seatId) |
| `/topic/groups/{id}` | `MEMBER_JOINED`, `MEMBER_LEFT`, `RSVP_UPDATED`, `VOTE_UPDATED`, `STATUS_CHANGED`, `SEATS_PREVIEWED`, `SEATS_HELD`, `PAID` |
| `/user/queue/notifications` | `NOTIFICATION_NEW` |

- Đăng ký `/topic/groups/{id}` chỉ được khi là thành viên nhóm (kiểm tra trong `ChannelInterceptor` ở frame `SUBSCRIBE`).
- Client khi kết nối lại phải **tải lại snapshot** qua REST (sự kiện chỉ là delta).
- Mọi thay đổi đi qua REST; WebSocket chỉ phát, không nhận lệnh nghiệp vụ.
- Phát sự kiện qua `RealtimePublisher` trong listener `AFTER_COMMIT` để client không thấy trạng thái chưa commit.

### 8.6 Giá vé & lịch chiếu (đã triển khai ở M2)

**Giá**: `giá ghế = giá cơ bản(định dạng, loại ngày) × số chỗ của ghế + phụ thu loại ghế`.
- Loại ngày theo giờ Việt Nam: ngày lễ (bảng `holidays`) → HOLIDAY; thứ 7/CN → WEEKEND; còn lại WEEKDAY.
- Ví dụ 2D ngày thường (75.000đ): ghế thường 75.000đ, VIP 95.000đ, ghế đôi 2 × 75.000 + 30.000 = 180.000đ.
- Giá được **chốt** vào `showtime_prices` khi tạo suất; quản trị viên có thể ghi đè từng loại ghế. Đổi bảng giá
  không ảnh hưởng suất đã tạo.

**Chống trùng lịch** (SCHADM04, 05): `end_at = start_at + thời lượng phim`, `blocked_until = end_at +
cleaning_buffer_min`. Ba lớp bảo vệ:
1. Service truy vấn các suất chồng khoảng `[start_at, blocked_until)` → trả `SHOWTIME_OVERLAP` kèm danh sách suất trùng.
2. `SELECT ... FOR UPDATE` trên dòng phòng → các yêu cầu tạo suất cùng phòng chạy tuần tự.
3. Ràng buộc `EXCLUDE USING gist` trong PostgreSQL — chốt chặn cuối kể cả khi bỏ qua tầng ứng dụng.

Test `concurrentCreationInSameSlotAllowsExactlyOne`: 6 yêu cầu đồng thời cho cùng khung giờ → đúng 1 thành công.

### 8.4 Thanh toán
- Interface `PaymentProvider { String code(); PaymentInitResult createPayment(Order order); PaymentCallbackResult verifyCallback(Map<String, String> params); }`; chọn provider qua `Map<String, PaymentProvider>` do Spring inject.
- `MockProvider`: trang giả lập Thành công / Thất bại / Gửi callback 2 lần (để demo BR05).
- `VnpayProvider`: xác thực `vnp_SecureHash`; xử lý cả Return URL (chỉ để hiển thị) và **IPN** (nguồn sự thật).
- Phiên thanh toán hết hạn trước `held_until`.

### 8.5 Bảo mật
- BCrypt (strength 10–12), JWT ngắn hạn + refresh token xoay vòng, lưu hash; Spring Security stateless.
- Rate limit (Bucket4j + Redis): đăng nhập, quên mật khẩu, giữ ghế, tạo nhóm.
- QR: `base64url(ticketCode.exp).HMAC_SHA256`, đối chiếu CSDL khi check-in, chỉ check-in được một lần (`UPDATE ... WHERE status='VALID'`).
- Validate input bằng Jakarta Bean Validation; security headers mặc định của Spring Security; CORS theo domain cụ thể.
- Chống IDOR: mọi truy vấn đơn/vé/nhóm của khách đều lọc theo `currentUser` ở tầng service.
- Mã mời nhóm: 8 ký tự ngẫu nhiên, có thể tạo lại (vô hiệu mã cũ).

---

## 9. Tham số cấu hình (`system_settings`)

| Key | Mặc định | Ý nghĩa |
|---|---|---|
| `hold_ttl_single_sec` | 420 (7 phút) | Thời gian giữ ghế đặt vé cá nhân |
| `hold_ttl_group_sec` | 600 (10 phút) | Thời gian giữ ghế nhóm |
| `max_seats_per_booking` | 8 | Số ghế tối đa / đơn cá nhân |
| `group_max_size` | 20 | Số vé tối đa / nhóm |
| `max_active_groups_per_user` | 3 | BR15 |
| `max_active_holds_per_user` | 2 | BR15 |
| `cleaning_buffer_min` | 15 | Thời gian dọn phòng giữa hai suất |
| `cancel_before_hours` | 24 | Được hủy/hoàn tiền nếu trước giờ chiếu ≥ X giờ |
| `refund_percent` | 100 | Tỷ lệ hoàn tiền |
| `sale_close_after_start_min` | 15 | BR16 |
| `orphan_seat_rule` | `block` | `block` / `warn` / `off` (đặt vé cá nhân) |
| `seat_weights` | `{center:30, rows:25, align:10, orphan:15, price:10, waste:10}` | Trọng số thuật toán |
| `group_split_max_rows` | 3 | Số hàng tối đa khi chia nhóm |
| `rsvp_reminder_before_min` | 60 | Nhắc thành viên trước hạn chốt |

---

## 10. API (tóm tắt — tiền tố `/api/v1`)

```
Auth      POST /auth/register | /auth/login | /auth/refresh | /auth/logout | /auth/forgot | /auth/reset | /auth/verify-email
Me        GET/PATCH /me · POST /me/password · GET /me/orders · GET /me/tickets · GET /me/groups · GET /me/notifications

Catalog   GET /movies?q=&genre=&age=&lang=&format=&status= · GET /movies/:slug · GET /genres
          GET /cinemas · GET /cinemas/:id · GET /showtimes?movieId=&cinemaId=&date=
          GET /showtimes/:id · GET /showtimes/:id/seats          (snapshot sơ đồ + trạng thái)

Booking   POST /holds {showtimeId, seatIds}  → {holdId, expiresAt}
          DELETE /holds/:id
          POST /orders {holdId, ticketTypes, promoCode} → order
          POST /orders/:id/pay {provider} → {redirectUrl}
          POST /orders/:id/cancel
          GET  /orders/:code
Payment   GET /payments/:provider/return · POST|GET /payments/:provider/ipn

Groups    POST /groups · GET /groups/:id · PATCH /groups/:id
          POST /groups/join {code} · POST /groups/:id/leave · DELETE /groups/:id/members/:userId
          POST /groups/:id/transfer-leader · POST /groups/:id/invite-code/regenerate
          PUT  /groups/:id/rsvp {rsvp, guestCount, maxBudget}
          POST /groups/:id/options {showtimeId} · POST /groups/:id/options/:optionId/vote · DELETE .../vote
          GET  /groups/:id/summary
          POST /groups/:id/lock {showtimeId, ticketCount} · POST /groups/:id/unlock
          POST /groups/:id/seat-suggestions {allowSplit, maxPrice, seatTypes} → [options]
          POST /groups/:id/hold {seatIds} · DELETE /groups/:id/hold
          POST /groups/:id/order → order (leader thanh toán qua /orders/:id/pay)
          PUT  /groups/:id/assignments [{ticketId, userId|guestLabel}]
          POST /groups/:id/cancel

Staff     POST /checkin/scan {qr} · POST /checkin/tickets/:id

Admin     /admin/movies, /admin/genres, /admin/people, /admin/cinemas, /admin/rooms, /admin/rooms/:id/layout,
          /admin/showtimes (+ /validate), /admin/price-rules, /admin/orders, /admin/refunds,
          /admin/groups, /admin/users, /admin/roles, /admin/promotions, /admin/settings, /admin/audit-logs,
          /admin/stats/{overview,revenue,movies,showtimes,groups}, /admin/reports/export
```

Định dạng phản hồi thống nhất: thành công `{ success: true, data }`; lỗi `{ success: false, error: { code: "SEAT_UNAVAILABLE", message, details } }`. Tài liệu API bằng springdoc-openapi (`/swagger-ui.html`).

---

## 11. Lộ trình triển khai

Mỗi giai đoạn kết thúc bằng: code chạy được, test xanh, cập nhật mục **14. Nhật ký tiến độ** trong file này.

### M0 — Khởi tạo dự án ✅
- [x] `backend/`: parent POM + module `seat-engine`, `seat-bench`, `api`; Maven Wrapper
- [x] `api`: các package `common`, `config`, `security`, `exception`, `infrastructure`, `scheduler`, `modules` theo mục 2A.2
- [x] `ApiResponse`, `ErrorCode`, `BusinessException`, `GlobalExceptionHandler`; `BaseEntity` + JPA auditing; springdoc; Actuator health
- [x] Flyway `V1__init.sql`; profile `dev`/`test`; Testcontainers base test
- [x] **ArchUnit test** cho quy tắc L1–L5, L7, L8
- [x] `frontend/`: Angular 19 workspace, app `web`, `admin`, library `shared`; NG-ZORRO + locale `vi_VN`; interceptor, `API_BASE_URL`, proxy dev
- [x] Docker Compose: postgres, redis, mailpit; `.env.example`
- [x] Spotless (backend), ESLint + Prettier (frontend); README chạy dự án
- **Xong khi**: `./mvnw spring-boot:run` và `ng serve web` / `ng serve admin` chạy được, API kết nối DB và Redis, ArchUnit xanh.

### M1 — Xác thực, người dùng, phân quyền ✅
- [x] Flyway: users/roles/permissions/refresh_tokens; seed ADMIN, STAFF, CUSTOMER mẫu
- [x] Đăng ký, đăng nhập, refresh, đăng xuất, hồ sơ, đổi mật khẩu (USR01, 02, 05, 06)
- [x] Spring Security + `JwtAuthenticationFilter`, `@PreAuthorize` theo permission; audit log (AOP `@Audited`)
- [x] Trang đăng nhập/đăng ký (web), đăng nhập admin, quản lý người dùng (USRADM)
- [x] `docs/erd.md` bản đầu, `docs/state-machines.md`
- **Xong khi**: phân quyền chặn đúng ở API (có test).

### M2 — Danh mục & cấu hình rạp (admin) ✅
- [x] CRUD phim, thể loại, người (MOVADM)
- [x] CRUD rạp, phòng; **trình thiết kế sơ đồ ghế dạng lưới** (CIN02) — lưu `seats` theo grid
- [x] Suất chiếu: kiểm tra trùng phòng + thời gian dọn phòng; sinh `showtime_seats` (SCHADM01)
- [x] Bảng giá & ngày lễ (SCHADM02); `system_settings` + trang cấu hình (SYS01)
- [x] Seed dữ liệu demo: 3 rạp, 8 phòng (nhiều kiểu sơ đồ có lối đi và ghế đôi), 15 phim, suất chiếu 7 ngày tới (đủ cho demo, khởi động nhanh)
- **Xong khi**: admin tạo được phòng có lối đi + ghế đôi và lên lịch chiếu không bị trùng.

### M3 — Duyệt phim phía người dùng ✅
- [x] Trang chủ, danh sách & lọc phim, chi tiết phim + trailer (MOV01–03)
- [x] Rạp, lịch chiếu theo ngày (SCH01–03)
- [x] Component sơ đồ ghế `lib-seat-grid` trong `projects/shared` (render theo grid bằng CSS Grid, chú thích loại ghế, phóng to/thu nhỏ trên mobile)
- **Xong khi**: người dùng đi từ trang chủ tới sơ đồ ghế của một suất chiếu.

### M4 — Đặt vé cá nhân (end-to-end với Mock payment)
- [x] API hold theo mục 8.1 + BR13/BR14/BR15; job giải phóng ghế hết hạn
- [x] WebSocket STOMP (`/ws`) + topic `/topic/showtimes/{id}/seats`; `StompService` ở frontend; sơ đồ ghế cập nhật realtime
- [x] Tạo đơn, tính tiền, MockProvider, xác nhận idempotent, tạo vé + QR
- [x] Trang xác nhận, thanh toán, vé điện tử; Lịch sử đơn & Vé của tôi (BKG01–09)
- [x] Thông báo in-app (NOT01); email xác nhận qua event `AFTER_COMMIT` + `@Async` (BKG08)
- [x] Test tích hợp: 2 người giữ cùng ghế; callback thanh toán 2 lần; thanh toán sau khi hết hạn
- **Xong khi**: mở 2 trình duyệt, chọn ghế ở bên này thì bên kia thấy ngay; không thể bán trùng.

### M5 — Thuật toán cụm ghế
- [ ] Module `seat-engine`: tìm đoạn, ứng viên một hàng, nhiều hàng (beam search), chấm điểm, top-K đa dạng, giải thích
- [ ] Unit test các ca: lối đi, ghế đôi, ghế lẻ, phòng gần đầy, n lớn hơn mọi hàng, không có phương án
- [ ] Module `seat-bench`: bộ sinh phòng/tỷ lệ lấp đầy, 2 baseline, JMH; xuất CSV vào `docs/experiments/`
- **Xong khi**: phòng 300 ghế, n ≤ 10, p95 < 50 ms; tất cả test xanh.

### M6 — Đặt vé theo nhóm (Mô hình A)
- [ ] Schema nhóm; tạo nhóm, mã mời/link, tham gia, rời, loại, chuyển trưởng nhóm (GRP01–04)
- [ ] Phản hồi + khách đi kèm + ngân sách; đề xuất & bình chọn suất; tổng hợp + cảnh báo cụm ghế còn lại + cảnh báo độ tuổi (GRP05–08)
- [ ] Chốt / mở chốt; job hết hạn nhóm + nhắc nhở (GRP09, GRP18)
- [ ] Màn hình chọn ghế nhóm: danh sách phương án, xem trước trên sơ đồ, chỉnh thủ công (GRP10–13)
- [ ] Giữ ghế nhóm nguyên tử; thất bại → đề xuất lại (GRP14); thành viên xem realtime (GRP15)
- [ ] Thanh toán của trưởng nhóm, chia tiền tham khảo, phân phối vé (GRP16–17)
- [ ] Trang tiến trình nhóm, Nhóm của tôi (GRP19–20); ghi `group_events`, `seat_suggestion_logs`
- [ ] E2E Playwright: 3 tài khoản → tạo nhóm → bình chọn → chốt → giữ ghế → thanh toán → mỗi người thấy vé
- **Xong khi**: kịch bản E2E chạy xanh.

### M7 — Thanh toán thật, hủy/hoàn tiền, check-in
- [ ] VnpayProvider (sandbox), IPN
- [ ] Hủy vé/đơn nhóm theo chính sách, hoàn tiền (BKG10, GRP21, ORDADM-R)
- [ ] Admin hủy suất chiếu → hoàn tiền hàng loạt + thông báo (SCHADM03)
- [ ] Trang check-in STAFF: quét QR vé / QR đơn nhóm (CHK01)
- **Xong khi**: luồng VNPay sandbox thành công; quét QR lần 2 bị từ chối.

### M8 — Quản trị & thống kê
- [ ] Dashboard tổng quan (ADM01), thống kê nhóm (ADM02), lọc + xuất CSV/Excel (ADM03)
- [ ] Quản lý đơn hàng, nhóm, nhật ký (ORDADM, GRPADM, SYS02)
- [ ] Mã giảm giá cơ bản + giảm theo số vé nhóm (PROADM, BKG05)
- **Xong khi**: số liệu dashboard khớp với dữ liệu seed và dữ liệu kiểm thử.

### M9 — Kiểm thử, hoàn thiện, tài liệu
- [ ] k6: 100–500 VU giữ cùng một ghế / cùng một cụm → đúng 1 thành công; ghi kết quả
- [ ] k6: tải hỗn hợp (xem sơ đồ + giữ ghế + thanh toán), đo p95
- [ ] Rà soát bảo mật (quyền, rate limit, IDOR trên đơn/nhóm/vé)
- [ ] Responsive mobile, trạng thái loading/lỗi, rỗng
- [ ] Tài liệu: use case, ERD, state machine, sequence (giữ ghế nhóm, thanh toán IPN, hết hạn), kết quả thực nghiệm, hướng dẫn cài đặt
- **Xong khi**: toàn bộ checklist bảo vệ ở mục 12 đạt.

### M10 — Mở rộng (nếu còn thời gian)
- Mô hình B (mỗi người tự thanh toán), sở thích chỗ ngồi thành viên, combo bắp nước, đánh giá phim, điểm thưởng/voucher, phim yêu thích, gợi ý phim, MoMo.

---

## 12. Kiểm thử & tiêu chí bảo vệ

| Hạng mục | Tiêu chí |
|---|---|
| Unit | `seat-engine` ≥ 90% coverage; tính tiền; chuyển trạng thái |
| Tích hợp | Hold đồng thời, nguyên tử nhóm, idempotent thanh toán, BR17, hết hạn hold, phân quyền |
| E2E | Đặt vé cá nhân; đặt vé nhóm 3 người; check-in |
| Đồng thời (k6) | 0 lần bán trùng ghế qua mọi kịch bản; có số liệu và biểu đồ |
| Hiệu năng | API sơ đồ ghế p95 < 200 ms; đề xuất cụm ghế p95 < 50 ms (300 ghế) |
| Thực nghiệm thuật toán | Bảng so sánh với 2 baseline theo tỷ lệ lấp đầy |
| Demo | Script demo: 2 trình duyệt realtime, nhóm 5 người, tranh ghế, callback trùng |

---

## 13. Vấn đề cần chủ dự án xác nhận

1. ~~Công nghệ~~ → **Đã chốt**: Java 17 + Spring Boot 3, Angular 19 + NG-ZORRO, PostgreSQL.
2. Có cần **triển khai lên server/cloud** để demo không, hay chỉ chạy local?
3. Số người làm & mốc thời gian nộp để phân bổ lại M0–M9.
4. Có yêu cầu thanh toán thật (VNPay sandbox) hay Mock là đủ?

---

## 14. Nhật ký tiến độ

| Ngày | Giai đoạn | Nội dung | Ghi chú |
|---|---|---|---|
| 2026-10-30 | — | Tạo PLAN.md v1.0 | |
| 2026-10-30 | — | v1.1: đổi stack sang Spring Boot 3 + Angular 19/NG-ZORRO, thêm mục 2A (cấu trúc & quy tắc phân tầng) | |
| 2026-10-30 | M0 | Khung backend (Spring Boot 3.5.16, 3 module Maven, xử lý lỗi chung, security stateless, Flyway, ShedLock, ArchUnit) và frontend (Angular 19.2, NG-ZORRO 19.3, `@mb/shared`, layout web/admin). Backend `mvnw verify` xanh (14 test chạy, 3 test tích hợp bị bỏ qua do máy chưa có Docker); frontend lint/build/test xanh. | Định dạng lỗi API: `{success:false, error:{code,message,details}}`. `/api/**` tạm `permitAll` đến M1. |
| 2026-10-30 | M1 | JWT (access 15 phút, trong bộ nhớ) + refresh token xoay vòng trong cookie httpOnly/SameSite=Strict, phát hiện dùng lại (ân hạn 30s); RBAC theo permission (`/api/v1/admin/**` cần `admin:access` + `@PreAuthorize`); audit log (`@Audited` + ghi đăng nhập thất bại); API USRADM; web: đăng ký/đăng nhập/tài khoản; admin: đăng nhập, menu theo quyền, quản lý người dùng. Test tích hợp chuyển sang PostgreSQL nhúng (Zonky) → 40 test backend chạy thật, 0 bỏ qua; frontend 12 unit test. | Mặc định mọi `/api/**` cần đăng nhập — API công khai phải mở rõ trong `SecurityConfig`. Chạy backend không cần Docker: `spring-boot:test-run`. Web và admin cùng host khi dev nên dùng chung cookie phiên. Chưa có rate limit đăng nhập (làm cùng Bucket4j ở M4), chưa có xác thực email/quên mật khẩu (USR03/04, P1). |
| 2026-10-30 | M2 | Module movie (thể loại, đạo diễn/diễn viên, phim; slug tiếng Việt), cinema (rạp, phòng, sơ đồ ghế dạng lưới có lối đi/ghế đôi/ghế khuyết tật, nhãn hàng/số ghế do backend tính, khóa sơ đồ khi có suất chiếu, bật/tắt bán ghế), showtime (giá = cơ bản × số chỗ + phụ thu, chốt giá theo suất, ngày lễ, chống trùng lịch 3 lớp kèm EXCLUDE constraint), setting (13 tham số có kiểm tra giá trị). Admin UI: phim, rạp/phòng, trình thiết kế sơ đồ, lịch chiếu (xem trước giá, báo suất trùng), giá vé & ngày lễ, cấu hình. Seeder dev: 15 phim, 3 rạp, 8 phòng, ~340 suất. Backend 74 test, frontend 18 unit test. | Đổi so với thiết kế ban đầu: bảng `price_rules` thay bằng `ticket_base_prices` + `seat_types.surcharge` + `showtime_prices` (mục 8.6). Chưa có API công khai cho khách (M3). Hủy suất chiếu đã có vé + hoàn tiền: M7. |
| 2026-10-30 | M3 | API công khai (không cần đăng nhập): phim đang/sắp chiếu + lọc thể loại/độ tuổi/ngôn ngữ, chi tiết theo slug, rạp đang hoạt động, suất chiếu còn bán vé theo ngày (giờ VN, áp dụng `sale_close_after_start_min`), chi tiết suất + giá, sơ đồ ghế với trạng thái hiệu lực. Web: trang chủ (banner trượt + đang chiếu/sắp chiếu), danh sách phim, chi tiết phim (trailer YouTube đã kiểm tra URL, lịch chiếu 7 ngày nhóm theo rạp → định dạng), danh sách/chi tiết rạp (lịch chiếu nhóm theo phim), trang suất chiếu (cảnh báo độ tuổi, chú thích giá, sơ đồ ghế). Backend 80 test, frontend 24 unit test. | Sửa lỗi phát hiện khi chạy thử: đạo diễn/diễn viên bị nhân đôi theo số thể loại (fetch join List + Set) → `Movie.credits` đổi sang Set có thứ tự, thêm test hồi quy; carousel NG-ZORRO tính sai chiều cao → thay bằng slider CSS scroll-snap; menu đầu trang tràn trên mobile. Lọc phim theo định dạng chiếu (MOV04) chuyển sang lọc suất chiếu (SCH04) để tránh phụ thuộc vòng movie ↔ showtime. Chọn/giữ ghế: M4. |
| 2026-10-30 | M4 | Module booking: giữ ghế nguyên tử (UPDATE có điều kiện, khóa dòng theo thứ tự `seat_id` chống deadlock; giữ đủ hoặc không giữ ghế nào), quy tắc ghế lẻ block/warn/off, giới hạn số ghế/lượt giữ, chọn lại thay lượt giữ cũ, rate limit giữ ghế + đăng nhập (Bucket4j). Đơn hàng idempotent theo lượt giữ, giá chụp lại, lịch sử `order_events`, chống IDOR. Module payment: `PaymentProvider` + cổng giả lập ký HMAC, IPN idempotent (`ON CONFLICT DO NOTHING` theo mã giao dịch), thanh toán thất bại được thử lại, tiền về sau hết hạn → nhận lại ghế hoặc REFUND_PENDING (BR17). Vé + QR ký HMAC. Module notification: thông báo in-app + đẩy `/user/queue/notifications`, email Thymeleaf `@Async` sau commit. Job dọn lượt giữ hết hạn (ShedLock). STOMP `/ws` + `/topic/showtimes/{id}/seats`. Web: chọn ghế realtime + thanh đặt vé, trang thanh toán có đếm ngược theo giờ máy chủ, cổng giả lập (thành công/thất bại/gửi 2 lần), kết quả đơn + vé QR, Vé của tôi, Lịch sử đơn, chuông thông báo realtime. Backend 96 test (10 test đặt vé, gồm 8 luồng tranh cùng ghế → đúng 1 thành công). | Kiểm tra thủ công trên trình duyệt: tài khoản thứ hai (gọi API) giữ ghế đang được chọn → trang bên kia thấy ghế chuyển "đang giữ" và tự bỏ chọn; luồng chọn ghế → thanh toán giả lập → vé QR; callback lặp bị bỏ qua; thông báo realtime. Pane trình duyệt chỉ mở được 1 tab nên "trình duyệt thứ hai" là client API. Sửa lỗi phát hiện khi chạy thử: trang thanh toán thiếu provider `NzModalService`. Email ở dev chỉ ghi log (`MAIL_ENABLED=false`). |
