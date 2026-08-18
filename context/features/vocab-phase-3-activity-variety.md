# Feature: Vocabulary Learning — Phase 3: Đa dạng hoá Activity Type

> Phần của `vocab-learning-roadmap.md` — Phase 3/4. Phụ thuộc Phase 2 (khung `activityType`/`ActivityScoringPolicy` đã tồn tại). Khác với Phase 1/2, đây **không phải 1 feature liền khối** mà là 1 tập hợp sub-feature nhỏ, độc lập — đề xuất `/feature load` từng cái riêng khi tới lượt làm, file này chỉ phác thảo để bạn chọn thứ tự/phạm vi.

## Summary

Nhờ Phase 2 đã tách `activityType` khỏi schema cứng (lưu string, có bảng hệ số điểm riêng trong code), mỗi activity type mới ở đây về nguyên tắc chỉ cần: 1 constant enum mới + 1 dòng hệ số XP/energy + (nếu cần) field đã có sẵn từ Phase 1 (`imageUrl`, `audioUrl`) — không cần migration DB cho phần lớn trường hợp.

## Các activity type đề xuất (độc lập, làm theo thứ tự tuỳ ưu tiên)

### 1. Điền từ vào câu (Fill-in-blank)

- Dữ liệu: dùng thẳng `exampleSentence` đã có ở `VocabWord` (Phase 1) — **không thêm cột mới**.
- **Quyết định cần chốt**: ai làm việc "ẩn từ trong câu" — BE trả nguyên câu, FE tự thay `word` bằng `___` (đơn giản, không cần đổi API); hay BE trả sẵn câu đã ẩn qua 1 field riêng (BE chịu trách nhiệm, tránh lộ đáp án nếu FE có bug hiển thị)? Đề xuất hướng đầu (FE tự ẩn) vì câu ví dụ vốn không phải bí mật, không có rủi ro bảo mật thật sự nếu FE lỡ hiện nguyên câu.
- Backend: chỉ thêm `ActivityType.FILL_IN_BLANK` + hệ số điểm.

### 2. Ghép hình ↔ từ (Picture match)

- Dữ liệu: dùng `imageUrl` đã có ở `VocabWord` (Phase 1, hiện đang null hết).
- **Việc cần làm thật sự nằm ở nội dung, không phải code**: phải có ảnh thật cho từng từ trước — đây là công việc chuẩn bị dữ liệu (tìm/generate ảnh, upload lên đâu đó — S3/Cloudinary/thư mục static), cần quyết định riêng, không phải quyết định kỹ thuật của backend.
- Backend: chỉ thêm `ActivityType.PICTURE_MATCH` + hệ số điểm. Không cần API mới — `GET /vocab/topics/{code}` đã trả `imageUrl` sẵn từ Phase 1.

### 3. Gõ lại từ (Spelling)

- Không cần field mới, không cần thay đổi response — FE so khớp input người dùng gõ với `word` đã có sẵn ngay từ Phase 1.
- Backend: chỉ thêm `ActivityType.SPELLING` + hệ số điểm (đề xuất hệ số cao hơn quiz vì đòi hỏi active recall, khó hơn multiple-choice).

### 4. Nghe - chọn từ đúng (Listening)

- Dữ liệu: dùng `audioUrl` đã có ở `VocabWord` (Phase 1, hiện đang null hết) — cùng vấn đề chuẩn bị nội dung như Picture match (cần file audio thật, quyết định lưu trữ ở đâu).
- Đây cũng là bước đệm tự nhiên hướng tới "pronunciation scoring" (roadmap Phase 2 trong `project-overview.md` §6) nếu sau này muốn làm — nhưng bản thân "nghe - chọn từ" ở đây chỉ là nghe thụ động, chưa chấm phát âm của user.
- Backend: chỉ thêm `ActivityType.LISTENING` + hệ số điểm.

### 5. Chế độ "sprint" ôn tập tốc độ

- Đây **không phải** 1 `activityType` để chấm điểm — nó là cách **gộp nhiều từ thành 1 phiên** có giới hạn thời gian, activity bên trong vẫn là quiz/spelling/... như bình thường.
- Cần 1 API mới để lấy "mẻ từ cần ôn": `GET /api/v1/vocab/review-batch?limit=20` — chọn từ theo tiêu chí gì?
- **Quyết định cần chốt (quan trọng, ảnh hưởng kiến trúc)**: hiện tại state "từ nào cần ôn lại" (kiểu Leitner box) đang nằm ở đâu?
  - Nếu vẫn để FE tự quản lý (localStorage, như 1 số bản FE cũ đã làm) → BE không cần biết gì, `review-batch` chỉ đơn giản là "N từ ngẫu nhiên/theo topic", FE tự lọc từ nào cần ôn.
  - Nếu muốn Leitner state đồng bộ nhiều thiết bị (đúng tinh thần "server là nguồn sự thật duy nhất" đã áp dụng cho `Progress`) → cần thêm bảng `WordMastery` (userId, vocabWordId, box, nextReviewAt) cập nhật mỗi lần có `Progress` mới, và `review-batch` sẽ query theo `nextReviewAt <= now`.
  
  Đây là quyết định lớn nhất trong Phase 3, đề xuất bàn riêng kỹ hơn khi tới lượt làm sprint mode, không quyết vội ở đây.

## Out of Scope

- Chấm điểm phát âm thật (so khớp giọng nói với AI) — khác hẳn về hạ tầng (cần tích hợp speech-to-text/AI service), nằm ở "Phase 2" của `project-overview.md` roadmap tổng, không phải phase này.
- Lưu trữ file ảnh/audio (S3, CDN...) — là quyết định hạ tầng/vận hành, không thuộc phạm vi 1 feature backend đơn lẻ.

## Notes

- Vì mỗi mục ở trên độc lập và nhỏ, đề xuất khi bắt đầu thực sự làm 1 mục thì tách thành file spec riêng (vd `vocab-phase-3a-fill-in-blank.md`) rồi mới `/feature load` — file này chỉ để bạn nhìn tổng quan và chọn cái nào làm trước.
- Thứ tự gợi ý theo độ khó tăng dần: Spelling (không cần data mới) → Fill-in-blank (không cần data mới) → Picture match/Listening (cần chuẩn bị ảnh/audio trước) → Sprint mode (cần chốt kiến trúc Leitner state trước).
