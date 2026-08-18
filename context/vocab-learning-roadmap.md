# Vocabulary Learning — Multi-Phase Roadmap (Backend)

> Roadmap sống, cập nhật dần khi mỗi phase được load/complete qua `/feature`. Không phải spec để load trực tiếp.
>
> Spec chi tiết từng phase (đã viết, kèm các điểm cần chốt) — dùng `/feature load` đúng tên file khi bắt đầu:
> - Phase 1: `context/features/vocab-phase-1-content-api.md`
> - Phase 2: `context/features/vocab-phase-2-progress-pet-xp.md`
> - Phase 3: `context/features/vocab-phase-3-activity-variety.md` (tổng quan nhiều sub-feature nhỏ, không load thẳng — tách file con khi bắt tay vào từng activity type)
> - Phase 4: `context/features/vocab-phase-4-goal-personalization.md`

## Vì sao chia phase thế này

Flashcard + quiz + spaced repetition (đã có ở FE, mock data) là thứ mọi app học từ vựng đều có. Thứ PawLingo có mà nơi khác không có là **pet gắn trực tiếp với tiến độ học thật** (xem `project-overview.md` §1). Roadmap này ưu tiên dựng nền tảng data (Phase 1) đủ tổng quát để 2 trục khác biệt hoá — (a) pet phản hồi theo hoạt động học, (b) nhiều kiểu bài tập — cắm vào được mà **không phải đập lại schema**, thay vì lao vào thêm tính năng bề mặt trước.

Nguyên tắc xuyên suốt: **"loại hoạt động học" (activity type) và "phản hồi lên pet" (XP/energy) là 2 khái niệm tách biệt ngay từ đầu**, dù Phase 1 chỉ ship 1-2 activity type.

---

## Phase 1 — Vocab Content API (nền tảng)

**Trạng thái**: Not Started. **Mục tiêu**: thay mock data FE (`src/lib/vocab/topics.ts`) bằng API thật, không đổi UI.

- `GET /api/v1/vocab/topics` — danh sách topic (đã có 2 topic mock: Animals, Everyday Food)
- `GET /api/v1/vocab/topics/{topicId}` — danh sách từ trong topic
- Entity `VocabWord` tối thiểu: `id`, `topicId`, `word`, `meaning`, `example`, `imageUrl?`, `audioUrl?` — để hình + audio sẵn chỗ cho Phase 3, không cần dùng ngay.
- Entity `Topic` (hoặc enum nếu topic set cố định — cần quyết định khi viết spec chi tiết).
- Chưa đụng đến `Pet`/XP ở phase này — chỉ là content API thuần, giữ scope hẹp để ship nhanh, khớp đúng shape dữ liệu FE đang cần.

## Phase 2 — Progress + Pet-linked XP (đây là phase tạo khác biệt thật sự)

**Trạng thái**: Not Started. **Phụ thuộc**: Phase 1 (cần `VocabWord` để biết học đúng/sai gì) + cần `Pet` entity tối thiểu tồn tại (đang là TODO treo từ feature Auth MVP).

- Entity `Pet` tối thiểu: `id`, `userId`, `stage`, `energy`, `xp` — đóng nốt TODO auto-tạo pet khi register.
- `POST /api/v1/progress` — ghi nhận 1 lượt trả lời (đúng/sai, từ nào, **activity type nào**) → tính XP, cộng vào pet, trừ/cộng energy.
- Thiết kế field `activityType` trên `Progress` **ngay từ đầu** (dù Phase 1 FE chỉ có "quiz") — để Phase 3 thêm activity type mới không cần migration phá vỡ dữ liệu cũ.
- Rule khác biệt hoá: mỗi `activityType` cho XP/energy khác nhau (vd nghe-chọn cho nhiều XP hơn quiz trắc nghiệm vì khó hơn) — bảng mapping activityType → hệ số, để trong code hoặc config, không hardcode rải rác.
- Milestone XP → pet "học trick" mới — cần định nghĩa bảng mốc (vd mỗi 100 XP mở 1 trick), chi tiết hoá khi viết spec.

## Phase 3 — Đa dạng hoá activity type

**Trạng thái**: Not Started. **Phụ thuộc**: Phase 2 (khung `activityType` đã tồn tại, chỉ thêm giá trị mới + UI tương ứng bên FE).

Thêm dần (không cần làm hết 1 lần, mỗi cái là 1 sub-feature nhỏ):
- Điền từ vào câu (context, không học từ đơn lẻ trơ trọi)
- Ghép hình ↔ từ (ưu tiên cho persona `for-child`)
- Gõ lại từ (spelling / active recall)
- Chế độ "sprint" ôn tập tốc độ

## Phase 4 — Cá nhân hoá theo `goal`

**Trạng thái**: Not Started. **Phụ thuộc**: Phase 3 (cần đủ >1 activity type mới có gì để chọn tổ hợp).

- Field `goal` trên `User` đã tồn tại nhưng chưa dùng để quyết định UX học — thiết kế mapping `goal` → tổ hợp activity type ưu tiên (vd `for-child` ưu tiên ghép hình, `test-prep` ưu tiên điền câu).
- Có thể là logic thuần BE (API trả gợi ý activity type theo goal) hoặc để FE tự quyết theo `goal` lấy từ `/auth/me` — quyết định khi viết spec.

## Chưa scope (đã có trong `project-overview.md` roadmap, không lặp lại chi tiết ở đây)

- Pronunciation scoring (AI-based, Phase 2 theo `project-overview.md` §6) — có thể là 1 activity type trong Phase 3 sau này, nhưng cần entity/tích hợp AI riêng, không gộp vào roadmap này.
- Social/leaderboard (`project-overview.md` "Future") — phụ thuộc nhiều feature khác chưa có (bạn bè, notification...), không phải phần của Vocabulary Learning.

---

## Cách dùng roadmap này

Khi sẵn sàng làm 1 phase: viết `context/features/vocab-phase-{N}-{ten-ngan}.md` chi tiết hoá đúng phase đó (endpoints, DTO, migration, error code — theo format giống `google-auth-spec.md`), rồi `/feature load` file đó. Sau khi complete 1 phase, quay lại cập nhật trạng thái phase tương ứng trong file này (Not Started → Done) trước khi bắt đầu phase kế tiếp.
