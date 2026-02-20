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
```

> **重要**：`--within-text` 和 `--index` 可以组合使用。当同一卡片内有多个相同 tag 时，
> `--within-text` 先缩小范围到目标卡片，`--index` 再从该范围内选第 N 个。

---

## 完整命令参考

### `dump` — 查看当前界面所有可点击元素

```bash
python3 .github/skills/ui-test/llm_test_helper.py dump
```

输出示例（带序号，LLM 可用序号决定 `--index`）：
```
可点击元素（12 个，按屏幕顺序）:

  [ 0] tag:nav_tab_home                                       [0,1234][216,1344]
  [ 1] tag:nav_tab_hotlist                                    [216,1234][432,1344]
  [ 2] tag:feed_card                                          [0,80][1080,320]
  [ 3] tag:feed_card_more_btn                                 [1020,80][1080,140]
  [ 4] tag:feed_card                                          [0,320][1080,560]
  [ 5] tag:feed_card_more_btn                                 [1020,320][1080,380]
  ...
```

**LLM 使用规则**：执行任何 tap 前，先运行 `dump` 确认目标元素存在及其序号。

---

### `find` — 查找元素坐标（不点击）

```bash
# 列出所有 feed_card 的坐标和序号
python3 .github/skills/ui-test/llm_test_helper.py find --tag feed_card

# 找包含"AI"的卡片内的 more_btn 坐标
python3 .github/skills/ui-test/llm_test_helper.py find --tag feed_card_more_btn --within-text "AI"
```

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
| `nav_tab_hotlist` | 热榜 |
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
| `[OUT OF RANGE] index=N` | `--index` 超出候选数量 | 先 `find --tag xxx` 确认数量，再选合法序号 |

---

## 添加新 testTag

```kotlin
Modifier.testTag("my_new_tag")
```

`testTagsAsResourceId = true` 已在 `ZhihuMain.kt` 根 Scaffold 启用，加 tag 后立即生效，将新 tag 补充到上方"已知 testTag 列表"。
