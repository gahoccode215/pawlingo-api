# Feature: Vocabulary Learning — Phase 2: Progress + Pet-linked XP

> Phần của `vocab-learning-roadmap.md` — Phase 2/4. Phụ thuộc Phase 1 (`VocabWord` phải tồn tại). Thiết kế độc lập, không tham chiếu FE. Điểm cần bạn chốt đánh dấu **"Quyết định cần chốt"**.

## Summary

Đây là phase tạo ra khác biệt hoá thật sự của PawLingo: mỗi lượt trả lời từ vựng ghi nhận thành `Progress`, quy đổi ra XP/energy cho `Pet` của user. Đóng nốt TODO còn treo từ feature Auth MVP ("tự động tạo Pet khi register").

Nguyên tắc thiết kế: **`activityType` là 1 enum mở rộng được** (lưu dạng string trong DB, giống cách `Goal`/`AuthProvider` đang làm) — Phase 3 thêm activity type mới chỉ là thêm 1 constant + 1 dòng cấu hình điểm số, **không cần migration**.

## Goals

- User có `Pet` ngay sau khi đăng ký (cả LOCAL và GOOGLE) — không còn là TODO.
- Mỗi lượt trả lời đúng/sai một từ vựng ghi được vào `Progress`, gắn với `activityType` cụ thể.
- XP tích luỹ → `Pet` lên "stage" theo ngưỡng XP.
- Energy của `Pet` tăng khi học đúng, giảm khi sai — phản hồi tức thời, không cần chờ job nền.
- Khung điểm số (XP/energy theo từng `activityType`) tách khỏi logic ghi nhận — đổi hệ số không cần sửa luồng xử lý.

## Endpoints

| Method | Path | Status |
|---|---|---|
| GET | `/api/v1/pet` | Planned |
| POST | `/api/v1/progress` | Planned |

### GET /api/v1/pet

Header: `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "data": { "id": "uuid", "stage": 1, "xp": 40, "energy": 85 },
  "error": null
}
```

### POST /api/v1/progress

Request:
```json
{ "vocabWordId": "uuid", "activityType": "quiz", "correct": true }
```

Response `200`:
```json
{
  "success": true,
  "data": {
    "xpEarned": 10,
    "totalXp": 50,
    "petStage": 1,
    "energyDelta": 5,
    "newEnergy": 90
  },
  "error": null
}
```

## Data Model

```
Pet (mới)
- id            (UUID, PK)
- userId        (UUID, FK -> users.id, unique, not null) — 1 user 1 pet
- stage         (INT, not null, default 1)
- xp            (INT, not null, default 0)
- energy        (INT, not null, default 100) — clamp 0-100
- createdAt / updatedAt

Progress (mới)
- id            (UUID, PK)
- userId        (UUID, FK -> users.id, not null)
- vocabWordId   (UUID, FK -> vocab_words.id, not null)
- activityType  (VARCHAR, not null) — enum lưu dạng string, giống AuthProvider/Goal
- correct       (BOOLEAN, not null)
- xpEarned      (INT, not null)
- createdAt     (not null) — không cần updatedAt, đây là log append-only
```

Migration Flyway: `V4__create_pets_and_progress.sql`.

`ActivityType` enum (Java, package `vocab/enums`):
```
QUIZ   // Phase 1/2 dùng ngay
// Phase 3 thêm dần: FILL_IN_BLANK, PICTURE_MATCH, SPELLING, LISTENING — không cần migration
```

**Quyết định cần chốt #1 — bảng hệ số XP/energy**: đề xuất 1 map hằng trong code (vd `ActivityScoringPolicy`, kiểu `Map<ActivityType, ScoringRule(xpCorrect, xpWrong, energyCorrect, energyWrong)>`) thay vì bảng DB riêng — đơn giản, đủ dùng khi số activity type còn ít (<10). Nếu sau này cần đội sản phẩm tự chỉnh số liệu mà không deploy lại, mới đáng chuyển sang bảng DB + có thể cả admin UI — nhưng đó là over-engineering ở giai đoạn này.

**Quyết định cần chốt #2 — ngưỡng lên stage**: cần 1 bảng ngưỡng XP → stage (vd stage 1: 0-99, stage 2: 100-299...). Đề xuất tạm 4-5 stage tuyến tính tăng dần (vd 0, 100, 300, 600, 1000) để có cái chạy được — đây là con số **placeholder**, nên coi là input từ đội thiết kế game/sản phẩm, không phải quyết định kỹ thuật. Sửa sau chỉ đổi 1 hằng số, không ảnh hưởng schema.

## Auto-create Pet khi đăng ký

Sửa lại 2 chỗ đang có TODO/thiếu:
- `AuthServiceImpl.register()` — sau khi save `User` LOCAL, gọi `petService.createDefaultPet(userId)`.
- `AuthServiceImpl.loginWithGoogle()` — nhánh tạo `User` mới (`isNewUser = true`), cũng gọi `createDefaultPet(userId)`.

Cả 2 nên nằm trong cùng `@Transactional` với việc tạo `User`, để không có user nào tồn tại mà thiếu `Pet`.

## Validation Rules

- `vocabWordId` không tồn tại → `404 VOCAB_WORD_NOT_FOUND`.
- `activityType` gửi lên không khớp giá trị enum hợp lệ → `400 VALIDATION_ERROR` (Jackson tự báo lỗi deserialize, xử lý qua `GlobalExceptionHandler` như các enum khác, xem cách `Goal` đang làm với `@JsonCreator`).

## Error Handling

| Tình huống | HTTP status | error.code |
|---|---|---|
| `vocabWordId` không tồn tại | 404 | `VOCAB_WORD_NOT_FOUND` |
| Input không hợp lệ | 400 | `VALIDATION_ERROR` |

## Out of Scope (phase sau, hoặc không nằm trong roadmap này)

- **Trick unlock** (pet học chiêu trò mới theo mốc XP cụ thể) — ý tưởng hay nhưng cần thêm data model riêng (catalog trick + bảng unlocked-trick-per-pet). Đề xuất tách thành sub-feature riêng sau khi Phase 2 core chạy ổn, tránh 1 PR ôm quá nhiều.
- **Energy tự giảm theo thời gian** (pet "đói" nếu không học) — cần scheduled job (`@Scheduled` hoặc cron), khác hẳn về hạ tầng so với phần còn lại của phase này (vốn chỉ phản hồi theo request). Tách riêng thành 1 feature nhỏ sau.
- Leitner box / spaced-repetition state — hiện đang là state cục bộ phía FE (nếu FE có làm); có nên chuyển state này lên backend theo `Progress` luôn không là **quyết định cần bàn ở Phase 3**, không quyết ở đây.

## Notes

- Package mới `pet` (ngang hàng `vocab`, `auth`, `user`): `pet/entity`, `pet/repository`, `pet/service`(+`impl`), `pet/controller`, `pet/dto/response`, `pet/enums` (nếu cần).
- `Progress` có thể nằm trong package `vocab` (vì gắn chặt với `VocabWord`) hoặc package `progress` riêng (khớp tên trong `project-overview.md` §2 "entities: User, Pet, VocabWord, Progress"). Đề xuất tạo package `progress` riêng để giữ đúng ranh giới package-by-feature, dù nó phụ thuộc cả `vocab` lẫn `pet`.
