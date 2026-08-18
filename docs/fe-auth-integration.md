# FE Integration Guide — Authentication (Email/Password + Google)

> Dành cho `pawlingo-ui` (Next.js). Copy file này vào repo FE (ví dụ `docs/backend-auth-integration.md`) làm tài liệu tham chiếu khi implement `src/lib/api.ts` và các màn hình đăng ký/đăng nhập.
>
> Backend: `pawlingo-api` (Spring Boot). 3 tính năng đã **implement và test xong** trên `main`: **Đăng ký**, **Đăng nhập** (email/password), **Đăng nhập/Đăng ký qua Google**. Toàn bộ đã merge, có test, đã verify thủ công với DB thật.

---

## 1. Quy ước chung

- **Base URL**: `{BACKEND_URL}/api/v1` (BE chạy local mặc định ở `http://localhost:8080`).
- **Response envelope** — mọi endpoint đều trả đúng shape này, kể cả lỗi:
  ```json
  { "success": true,  "data": { ... }, "error": null }
  { "success": false, "data": null,    "error": { "code": "SOME_CODE", "message": "..." } }
  ```
- **Auth**: JWT qua header `Authorization: Bearer <accessToken>` — **không dùng cookie**. FE tự chọn nơi lưu token (localStorage / memory / v.v.), BE không set cookie nào.
- **CORS**: BE chỉ chấp nhận origin nằm trong biến môi trường `CORS_ALLOWED_ORIGINS` (mặc định `http://localhost:3000`). Nếu FE chạy port khác, báo backend team thêm origin vào `.env`.
- **Token hết hạn**: mặc định `86400` giây (24h), cấu hình qua `JWT_EXPIRATION_SECONDS` phía BE. Không có refresh token ở giai đoạn này — hết hạn thì phải đăng nhập lại (register/login/google), không có endpoint refresh.
- Tất cả timestamp trả về dạng ISO-8601 (`Instant`).

---

## 2. `Goal` enum (dùng trong `register` và response user)

Giá trị JSON hợp lệ (không phân biệt hoa/thường khi gửi lên, nhưng response luôn trả lowercase-kebab):

| Value | Ý nghĩa |
|---|---|
| `beginner` | mặc định nếu không truyền |
| `test-prep` | |
| `professional` | |
| `for-child` | |

---

## 3. Đăng ký — `POST /auth/register`

Request:
```json
{
  "email": "user@example.com",
  "password": "minimum8chars",
  "goal": "beginner"
}
```
- `email`: bắt buộc, đúng format email.
- `password`: bắt buộc, tối thiểu 8 ký tự.
- `goal`: optional, mặc định `beginner`.

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

Lỗi có thể gặp: xem bảng error code ở mục 6 (`DUPLICATE_EMAIL`, `VALIDATION_ERROR`).

---

## 4. Đăng nhập — `POST /auth/login`

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

Lưu ý: response login **không** trả `id`/`email`/`goal` (khác register) — nếu FE cần thông tin profile ngay sau login, gọi tiếp `GET /auth/me` (mục 7) bằng token vừa nhận.

Lỗi có thể gặp: `INVALID_CREDENTIALS` (dùng chung cho cả "sai email" lẫn "sai password" — cố tình không phân biệt để tránh lộ email nào đã tồn tại).

---

## 5. Đăng nhập/Đăng ký qua Google — `POST /auth/google`

### Flow tổng quan (khác hẳn OAuth redirect truyền thống)

BE dùng **ID-token verification**, không dùng authorization-code/redirect flow. Nghĩa là:

1. FE tự lấy **Google ID token** ở phía client bằng **Google Identity Services (GSI)** — không cần BE tham gia bước này.
2. FE POST `idToken` đó lên BE.
3. BE verify token với Google, tự quyết định đây là login hay tạo mới, trả JWT của chính BE (giống hệt JWT ở mục 3/4) — từ đây FE xử lý y như đăng nhập thường, không cần quan tâm gì đến Google token nữa.

### Setup phía FE

1. Lấy `GOOGLE_CLIENT_ID` từ backend team (cùng project Google Cloud với BE — **không tự tạo OAuth Client ID mới**, phải dùng chung 1 client để BE verify được `aud`).
2. Thêm domain FE (vd `http://localhost:3000`, domain staging/production) vào **Authorized JavaScript origins** của OAuth Client đó trên Google Cloud Console (nhờ backend team hoặc người quản lý project add nếu FE không có quyền).
3. Nhúng script GSI:
   ```html
   <script src="https://accounts.google.com/gsi/client" async defer></script>
   ```
