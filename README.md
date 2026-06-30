# NTUST CSIE Camp — 有漏洞的 Minecraft Plugin

台科大資工系營隊用：兩個故意帶有漏洞的 Minecraft Plugin，讓高中生體驗滲透測試與漏洞利用。

## 插件清單

| Plugin | 正常功能 | 漏洞數 |
|--------|---------|--------|
| **Block Replacer** | 把 Y=N 高度的指定方塊全部變成 Barrier | 2 |
| **Teleport** | 限制性平面傳送（距離上限 100 格） | 2 |

---

## 環境需求

- **Java**: JDK 17
- **Maven**: 3.6+
- **Minecraft Server**: Paper/Spigot 1.18.x
- **設定**: `JAVA_HOME` 指向 JDK 17

## 建置

```bash
# 全部 build
mvn clean package

# 產出位置:
#   block-replacer/target/BlockReplacer-1.0.0.jar
#   teleport/target/Teleport-1.0.0.jar
```

## 安裝到 Minecraft Server

把兩個 `.jar` 放到 server 的 `plugins/` 目錄，重啟 server。

---

## Block Replacer — 功能說明

- **指令**: `/br <block_type>` — 把玩家周圍半徑 30 格內，Y=64 高度上所有 `<block_type>` 方塊變成 Barrier
- **Config**: `plugins/BlockReplacer/config.yml`，可設定 target_y 和 output_block

## Teleport — 功能說明

- **指令**: `/tp2 <x> <z>` — 傳送到指定 XZ 座標（保持當前 Y 高度），最遠 100 格
- **Config**: `plugins/Teleport/config.yml`，可設定 max_distance

---

## 🚨 漏洞摘要（給助教/出題者看，不要讓學員看到）

### Block Replacer 漏洞

| # | 漏洞 | 觸發方式 | 效果 |
|---|------|---------|------|
| **Vuln 1** | Hidden debug command: Y-axis bypass | `/br sety <y>` | 更改 target Y，可 replace 任意高度的方塊 |
| **Vuln 2** | Hidden debug command: output block bypass | `/br output <material>` | 更改輸出方塊（例如設為 AIR 讓黑曜石消失） |

**線索**: 這兩個 debug 指令不會出現在 `/help` 或 tab completion 中，但可以手動輸入。
學生可以先試 `/br sety 10` 看看會不會報錯來發現。

### Teleport 漏洞

| # | 漏洞 | 觸發方式 | 效果 |
|---|------|---------|------|
| **Vuln 1** | Hidden 3D teleport | `/tp2 <x> <z> <y>` (三個參數) | 可以指定 Y 座標，突破「只能平面傳送」的限制 |
| **Vuln 2** | Integer overflow 距離檢查 | `/tp2 65536 0` | dx² overflow 成負值/小值，繞過距離上限檢查，傳送到極遠處 |

**線索**: 
- Vuln 1: 多給一個參數試試
- Vuln 2: `max_distance: 100`，試試看各種極端數字，Java 的 int 最大值是 2,147,483,647

### 數學補充：Integer Overflow

Java 的 `int` 是 32-bit signed，範圍是 `[-2³¹, 2³¹-1]` = `[-2,147,483,648, 2,147,483,647]`。

當 `dx = 65536` 時：
- `dx * dx = 4,294,967,296` > `Integer.MAX_VALUE` → **overflow**
- Java 會 wrap around：`4,294,967,296 - 2³² = 0`
- 所以 `distSq = 0 ≤ 10000`（maxDistSq）→ **距離檢查通過！**
- 但實際 teleport 用的是原始 double 值 → 傳送到 65,536 格外

---

## 地圖整合指南

Block Replacer 用於**迷宮關卡**：
- 迷宮的黑曜石牆在特定 Y 層 → 學生需用 Vuln 1 改 Y、用 Vuln 2 改 output block，才能破壞黑曜石取得 FLAG
- 建議 default `target_y` 設在不影響迷宮結構的高度

Teleport 用於**跑酷關卡**：
- 「不可能完成」的關卡需用 Vuln 1 (Y-axis) 或 Vuln 2 (遠距離傳送) 才能到達終點
- 終點 FLAG 放在正常無法到達的位置

---

## License

MIT — for educational use at NTUST CSIE Camp.
