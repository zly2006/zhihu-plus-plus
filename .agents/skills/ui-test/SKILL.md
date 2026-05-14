---
name: ui-test
description: Zhihu++ LLM 自动化 UI 测试。使用 testTag 系统精准定位 Compose 元素并交互，替代硬编码坐标的 adb tap。提供已知 tag 列表、文字内容点击、截图验证等能力。适用于：功能验证、UI 回归测试、自动化交互流程。
license: CC BY-NC-SA 4.0
---

# UI 自动化测试 Skill

## 脚本入口

所有操作通过以下脚本完成（在项目根目录执行）：

```bash
python3 .github/skills/ui-test/llm_test_helper.py <command> [options]
```

---

## LLM 操作决策树

**每次需要点击元素时，按以下顺序判断如何构造命令：**

```
需要点击某个元素
│
├─ 该 tag 在当前界面是否唯一？
│   ├─ 是（如导航 tab）→ 直接用 --tag
│   │     python3 ... tap --tag nav_tab_hotlist
│   │
│   └─ 否（如 feed_card_more_btn 会出现多次）→ 需要消歧
│         │
│         ├─ 知道目标卡片的标题/关键词？→ 用 --tag + --within-text
│         │     python3 ... tap --tag feed_card_more_btn --within-text "ChatGPT"
│         │
│         └─ 不知道具体内容，只知道位置？→ 用 --tag + --index
│               python3 ... tap --tag feed_card_more_btn --index 2
│                                                              ↑ 屏幕从上到下第3个（0-based）
│
└─ 没有 testTag？→ 用 --text 按显示文字点击
      python3 ... tap --text "屏蔽用户"
      │
      ├─ 没有 text，但有 content-desc？→ 用 --desc
      │    python3 ... tap --desc "返回"
      │
      └─ text/desc/tag 都没有（无标识可点击节点）？→ 用 --text "" + --index
           python3 ... tap --text "" --index 19
```

> **重要**：`--within-text` 和 `--index` 可以组合使用。当同一卡片内有多个相同 tag 时，
> `--within-text` 先缩小范围到目标卡片，`--index` 再从该范围内选第 N 个。

---

## 完整命令参考

### `dump` — 查看当前界面关键信息元素（可点击 + 不可点击）

```bash
python3 .github/skills/ui-test/llm_test_helper.py dump
```

输出示例（带序号，LLM 可用序号决定 `--index`）：
```
关键信息元素（18 个，按屏幕顺序；含可点击与不可点击）:

  [ 0] [C] 搜索 | 搜索内容                                         [42,86][891,212]
  [ 1] [C] 某条 Feed 卡片内容                                       [42,273][1038,775]
  [ 2] [C] 更多选项                                               [924,661][1050,787]
  [ 3] [N] 更多选项                                               [966,703][1008,745]
  [ 4] [C] 主页                                                  [0,2127][200,2274]
  [ 5] [C] 账号                                                  [881,2127][1080,2274]
  ...
```

`[C]` = 可点击，`[N]` = 不可点击。优先点击 `[C]` 元素；`[N]` 主要用于理解上下文和做 `--within-text` 消歧。
`label` 为脚本聚合后的可读信息，不再保证固定显示 `tag:/text:/desc:` 前缀。

**弹窗模式**：检测到弹窗时，`dump` 只输出弹窗内容并自动过滤无用节点（避免被背景页面干扰）。

**LLM 使用规则**：执行任何 tap 前，先运行 `dump` 确认目标元素存在及其序号。

---

### `find` — 查找元素坐标（不点击）

```bash
# 列出所有 feed_card 的坐标和序号
python3 .github/skills/ui-test/llm_test_helper.py find --tag feed_card

# 找包含"AI"的卡片内的 more_btn 坐标
python3 .github/skills/ui-test/llm_test_helper.py find --tag feed_card_more_btn --within-text "AI"
```

> `--tag` 支持两种 resource-id 形式：`com.xxx:id/tag` 和裸 `tag`（如 `nav_tab_account`）。

---

### `tap` — 点击元素

#### 场景 1：唯一元素（导航 tab、全局按钮）

```bash
python3 .github/skills/ui-test/llm_test_helper.py tap --tag nav_tab_home
python3 .github/skills/ui-test/llm_test_helper.py tap --tag nav_tab_hotlist
python3 .github/skills/ui-test/llm_test_helper.py tap --tag nav_tab_account
```

#### 场景 2：按内容定位（知道目标卡片的关键词）

```bash
# 点击标题含"ChatGPT"的卡片本体
python3 .github/skills/ui-test/llm_test_helper.py tap --tag feed_card --within-text "ChatGPT"

# 点击标题含"ChatGPT"的卡片的更多按钮
python3 .github/skills/ui-test/llm_test_helper.py tap --tag feed_card_more_btn --within-text "ChatGPT"
```

#### 场景 3：按位置定位（知道是第几个）

```bash
# 点击屏幕上从上到下第 1 个卡片（0-based）
python3 .github/skills/ui-test/llm_test_helper.py tap --tag feed_card --index 0

# 点击第 2 个卡片的更多按钮
python3 .github/skills/ui-test/llm_test_helper.py tap --tag feed_card_more_btn --index 1
```

