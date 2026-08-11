# Công thức chế tạo & nâng cấp sách — Hardcore Mod

Tất cả công thức chế tạo ở **bàn chế tạo** (shapeless). Nâng cấp dùng **anvil**.

## 1. Tăng sát thương kèm theo (I–V)

| Mục | Công thức |
|---|---|
| Chế tạo Cuốn I | Sách + Kim cương + 2 Sắt |
| Nâng cấp | Anvil: 2 cuốn cùng cấp → cấp +1 (I+I=II, II+II=III, ... tối đa V) |

Tác dụng: mỗi đòn cộng % sát thương kiếm + % sát thương stat, nhận 1 tầng sát thương (reset 5 phút, mỗi tầng +2% (I) → +5% (V), tối đa 5–20 tầng).

## 2. Tăng sát thương <nguyên tố> (I–XX)

Chế tạo Cuốn I: **Sách + Kim cương + vật phẩm nguyên tố**

| Nguyên tố | Vật phẩm | Công thức |
|---|---|---|
| Lửa | Bột lửa (Blaze Powder) | Sách + Kim cương + Bột lửa |
| Băng | Băng xanh (Blue Ice) | Sách + Kim cương + Băng xanh |
| Lượng tử | Mảnh thạch anh tím (Amethyst Shard) | Sách + Kim cương + Mảnh thạch anh tím |
| Sét | Thuốc súng (Gunpowder) | Sách + Kim cương + Thuốc súng |
| Nước | Mảnh vỏ sò (Prismarine Shard) | Sách + Kim cương + Mảnh vỏ sò |
| Đất | Đá hắc ám (Obsidian) | Sách + Kim cương + Đá hắc ám |
| Gió | Màng Phantom (Phantom Membrane) | Sách + Kim cương + Màng Phantom |
| Vật lý | Vàng (Gold Ingot) | Sách + Kim cương + Vàng |

Nâng cấp: Anvil — 2 cuốn cùng cấp → cấp +1 (tối đa XX).
Tác dụng: I–X +10% sát thương nguyên tố/cấp; từ XI +5%/cấp.

## 3. Tăng tỉ lệ <nguyên tố> (I–V)

Chế tạo Cuốn I: **Sách + Kim cương + Đá đỏ + vật phẩm nguyên tố**

| Nguyên tố | Công thức |
|---|---|
| Lửa | Sách + Kim cương + Đá đỏ + Bột lửa |
| Băng | Sách + Kim cương + Đá đỏ + Băng xanh |
| Lượng tử | Sách + Kim cương + Đá đỏ + Mảnh thạch anh tím |
| Sét | Sách + Kim cương + Đá đỏ + Thuốc súng |
| Nước | Sách + Kim cương + Đá đỏ + Mảnh vỏ sò |
| Đất | Sách + Kim cương + Đá đỏ + Đá hắc ám |
| Gió | Sách + Kim cương + Đá đỏ + Màng Phantom |
| Vật lý | Sách + Kim cương + Đá đỏ + Vàng |

Nâng cấp: Anvil — 2 cuốn cùng cấp → cấp +1 (tối đa V).
Tác dụng: +8% tỉ lệ hiệu ứng nguyên tố / cấp.

## 4. Cách lấy sách cấp chỉ định (admin)

`/hc book <loại> <cấp> [player]`
- Loại: `combo` | `fire` | `ice` | `quantum` | `lightning` | `water` | `earth` | `wind` | `physics` | `chance_fire` ... `chance_physics`
- Ví dụ: `/hc book ice 10` — sách Tăng sát thương băng X; `/hc book chance_lightning 5` — sách Tăng tỉ lệ sét V.
