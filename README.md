# Hardcore Mod (Fabric 1.21.11)

Mod Fabric cho Minecraft 1.21.11 (Mojang mappings, Loom 1.17, Loader 0.19+).

## Tính năng

### 1. Replay game cho world Hardcore
- Chỉ hoạt động ở chế độ đơn / LAN (integrated server).
- Khi **tất cả** người chơi đã chết (spectator hoặc đang màn hình chết), gõ `/replay` (mọi người, không cần op) → GUI 2 nút:
  - **Chơi tiếp (+25%)**: về survival ngay, trả lại toàn bộ item trước khi chết (dọn sạch item đã rơi xuống đất lúc chết, không nhặt trùng), tạo backup world (zip), quái mạnh hơn 25% (tốc độ, sát thương, creeper nổ to & nhanh hơn, spawn nhiều hơn). Độ khó nether/end được reset về ×1 khi replay.
  - **Chơi lại từ backup (−25%)**: load backup gần nhất **ngay trong server đang chạy — không kick người chơi LAN**. Mọi người được dịch chuyển tạm ra xa ~2s, chunk cũ được unload sạch (không ghi đè file đã restore), world reload từ file backup, rồi tất cả về spawn ở chế độ survival, quái yếu hơn 25%. Độ khó nether/end reset về ×1.
  - **Nhiều người chơi (LAN)**: chọn chế độ bằng **vote 180 giây** — bên nhiều phiếu hơn thắng (hòa → Chơi tiếp). Màn replay hiển thị phiếu + đếm ngược + độ khó hiện tại.
  - **Độ khó theo tiến trình**: kích hoạt cổng Nether lần đầu → quái ×2; vào The End lần đầu → ×4 (không cộng dồn với ±25% replay).
- Backup lưu tại `hardcore-mod-backups/` trong game dir; cấu hình độ khó ở `config/hardcoremod.json` (nằm ngoài world nên sống sót qua restore).

### 2. Stat Points (phím G)
- Nguồn điểm: giết quái = 1 Points Shard (10 shard = 1 Point), lên 1 level XP = 1 Point, Warden = 20, Ender Dragon = 50.
- GUI (G) cộng: Sức mạnh (+0.5 dmg/cấp), Tốc độ đào (+0.15/cấp), Máu (+2 HP/cấp). Giá = cấp hiện tại + 1.

### 3. Nguyên tố
- Vào world lần đầu → GUI chọn nguyên tố (Lửa/Băng/Lượng tử/Sét/Nước/Đất/Gió/Vật lý). Mỗi nguyên tố có passive + hiệu ứng đánh + miễn nhiễm riêng. Chọn xong vào chơi luôn (không có màn loading giả).
- Chọn lại: cần đủ 100 Points, gõ `/element`.

| Nguyên tố | Passive | Hiệu ứng khi đánh | Miễn nhiễm |
|---|---|---|---|
| Lửa | +1.5 sát thương | 20%+ cháy 3s | lửa |
| Băng | +2 giáp | đóng băng + chậm | đóng băng |
| Lượng tử | +10% tốc độ | 10% x2 sát thương | — (né 10% đòn) |
| Sét | +20% tốc độ đánh | gọi sét đánh trúng | sét |
| Nước | +4 máu | +50% vs mục tiêu đang cháy | — |
| Đất | +3 giáp, 50% kháng knockback | hất văng kẻ địch | — |
| Gió | nhảy cao hơn (+0.3) | hất tung kẻ địch lên trời | ngã |
| Vật lý | +2 độ bền giáp | +50% sát thương + hất văng mạnh | nổ |

