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
3. UI 验证时如果启动后看到“下载官方App”“查看协议”“查看设置”这类官方 App/协议确认页，或进入知乎网页登录/安全验证页，不要当成普通业务 UI 问题；这表示当前 AVD 登录态缺失或失效。应先按 `.agents/skills/launch-on-device/SKILL.md` 的 Login JSON Backup and Restore 流程恢复/覆盖 `files/account.json`，确认已登录后再继续 UI 验证；不要反复卡在登录流程里。

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

例外：在 `codex/kmp-migration` worktree 执行本轮 KMP 迁移重构期间，只有当所有迁移工作全部完成、准备做最终整体 UI 验证时，才允许启动 `$ui-voyager` 和 `$picky-user` 两个 UI 检查 agent；中间的迁移切片只做必要的构建、编译、AVD 基础验证和页面 smoke test，不因每次移动 UI 代码就启动这两个 UI 检查 agent。这条例外只约束本次重构过程，最终合并 PR 时可以忽略。

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

- 迁移时必须保留现有 `skills`、`agents`/`AGENTS.md`/`.agents`、`.codex`、`.claude`、`.mcp.json`、文档、报告、脚本、Rust 配套服务、fastlane 等 AI/文档/配套服务文件；只能增量新增或移动到兼容位置，不能无故删减。例外：当前 worktree 中 `.codex/hooks.json` 明确应删除，不要再把它当作误删恢复。
- 不能无脑覆盖 demo1。复制或复用 demo1 内容前必须检查包名、namespace、applicationId、模块名、资源名和入口类，改成当前项目需要的 `com.github.zly2006.zhihu` / `com.github.zly2006.zhplus` 约定。
- 继续迁移前必须先查看并遵循 `docs/kmp-migration-status.md` 和 `docs/superpowers/plans/2026-05-21-kmp-migration-remaining.md`。这个 plan 是当前 KMP 迁移的执行索引：开始新切片前先确认已完成/未完成边界，执行时按 plan 勾选或补充最新状态，避免重复迁移同一模块、重复试错，或把已经纠正的平台边界改回去。
- 遇到计划/文档没有明确说明的新文件、新类、新模块或新依赖，迁移前必须先启动一个独立 subagent 做 shared 边界审查；subagent 必须使用 `gpt-5.5` 且 reasoning effort 为 `xhigh`。审查输入要包含目标路径、import、调用方、被调用方、预期迁移目标、相关 Gradle 依赖、source set、当前副作用和验证命令；输出必须判断语义所有权（shared / Android-only / JVM-only / 需拆分）、哪些依赖应 shared、哪些必须留平台、是否存在 KMP 变体或跨平台替代、哪些函数只是平台副作用、是否涉及生命周期/线程/持久化/序列化/数据库/网络/导航/主题状态、是否能直接 `git mv`、最小迁移步骤、测试迁移方式和 boundary grep。主 agent 必须结合当前代码和编译结果复核，不能把 subagent 结论当免检结论。如果环境无法启动 subagent，必须在回复和相关文档中记录，并按同一清单本地审查。
- 强制性 subagent 生命周期规则：只要启动、接手或在环境/摘要中看到任何 subagent，主 agent 就必须把它们视为本轮责任并等待所有仍存活的 subagent 工作完成。subagent 是前台阻塞任务，不是可遗留后台任务；只要任何 subagent 还活着，主 agent 就不能自行决策、不能修改代码或文档、不能提交、不能宣布完成、不能发送最终回复，也不能继续推进任何实现或迁移工作。等待完成并消费结论是唯一默认路径，且优先级高于并行推进、节省时间、本地已有判断、继续实现或提交切片；不能因为本地判断似乎足够、等待较久、想继续推进、或认为结果大概率不重要而绕过。不得让 subagent 成为孤儿 agent。`wait_agent` 超时、返回空状态、或没有明确 `completed` 结论时，一律按该 subagent 仍存活处理，必须继续等待、补充输入或调整任务；不能转为本地主观判断，也不能一边等一边做会影响结论的本地工作。任一 subagent 仍存活时，主 agent 不能把等待中的审查结果当作可忽略项。主动关闭不是常规退路；只有用户明确取消、任务已作废、或该 subagent 的结论经书面说明已不再可能影响当前工作时，才能主动关闭，并必须在回复或相关审查文档中记录关闭原因。每次准备开始执行、修改文件、提交、宣布完成、停止当前回合或发送最终回复前，都必须盘点本回合和接手上下文中的 subagent：仍存活的必须 `wait_agent` 到完成，不能静默遗留。
- 收到上下文摘要或环境列表里的既有 subagent 时，必须把它们当成本轮责任的一部分先收尾；不能因为不是自己刚刚启动的 agent 就跳过等待。
- `gpt-5.5 xhigh` 边界审查得出结论后，必须输出一份新的独立审查文档，默认放在 `docs/superpowers/reviews/` 或对应任务文档目录，记录输入、结论、证据、风险和验证命令。现有 plan、AGENTS、CLAUDE、status 文档只在发现错误、过期、冲突或遗漏会误导后续执行时，才做必要的最小修改，并在新审查文档里说明修改原因；不要把每次审查结论直接塞进长期规则或状态文档。
- 对两个以上互不重叠的迁移 lane，默认优先并行推进：能用 subagent 时按文件所有权拆分给 subagent；不能用 subagent 时也要按 lane 批量审查、批量验证，避免串行地反复读同一批文件。并行 subagent 只是缩短等待时间，不是后台托管机制；一旦启动或接手，就必须按上面的生命周期规则等待全部完成并消费结论，不能留下孤儿 agent。若并行要求和生命周期等待冲突，必须先等待并收尾所有仍存活的 subagent。
- `fd313cd refactor: 收紧 shared 平台边界` 的根本教训必须记住：当时的根本误解是把“代码当前放在 Android app、由 Android UI 调用、或 import 名字看起来像 androidx”误当成“代码所有权属于 Android”。KMP 迁移判断所有权应看语义和副作用，不看当前位置：route/model/helper、导航语义、状态模型、URL/JSON 映射、过滤/打开记录算法属于 shared；`Context`、`Intent`、WebView、APK/update/install、Toast/Dialog/Activity、文件路径、系统通知才属于平台适配。另一个根本误解是把“某个调用点需要平台能力”扩大成“整份文件不能跨平台”，正确做法是先 `git mv` 保留主体，再只拆实际触碰平台 API 的函数。第三个根本误解是没有先查 KMP 变体或实际编译验证，例如 `org.jetbrains.androidx.navigation:navigation-compose` 有 KMP 变体，不能因包名或 Android 既有用法就判死刑。已经纠正的错误包括把 `NavDestination`、`FeedNavigation`、`ArticleState`、`CommentRoutes`、`ContentOpenEventSupport`、`CommentItem` 下沉到 app。
- `shared/src/androidMain` 不得保留任何完整 UI 页面、完整 Compose screen、整页导航壳或整页 `expect/actual` 实现；现有整页 `expect/actual` 只是迁移期债务，必须全部推进到 `shared/commonMain`。`expect/actual` 只允许用于小粒度平台能力，例如 Context/Intent/WebView/Activity、Toast/Dialog/通知、文件选择、系统打开链接、数据库 builder/文件路径、偏好存储、动态色/系统深色、TTS engine 等具体副作用；不能用来包住整个 UI、整个 Screen、整个导航图或完整页面运行时。Android source set 只能提供这些最小 adapter/provider/slot，不能承载完整 UI 代码。
- 导航语义和导航壳应共享：`NavDestination`/route sealed model、`ZhihuMain.kt`、`LocalNavigator.kt`、`AnswerNavigator.kt` 这类页面和内容目的地语义及主导航 UI 壳，目标都应迁入 `shared/commonMain`，Android 和 desktop 复用同一套模型与 UI 结构。优先使用 `org.jetbrains.androidx.navigation:navigation-compose` 的 KMP 变体；当前项目已在 Android 侧使用 `org.jetbrains.androidx.navigation:navigation-compose:2.9.2`，继续迁移时应先把它加到 shared/commonMain 并验证 `:shared:compileKotlinJvm` / `:desktopApp:compileKotlin`。不能迁入 shared 的是 Android `Context`、`Intent`、WebView、APK/lite/full 发行语义、平台回调和经验证不支持跨平台的代码片段；这些只能拆成 Android/desktop 薄适配。现阶段 desktop 不做独立适配，优先还原 Android UI 和导航语义。
- `ZhihuMain` 必须参考 `master` 保持大函数结构：shared `ZhihuMain` 自己负责 `NavHost` 和全部 `composable<...>` route 注册。不要把 route graph 拆成 `androidZhihuMainRouteContent`、screen registry、大注入表或整页 platform slot；Android/desktop 只注入 Activity/ViewModel 创建、偏好读取、WebView/Toast/Dialog 等平台副作用。
- `ThemeManager` / `ZhihuTheme` 的目标也应是 shared，而不是因为当前放在 app 或依赖 Android preference/system dark/dynamic color 就整体判为 Android-only。本次误判的根本原因是又把“状态读取的当前平台来源”误当成“主题状态所有权”：主题模式、自定义色、深浅色判断、Material color scheme 和 UI 主题壳属于跨平台 UI 状态；只有 SharedPreferences 读写、Android dynamic color、系统深色模式探测等是平台 adapter。迁移 `ZhihuMain` 时不得把 `ThemeManager.isDarkTheme()` 这类主题状态临时塞进 unrelated preference snapshot 来绕开依赖；应先抽 shared theme core，再由 Android/JVM 注入平台环境和持久化。
- `ArticleHost`、`ArticleAnswerSwitchState`、`TtsState` 这类文章页运行语义也应优先进入 `shared/commonMain`，不能加 `Android` 前缀或放在 `androidMain` 来表达“当前只有 Android 实现”。本次误判的根本原因是把“desktop 暂时没有 TTS/预览 WebView 实现”误当成“文章 TTS 状态和回答切换 host 是 Android-only”，并且只看了一个缺能力的平台，没有同时检查 Android/iOS 等其他目标的真实能力；实际 Android 和 iOS 都有 TTS 能力，TTS 状态、朗读请求、回答切换方向、pending 内容、历史/openFrom/复制目标语义属于 shared。只有 Android `Context`、TextToSpeech engine、WebView cache、clipboard 写入、Activity/history 具体落地等是平台 adapter；desktop 暂缺能力应表现为薄 adapter 空能力或禁用状态，不能把 shared 状态模型退回 Android。
- `PaginationViewModel` 的目标是迁入 `shared/commonMain`，不是新建一次性 loader 替代它。迁移时先把 Android 副作用拆成小的跨平台接口/回调和平台 adapter，例如认证过期处理、Toast/Dialog/clipboard、Activity 导航、signed fetch/http client/provider、偏好读取等；保留 ViewModel 的分页状态、刷新/加载流程、JSON 解码和 feed/list 状态语义，然后用 `git mv` 移入 shared。不得再引入 `ZhihuPageLoader` 这类绕开现有 ViewModel 的临时抽象。
- `ContentFilterManager` 和 `ContentFilterExtensions` 是本应用的关键过滤器与核心功能，目标也必须迁入 `shared/commonMain`，不能因为当前位于 app、依赖 Android `Context`/SharedPreferences/Room builder/Log/Toast 或由 Android feed ViewModel 调用就判为 Android-only。正确边界是：过滤编排、曝光/交互记录语义、前台已读过滤、广告/付费/关键词/用户/主题过滤策略、统计与清理规则、`FilterableContent` 映射、去重和保存屏蔽记录算法属于 shared；Android 只保留数据库 builder/文件路径、偏好存取 adapter、Toast/Dialog/log、平台生命周期和必要的内容详情 fetch/provider。迁移相关设置页或 feed ViewModel 时，优先拆这些小 adapter，再用 `git mv` 保留 `ContentFilterManager`/`ContentFilterExtensions` 主体进入 shared，不得用空实现或绕开过滤逻辑。
- 内容过滤设置页平台能力拆分的根本教训：不要为了让页面先进入 common，把偏好设置存储、内容过滤统计/维护、Toast/消息提示三种职责塞进一个页面专用混合 adapter。根本错误是把“页面需要几个平台能力”误解成“这些能力可以合并并按页面命名”，这会掩盖真实 shared 边界，并诱导重复实现 `ContentFilterManager` 的统计/清理逻辑。正确拆法是三块通用能力：`SettingsStore` 只负责设置读写；`ContentFilterMaintenance` 只负责过滤统计、清理、重置，后续应委托迁入 shared 的过滤 manager/数据库能力；`UserMessageSink` 只负责 Toast/Dialog/通知等平台提示。页面只能依赖这些通用最小接口，不得把过滤核心逻辑沉进页面 adapter。
- 新拆出的通用能力必须主动复用：每次抽出 `SettingsStore`、`UserMessageSink`、`ContentFilterMaintenance` 或类似 adapter 后，必须立刻 grep 同类 `Toast.makeText`、`getSharedPreferences`、统计/清理/维护动作和页面级回调，优先替换低风险调用点；不能只服务当前页面就停止。暂不替换的调用点要有明确原因，例如页面仍整体停留 Android source set、依赖 Activity/WebView/variant 能力、或会扩大当前迁移切片。
- 迁移代码时默认先尝试 `git mv`/`mv` 整体移动现有文件或目录，保留原实现、历史和测试结构，然后只做必要的小幅包名、import、source set、依赖和平台边界修改，以最快路径让代码通过编译。不要先手工重写、复制粘贴重建已有代码，或把简单移动问题做成大重构。只有在原文件混入平台副作用、需要拆分纯逻辑和平台适配，或直接移动会破坏模块边界时，才拆文件并记录原因。
- 迁移后的代码必须尽量保持和 `master` 分支里 `git mv` 之前的原函数一致：主体结构和关键函数名称必须 100% 相似，关键函数相似度必须达到 80% 以上（只变化空格的行不计入差异）。开始迁移、抽取或提交前必须对照 `master` 原文件检查；不满足该要求的迁移切片必须返工重做，不能继续在错误抽象上叠加修改。
- 不得为了“迁入 common”或“减少重复”抽取无语义价值的微型 UI 组件。几个 `Text`、单个标签、单个字段展示、一次性 wrapper 等应优先保留在原函数原位置内联；只有能保留原函数主体、表达真实业务/页面结构、减少有意义平台边界或复用既有 shared 语义时，才允许抽组件。`ArticleIpInfoText` / `ArticleMetaTexts` 这类只包装少量文本的抽象是错误示例，必须撤回或返工。
- iOS 目标可以保留在工程结构中，但本次不执行任何 iOS 相关构建、测试、调试或发布任务。
- Android 必须使用 AVD 验证，不使用真机；lite 包名仍为 `com.github.zly2006.zhplus.lite`。
- JVM/desktop 端不能依赖或引入任何 WebView 相关实现。需要扫码登录时，先用 `terminal-notifier -message "需要扫码登录 JVM 端" -sound default` 提醒用户；登录成功后必须备份 cookie，避免重复要求登录。
- `desktopApp` 必须保持和 `/Users/zhaoliyan/IdeaProjects/demo1` 一样的薄入口结构：`desktopApp/src/main/kotlin/.../Main.kt` 只负责启动 Compose/app 壳和注入平台能力，不承载 QR 登录、cookie 备份、业务导航、网络请求或主要 UI 状态机；这些跨平台逻辑应放在 `shared` 中，由 Android/JVM 平台层提供最小必要适配。
- Android 端可以在 AVD 正常登录；覆盖 cookie 后必须能执行现有操作，且 UI 不应发生非预期变化。
- JVM 端必须可以扫码登录；覆盖 cookie 后必须能执行现有操作。桌面端 UI 自适配不属于本次范围，但核心 UI 应与 Android 保持一致。
- `sentence_embeddings` 只需要在 Android/full variant 提供；JVM/desktop 不要求接入。`SentenceSimilarityTestScreen` 依赖 full/lite 变体能力：full 变体有真实模型测试页面，lite 变体有自己的 fallback 页面，不能迁入 `shared/commonMain` 做占位或空实现。shared 只能通过最小平台 slot 调用当前 Android 变体提供的页面。
- 迁移中遇到不支持跨平台的库时，优先查找该库的 KMP 变种或兼容实现；没有合适 KMP 变种时，再评估其他跨平台替代库。只有在没有可靠替代或迁移风险过高时，才把依赖和实现留在对应平台 source set，并记录原因。
- Room 支持 Kotlin Multiplatform，不能因为 Room 当前在 Android 侧就直接判定为平台独有。迁移数据库时必须优先按官方 KMP Room 方案实现真实跨平台 Room：实体、DAO、Database 放入 `shared`，使用 Room KMP 的 common schema/constructor 配置，平台 source set 只提供 database builder、文件路径和 driver 等必要适配。参考官方文档：https://developer.android.com/kotlin/multiplatform/room?hl=zh-cn 。只有确认某个数据库能力无法可靠跨平台或迁移风险过高时，才暂留平台侧，并记录原因。
- APK、`lite`/`full` variant、安装包下载/选择/安装、Android `Context`、WebView、Android intent/file provider 等平台运行时或发行语义不得迁入 `shared`；需要共享时只能抽取纯数据模型、纯算法或跨平台 UI/导航语义，平台适配、资源选择和副作用留在对应平台模块。
- 编译耗时较长时，尽量在完成一个大任务后再构建验证；每完成一个大任务，在本迁移分支里及时提交。
- 阶段性迁移写得差不多，且 `./gradlew assembleLiteDebug` 和 `./gradlew :desktopApp:compileKotlin` 都通过后，可以在本迁移分支里及时提交一次。
- 本迁移仍需遵守 `$superpowers:using-superpowers` 和 `$superpowers:using-git-worktrees` 的流程要求。
- 每完成一个功能点，且编译通过，要及时commit。


# 迁移code review关键中的关键：保持一致，方便PR检查

- 迁移后的代码必须尽量保持和 `master` 分支里 `git mv` 之前的原函数一致：主体结构和关键函数名称必须 100% 相似，关键函数相似度必须达到 80% 以上（只变化空格的行不计入差异）。开始迁移、抽取或提交前必须对照 `master` 原文件检查；不满足该要求的迁移切片必须返工重做，不能继续在错误抽象上叠加修改。
