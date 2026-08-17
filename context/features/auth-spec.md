# Feature: Authentication (Email/Password) — MVP

## Summary

Cho phép user đăng ký và đăng nhập bằng email/password, backend phát hành JWT để FE (`pawlingo-ui`) dùng cho các request tiếp theo. Google OAuth **không** nằm trong scope này — sẽ làm ở feature riêng sau, để không chặn đường build Pet/Vocab/Progress.

## Goals

- User đăng ký tài khoản mới bằng email + password
- User đăng nhập bằng email + password, nhận về JWT access token
- Password được hash bằng BCrypt, không bao giờ lưu plaintext
- Endpoint được bảo vệ bằng JWT filter (Spring Security), request không có/token sai bị từ chối với 401
- Trả lỗi rõ ràng khi: email đã tồn tại, sai email/password, input không hợp lệ
- Tự động tạo `Pet` mặc định cho user ngay sau khi đăng ký thành công (để Pet feature có sẵn data khi build tiếp)

## Endpoints

| Method | Path | Status |
|---|---|---|
| POST | `/api/v1/auth/register` | Planned |
| POST | `/api/v1/auth/login` | Planned |
| GET | `/api/v1/auth/me` | Planned |

### POST /api/v1/auth/register

Request:
```json
{
  "email": "user@example.com",
  "password": "minimum8chars",
  "goal": "beginner"
}
```

Response `201`:
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    "goal": "beginner",
    "accessToken": "jwt..."
  },
  "error": null
}
```

### POST /api/v1/auth/login

Request:
```json
{ "email": "user@example.com", "password": "minimum8chars" }
```

Response `200`:
```json
{
  "success": true,
  "data": { "accessToken": "jwt...", "expiresIn": 86400 },
  "error": null
}
```

### GET /api/v1/auth/me

Header: `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "data": { "id": "uuid", "email": "user@example.com", "goal": "beginner" },
  "error": null
}
```

## Data Model

Chỉ cần entity `User` (đã mô tả trong `project-overview.md`), thu hẹp cho scope này:

```
User
- id            (UUID, PK)
- email         (unique, not null)
- passwordHash  (not null cho local account)
- goal          (enum: beginner | test-prep | professional | for-child, default beginner)
- authProvider  (enum: LOCAL — chỉ LOCAL trong scope này, GOOGLE để dành sau)
- createdAt / updatedAt
```

Migration Flyway: `V1__create_users_table.sql`.

Khi register thành công, tạo kèm 1 dòng `Pet` mặc định (stage khởi đầu, energy đầy, XP = 0) — cần entity `Pet` tối thiểu tồn tại trước, hoặc để trống hook này nếu Pet entity chưa có (ghi rõ trong code bằng TODO có ticket tham chiếu, không để TODO mồ côi).

## Validation Rules

- `email`: bắt buộc, đúng định dạng email, unique trong DB → trùng thì trả lỗi `409 DUPLICATE_EMAIL`.
- `password`: bắt buộc, tối thiểu 8 ký tự. Không giới hạn ký tự đặc biệt ở MVP (giữ đơn giản).
- `goal`: optional, mặc định `beginner` nếu không truyền.

## Security Requirements

- Password hash bằng `BCryptPasswordEncoder`.
- JWT ký bằng secret lấy từ biến môi trường (`JWT_SECRET`), không hardcode.
- Access token only ở MVP này — **không làm refresh token** (out of scope, xem bên dưới).
- Token truyền qua header `Authorization: Bearer <token>` (không dùng cookie ở bước này — đơn giản hơn cho MVP, tránh phải xử lý CORS/cookie config; có thể đổi sang httpOnly cookie sau nếu cần bảo mật cao hơn — quyết định này cần FE xác nhận trước khi bắt đầu code `AuthController`).
- Endpoint `register`, `login` là public; mọi endpoint khác (kể cả các endpoint Pet/Vocab/Progress sau này) mặc định require JWT hợp lệ.

## Error Handling

| Tình huống | HTTP status | error.code |
|---|---|---|
| Email đã tồn tại khi register | 409 | `DUPLICATE_EMAIL` |
| Sai email hoặc password khi login | 401 | `INVALID_CREDENTIALS` |
| Input không hợp lệ (validation) | 400 | `VALIDATION_ERROR` |
| Token thiếu/hết hạn/không hợp lệ | 401 | `UNAUTHORIZED` |

Tất cả đi qua `GlobalExceptionHandler`, dùng response envelope `{success, data, error}` theo `coding-standards.md`.

## Out of Scope (feature sau)

- Google OAuth login.
- Refresh token / logout tất cả thiết bị.
- Quên mật khẩu / reset password qua email.
- Rate limiting cho login (chống brute-force) — nên làm sớm ở phase sau nhưng không chặn MVP.

## Notes

- Cần chốt với FE: header `Authorization: Bearer` (đề xuất trong spec này) hay httpOnly cookie — nếu FE có lý do cần cookie (VD: SSR trong Next.js muốn đọc token phía server dễ hơn), có thể đổi trước khi implement.
- `Pet` entity cần tối thiểu tồn tại (id, userId, stage, energy, các trường XP mặc định 0) trước khi register có thể auto-tạo pet — nếu chưa có, tách hook tạo Pet ra thành bước riêng ngay sau khi entity Pet sẵn sàng, không block việc merge auth.