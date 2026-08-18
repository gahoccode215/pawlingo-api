# Feature: Authentication & Authorization qua Google (Google OAuth)

## Summary

Cho phép user đăng nhập/đăng ký bằng tài khoản Google, dùng chung cơ chế JWT với flow email/password đã có (xem `auth-spec.md`, đã merge vào `main`). Đây là phần "Google OAuth" từng bị đánh dấu Out of Scope ở feature Authentication (Email/Password) MVP.

Vì FE (`pawlingo-ui`, Next.js) không giữ session phía server, chọn flow **ID token verification** thay vì server-side redirect (authorization code) flow:

1. FE dùng Google Identity Services (GSI) JS SDK, lấy về một Google **ID token** (JWT do Google ký) sau khi user chọn tài khoản Google.
2. FE gửi ID token đó lên backend: `POST /api/v1/auth/google`.
3. Backend verify chữ ký + `aud`/`iss`/`exp` của ID token với public key của Google, lấy `sub` (Google user id), `email`, `email_verified`, `name`.
4. Backend find-or-create `User`, phát hành JWT access token của **chính backend** (giống hệt JWT ở flow login email/password) — FE tiếp tục dùng JWT này cho mọi request khác như cũ, không cần biết gì về Google token nữa sau bước này.

Backend **không** cần Google client secret vì không tự thực hiện authorization code exchange — chỉ verify ID token mà FE đã lấy được. Do đó **không** cần thêm `spring-boot-starter-oauth2-client` (dù `coding-standards.md` §8 và `project-overview.md` có nhắc tới nó như hướng đi ban đầu — quyết định lại ở đây vì FE là SPA/Next.js client-side, không phải flow redirect truyền thống).

## Goals

- User có thể đăng nhập bằng Google, không cần tạo password.
- User có tài khoản Google với email trùng email đã đăng ký bằng password (LOCAL) **không** bị tự động gộp — trả lỗi rõ ràng, giữ boundary bảo mật giữa 2 provider.
- Backend verify ID token đúng cách (chữ ký, `aud` = Google Client ID của app, `iss` là Google, chưa hết hạn) — không tin dữ liệu email/sub gửi thẳng từ client mà không verify.
- User mới qua Google được tạo với `authProvider = GOOGLE`, không có `passwordHash`.
- Response trả về JWT access token cùng format với flow login/register hiện tại, để FE xử lý đồng nhất.
- `GET /api/v1/auth/me` vẫn hoạt động y nguyên cho cả user LOCAL và GOOGLE (không đổi).

## Endpoints

| Method | Path | Status |
|---|---|---|
| POST | `/api/v1/auth/google` | Planned |

### POST /api/v1/auth/google

Request:
```json
{
  "idToken": "eyJhbGciOi..."
}
```

Response `200` (user đã tồn tại — login) hoặc `201` (user mới — register qua Google):
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    "goal": "beginner",
    "accessToken": "jwt...",
    "expiresIn": 86400,
    "isNewUser": true
  },
  "error": null
}
```

`isNewUser` giúp FE quyết định có cần hỏi thêm goal/onboarding ngay sau khi đăng nhập Google lần đầu hay không (Google không cung cấp field `goal`).

> Note cho FE: cập nhật `project-overview.md` §5 — bảng API contract hiện ghi `GET/POST /auth/google`, nhưng flow ID-token-verification thực tế chỉ cần `POST`. Xoá `GET` khỏi bảng khi implement xong.

## Data Model

Thay đổi trên entity `User` hiện có (`user/entity/User.java`), không tạo bảng mới:

```
User (existing, thay đổi)
- passwordHash   NULLABLE bây giờ (chỉ bắt buộc khi authProvider = LOCAL)
- googleId       (VARCHAR, unique, nullable) — lưu Google `sub`, dùng để tra cứu ổn định
                 thay vì chỉ dựa vào email (email đổi được ở phía Google, sub thì không)
