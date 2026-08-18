# Feature: Vocabulary Learning — Phase 4: Cá nhân hoá theo Goal

> Phần của `vocab-learning-roadmap.md` — Phase 4/4. Phụ thuộc Phase 3 (cần >1 activity type mới có gì để chọn tổ hợp). Đây là phase **nhẹ nhất về kỹ thuật** trong 4 phase — bản chất chỉ là 1 bảng tra cứu, điểm cần chốt là tra ở đâu (BE hay FE).

## Summary

`User.goal` (enum `beginner`/`test-prep`/`professional`/`for-child`) đã tồn tại từ feature Auth MVP nhưng chưa ảnh hưởng gì đến trải nghiệm học. Phase này dùng `goal` để quyết định tổ hợp `activityType` nào nên ưu tiên hiện cho user đó (vd `for-child` ưu tiên ghép hình, `test-prep` ưu tiên điền câu).

## Quyết định cần chốt — logic này nằm ở BE hay FE?

**Phương án A — BE quyết định (thêm endpoint mới)**:
```
GET /api/v1/vocab/recommended-activities
→ { "data": ["picture_match", "quiz", "spelling"] }  // theo goal của user đang đăng nhập
```
Ưu điểm: đổi mapping goal→activity không cần build lại FE, logic tập trung 1 chỗ. Nhược điểm: thêm 1 network round-trip, BE phải biết về khái niệm UI "ưu tiên hiển thị" vốn thuần là mối quan tâm của FE.

**Phương án B — FE tự quyết định**: BE không làm gì thêm — `goal` đã có sẵn trong response của `GET /auth/me`, FE tự giữ 1 bảng mapping tĩnh `goal → activityType[]` ở phía client.
Ưu điểm: không cần API mới, không round-trip thêm. Nhược điểm: đổi mapping phải deploy lại FE.

**Đề xuất**: Phương án B trước — vì đây chỉ là 1 bảng tra cứu tĩnh, chưa có lý do gì cần đổi "live" mà không deploy được. Chỉ chuyển sang Phương án A nếu sau này mapping cần cá nhân hoá động (vd dựa thêm vào lịch sử học, A/B test...), lúc đó mới đáng có 1 endpoint riêng.

## Nếu chọn Phương án A (BE quyết định)

### Endpoints

| Method | Path | Status |
|---|---|---|
| GET | `/api/v1/vocab/recommended-activities` | Planned (chỉ nếu chọn phương án A) |

Response `200`:
```json
{ "success": true, "data": ["picture_match", "quiz", "spelling"], "error": null }
```

### Data Model

Không cần bảng DB mới — mapping `Goal → List<ActivityType>` là hằng số trong code (vd `GoalActivityPolicy`), cùng tinh thần với `ActivityScoringPolicy` ở Phase 2 (bảng tra cứu tĩnh trong code, không phải DB, vì chưa có nhu cầu chỉnh động).

### Security

Cần JWT (đọc `goal` của chính user đang đăng nhập qua token, không nhận `userId`/`goal` từ query param — tránh lộ thông tin cá nhân hoá của user khác).

## Nếu chọn Phương án B (FE tự quyết định)

Không có việc gì cho backend — đóng phase này bằng cách xác nhận `GET /auth/me` đã đủ thông tin (`goal`) để FE tự xử lý, không cần thay đổi gì thêm ở BE.

## Out of Scope

- Cá nhân hoá động dựa trên hành vi học thực tế (vd user hay sai activity nào thì ưu tiên activity đó nhiều hơn) — đây là gợi ý kiểu adaptive learning, cần dữ liệu `Progress` tích luỹ đủ lâu mới có ý nghĩa, nên tách thành 1 initiative riêng hẳn sau này, không nằm trong roadmap Vocabulary Learning 4 phase này.

## Notes

- Đây là phase nhẹ nhất, có thể gộp chung vào cuối Phase 3 thay vì tách feature riêng nếu muốn — tách riêng ở đây chỉ để giữ đúng ranh giới "1 mối quan tâm = 1 phase" theo roadmap gốc.