#### 场景 4：组合使用（最精确，内容 + 位置双重限定）

```bash
# 如果某个话题下有多张卡片都含"AI"，指定取其中第 2 个的更多按钮
python3 .github/skills/ui-test/llm_test_helper.py tap --tag feed_card_more_btn --within-text "AI" --index 1
```

#### 场景 5：按显示文字点击（无 testTag 的动态元素）

```bash
python3 .github/skills/ui-test/llm_test_helper.py tap --text "屏蔽用户"
python3 .github/skills/ui-test/llm_test_helper.py tap --text "取消"
python3 .github/skills/ui-test/llm_test_helper.py tap --text "登录"
```

#### 场景 6：按 contentDescription 点击（无文本但有无障碍描述）

```bash
python3 .github/skills/ui-test/llm_test_helper.py tap --desc "返回"
```

> 注意：若 `--desc` 匹配多个节点，脚本当前不会对 `desc` 提供 `--index` 消歧。
> 这种情况下请优先改用 `--tag`；如果没有可用 tag，请先 `dump` 后用 `--text "" --index N`。

#### 场景 7：点击无标识可点击节点（text/desc/tag 全为空）

```bash
# 1) 先 dump，确认目标在当前页面中的相对位置
python3 .github/skills/ui-test/llm_test_helper.py dump

# 2) 再用空文本 + index 点击（index 基于当前页面 text="" 节点顺序）
python3 .github/skills/ui-test/llm_test_helper.py tap --text "" --index 19
```

> 适用场景：颜色面板色块、部分开关、部分“更多”按钮容器。

---

### `screenshot` — 截图

```bash
python3 .github/skills/ui-test/llm_test_helper.py screenshot /tmp/result.png
```

截图后必须用 `view` 工具查看，不要在同一 response 中同时发出截图和 view 命令（截图需要时间）。

---

## 标准测试流程模板

```bash
# 1. 启动应用
adb shell am force-stop com.github.zly2006.zhplus.lite
adb shell monkey -p com.github.zly2006.zhplus.lite -c android.intent.category.LAUNCHER 1
sleep 10

# 2. 确认界面元素
python3 .github/skills/ui-test/llm_test_helper.py dump

# 3. 交互（根据 dump 结果选择命令）
python3 .github/skills/ui-test/llm_test_helper.py tap --tag nav_tab_hotlist

# 4. 截图验证
python3 .github/skills/ui-test/llm_test_helper.py screenshot /tmp/after_tap.png
# （新 response 中）view /tmp/after_tap.png

# 5. 滚动（无 tag 时）
adb shell input swipe 540 1200 540 400 500   # 上滑
adb shell input swipe 540 400 540 1200 500   # 下拉刷新
```

---

## 已知 testTag 列表

### 底部导航栏（全局唯一，直接用 --tag）

| testTag | 描述 |
|---------|------|
| `nav_tab_home` | 主页 |
| `nav_tab_follow` | 关注 |
| `nav_tab_hotlist` | 热榜（可选，默认可能不显示） |
| `nav_tab_daily` | 日报 |
| `nav_tab_onlinehistory` | 历史 |
| `nav_tab_account` | 账号 |

### Feed 卡片（每屏可能出现多个，必须配合 --within-text 或 --index）

| testTag | 描述 | 每屏数量 |
|---------|------|----------|
| `feed_card` | 卡片主体（点击进入详情） | 多个 |
| `feed_card_more_btn` | 卡片右上角更多菜单 | 多个，与 feed_card 一一对应 |

---

## 错误排查

| 错误前缀 | 含义 | 解决 |
|----------|------|------|
| `[NOT FOUND] tag='xxx'` | 元素不在当前界面 | 先 `dump` 确认元素存在 |
| `[NOT FOUND] tag='xxx'` | APK 未更新 | 重新构建并 `adb install -r` |
| `[NOT FOUND] tag='xxx' 在包含...内不存在` | `--within-text` 关键词不匹配 | 检查拼写，或改用 `--index` |
| `[AMBIGUOUS] tag='xxx' 匹配到 N 个` | **未消歧**，脚本拒绝静默选第一个 | 按错误输出中的列表，加 `--within-text` 或 `--index N` 重试 |
| `[AMBIGUOUS] desc='xxx' 匹配到 N 个` | `--desc` 出现歧义且不支持 `--index` | 改用 `--tag`，或改走 `--text "" --index N` |
| `[OUT OF RANGE] index=N` | `--index` 超出候选数量 | 先 `find --tag xxx` 确认数量，再选合法序号 |
| `uiautomator dump 失败` / `adb pull dump 失败` | ADB 异常或设备未连接 | 先执行 `adb devices`，确认设备状态为 `device` |

---

## 添加新 testTag

```kotlin
Modifier.testTag("my_new_tag")
```

`testTagsAsResourceId = true` 已在 `ZhihuMain.kt` 根 Scaffold 启用，加 tag 后立即生效，将新 tag 补充到上方"已知 testTag 列表"。