- authProvider   đã có sẵn enum LOCAL | GOOGLE, không cần đổi
```

**Chuẩn hoá email**: luôn lowercase email trước khi lưu hoặc dùng để tra cứu (cả ở `register`/`login` LOCAL lẫn ở find-or-create của Google) — Google có thể trả về email khác casing so với lúc user tự nhập khi đăng ký LOCAL, nếu không chuẩn hoá thì check trùng email ở bước find-or-create sẽ bị "miss" và tạo nhầm 2 account cho cùng 1 người. Áp dụng bằng cách lowercase trong service layer trước khi query/insert, không sửa lại dữ liệu cũ trong migration này (out of scope, chỉ áp dụng cho email mới từ nay).

**Bất biến `passwordHash` theo `authProvider`** — enforce ở tầng service (`AuthServiceImpl`), không chỉ dựa vào cột nullable trong DB:
- `authProvider = LOCAL` → `passwordHash` bắt buộc khác null (đã đúng ở flow register hiện tại).
- `authProvider = GOOGLE` → `passwordHash` luôn là `null`, không có code path nào được set password cho user GOOGLE trong feature này.
Vi phạm 1 trong 2 điều trên là lỗi logic (bug), nên có thể assert bằng unit test ở `AuthServiceImplTest` chứ không cần validation runtime phía user-facing.

Migration Flyway mới: `V2__alter_users_for_google_oauth.sql`

```sql
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE users ADD COLUMN google_id VARCHAR(255);
ALTER TABLE users ADD CONSTRAINT uq_users_google_id UNIQUE (google_id);
```

Lưu ý: constraint `UNIQUE` trên cột nullable cho phép nhiều NULL cùng lúc trên Postgres (mỗi NULL không so với NULL khác) — không ảnh hưởng user LOCAL hiện có (không có `google_id`).

## Find-or-create Logic (Service layer)

Thứ tự xử lý trong `AuthServiceImpl` (hoặc method mới `loginWithGoogle` trên `AuthService`):

1. Verify ID token → lấy `googleId` (sub), `email`, `emailVerified`.
2. Nếu `emailVerified = false` → từ chối, lỗi `403 GOOGLE_EMAIL_NOT_VERIFIED` (hiếm khi xảy ra nhưng phải chặn).
3. Tìm `User` theo `googleId`:
   - Có → login, phát JWT, `isNewUser = false`.
4. Không có `googleId` khớp → tìm theo `email`:
   - Có user với `authProvider = LOCAL` → từ chối, lỗi `409 ACCOUNT_EXISTS_WITH_PASSWORD` (yêu cầu FE báo user đăng nhập bằng password, không tự merge account để tránh account takeover qua email giả mạo chưa verify ở phía khác).
   - Có user với `authProvider = GOOGLE` nhưng thiếu `googleId` (không nên xảy ra ở data mới, nhưng phòng hờ dữ liệu cũ) → gắn `googleId` vào, login.
   - Không có → tạo mới `User` (`authProvider = GOOGLE`, `passwordHash = null`, `goal = beginner` mặc định), `isNewUser = true`.
5. Phát JWT giống hệt cơ chế login email/password (`JwtService`), trả về theo response shape ở trên.

## Security Requirements

- Verify ID token bằng thư viện chính thức của Google (`com.google.api-client:google-api-client`, class `GoogleIdTokenVerifier`) — **không** tự parse JWT bằng `jjwt` cho token của Google, vì cần fetch/cache đúng JWKS của Google và xử lý key rotation, việc mà `google-api-client` đã làm sẵn.
- `GoogleIdTokenVerifier` phải được cấu hình với `audience` = `GOOGLE_CLIENT_ID` (env var mới) để chặn token phát hành cho app Google khác.
- `GOOGLE_CLIENT_ID` lấy từ biến môi trường, thêm vào `.env.example`. Không cần `GOOGLE_CLIENT_SECRET` ở backend (xem Summary).
- Không tự động merge account LOCAL/GOOGLE trùng email (xem Find-or-create Logic bước 4) — đây là quyết định bảo mật, không phải thiếu sót.
- JWT phát hành sau khi Google login dùng chung `JwtService`/`JWT_SECRET` đã có, không có gì khác biệt với JWT của flow password.
- Endpoint `POST /api/v1/auth/google` là public (giống `register`/`login`), khai báo trong `SecurityConfig`.

## Error Handling

| Tình huống | HTTP status | error.code |
|---|---|---|
| ID token không verify được (sai chữ ký/audience/hết hạn) | 401 | `GOOGLE_TOKEN_INVALID` |
| Email trên Google chưa được Google verify | 403 | `GOOGLE_EMAIL_NOT_VERIFIED` |
| Email đã tồn tại với tài khoản LOCAL (có password) | 409 | `ACCOUNT_EXISTS_WITH_PASSWORD` |
| Input không hợp lệ (thiếu `idToken`) | 400 | `VALIDATION_ERROR` |

Thêm các case này vào `ErrorCode` enum hiện có (`common/exception`), không tạo exception class mới — theo đúng `coding-standards.md` §6.

## Out of Scope (feature sau)

- Server-side redirect / authorization code flow (nếu sau này cần login từ mobile native app hoặc muốn backend tự quản lý refresh token của Google).
- **Account linking chủ động** (feature sau, không phải thiếu sót): thêm endpoint kiểu `POST /auth/link-google` chỉ cho phép khi user đã đăng nhập bằng JWT hợp lệ (LOCAL) và tự bấm "Liên kết Google" trong settings — khác với hành vi hiện tại (từ chối hoàn toàn khi trùng email lúc login). Việc này cho user có cả 2 account do nhầm lẫn một lối thoát, mà vẫn giữ nguyên tắc không tự merge ngầm lúc đăng nhập.
- Google refresh token / offline access (không cần vì chỉ dùng Google để xác thực danh tính lần đăng nhập, không gọi Google API thay mặt user).
- Các provider OAuth khác (Facebook, Apple...).

## Notes

- Cần thêm dependency `com.google.api-client:google-api-client` vào `pom.xml` (version cần chốt khi implement — kiểm tra bản mới nhất tương thích Java 21).
- Cần chốt với FE: FE gọi Google Identity Services (`accounts.google.com/gsi/client`) ở client-side để lấy ID token — xác nhận FE dùng flow này (One Tap / Sign In With Google button) chứ không phải flow OAuth redirect truyền thống, vì spec này được thiết kế riêng cho ID-token-verification.
- **Bất đối xứng về độ tin cậy email giữa 2 provider**: user Google luôn có email đã được Google verify (`email_verified` claim), trong khi user LOCAL hiện tại (`auth-spec.md`) không có bước gửi email xác nhận khi đăng ký — nghĩa là ai đó có thể đăng ký LOCAL bằng email không thuộc sở hữu thật của họ. Không chặn ở feature này, nhưng ghi nhận làm roadmap item (thêm email verification flow cho LOCAL) để 2 provider đạt cùng mức độ tin cậy, đặc biệt quan trọng nếu sau này có tính năng account linking (xem Out of Scope) dựa trên email trùng khớp.
- Sau khi implement, cập nhật bảng API contract ở `project-overview.md` §5 (đổi `GET/POST /auth/google` thành `POST /auth/google`, đổi Status thành Implemented) và §3 (tech stack: thêm `google-api-client`, cập nhật dòng "Auth (OAuth)" từ "Not added" sang "In use" — nhưng ghi rõ đây không phải `spring-boot-starter-oauth2-client` như dự kiến ban đầu).
