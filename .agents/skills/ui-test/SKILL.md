---
name: ui-test
description: Zhihu++ LLM 自动化 UI 测试。使用 testTag 系统精准定位 Compose 元素并交互，替代硬编码坐标的 adb tap。提供已知 tag 列表、文字内容点击、截图验证等能力。适用于：功能验证、UI 回归测试、自动化交互流程。
license: CC BY-NC-SA 4.0
---

# UI 自动化测试 Skill

## 执行环境优先级

- 调用本 skill 的 UI 自动化 agent 或 subagent 时，尽量使用 `gpt-5.4-mini`；复杂视觉或流程判断再使用 `gpt-5.4`，避免使用反应较慢的模型拖慢 AVD 交互。
- 若 `/Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/SKILL.md` 存在，必须优先按 `$off-android-avd-ci-debug` 使用远端 `off` AVD runner，不要默认启动本地 AVD。
- 远端 runner 只用于短生命周期 UI/AVD 验证：先运行 `status` / `boot-check` 确认健康；`boot-check` 会自行清理模拟器，真实 UI 交互要在 `off` 的远端 ADB 环境中启动短生命周期 AVD 后执行；验证结束后运行 `kill` 清理。
- 只有 `$off-android-avd-ci-debug` 不存在，或远端 runner 明确不可用时，才退回本地 AVD。

```bash
/Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/scripts/off-avd-ci-debug.sh status
/Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/scripts/off-avd-ci-debug.sh boot-check
/Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/scripts/off-avd-ci-debug.sh kill
```

## 失败经验

### 先判断布局占位是否破坏原有中心

在已有头部或居中布局里新增按钮时，必须先看新增控件是否占用了布局槽位并挤压原内容，而不是只评价图标本身好不好看。例子：个人页头部放在 `TopAppBar` title 区时，右侧 `actions` 会额外占据横向空间，导致头像、昵称、统计和操作按钮整体被压窄或偏离原来的视觉中心；这类问题的根因是新增按钮破坏了正常空间分配和居中关系，不是简单的“右上角是否能放搜索”。

### 参考图和实际截图必须逐项对齐

按参考图实现 UI 时，不能只验证自己理解的某个技术根因已经解决，还要把参考图和实际截图并排核对关键元素的位置、大小、间距和对齐关系。例子：搜索按钮即使已经不再挤压 `TopAppBar` 的 title 区，如果实际截图里的按钮比参考图明显更低、更靠内容中线，仍然没有照图实现；这类偏差必须继续调整，不能用“没有挤压居中”替代“位置一致”。

## 脚本入口

所有操作通过以下脚本完成（在项目根目录执行）：

```bash
python3 .agents/skills/ui-test/llm_test_helper.py <command> [options]
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
python3 .agents/skills/ui-test/llm_test_helper.py dump
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
python3 .agents/skills/ui-test/llm_test_helper.py find --tag feed_card

# 找包含"AI"的卡片内的 more_btn 坐标
python3 .agents/skills/ui-test/llm_test_helper.py find --tag feed_card_more_btn --within-text "AI"
```

> `--tag` 支持两种 resource-id 形式：`com.xxx:id/tag` 和裸 `tag`（如 `nav_tab_account`）。

---

### `tap` — 点击元素

#### 场景 1：唯一元素（导航 tab、全局按钮）

```bash
python3 .agents/skills/ui-test/llm_test_helper.py tap --tag nav_tab_home
python3 .agents/skills/ui-test/llm_test_helper.py tap --tag nav_tab_hotlist
python3 .agents/skills/ui-test/llm_test_helper.py tap --tag nav_tab_account
```

#### 场景 2：按内容定位（知道目标卡片的关键词）

```bash
# 点击标题含"ChatGPT"的卡片本体
python3 .agents/skills/ui-test/llm_test_helper.py tap --tag feed_card --within-text "ChatGPT"

# 点击标题含"ChatGPT"的卡片的更多按钮
python3 .agents/skills/ui-test/llm_test_helper.py tap --tag feed_card_more_btn --within-text "ChatGPT"
```

#### 场景 3：按位置定位（知道是第几个）

```bash
# 点击屏幕上从上到下第 1 个卡片（0-based）
python3 .agents/skills/ui-test/llm_test_helper.py tap --tag feed_card --index 0

# 点击第 2 个卡片的更多按钮
python3 .agents/skills/ui-test/llm_test_helper.py tap --tag feed_card_more_btn --index 1
```

