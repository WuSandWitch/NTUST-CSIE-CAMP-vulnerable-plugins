# NTUST CSIE Camp — 有漏洞的 Minecraft Plugin

台科大資工系營隊用：兩個故意帶有漏洞的 Minecraft Plugin，讓高中生體驗 JAR 逆向工程與漏洞利用。

## 插件清單

| Plugin | 指令 | 正常功能 | 漏洞 |
|--------|------|---------|------|
| **Block Replacer** | `/br` | 把玩家周圍 3×3 的方塊變成基岩 | Y-axis 竄改 + whitelist 繞過 |
| **Teleport** | `/tp2` | 限制性平面傳送（3 格內） | Y-axis 突破 + Integer Overflow |

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

把你腳下 3×3 範圍內的 <block> 方塊變成基岩。
```

### Teleport

```
/tp2 <x> <z>

傳送到指定 XZ 座標（相同高度），最遠 3 格。
```

---

## 🚨 漏洞摘要（助教/出題者用，不要讓學員看到）

### 發現方式：JAR 逆向

所有隱藏參數和邏輯漏洞都**不會出現在 help、tab completion 或 error message 中**。
學員需反編譯 JAR 來發現。

---

### Block Replacer

**隱藏指令（plugin.yml 裡註冊為 `/br` 的 alias）:**

| 指令 | 功能 | 漏洞 |
|------|------|------|
| `/br_y <y>` | 修改 target Y | **無權限檢查、無 bounds check** — 可以設任意高度 |
| `/br_target <allowed> [actual]` | 修改輸出方塊 | **whitelist 只在第一參數檢查**，第二參數直接拿來用 |

**Vuln 1: `/br_y`**
- 反編譯後在 plugin.yml 會看到 `aliases: [br_y, br_target]`
- 或在原始碼看到 `label.equalsIgnoreCase("br_y")` 分支
- 沒有任何權限或範圍驗證

**Vuln 2: `/br_target` 的 whitelist 繞過**
```java
// Whitelist only checks args[0]
if (!ALLOWED_TARGETS.contains(args[0].toUpperCase())) { reject; }

// But applies args[1] if present!
String actualName = args.length >= 2 ? args[1] : args[0];
Material mat = Material.matchMaterial(actualName.toUpperCase());
config.setOutputBlock(mat);
```
- `/br_target BEDROCK` → whitelist 通過，輸出 = 基岩
- `/br_target BEDROCK AIR` → whitelist 檢查 "BEDROCK" 通過，但輸出 = AIR
- 學員需反編譯發現第二參數的存在

---

### Teleport

| 漏洞 | 觸發 | 反編譯線索 |
|------|------|-----------|
| **Vuln 1**: Y-axis 突破 | `/tp2 100 200 64`（三個空格分隔的參數） | `args.length == 3` 分支 |
| **Vuln 2**: Integer Overflow | `/tp2 65536 0`（65536² overflow 成 0 ≤ 9） | `int distSq = dx*dx + dz*dz` |

**Vuln 2 數學**: `maxDistance=3` → `maxDistSq=9`。`dx=65536` → `dx² = 4,294,967,296` 超出 `Integer.MAX_VALUE`，wrap 成 0。`0 ≤ 9` → 檢查通過。

**走三格回來**: 傳到 65536 格外後，需走回 3 格內才能再用 `/tp2` 傳送。

---

## 地圖整合

- **迷宮**（Block Replacer）：學員需反編譯找到 `/br_y` 和 `/br_target` 才能打通黑曜石牆
- **跑酷**（Teleport）：正常 3 格傳送不夠，需用 Vuln 1 或 Vuln 2 才能到終點

---

## License

MIT — for educational use at NTUST CSIE Camp.
