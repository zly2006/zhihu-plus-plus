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
5. ✅ 使用 ui-test 技能查看当前页面状态：`python3 .agents/skills/ui-test/llm_test_helper.py dump`
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
python3 .agents/skills/ui-review-memory/memory_store.py update-status \
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
- 不仅要进行上述所有检查，还要检查是否有代码重复片段，是否有未使用的变量或函数，是否有潜在的性能问题等
- 不仅要检查当前代码，还要把关键地方都grep一下，检查你写的代码是否和其他地方重复了，是否有类似的代码片段可以复用
- 在不降低注释质量的前提下，代码越短越好，避免过度设计和过度抽象

## ⏰重要提醒，在每次编写代码时必须遵守：
- 不得擅自简化代码实现，如果确实有的功能难以实现，停下来等待我的反馈，不要私自修改设计。
- 必须按照上述流程进行调试验证，尤其是 UI 相关的修改，不能跳过任何一步，确保你写的功能正常可用。
- 每次修改完代码后必须进行review，不能直接提交，必须等待我的反馈和批准后才能合并到主分支。

## Pull Requests

当我要求你发 PR 的时候，PR 的title必须以feat: /fix: /refactor: 开头，标题和内容必须用中文写。
提交PR前，先更新master与远程同步或领先，并确保当前分支基于master，而不包括其他feature branch的内容。
如果一开始给你的提示词包括了issue链接，并且此PR解决了这个issue，应该写上Resolves #issue_number在PR描述里，这样GitHub会自动关联并在PR合并时关闭这个issue。

## KMP 迁移工作约束

当前 `codex/kmp-migration` worktree 用于把现有 Zhihu++ Android 项目迁移到 `/Users/zhaoliyan/IdeaProjects/demo1` 风格的 Kotlin Multiplatform 项目。

- 迁移时必须保留现有 `skills`、`agents`/`AGENTS.md`/`.agents`、`.codex`、`.claude`、`.mcp.json`、文档、报告、脚本、Rust 配套服务、fastlane 等 AI/文档/配套服务文件；只能增量新增或移动到兼容位置，不能无故删减。
- 不能无脑覆盖 demo1。复制或复用 demo1 内容前必须检查包名、namespace、applicationId、模块名、资源名和入口类，改成当前项目需要的 `com.github.zly2006.zhihu` / `com.github.zly2006.zhplus` 约定。
- 迁移代码时默认先尝试 `git mv`/`mv` 整体移动现有文件或目录，保留原实现、历史和测试结构，然后只做必要的小幅包名、import、source set、依赖和平台边界修改，以最快路径让代码通过编译。不要先手工重写、复制粘贴重建已有代码，或把简单移动问题做成大重构。只有在原文件混入平台副作用、需要拆分纯逻辑和平台适配，或直接移动会破坏模块边界时，才拆文件并记录原因。
- iOS 目标可以保留在工程结构中，但本次不执行任何 iOS 相关构建、测试、调试或发布任务。
- Android 必须使用 AVD 验证，不使用真机；lite 包名仍为 `com.github.zly2006.zhplus.lite`。
- JVM/desktop 端不能依赖或引入任何 WebView 相关实现。需要扫码登录时，先用 `terminal-notifier -message "需要扫码登录 JVM 端" -sound default` 提醒用户；登录成功后必须备份 cookie，避免重复要求登录。
- `desktopApp` 必须保持和 `/Users/zhaoliyan/IdeaProjects/demo1` 一样的薄入口结构：`desktopApp/src/main/kotlin/.../Main.kt` 只负责启动 Compose/app 壳和注入平台能力，不承载 QR 登录、cookie 备份、业务导航、网络请求或主要 UI 状态机；这些跨平台逻辑应放在 `shared` 中，由 Android/JVM 平台层提供最小必要适配。
- Android 端可以在 AVD 正常登录；覆盖 cookie 后必须能执行现有操作，且 UI 不应发生非预期变化。
- JVM 端必须可以扫码登录；覆盖 cookie 后必须能执行现有操作。桌面端 UI 自适配不属于本次范围，但核心 UI 应与 Android 保持一致。
- `sentence_embeddings` 只需要在 Android/full variant 提供；JVM/desktop 不要求接入。
- 迁移中遇到不支持跨平台的库时，优先查找该库的 KMP 变种或兼容实现；没有合适 KMP 变种时，再评估其他跨平台替代库。只有在没有可靠替代或迁移风险过高时，才把依赖和实现留在对应平台 source set，并记录原因。
- Room 支持 Kotlin Multiplatform，不能因为 Room 当前在 Android 侧就直接判定为平台独有。迁移数据库时必须优先按官方 KMP Room 方案实现真实跨平台 Room：实体、DAO、Database 放入 `shared`，使用 Room KMP 的 common schema/constructor 配置，平台 source set 只提供 database builder、文件路径和 driver 等必要适配。参考官方文档：https://developer.android.com/kotlin/multiplatform/room?hl=zh-cn 。只有确认某个数据库能力无法可靠跨平台或迁移风险过高时，才暂留平台侧，并记录原因。
- APK、`lite`/`full` variant、安装包下载/选择/安装、Android `Context`、WebView、Android intent/file provider 等平台运行时或发行语义不得迁入 `shared`；需要共享时只能抽取纯数据模型或纯算法，平台适配、资源选择和副作用留在对应平台模块。
- 编译耗时较长时，尽量在完成一个大任务后再构建验证；每完成一个大任务，在本迁移分支里及时提交。
- 阶段性迁移写得差不多，且 `./gradlew assembleLiteDebug` 和 `./gradlew :desktopApp:compileKotlin` 都通过后，可以在本迁移分支里及时提交一次。
- 本迁移仍需遵守 `$superpowers:using-superpowers` 和 `$superpowers:using-git-worktrees` 的流程要求。
- 每完成一个功能点，且编译通过，要及时commit。
