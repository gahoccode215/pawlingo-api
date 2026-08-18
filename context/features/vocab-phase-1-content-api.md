# Feature: Vocabulary Learning — Phase 1: Vocab Content API

> Phần của `vocab-learning-roadmap.md` — Phase 1/4. Thiết kế độc lập theo best-practice backend, **không** tham chiếu FE (`pawlingo-ui`) vì FE đang là prototype, có thể đổi. Các điểm cần bạn chốt được đánh dấu rõ **"Quyết định cần chốt"**.

## Summary

API đọc nội dung từ vựng (chủ đề + từ trong chủ đề) để FE thay mock data bằng dữ liệu thật. Thuần content API — chưa đụng `Pet`, XP, hay tiến độ cá nhân của user (đó là Phase 2).

## Goals

- FE lấy được danh sách chủ đề (topic) để hiện màn chọn chủ đề.
- FE lấy được danh sách từ trong 1 chủ đề để chạy flashcard/quiz.
- Schema đủ chỗ cho ảnh/audio ngay từ đầu (dù chưa dùng) để Phase 3 (ghép hình, nghe) không cần migration thêm cột.
- Nội dung (chủ đề, từ vựng) quản lý được qua DB, không hardcode trong code Java — thêm chủ đề mới không cần build lại app.

## Endpoints

| Method | Path | Status |
|---|---|---|
| GET | `/api/v1/vocab/topics` | Planned |
| GET | `/api/v1/vocab/topics/{topicCode}` | Planned |

### GET /api/v1/vocab/topics

Response `200`:
```json
{
  "success": true,
  "data": [
    { "code": "animals", "name": "Animals", "description": "...", "wordCount": 20 }
  ],
  "error": null
}
```

### GET /api/v1/vocab/topics/{topicCode}

Response `200`:
```json
{
  "success": true,
  "data": {
    "code": "animals",
    "name": "Animals",
    "description": "...",
    "words": [
      {
        "id": "uuid",
        "word": "dog",
        "meaning": "con chó",
        "exampleSentence": "The dog is running.",
        "imageUrl": null,
        "audioUrl": null,
        "orderIndex": 1
      }
    ]
  },
  "error": null
}
```

**Quyết định cần chốt #1 — định danh topic trong URL**: đề xuất dùng `code` dạng slug (`animals`, `everyday-food`) làm path param thay vì UUID — URL dễ đọc, ổn định qua các lần re-seed dữ liệu (UUID sẽ đổi nếu xoá-tạo lại). `id` (UUID) vẫn là PK nội bộ, `code` là cột unique riêng. Nếu bạn muốn URL bằng UUID cho đơn giản/nhất quán với các resource khác thì báo lại, đổi 1 dòng migration + DTO là xong.

## Data Model

```
Topic (mới)
- id            (UUID, PK)
- code          (VARCHAR, unique, not null) — slug dùng trong URL, vd "animals"
- name          (VARCHAR, not null) — tên hiển thị
- description   (TEXT, nullable)
- orderIndex    (INT, not null, default 0) — thứ tự hiện trên màn chọn topic
- createdAt / updatedAt

VocabWord (mới)
- id                (UUID, PK)
- topicId           (UUID, FK -> topics.id, not null)
- word              (VARCHAR, not null)
- meaning           (VARCHAR, not null) — nghĩa tiếng Việt
- exampleSentence   (TEXT, nullable)
- imageUrl          (VARCHAR, nullable) — chưa dùng ở Phase 1, chuẩn bị cho Phase 3 (ghép hình)
- audioUrl          (VARCHAR, nullable) — chưa dùng ở Phase 1, chuẩn bị cho Phase 3 (nghe-chọn)
- orderIndex        (INT, not null, default 0)
- createdAt / updatedAt
```

Migration Flyway: `V3__create_topics_and_vocab_words.sql`.

**Quyết định cần chốt #2 — nguồn nội dung**: bảng có rồi nhưng ai đổ dữ liệu từ vựng thật vào? 3 hướng:
1. Seed cứng vài chục từ mẫu ngay trong migration Flyway (nhanh, đủ để FE tích hợp, nhưng sửa nội dung sau phải viết migration mới).
2. Viết sẵn `POST /api/v1/vocab/topics` + `POST /api/v1/vocab/topics/{id}/words` (admin-only, cần role/permission — hiện chưa có khái niệm role trong `User`) để nhập liệu qua API.
3. Import từ file (CSV/JSON) bằng 1 script chạy tay, không qua API — nhanh gọn cho giai đoạn còn ít nội dung, không cần xây admin API sớm.

Đề xuất **hướng 1** cho Phase 1 (seed cứng vài chục từ) để không mất công xây admin API khi nội dung còn ít, chuyển sang hướng 2/3 khi số lượng từ vựng lớn dần.

## Validation Rules

- `topicCode` không tồn tại → lỗi `404 TOPIC_NOT_FOUND`.

## Security Requirements

**Quyết định cần chốt #3 — endpoint có cần JWT không?**: nội dung từ vựng bản thân không nhạy cảm, nhưng theo convention hiện tại (`SecurityConfig`) mọi endpoint ngoài `register/login/google` đều yêu cầu JWT mặc định. Đề xuất **giữ yêu cầu JWT** (không thêm vào `WHITELIST_ENDPOINTS`) vì: (a) nhất quán, (b) Phase 2 chắc chắn cần biết user là ai để tính progress, nên tách "công khai ở Phase 1, khoá lại ở Phase 2" chỉ tạo thêm 1 lần đổi không cần thiết.

## Error Handling

| Tình huống | HTTP status | error.code |
|---|---|---|
| `topicCode` không tồn tại | 404 | `TOPIC_NOT_FOUND` |

Thêm `TOPIC_NOT_FOUND` vào `ErrorCode` enum hiện có.

## Out of Scope (phase sau)

- Mọi thứ liên quan `Pet`/XP/tiến độ cá nhân — Phase 2.
- Admin CRUD API cho topic/word — chỉ làm nếu chọn hướng 2 ở Quyết định #2.
- Lọc/gợi ý topic theo `goal` của user — Phase 4.

## Notes

- Package: tạo package mới `vocab` (ngang hàng `auth`, `user`) theo đúng package-by-feature — `vocab/entity`, `vocab/repository`, `vocab/service`(+`impl`), `vocab/controller`, `vocab/dto/response`.
- `wordCount` trong response list topic là field tính (COUNT theo `topicId`), không lưu cột riêng trong `Topic` — tránh dữ liệu bị lệch khi thêm/xoá từ.
