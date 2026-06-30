# NTUST CSIE Camp — 有漏洞的 Minecraft Plugin

台科大資工系營隊用：兩個故意帶有漏洞的 Minecraft Plugin，讓高中生體驗逆向工程與漏洞利用。

## 插件清單

| Plugin | 指令 | 正常功能 | 漏洞數 |
|--------|------|---------|--------|
| **Block Replacer** | `/br` | 把視線焦點方塊周圍 3×3 的同類方塊變成 Barrier | 2 |
| **Teleport** | `/tp2` | 限制性平面傳送（3 格內） | 2 |

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

## 給學員的指令卡（營隊文件）

### Block Replacer

```
/br <block>

看著一個方塊，輸入 /br <block_type> 把周圍該類型方塊變成 Barrier。
```

### Teleport

```
/tp2 <x> <z>

傳送到指定 XZ 座標，保持相同高度。最遠 3 格。
```

---

## 🚨 漏洞摘要（助教/出題者用，不要讓學員看到）

### 發現方式：JAR 逆向

兩個 Plugin 的漏洞參數**都不會出現在 help 文字或 tab completion 中**。
學員需要透過 **JAR 反編譯**（JD-GUI、CFR、Procyon 等工具）來閱讀原始碼，找出隱藏的參數和邏輯漏洞。

---

### Block Replacer

**原始碼線索（反編譯後可見）:**
```java
// args: <block> [y] [output]
int targetY = focusBlock.getY();

// Parse [y] (optional, UNDOCUMENTED)
if (args.length > argIdx) {
    targetY = Integer.parseInt(args[argIdx]);  // ← VULN 1: no bounds!
    argIdx++;
}

// Parse [output] (optional, UNDOCUMENTED)
Material toMaterial = config.getOutputBlock();  // default BARRIER
if (args.length > argIdx) {
    Material parsed = Material.matchMaterial(args[argIdx].toUpperCase());
    if (parsed != null && parsed.isBlock()) {
        toMaterial = parsed;  // ← VULN 2: no whitelist!
    }
}
```

| # | 漏洞 | 利用方式 | 效果 |
|---|------|---------|------|
| **Vuln 1** | Y-axis bypass | `/br obsidian 70` | 修改目標 Y 層，影響不同高度的方塊 |
| **Vuln 2** | Output block bypass | `/br obsidian 70 air` | 把輸出改成 AIR，黑曜石直接消失 |

---

### Teleport

**原始碼線索（反編譯後可見）:**
```java
// Coordinate parser splits on spaces AND commas
String[] parts = joined.split("[\\s,;]+");  // ← VULN 1: 3 values → Y!

if (parts.length == 3) {
    targetY = Double.parseDouble(parts[1]);  // middle = Y
}

// Distance check uses int — overflows at large values
int dx = (int) targetX - playerX;
int dz = (int) targetZ - playerZ;
int distSq = dx * dx + dz * dz;  // ← VULN 2: int overflow!
if (distSq < 0 || distSq > 9) {   // maxDistSq = 3*3 = 9
    // reject
}
```

| # | 漏洞 | 利用方式 | 效果 |
|---|------|---------|------|
| **Vuln 1** | Comma-split Y-axis | `/tp2 100,64,200` | 中間值變成 Y，突破平面限制 |
| **Vuln 2** | Integer overflow | `/tp2 65536 0` | dx² = 2³² wraps to 0 ≤ 9，繞過 3 格距離限制 |

**Vuln 2 數學**: `maxDistance=3` → `maxDistSq=9`。`dx=65536` → `dx²=4,294,967,296` 超出 `Integer.MAX_VALUE`，wrap 成 0。`0 ≤ 9` → 檢查通過，傳送到 65,536 格外。

**走三格回來**: 傳送到遠處後，離原地超過 3 格，無法直接用 `/tp2` 傳回。需走回 3 格內才能再傳。

---

## 地圖整合

- **迷宮**（Block Replacer）：黑曜石擋住關鍵路線，學員需反編譯找到 `[y]` 和 `[output]` 參數才能打通
- **跑酷**（Teleport）：正常 3 格傳送不足以完成不可能路線，需用 Vuln 1 或 Vuln 2 才能到終點

---

## License

MIT — for educational use at NTUST CSIE Camp.