#### 场景 4：组合使用（最精确，内容 + 位置双重限定）

```bash
# 如果某个话题下有多张卡片都含"AI"，指定取其中第 2 个的更多按钮
python3 .agents/skills/ui-test/llm_test_helper.py tap --tag feed_card_more_btn --within-text "AI" --index 1
```

#### 场景 5：按显示文字点击（无 testTag 的动态元素）

```bash
python3 .agents/skills/ui-test/llm_test_helper.py tap --text "屏蔽用户"
python3 .agents/skills/ui-test/llm_test_helper.py tap --text "取消"
python3 .agents/skills/ui-test/llm_test_helper.py tap --text "登录"
```

#### 场景 6：按 contentDescription 点击（无文本但有无障碍描述）

```bash
python3 .agents/skills/ui-test/llm_test_helper.py tap --desc "返回"
```

> 注意：若 `--desc` 匹配多个节点，脚本当前不会对 `desc` 提供 `--index` 消歧。
> 这种情况下请优先改用 `--tag`；如果没有可用 tag，请先 `dump` 后用 `--text "" --index N`。

#### 场景 7：点击无标识可点击节点（text/desc/tag 全为空）

```bash
# 1) 先 dump，确认目标在当前页面中的相对位置
python3 .agents/skills/ui-test/llm_test_helper.py dump

# 2) 再用空文本 + index 点击（index 基于当前页面 text="" 节点顺序）
python3 .agents/skills/ui-test/llm_test_helper.py tap --text "" --index 19
```

> 适用场景：颜色面板色块、部分开关、部分“更多”按钮容器。

---

### `screenshot` — 截图

```bash
python3 .agents/skills/ui-test/llm_test_helper.py screenshot /tmp/result.png
```

截图后必须用 `view` 工具查看，不要在同一 response 中同时发出截图和 view 命令（截图需要时间）。

---

## 标准测试流程模板

远端路径和本地回退路径必须分开执行。选择 `$off-android-avd-ci-debug` 时，后续 `adb` / `llm_test_helper.py` 都必须在能访问远端 emulator 的 `off` 环境中运行；裸 `adb` 只属于本地回退路径。如果远端 skill 当前只有 `status` / `boot-check` / `kill`，没有能保持 emulator 运行的交互入口，不能把 `boot-check` 后面接本机 `adb`；应先补远端交互脚本，或把远端 runner 明确标记为当前不可用后再走本地回退。

### 远端优先流程

```bash
# 1. 先检查 off runner 健康状态
/Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/scripts/off-avd-ci-debug.sh status
/Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/scripts/off-avd-ci-debug.sh boot-check

# 2. boot-check 会清理模拟器；真实 UI 交互必须在 off runner 的远端 ADB 环境执行。
# 不要在本机继续执行裸 adb。需要设备命令时，用 ssh off 进入同一套远端环境：
ssh off 'bash -lc '"'"'
BASE=/home/dom/android-ci
export JAVA_HOME="$BASE/java"
export ANDROID_HOME="$BASE/android-sdk"
export ANDROID_SDK_ROOT="$BASE/android-sdk"
export ANDROID_USER_HOME="$BASE/android-home"
export ANDROID_AVD_HOME="$BASE/avd"
export ANDROID_EMULATOR_HOME="$BASE/emulator-home"
export TMPDIR="$BASE/tmp"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
adb devices
'"'"''

# 3. 验证结束后清理
/Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/scripts/off-avd-ci-debug.sh kill
```

### 本地回退流程

仅当 `$off-android-avd-ci-debug` 不存在，或远端 runner 明确不可用时执行：

```bash
# 1. 启动本地 AVD
emulator -avd Medium_Phone_2

# 2. 启动应用
adb shell am force-stop com.github.zly2006.zhplus.lite
adb shell monkey -p com.github.zly2006.zhplus.lite -c android.intent.category.LAUNCHER 1
sleep 10

# 3. 确认界面元素
python3 .agents/skills/ui-test/llm_test_helper.py dump

# 4. 交互（根据 dump 结果选择命令）
python3 .agents/skills/ui-test/llm_test_helper.py tap --tag nav_tab_hotlist

# 5. 截图验证
python3 .agents/skills/ui-test/llm_test_helper.py screenshot /tmp/after_tap.png
# （新 response 中）view /tmp/after_tap.png

# 6. 滚动（无 tag 时）
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