### 4. Enchantments (sách tự xuất hiện ở bàn mê hoặc, `/hc book` để lấy sách cấp chỉ định)
- **Tăng sát thương kèm theo I–V** (5 cấp): mỗi đòn cộng thêm % sát thương kiếm + % sát thương từ stat, nhận stack "Tăng sát thương" (5–20 tầng, 5 phút, reset khi đánh), mỗi tầng tăng thêm sát thương.
- **Tăng sát thương <nguyên tố> I–X** (+10%/cấp), từ XI trở đi +5%/cấp (tối đa 20 cấp) — nhân sát thương nguyên tố của đòn đánh và tăng tỉ lệ hiệu ứng nguyên tố.

### 5. Damage counter
- Góc phải trên: tên nguyên tố + damage cộng dồn, màu theo nguyên tố, tự mờ dần sau 7 giây.
- Góc trái trên: thanh "Tăng sát thương" stacks.
- Counter và stacks là **riêng cho từng người chơi** — chỉ hiển thị đòn đánh của chính bạn.

### 6. Admin Panel
- `/admin gui <password>` — mật khẩu: `TakanashiHoshiles`.
- GUI cho phép: đổi game mode, cộng Points/Shards/cấp stat, chọn lại nguyên tố, mở Replay ngay (không cần điều kiện chết), bật/tắt 10 gamerule (send command output, mob griefing, spawn monsters, fire damage, keep inventory, daylight/weather cycle, natural regen, mob/block drops).
- Ô nhập lệnh ở cuối GUI: gõ lệnh (bấm **Chạy** hoặc **Enter**) để thực thi với quyền console — ví dụ `/give @p diamond 64`, `/weather clear`.

### 7. Chế tạo sách (bàn chế tạo) — nguyên liệu hiếm
- Tăng sát thương kèm theo I: Sách + Kim cương + 2 Sắt.
- Tăng sát thương <nguyên tố> I: Sách + Kim cương + vật phẩm nguyên tố (Lửa=Bột lửa, Băng=Băng xanh, Lượng tử=Mảnh thạch anh tím, Sét=Thuốc súng, Nước=Mảnh vỏ sò, Đất=Đá hắc ám, Gió=Màng Phantom, Vật lý=Vàng).
- Tăng tỉ lệ <nguyên tố> I: Sách + Kim cương + Đá đỏ + vật phẩm nguyên tố.
- Nâng cấp: 2 cuốn cùng cấp trong **anvil** → cấp +1.
- `/akumiyuukiirecipe book` — xem toàn bộ công thức theo danh mục.
- Nút "Chơi tiếp" **xóa backup cũ** trước khi tạo backup mới (tùy chọn "Backup cũ: GIỮ/XÓA" trong `/admin gui`).

## Lệnh
- `/replay` — mở GUI replay (hardcore, mọi người đã chết).
- `/element` — chọn lại nguyên tố (tốn 100 Points).
- `/admin gui <password>` — mở Admin Panel.
- `/akumiyuukiirecipe book` — mở sách công thức chế tạo.
- `/hc book <combo|fire|ice|quantum|lightning|water|earth|wind|physics|chance_...> <cấp> [player]` (op)
- `/hc points add|set <n> [player]` (op)
- `/hc element <player>` — mở lại GUI chọn nguyên tố (op, test)

## Phím
- `G` — mở GUI Stat Points.

## Ghi chú giới hạn
- Soft restore: chunk ở xa người chơi unload dần trong vài giây — quay lại khu vực cũ ngay lập tức có thể thấy terrain cũ trong thời gian ngắn. Trạng thái level.dat trong bộ nhớ (giờ, gamerule) giữ nguyên bản hiện tại; level.dat của backup áp dụng khi mở lại world lần sau. Inventory/XP người chơi giữ nguyên (world + spawn + survival được khôi phục).
- Sét nguyên tố có thể đốt cháy khối gần đó (vanilla).
- Lượng spawn quái được scale qua `MobCategory.getMaxInstancesPerChunk` (xấp xỉ).
- Backup zip thời điểm bấm nút (có thể lệch vài tick autosave).
- Enchantments là data-driven (data/hardcoremod/enchantment/*.json).

## Build
```
./gradlew build
```
Jar ở `build/libs/`.
