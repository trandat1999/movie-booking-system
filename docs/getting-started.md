# Hướng dẫn cài đặt, chạy và kiểm thử

Dành cho người mới clone repo về. Làm lần lượt từ trên xuống. **Không cần Docker**: backend dùng PostgreSQL
nhúng (tự tải về khi chạy lần đầu).

## 1. Cài công cụ

| Công cụ | Phiên bản | Ghi chú |
|---|---|---|
| Git | bất kỳ | |
| **JDK 17** | 17.x (Temurin, Microsoft, Oracle…) | `java -version` phải ra 17. Maven **không** cần cài — repo có sẵn Maven Wrapper (`mvnw`) |
| **Node.js** | ≥ 20.11, khuyến nghị **22 LTS** | `node -v` |
| **Google Chrome** | bản mới | Unit test frontend (Karma) và E2E (Playwright) dùng Chrome đã cài trên máy |
| Docker Desktop | tùy chọn | Chỉ cần nếu muốn chạy PostgreSQL/Redis/Mailpit thật (mục 7) |

Lần đầu chạy cần **Internet** để tải thư viện Maven, npm và bản PostgreSQL nhúng (~vài trăm MB).

> **Windows**: chạy các lệnh trong **Git Bash** (giống Linux/macOS). Nếu dùng PowerShell/CMD thì thay
> `./mvnw` bằng `mvnw.cmd`.
>
> Máy có nhiều JDK: đặt `JAVA_HOME` trỏ tới JDK 17 trước khi chạy `./mvnw`, ví dụ
> `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17..."` (Git Bash) hoặc
> `$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17..."` (PowerShell).

## 2. Clone

```bash
git clone https://github.com/trandat1999/movie-booking-system.git
cd movie-booking-system
```

## 3. Chạy test (không cần khởi động gì trước)

### Backend — unit + kiến trúc + tích hợp

```bash
cd backend
./mvnw verify
```

Lần đầu mất khoảng 5–10 phút (tải thư viện). Kết quả mong đợi: `BUILD SUCCESS`, gồm

- `seat-engine`: unit test thuật toán đề xuất cụm ghế và xếp chỗ theo sở thích,
- `seat-bench`: test bộ sinh dữ liệu thực nghiệm,
- `api`: test tích hợp với PostgreSQL nhúng (đặt vé đồng thời, nhóm, thanh toán, hoàn tiền, phân quyền…) và
  `ArchitectureTest` (quy tắc phân tầng).

Báo cáo từng test: `backend/*/target/surefire-reports/`.

### Frontend — unit test + lint

```bash
cd frontend
npm ci
npm test        # Karma, Chrome headless — 3 dự án: web, admin, shared
npm run lint
npm run build   # build production cả web và admin
```

Nếu Karma báo không tìm thấy Chrome: đặt biến `CHROME_BIN` trỏ tới file chạy Chrome, ví dụ
`export CHROME_BIN="/c/Program Files/Google/Chrome/Application/chrome.exe"`.

## 4. Chạy ứng dụng

Mở **3 cửa sổ terminal**.

**Terminal 1 — backend** (cổng 8080):

```bash
cd backend
./mvnw install -DskipTests          # lần đầu, hoặc sau khi sửa seat-engine
./mvnw -pl api spring-boot:test-run
```

Đợi dòng `Started TestMovieBookingApplication`. Lệnh này khởi động PostgreSQL nhúng (cổng 54329, dữ liệu lưu ở
`backend/.dev-postgres`), chạy migration và **tự tạo dữ liệu mẫu**: 3 rạp, 8 phòng, 15 phim, lịch chiếu 7 ngày tới
và các tài khoản dưới đây.

