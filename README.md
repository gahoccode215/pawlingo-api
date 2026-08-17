# PawLingo API

Backend Spring Boot cho PawLingo. Xem `context/project-overview.md` và `context/coding-standards.md` để hiểu kiến trúc/quy ước.

## Chạy project

1. Đảm bảo có file `.env` ở root (đã gitignore, không commit). Copy từ `.env.example` nếu chưa có, điền `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` (Postgres) và `JWT_SECRET`.
2. Chạy:

```bash
./mvnw spring-boot:run
```

(Windows PowerShell dùng `mvnw` hoặc `mvnw.cmd`, không cần `./`.)

3. App chạy ở `http://localhost:8080` (đổi qua `SERVER_PORT` trong `.env` nếu cần).

Dừng: `Ctrl+C` trong terminal đang chạy.

## Swagger UI (test API thủ công)

- UI: **http://localhost:8080/swagger-ui.html**
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Request body của `register`/`login` đã có sẵn data mẫu (`user@example.com` / `password123`) — bấm "Try it out" là điền sẵn, chỉ cần "Execute". Với `/auth/me` (cần token), bấm nút **Authorize** ở góc trên Swagger UI, dán `accessToken` nhận được từ response `register`/`login` vào (không cần gõ chữ `Bearer `, Swagger tự thêm).

## Lệnh hay dùng

```bash
./mvnw compile              # build, không chạy test
./mvnw test                 # chạy toàn bộ unit test
./mvnw test -Dtest=TenClass # chạy 1 test class
./mvnw spring-boot:run      # chạy app
```

## ⚠️ Vấn đề đang biết (chưa fix)

Khi test `register`/`login` qua Swagger với Neon Postgres, API trả `500 INTERNAL_ERROR` vì bảng `users` **chưa tồn tại** trong DB — Flyway migration (`V1__create_users_table.sql`) không tự chạy khi start app, dù `spring.flyway.enabled: true` đã bật trong `application.yaml`. Chưa xác định được nguyên nhân chính xác (nghi ngờ liên quan đến việc Spring Boot 4.1 di chuyển namespace autoconfiguration — IDE báo `spring.flyway` là "unknown property" — nhưng chưa xác nhận được prefix mới). Cần điều tra tiếp trước khi test được flow register/login thật.
