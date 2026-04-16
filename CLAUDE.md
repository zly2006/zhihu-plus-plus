# Zhihu++ Agent Instructions

本项目是隐私增强的知乎 Android 客户端，支持本地推荐算法、广告屏蔽、内容过滤。

## 构建与测试

```bash
# 验证修改（必须按顺序执行）
./gradlew assembleLiteDebug  # 构建 lite 变体
./gradlew ktlintFormat        # 格式化代码
```

**重要**: 修改后必须先构建验证，再格式化，最后提交。

## 项目结构

- **app**: 主应用（Jetpack Compose UI）
    - `src/main`: 共享代码
    - `src/full`: Full variant（含 NLP）
    - `src/lite`: Lite variant（轻量级）
- **Module**: `sentence_embeddings`（Rust tokenizer，仅 full variant）

### Build Variants
- **lite**: 轻量版 (~4MB)，无 ML 功能，包名 `com.github.zly2006.zhplus.lite`
- **full**: 完整版，含 HanLP NLP，包名 `com.github.zly2006.zhplus`

## 关键约定

### 数据序列化
- **DataHolder** 和 data classes 使用 `camelCase`
- **知乎 API** 返回 `snake_case`
- **自动转换**: `AccountData.fetch*()` 和 `decodeJson()` 内部自动调用 `snake_case2camelCase()`
- 不要手动转换或在 data class 中使用 snake_case

### HTTP 客户端
- 使用 `AccountData.httpClient(context)` 获取配置好的客户端
- Web API 需要 `signFetchRequest(context)` 用于 zse96 v2 签名
- Android API 使用 `AccountData.ANDROID_HEADERS` 和 `ANDROID_USER_AGENT`

### Compose
- Material 3 组件
- 用 `LaunchedEffect` 处理副作用，设置正确的 key
- 用 `collectAsState()` 观察 Flow/StateFlow

### git worktree

新开worktree的时候，记得把 local.properties 复制过去，避免构建问题.

### 导航
- 使用 Jetpack Navigation Compose
- 定义 sealed interface `NavDestination` 表示不同页面，包含 route 和参数
- 在编写导航代码前必须检查 NavDestination.kt

## Android 调试标准流程

注意：
1. 必须使用avd验证，不要使用真机。
2. 时刻注意你是一个LLM，延迟很高。所以大多数情况下不需要你执行sleep指令，你本身的反应就很慢，足够程序响应了。这也是说，如果需要执行双击等复杂手势，必须用&&来串联多个adb指令，不然你的反应太慢就不是双击了。

### 应用启动与验证
```bash
# 1. 检查包名（必须先做）
grep "applicationId" app/build.gradle.kts
# lite variant: com.github.zly2006.zhplus.lite

# 2. 启动模拟器（如果还没启动）
emulator -avd Medium_Phone_2

# 3. 构建并安装
./gradlew assembleLiteDebug
adb install -r app/build/outputs/apk/lite/debug/app-lite-debug.apk

# 4. 启动
adb shell am force-stop com.github.zly2006.zhplus.lite
adb shell monkey -p com.github.zly2006.zhplus.lite -c android.intent.category.LAUNCHER 1
```

### UI 调试强制清单
修改 UI 代码后**必须**：
1. ✅ 构建 + 格式化
2. ✅ 安装到设备
3. ✅ 正确启动应用（检查包名！）
4. ✅ 等待加载完成（至少 8-10 秒）
5. ✅ 使用 ui-test 技能查看当前页面状态：`python3 .github/skills/ui-test/llm_test_helper.py dump`
6. ✅ 先 `dump` 再 `tap`，优先通过 `--tag/--text/--desc` 交互，不使用硬编码坐标 tap
7. ✅ 若目标是无标识可点击节点，使用 `--text "" --index N`（N 来自当前页面 dump）
8. ✅ 交互后再次 `dump` 或截图验证状态
9. ✅ 仅在无 tag/文字可用且必须手势操作时，才使用 `adb shell input swipe` 等手势
10. ❌ 异常时检查 logcat：`adb logcat | grep -i error`

### UI 双代理复检

只要修改内容涉及 Compose、布局、样式、导航、交互、可见文案或任何用户可见 UI，主 agent 在完成上面的基础验证后，**必须**再执行以下流程：

1. 必须启动两个 subagent skill，不能由主 agent 自己扮演：
   - `$ui-voyager`（UI漫游者）：系统性探索目标页面，把能点的尽量都点一遍，把上下左右的滑动都试一遍，重点找空白页、越界、裁切、重叠、错位、状态切换异常。
   - `$picky-user`（挑剔的用户）：分别扮演新用户和老用户，对 self explain、明确性、直觉性、效率、布局和操作习惯提出高标准意见。
2. 两个 skill 都必须先读取自己的持久化记忆：
   - `.memory/YYYY-MM-DD/picky-user/`
   - `.memory/YYYY-MM-DD/ui-volayor/`
3. 两个 skill 都允许在 `ui-test` 之外结合截图做视觉判断，但交互仍优先走 `ui-test` 的 `dump` / `tap` / `screenshot` 工作流。
4. `ui-voyager` 遇到拿不准的地方，必须把复现步骤和犹豫原因交给 `$picky-user` 或主 agent，请其再判断，不要含糊带过。
5. 主 agent 只有在以下条件满足后，才能停止工作、宣布 UI 修改完成，或请求我做下一步决策：
   - `$ui-voyager` 没有新的有效问题；
   - `$picky-user` 没有新的有效意见；
   - 或者它们提出的意见都已经被修复，或被明确标记为无效/驳回并留下充分理由。
6. 主 agent 对每条意见都必须写回 memory，至少标记为 `fixed`、`rejected` 或 `invalid`，不能口头略过。

记忆回写命令示例：

```bash
TODAY=$(date +%F)
python3 .github/skills/ui-review-memory/memory_store.py update-status \
  --agent picky-user \
  --date "$TODAY" \
  --id PU-20260417-001 \
  --status fixed \
  --note "已修复并复测通过。"
```

`update-status` 会按 `id` 自动定位历史记录，所以 issue 即使不是今天创建的，也必须继续回写，而不是新建另一个编号。

## 代码风格
- Kotlin Serialization with `@Serializable`
- 只在必要时注释，不过度注释
- ktlint 格式化（14.0.1）

## Code Review
- 每次修改后，必须进行代码 review，等待批准后才能 commit
- 不仅要进行上述所有检查，还要检查是否有代码重复片段，是否有未使用的变量或函数，是否有潜在的性能问题等
- 不仅要检查当前代码，还要把关键地方都grep一下，检查你写的代码是否和其他地方重复了，是否有类似的代码片段可以复用
- 在不降低注释质量的前提下，代码越短越好，避免过度设计和过度抽象

## ⏰重要提醒，在每次编写代码时必须遵守：
- 不得擅自简化代码实现，如果确实有的功能难以实现，停下来等待我的反馈，不要私自修改设计。
- 必须按照上述流程进行调试验证，尤其是 UI 相关的修改，不能跳过任何一步，确保你写的功能正常可用。
- 每次修改完代码后必须进行review，不能直接提交，必须等待我的反馈和批准后才能合并到主分支。