**Terminal 2 — web người dùng** (http://localhost:4200):

```bash
cd frontend
npm run start:web
```

**Terminal 3 — web quản trị** (http://localhost:4300):

```bash
cd frontend
npm run start:admin
```

Swagger UI (tài liệu API): http://localhost:8080/swagger-ui.html

### Tài khoản mẫu (mật khẩu chung `Password123`)

| Email | Vai trò | Đăng nhập ở |
|---|---|---|
| `customer@moviebooking.local` | Khách hàng | Web người dùng |
| `admin@moviebooking.local` | Quản trị (toàn quyền) | Web quản trị |
| `staff@moviebooking.local` | Nhân viên soát vé | Web quản trị |

Muốn thêm 5 tài khoản `demo1…demo5@demo.local` (mật khẩu `Demo@1234`) và mã giảm giá `NHOM5` để thử đặt vé nhóm
(backend phải đang chạy):

```bash
node tools/demo/seed-demo.mjs
```

Kịch bản thử từng tính năng: [demo-script.md](demo-script.md).

> Hai web cùng chạy trên `localhost` nên dùng chung cookie phiên: đăng nhập ở web này sẽ thay phiên ở web kia. Muốn
> đăng nhập 2 tài khoản cùng lúc, dùng thêm một cửa sổ ẩn danh hoặc trình duyệt khác.

## 5. Test E2E (Playwright)

Cần **backend, web và admin đang chạy** (mục 4). Playwright dùng Chrome đã cài trên máy nên không phải tải trình
duyệt riêng.

```bash
cd frontend
npm run e2e
```

Gồm 6 kịch bản: đặt vé cá nhân → thanh toán → vé QR → hủy & hoàn tiền; đặt vé nhóm 3 người; soát vé; yêu thích &
đánh giá phim; giao diện điện thoại. Mỗi lần chạy tự đăng ký tài khoản mới nên chạy lại được nhiều lần.

- Báo cáo HTML: `frontend/e2e-report/index.html`; ảnh/trace khi lỗi: `frontend/test-results/`.
- Kịch bản **soát vé** và **đánh giá phim** cần một suất chiếu bắt đầu trong 2 giờ tới (khung giờ soát vé). Nếu
  không có, chúng được đánh dấu *skipped* — không phải lỗi.
- Đăng nhập bị giới hạn 10 lần/phút/IP: chạy `npm run e2e` liên tiếp nhiều lần có thể gặp lỗi 429 — đợi 1 phút.
- Cổng khác mặc định: đặt `WEB_URL`, `ADMIN_URL`, `API_URL` (vd. `API_URL=http://localhost:8080/api/v1`).

## 6. Kiểm thử tải & đồng thời (tùy chọn)

Cần backend đang chạy. Kết quả ghi vào `docs/experiments/load/`.

```bash
node tools/load/run.mjs same-seat --vus 100,300,500   # N người cùng giữ 1 ghế → đúng 1 người thành công
node tools/load/run.mjs same-cluster --vus 100,300,500
node tools/load/run.mjs seatmap --vus 50 --duration 30
node tools/load/run.mjs mixed --vus 100 --duration 60
```

Có k6 thì dùng bản tương đương trong `tools/k6/`. Giải thích số liệu: [experiments/load/README.md](experiments/load/README.md).

Thực nghiệm thuật toán cụm ghế (so với 2 thuật toán đơn giản, đo tốc độ JMH): [experiments/README.md](experiments/README.md).

## 7. (Tùy chọn) Chạy với PostgreSQL/Redis thật bằng Docker

```bash
cp .env.example .env
docker compose up -d                      # PostgreSQL 5432, Redis 6379, Mailpit http://localhost:8025
cd backend
./mvnw install -DskipTests
./mvnw -pl api spring-boot:run
```

Email (xác nhận đặt vé…) mặc định tắt; đặt biến môi trường `MAIL_ENABLED=true` trước khi chạy backend để gửi sang
Mailpit, xem ở http://localhost:8025. Cách này chưa được kiểm thử trên máy phát triển (không cài Docker) —
cách mặc định ở mục 4 là cách đã được kiểm chứng.

## 8. Gặp lỗi thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| `./mvnw: Permission denied` (Linux/macOS) | `chmod +x backend/mvnw` |
| `release version 17 not supported` / lỗi biên dịch Java lạ | `JAVA_HOME` đang trỏ JDK khác 17 — xem mục 1 |
| `Port 8080 was already in use` | Tắt tiến trình đang dùng cổng 8080 (một backend cũ) rồi chạy lại |
| Backend báo `could not lock ... epg-lock` | Lần chạy trước chưa tắt hẳn PostgreSQL nhúng. Tắt mọi tiến trình `java` của backend; nếu vẫn lỗi, khởi động lại máy hoặc xóa thư mục `backend/.dev-postgres` (mất dữ liệu dev, lần chạy sau tự tạo lại) |
| Web không có suất chiếu nào để đặt | Dữ liệu mẫu chỉ tạo lịch chiếu 7 ngày kể từ lần chạy **đầu tiên**. Tắt backend, xóa `backend/.dev-postgres`, chạy lại để có lịch mới (hoặc thêm suất chiếu ở web quản trị) |
| `npm ci` lỗi phiên bản Node | Cần Node ≥ 20.11 (khuyến nghị 22) |
| Karma: `No binary for ChromeHeadless` | Đặt `CHROME_BIN` (mục 3) |
| E2E: `Chromium distribution 'chrome' is not found` | Cài Google Chrome, hoặc chạy `npx playwright install chromium` rồi xóa dòng `channel: 'chrome'` trong `frontend/playwright.config.ts` |
| E2E lỗi `ECONNREFUSED` | Backend/web/admin chưa chạy (mục 4) |

## 9. Đọc thêm

- [README](../README.md) — tổng quan và mục lục tài liệu
- [PLAN.md](../PLAN.md) — phạm vi, quy tắc nghiệp vụ, thiết kế, nhật ký tiến độ
- [use-cases.md](use-cases.md), [erd.md](erd.md), [state-machines.md](state-machines.md), [sequences.md](sequences.md),
  [security-review.md](security-review.md)
