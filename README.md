# MovieBooking — Hệ thống bán vé xem phim & đặt vị trí theo nhóm

Kế hoạch, phạm vi và thiết kế: [PLAN.md](PLAN.md).

| Thư mục | Nội dung |
|---|---|
| `backend/` | Java 17 + Spring Boot 3 (Maven multi-module: `seat-engine`, `seat-bench`, `api`) |
| `frontend/` | Angular 19 + NG-ZORRO (app `web`, app `admin`, thư viện `shared`) |
| `docs/` | ERD, sơ đồ trạng thái, sequence, kết quả thực nghiệm |
| `tools/k6/`, `tools/load/` | Kịch bản kiểm thử tải và đồng thời (k6 / Node) |
| `tools/demo/` | Chuẩn bị dữ liệu cho buổi demo |

## Yêu cầu

- **JDK 17** (biến `JAVA_HOME` phải trỏ tới JDK 17)
- **Node.js 20.11+** (khuyến nghị 22 LTS)
- **Docker Desktop** (tùy chọn: PostgreSQL, Redis, Mailpit) — không có Docker vẫn chạy được bằng PostgreSQL nhúng
- **Google Chrome** (unit test frontend và E2E Playwright)

## Chạy dự án

### 1. Dịch vụ phụ trợ

```bash
cp .env.example .env
docker compose up -d
```

PostgreSQL `localhost:5432`, Redis `localhost:6379`, Mailpit (xem email) http://localhost:8025.

### 2. Backend

```bash
cd backend
./mvnw -pl api -am spring-boot:run
```

- API: http://localhost:8080/api/v1
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

Flyway tự chạy migration khi khởi động.

**Không có Docker?** Chạy backend với PostgreSQL nhúng (dữ liệu giữ ở `backend/.dev-postgres`, không có Redis):

```bash
cd backend
./mvnw -pl api -am spring-boot:test-run
```

### Tài khoản dev

Profile `dev` tự tạo các tài khoản mẫu (xem `DevUserSeeder`), mật khẩu chung `Password123`:

| Email | Vai trò | Dùng cho |
|---|---|---|
| `admin@moviebooking.local` | ADMIN | Web quản trị (toàn quyền) |
| `staff@moviebooking.local` | STAFF | Web quản trị (check-in, xem đơn/nhóm) |
| `customer@moviebooking.local` | CUSTOMER | Web người dùng |

Khi dev, web (4200) và admin (4300) cùng host `localhost` nên dùng chung cookie phiên: đăng nhập ở app này sẽ thay phiên ở app kia.

### 3. Frontend

```bash
cd frontend
npm install
npm run start:web      # http://localhost:4200
npm run start:admin    # http://localhost:4300
```

`/api` và `/ws` được proxy tới `localhost:8080` (xem `projects/*/proxy.conf.json`).

## Kiểm thử & chất lượng code

```bash
cd backend && ./mvnw verify          # unit + ArchUnit + test tích hợp (PostgreSQL nhúng, không cần Docker)
cd backend && ./mvnw spotless:apply  # định dạng code Java
cd frontend && npm test              # unit test (Chrome headless)
cd frontend && npm run lint
cd frontend && npm run format
```

Quy tắc phân tầng backend (L1–L9) được kiểm tra tự động bởi `ArchitectureTest`; quy tắc frontend (F1–F5) xem PLAN.md mục 2A.3.

### E2E (Playwright)

Chạy trên hệ thống dev đang chạy (backend 8080, web 4200, admin 4300), dùng Chrome đã cài trên máy:

```bash
cd frontend && npm run e2e           # đặt vé cá nhân, nhóm 3 người, soát vé, responsive 375 px
```

Báo cáo HTML: `frontend/e2e-report/`.

### Kiểm thử tải & đồng thời

```bash
node tools/load/run.mjs same-seat --vus 100,300,500   # N người cùng giữ 1 ghế → đúng 1 thành công
node tools/load/run.mjs mixed --vus 100 --duration 60
```

Bản k6 tương đương ở `tools/k6/`. Kết quả và phân tích: [docs/experiments/load/README.md](docs/experiments/load/README.md).

## Tài liệu

| Tài liệu | Nội dung |
|---|---|
| [PLAN.md](PLAN.md) | Phạm vi, quy tắc nghiệp vụ BR01–BR18, thiết kế, lộ trình, nhật ký tiến độ |
| [docs/use-cases.md](docs/use-cases.md) | Tác nhân, sơ đồ use case, đặc tả use case trọng tâm |
| [docs/erd.md](docs/erd.md) | Mô hình dữ liệu |
| [docs/state-machines.md](docs/state-machines.md) | Máy trạng thái ghế, đơn, nhóm, refresh token |
| [docs/sequences.md](docs/sequences.md) | Giữ ghế nhóm, thanh toán IPN, hết hạn giữ ghế |
| [docs/experiments/README.md](docs/experiments/README.md) | Thực nghiệm thuật toán cụm ghế (so với 2 baseline, JMH) |
| [docs/experiments/load/README.md](docs/experiments/load/README.md) | Kiểm thử đồng thời & hiệu năng |
| [docs/security-review.md](docs/security-review.md) | Rà soát bảo mật |
| [docs/demo-script.md](docs/demo-script.md) | Kịch bản demo bảo vệ |