4. Render nút đăng nhập (hoặc dùng One Tap), lấy `idToken` trong callback:
   ```js
   function handleCredentialResponse(response) {
     const idToken = response.credential; // <-- gửi cái này lên BE
   }
   ```
5. POST lên BE:
   ```js
   const res = await fetch(`${BACKEND_URL}/api/v1/auth/google`, {
     method: 'POST',
     headers: { 'Content-Type': 'application/json' },
     body: JSON.stringify({ idToken }),
   });
   ```

### Request lên BE

```json
{ "idToken": "eyJhbGciOi..." }
```

### Response

- `201` nếu đây là user Google **mới** (lần đầu đăng nhập bằng Google account này):
- `200` nếu user Google này đã tồn tại (login bình thường):

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

`isNewUser` dùng để FE quyết định có cần dẫn user qua bước onboarding/chọn `goal` hay không (Google không cung cấp field `goal`, user mới qua Google luôn có `goal = beginner` mặc định — nếu muốn đổi thì gọi API cập nhật profile, **hiện chưa có endpoint này**, cần một feature riêng).

### ⚠️ Case quan trọng FE cần xử lý: email trùng account LOCAL

Nếu user bấm "Đăng nhập bằng Google" nhưng **email Google đó đã có tài khoản đăng ký bằng password (LOCAL)** trước đó, BE **từ chối, không tự gộp 2 tài khoản**:

```json
{
  "success": false,
  "data": null,
  "error": { "code": "ACCOUNT_EXISTS_WITH_PASSWORD", "message": "..." }
}
```
→ HTTP `409`. FE nên hiện thông báo dạng "Email này đã đăng ký bằng mật khẩu, vui lòng đăng nhập bằng email/password" thay vì cho tự động login. Đây là quyết định bảo mật có chủ đích, không phải bug.

---

## 6. Bảng error code đầy đủ

| HTTP | `error.code` | Xảy ra ở | Ý nghĩa |
|---|---|---|---|
| 409 | `DUPLICATE_EMAIL` | register | Email đã tồn tại (tài khoản LOCAL) |
| 401 | `INVALID_CREDENTIALS` | login | Sai email hoặc sai password (dùng chung 1 code cho cả 2 trường hợp) |
| 400 | `VALIDATION_ERROR` | register/login/google | Input không hợp lệ — `error.message` là các lỗi field nối bằng `"; "` (1 chuỗi, không phải mảng) |
| 401 | `UNAUTHORIZED` | mọi endpoint cần auth | Thiếu header `Authorization`, token sai/hết hạn — FE nên bắt lỗi này ở tầng fetch chung để tự động redirect về màn login |
| 500 | `INTERNAL_ERROR` | mọi endpoint | Lỗi không mong muốn phía BE |
| 401 | `GOOGLE_TOKEN_INVALID` | google | ID token Google không hợp lệ (sai chữ ký, sai `aud`, hết hạn...) |
| 403 | `GOOGLE_EMAIL_NOT_VERIFIED` | google | Email trên Google chưa được Google xác thực (hiếm gặp) |
| 409 | `ACCOUNT_EXISTS_WITH_PASSWORD` | google | Xem mục 5 — email đã là tài khoản LOCAL |

`error.message` là text mô tả, **không nên hiển thị thẳng cho user** (có thể đổi ngôn ngữ/nội dung sau) — nên FE tự map `error.code` → message tiếng Việt riêng của mình.

---

## 7. Lấy thông tin user hiện tại — `GET /auth/me`

Header bắt buộc: `Authorization: Bearer <accessToken>`

Response `200`:
```json
{
  "success": true,
  "data": { "id": "uuid", "email": "user@example.com", "goal": "beginner" },
  "error": null
}
```

Dùng endpoint này để: (a) lấy profile sau khi login (login response không có sẵn), (b) kiểm tra token còn hợp lệ khi FE khởi động app (nếu 401 → xoá token đã lưu, coi như chưa đăng nhập).

---

## 8. Những gì **chưa có**, đừng dựa vào

- Refresh token — hết hạn phải đăng nhập lại từ đầu.
- Quên mật khẩu / reset password.
- Đổi/cập nhật `goal` sau khi đã có tài khoản.
- Logout phía server (JWT là stateless, "logout" = FE tự xoá token đang lưu, BE không có endpoint logout).
- Link thủ công 1 tài khoản LOCAL đang đăng nhập với Google (account linking) — hiện chỉ có "tạo mới" hoặc "từ chối nếu trùng email", chưa có flow chủ động liên kết.
- Rate limiting cho login (chưa chống brute-force).
