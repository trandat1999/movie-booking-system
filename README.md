# MovieBooking — Hệ thống bán vé xem phim & đặt vị trí theo nhóm

Kế hoạch, phạm vi và thiết kế: [PLAN.md](PLAN.md).

| Thư mục | Nội dung |
|---|---|
| `backend/` | Java 17 + Spring Boot 3 (Maven multi-module: `seat-engine`, `seat-bench`, `api`) |
| `frontend/` | Angular 19 + NG-ZORRO (app `web`, app `admin`, thư viện `shared`) |
| `docs/` | ERD, sơ đồ trạng thái, sequence, kết quả thực nghiệm |
| `tools/k6/`, `tools/load/` | Kịch bản kiểm thử tải và đồng thời (k6 / Node) |
| `tools/demo/` | Chuẩn bị dữ liệu cho buổi demo |

## Bắt đầu nhanh

**Hướng dẫn đầy đủ (cài đặt, chạy, test, xử lý lỗi): [docs/getting-started.md](docs/getting-started.md).**

Cần JDK 17, Node.js 20.11+ (khuyến nghị 22), Google Chrome. Không cần Docker.

```bash
git clone https://github.com/trandat1999/movie-booking-system.git
cd movie-booking-system

# Test
(cd backend && ./mvnw verify)                       # unit + ArchUnit + tích hợp (PostgreSQL nhúng)
(cd frontend && npm ci && npm test && npm run lint)

# Chạy (3 terminal)
cd backend && ./mvnw install -DskipTests && ./mvnw -pl api spring-boot:test-run   # API :8080 + dữ liệu mẫu
cd frontend && npm run start:web                                                  # http://localhost:4200
cd frontend && npm run start:admin                                                # http://localhost:4300

# E2E (khi 3 thành phần trên đang chạy)
cd frontend && npm run e2e
```

Tài khoản mẫu (mật khẩu `Password123`): `customer@moviebooking.local`, `admin@moviebooking.local`,
`staff@moviebooking.local`. Swagger UI: http://localhost:8080/swagger-ui.html

Quy tắc phân tầng backend (L1–L9) được kiểm tra tự động bởi `ArchitectureTest`; quy tắc frontend (F1–F5) xem PLAN.md mục 2A.3.

## Tài liệu

| Tài liệu | Nội dung |
|---|---|
| [docs/getting-started.md](docs/getting-started.md) | Cài đặt, chạy, kiểm thử, xử lý lỗi thường gặp |
| [PLAN.md](PLAN.md) | Phạm vi, quy tắc nghiệp vụ BR01–BR18, thiết kế, lộ trình, nhật ký tiến độ |
| [docs/use-cases.md](docs/use-cases.md) | Tác nhân, sơ đồ use case, đặc tả use case trọng tâm |
| [docs/erd.md](docs/erd.md) | Mô hình dữ liệu |
| [docs/state-machines.md](docs/state-machines.md) | Máy trạng thái ghế, đơn, nhóm, refresh token |
| [docs/sequences.md](docs/sequences.md) | Giữ ghế nhóm, thanh toán IPN, hết hạn giữ ghế |
| [docs/experiments/README.md](docs/experiments/README.md) | Thực nghiệm thuật toán cụm ghế (so với 2 baseline, JMH) |
| [docs/experiments/load/README.md](docs/experiments/load/README.md) | Kiểm thử đồng thời & hiệu năng |
| [docs/security-review.md](docs/security-review.md) | Rà soát bảo mật |
| [docs/demo-script.md](docs/demo-script.md) | Kịch bản demo bảo vệ |
