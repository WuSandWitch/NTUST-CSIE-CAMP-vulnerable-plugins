# NTUST CSIE Camp — 有漏洞的 Minecraft Plugin

台科大資工系營隊用：兩個故意帶有漏洞的 Minecraft Plugin，讓高中生體驗滲透測試與漏洞利用。

## 插件清單

| Plugin | 指令 | 正常功能 | 漏洞數 |
|--------|------|---------|--------|
| **Block Replacer** | `/br` | 把指定方塊變成 Barrier | 2 |
| **Teleport** | `/tp2` | 限制性平面傳送（100 格內） | 2 |

---

## 環境需求

- **Java**: JDK 17
- **Maven**: 3.6+
- **Minecraft Server**: Paper/Spigot 1.18.x

## 建置

```bash
mvn clean package

# 產出:
#   block-replacer/target/BlockReplacer-1.0.0.jar
#   teleport/target/Teleport-1.0.0.jar
```

## 安裝

把兩個 `.jar` 放到 server 的 `plugins/` 目錄，重啟 server。

---

## Block Replacer

### 正常用法

```
/br <block>
```

把玩家周圍半徑 30 格內、Y=64 高度上所有 `<block>` 方塊變成 Barrier。

### Config（plugins/BlockReplacer/config.yml）

```yaml
target_y: 64
output_block: BARRIER
radius: 30
```

---

## Teleport

### 正常用法

```
/tp2 <x> <z>
```

傳送到指定 XZ 座標（保持當前 Y），最遠 100 格。支援空格或逗號分隔（如 `100,200`）。

### Config（plugins/Teleport/config.yml）

```yaml
max_distance: 100
```

---

## 🚨 漏洞摘要（助教/出題者用，不要讓學員看到）

### Block Replacer

| # | 類型 | 觸發方式 | 效果 | 線索 |
|---|------|---------|------|------|
| **Vuln 1** | Input validation: Y-axis | `/br obsidian 70` | 把 target Y 從 64 改成 70，影響其他高度的方塊 | 打 `/br` 看 help 會顯示完整用法 `/br <block> [y] [output]` |
| **Vuln 2** | Input validation: output block | `/br obsidian 64 air` | 把輸出方塊改成 AIR，黑曜石直接消失 | 同上，help text 暴露了 `[output]` 參數的存在 |

**設計理念**：開發者在測試時加了 `[y]` 和 `[output]` 兩個便利參數，測試完忘記拔掉。營隊文件只說 `/br <block>`，但打 `/br` 不帶參數就會看到完整用法。這不是通靈 — 是 reading the manual。

### Teleport

| # | 類型 | 觸發方式 | 效果 | 線索 |
|---|------|---------|------|------|
| **Vuln 1** | Input parsing: comma-split | `/tp2 100,64,200` | 中間值變成 Y 座標，突破「只能平面傳送」限制 | help 說支援逗號分隔；很多遊戲用 `x,y,z` 格式，學生自然會試 |
| **Vuln 2** | Integer overflow | `/tp2 65536 0` | dx² overflow → 距離檢查繞過，傳送到 65K 格外 | 被拒絕時顯示 `Distance: NaN blocks` — 學生看到 NaN 就知道數學爆了 |

**設計理念—Vuln 1**：開發者用 `split("[\\s,;]+")` 想支援 `100,200` 這種逗號格式，沒注意到 `100,64,200` 會被拆成三個值。程式把多出來的值當成 Y。

**設計理念—Vuln 2**：距離檢查用 `int distSq = dx*dx + dz*dz`，但 `int` 只有 32-bit。當 dx=65536 時，dx²=2³² 剛好 overflow 成 0，距離檢查通過，但實際傳送用 double 所以傳到 65536 格外。

---

## 地圖整合

- **迷宮**（Block Replacer）：黑曜石牆在特定 Y 層，學生需用 Vuln 1 改 Y + Vuln 2 改 output block 才能打通
- **跑酷**（Teleport）：「不可能完成」的關卡需用 Vuln 1 或 Vuln 2 才能到終點

---

## License

MIT — for educational use at NTUST CSIE Camp.
