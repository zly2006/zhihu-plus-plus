# KMP 剩余迁移计划

> 本计划只记录迁移顺序、边界判断、验证门禁和已知错误教训。不要在计划里写死具体 Kotlin 实现代码、临时类名、伪代码模板或“照着抄”的实现片段；具体实现必须以当前代码为准，先审计、再 `git mv`/小幅修改、再编译验证。

## 总目标

把当前 Android-only Zhihu++ 迁移为 `/Users/zhaoliyan/IdeaProjects/demo1` 风格的 KMP 项目：

- Android 保持现有 UI 和行为，lite 包名仍为 `com.github.zly2006.zhplus.lite`。
- desktop/JVM 复用 shared UI、导航语义、QR 登录、账号持久化和核心业务逻辑。
- `desktopApp` 保持浅入口，只启动 Compose/app 壳和注入平台能力。
- iOS target 保留，但本次不运行 iOS build/test/debug。
- 每个完成的大切片在验证后及时 commit。

## 通用边界审查规则

在迁移任何未在本计划明确说明的新文件、新类、新模块或新依赖前，必须先开一个独立 subagent 做边界审查：

- subagent 配置：`gpt-5.5`，reasoning effort `xhigh`。
- 输入必须包含：目标文件路径、当前 import、调用方、被调用方、预期迁移目标、相关 Gradle 依赖、source set、当前副作用、预期验证命令。
- subagent 必须回答：
  - 该文件/类的语义所有权是 shared、Android-only、JVM-only，还是需要拆分。
  - 哪些依赖应迁到 shared，哪些必须留在平台 source set。
  - 是否存在 KMP 变体或跨平台替代库；没有查证前不能按包名猜。
  - 哪些函数只是平台副作用，应该拆成 adapter/接口/expect-actual。
  - 是否涉及生命周期、线程调度、持久化、序列化、数据库、网络、导航或主题状态，哪些属于 shared 状态，哪些属于平台环境。
  - 直接 `git mv` 是否可行；如果不可行，阻碍点是什么。
  - 最小迁移步骤、测试迁移方式和必须运行的编译/grep 验证。
- xhigh 审查完成后必须生成新的独立审查文档，默认放在 `docs/superpowers/reviews/` 或对应任务目录，记录输入、结论、证据、风险和验证命令。
- 现有 plan、AGENTS、CLAUDE、status 文档只在发现错误、过期、冲突或遗漏会误导后续执行时，才做必要的最小修改；不要把每次审查结论都直接塞回长期规则文档。
- 主 agent 不能把 subagent 结论当成免检结论；必须结合当前代码和编译结果复核。
- 强制性 subagent 生命周期规则：只要启动、接手或在环境/摘要中看到任何 subagent，主 agent 必须等待所有仍存活的 subagent 工作完成。只要任何 subagent 还活着，主 agent 不能自行决策、不能提交、不能宣布切片完成、不能发送最终回复，也不能继续推进任何实现或迁移工作。等待完成并消费结论是唯一默认路径，不能因为本地已有判断、等待较久、想继续推进、或认为结果大概率不重要而绕过。不能留下孤儿 subagent。每次准备提交、宣布完成、停止当前回合或发送最终回复前，都必须盘点本回合和接手上下文中的 subagent：仍存活的必须 `wait_agent` 到完成，不能静默遗留。若 subagent 超时但仍存活，应继续等待、补充输入或调整任务，不能直接转为本地主观判断。主动关闭不是常规退路；只有用户明确取消、任务已作废、或该 subagent 的结论经书面说明已不再可能影响当前工作时，才能主动关闭 subagent，并必须在回复或相关审查文档中记录关闭原因。
- 并行 subagent 只能用来缩短互不重叠 lane 的等待时间，不能把任何 agent 当作可遗留后台任务；启动或接手后必须等待全部完成并消费结论，之后才能进入提交、最终回复或下一步实现/迁移。
- 如果当前环境不能启动 subagent，必须在当前回复和相关文档中明确记录，并改为本地按同一清单审查。

这个规则的目的，是防止再次把“当前文件位置、调用方、import 包名”误当成平台所有权。

## 所有权判断原则

属于 shared 的通常是：

- route/model/helper、导航语义、主导航 UI 壳。
- Compose UI 结构、跨平台页面壳、主题状态、展示模型。
- URL/JSON 映射、纯数据模型、序列化、分页状态、列表状态。
- 过滤、去重、内容打开记录、推荐评分、排序、纯策略。
- KMP 可用的数据库 entity/DAO/database 定义。
- KMP 可用的网络 client、解析器、签名算法、QR 登录状态机。

属于平台 adapter 的通常是：

- Android `Context`、`Intent`、Activity、Toast/Dialog、clipboard、FileProvider。
- WebView、APK/update/install、lite/full 发行语义。
- SharedPreferences 文件路径、JVM 文件路径、系统通知、terminal-notifier。
- Android dynamic color、系统深色模式探测、平台持久化。
- 任何经编译/文档验证确实无 KMP 支持的库调用点。

如果一个文件同时包含 shared 语义和平台副作用，默认先 `git mv` 保留主体，再只拆实际平台副作用。不要因为一个函数需要平台能力，就把整份文件判为平台独有。

## 已知错误教训

- `fd313cd refactor: 收紧 shared 平台边界` 的根本误解：把“代码当前放在 Android app、由 Android UI 调用、或 import 名字看起来像 androidx”误当成“代码所有权属于 Android”。已纠正的方向：`NavDestination`、`FeedNavigation`、`ArticleState`、`CommentRoutes`、`ContentOpenEventSupport`、`CommentItem` 等语义应在 shared。
- `ZhihuPageLoader` 的根本错误：把“迁移分页 ViewModel”误解成“绕开现有 ViewModel 新建 loader”。正确方向是拆 Android 副作用，让 `PaginationViewModel` 本体迁入 shared。
- `ThemeManager` / `ZhihuTheme` 的根本误判：把“主题状态当前从 Android preference/system dark 读取”误当成“主题状态属于 Android”。正确方向是主题模式、自定义色、深浅色状态、Material 主题壳进 shared；平台只提供持久化、系统深色探测和 dynamic color adapter。
- `ContentFilterManager` / `ContentFilterExtensions` 的根本边界：它们是核心过滤器，不是 Android-only 工具类。不能把“当前从 Android `Context` 取 SharedPreferences/Room/Toast/log”误判成“过滤所有权属于 Android”。正确方向是过滤编排、曝光/交互记录、前台已读过滤、广告/付费/关键词/用户/主题过滤策略、统计清理、去重和屏蔽记录保存算法进 shared；平台只提供数据库 builder/文件路径、偏好存取、Toast/Dialog/log、平台生命周期和内容详情 fetch/provider。
- 内容过滤设置页混合 adapter 的根本错误：为了让设置页先编译到 common，把偏好读写、过滤统计/清理/重置、Toast/消息提示塞进一个页面专用聚合对象，等于把三种不同所有权和变化频率的能力糊成“页面平台适配”，还按页面命名，妨碍后续复用。正确方向是拆成三块通用能力：`SettingsStore` 只负责设置存取；`ContentFilterMaintenance` 只负责统计/清理/重置，并在 `ContentFilterManager` 迁入 shared 后委托 shared 核心；`UserMessageSink` 只负责平台提示。不要在设置页 adapter 里复制或替代过滤核心逻辑。
- 通用 adapter 抽出后不复用也是错误：每次新增或拆出 shared 能力后，必须同步 grep 同类调用并替换低风险重复点，例如 `Toast.makeText` -> `UserMessageSink`、`getSharedPreferences` -> `SettingsStore`、过滤统计/清理 -> `ContentFilterMaintenance`。若暂不替换，必须在当前切片说明原因，不能默认让重复平台调用继续扩散。
- `ZhihuMainScreens`/大注入表方向错误：不要重写 `ZhihuMain`；优先保留 Android UI 结构，用小 platform slot/adapter 拆具体平台副作用。
- `androidZhihuMainRouteContent`/Android route graph adapter 方向错误：不要把 route 注册从 `ZhihuMain` 大函数里拆到 app。应参考 `master` 的写法，`ZhihuMain` 本体在 shared 内直接负责 `NavHost` 和所有 `composable<...>` 路由注册；Android/desktop 只注入具体页面实现、Activity/ViewModel 创建、偏好读取、Toast/Dialog/WebView 等平台副作用。
- 整页 `expect/actual` 方向错误：当前任务的主线是把所有 Compose UI 页面从 Android source set 推进 `shared/commonMain`，而不是在 `commonMain` 声明整页 `expect`、再让 Android/JVM 各自实现。页面级 `expect` 只能作为极短期编译桥，必须在后续切片中优先删除；长期允许的 `expect/actual` 只能是最小平台能力，例如偏好读写、数据库 builder、系统打开链接、Toast/通知、Activity/WebView/文件选择等具体副作用。
- `SentenceSimilarityTestScreen` 的根本边界：它不是普通可共享页面，而是 full/lite 变体页面。full 变体承载 `sentence_embeddings` 真实模型测试，lite 变体提供 fallback；不能迁入 `shared/commonMain` 做占位实现。shared 只能保留一个最小平台 slot，由 Android app 的当前 variant 注入真实页面。
- `.codex/hooks.json` 在当前 worktree 中是有意删除，不要恢复。

## 当前完成状态

- KMP 骨架已存在：`shared`、`desktopApp`、Android `app`、Android-only `sentence_embeddings`。
- `SentenceSimilarityTestScreen` 已确认保留为 Android full/lite 变体页面：`app/src/full/...` 是真实模型测试，`app/src/lite/...` 是 fallback；shared 只通过 platform slot 调用。
- `desktopApp` 已是浅入口，但 desktop 仍未复用完整 Android UI。
- QR 登录核心和 QR UI 已在 shared；Android 风控 WebView 留在 app。
- JVM QR 登录使用 shared 流程并通过 `DesktopAccountStore` 备份 cookie。
- KMP Room 已用于内容过滤和本地内容数据库。
- `NavDestination`、`LocalNavigator.kt`、`AnswerNavigator.kt` 已迁回 shared；`AnswerNavigator` 的 Android 数据访问通过 app adapter 留在平台侧。
- `ZhihuMain.kt` 主导航壳已迁入 shared；但 route 注册仍需按 `master` 的大函数结构继续收回 shared。Android 只应保留具体页面实现、偏好读取、`MainActivity`、ViewModel 创建和其他平台副作用 adapter。
- `ThemeManager` / `ZhihuTheme` 的主题状态和 Material3 主题壳已迁入 shared；Android 持久化、system dark、dynamic color 和 system bar 副作用留在 androidMain adapter。
- bottom navigation preference 规则已在 shared；Android 偏好页和 `ZhihuMain` adapter 复用该规则。
- 账号 session JSON 持久化核心已在 shared；Android/JVM 是文件路径 adapter。
- Feed 展示映射已通过 `Feed.toDisplayItem` 共享。
- 通用分页状态已使用 shared `ZhihuPaging`；`PaginationViewModel` 本体仍待迁移。
- `DailyScreen`、`CollectionScreen`、`NotificationScreen`、`OpenSourceLicensesScreen` 页面主体已迁入 `shared/commonMain`；Android 只保留必要 data/runtime adapter。
- `ContentFilterManager` / `ContentFilterExtensions` 仍在 app 或 Android 调用链中，但目标必须迁入 shared；迁移 ContentFilter 设置页、feed ViewModel 或 PaginationViewModel 时不得弱化、绕开或空实现过滤功能。

## 剩余任务顺序

### 任务 1：收敛文档和状态台账

目标：

- `AGENTS.md`、`CLAUDE.md`、`docs/kmp-migration-status.md`、本计划保持一致。
- 文档只保留边界、流程、状态和验证要求，不写死实现代码。
- 迁移前必须先读状态台账和本计划，避免重复做或把已纠正边界改回去。

验证：

```bash
rg -n "KMP|shared|ThemeManager|PaginationViewModel|subagent|gpt-5.5|ZhihuPageLoader" AGENTS.md CLAUDE.md docs/kmp-migration-status.md docs/superpowers/plans/2026-05-21-kmp-migration-remaining.md
git diff --check
```

### 任务 2：完成 shared 主导航壳

目标：

- `ZhihuMain.kt` 本体位于 `shared/commonMain`。
- shared `ZhihuMain` 继续保持 `master` 的大函数形状：同一个函数内负责底栏、pager、`NavHost` 和全部 `composable<...>` route 注册。
- UI 页面主体必须进入 `shared/commonMain`。不能把整页 screen 当成平台实现注入，也不能长期保留整页 `expect/actual`；Android/desktop 只提供最小平台能力 adapter，如 Activity、ViewModelProvider、WebView、Toast/Dialog、偏好读写和文件/Intent 副作用。例外是真实 variant-owned 页面，例如 `SentenceSimilarityTestScreen`：full/lite 变体代码本身就是发行能力边界，shared 只保留最小调用 slot。
- shared 主导航壳保留 Android 当前 UI 结构：底栏、pager、MainTabs、route 注册语义、导航动画语义。
- 不引入独立 desktop route model。
- 不用大而全 screen table 重写 Android UI。
- 当前临时整页 entrypoint `expect` 是迁移债务，消除优先级高于继续新增页面级 `expect`。每迁移一个页面，应先尝试 `git mv` 到 `commonMain`，再把实际平台触点拆成小 adapter/provider。

迁移前审查：

```bash
rg -n "android\\.|Context|Intent|WebView|FileProvider|APK|lite|full|MainActivity|Toast|AlertDialog|ViewModelProvider|LocalActivity|LocalContext" shared/src/commonMain/kotlin app/src/main/java/com/github/zly2006/zhihu/ui -g '*.kt'
rg -n "navigation-compose|androidx.navigation|org.jetbrains.androidx.navigation" build.gradle.kts app/build.gradle.kts shared/build.gradle.kts desktopApp/build.gradle.kts
```

验证：

```bash
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin
rg -n "android\\.|Context|Intent|WebView|FileProvider|APK|lite|full|MainActivity|Toast|AlertDialog|ViewModelProvider|LocalActivity|LocalContext" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui shared/src/commonMain/kotlin/com/github/zly2006/zhihu/navigation -g '*.kt'
```

### 任务 3：迁移 shared theme core

状态：已完成当前切片。

目标：

- `ThemeManager` / `ZhihuTheme` 的跨平台状态和 Compose 主题壳进入 shared。
- Android 只保留 SharedPreferences 持久化、system dark 探测、dynamic color adapter。
- JVM/desktop 能使用同一主题状态模型和默认主题。
- `ZhihuMain` 不通过 preference snapshot 临时携带主题状态。

迁移前必须由 subagent 审查：

- `app/src/main/java/com/github/zly2006/zhihu/theme/*`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/theme/*`
- 所有 `ThemeManager`、`ZhihuTheme` 调用方。

验证：

```bash
rg -n "ThemeManager|ZhihuTheme|dynamic|isSystemInDarkTheme|SharedPreferences|Context" app/src/main/java shared/src/commonMain/kotlin shared/src/androidMain/kotlin shared/src/jvmMain/kotlin -g '*.kt'
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin
```

### 任务 4：迁移 PaginationViewModel 本体

状态：

- 2026-05-21：`PaginationViewModel.kt` 本体已通过 `git mv` 进入 `shared/commonMain`，并改为依赖 common `PaginationEnvironment`。`./gradlew :shared:jvmTest :desktopApp:compileKotlin` 已通过。
- 2026-05-21：`HistoryViewModel`、`OnlineHistoryViewModel` 和 `OnlineHistoryScreen` 已改为使用 `shared/androidMain` 的 `HistoryStorage`，去掉对 `MainActivity.history` 的直接依赖。
- 剩余：feed/comment/list 子类仍在 Android source set，Android 调用链仍通过临时 `Context -> PaginationEnvironment` 适配；需要继续拆 `ContentFilterExtensions`、history repository、notification preferences、comment HTML/request helper、collection export 等平台副作用后再迁子类。
- 注意：`:app:compileLiteDebugKotlin` 当前仍因既有 `shared/androidMain` 迁移债失败，包括 `AccountSettingScreen`、`ArticleScreen`、`WebviewComp`、`ContentFilterExtensions`、`MainActivity`/`BuildConfig`/`R` 访问等；不能把这个 slice 说成 Android 编译通过。

目标：

- `PaginationViewModel.kt` 本体进入 `shared/commonMain`。
- 保留分页状态、刷新/加载更多流程、items/rawData、JSON decoding、processResponse 语义。
- Android 副作用拆到小 adapter：signed fetch、login expired/risk control、Toast/Dialog/clipboard、Activity 导航、偏好读取。
- 不创建 `ZhihuPageLoader` 或类似绕开 ViewModel 的临时 loader。

迁移前必须由 subagent 审查：

- `PaginationViewModel.kt`
- feed/list subclasses
- `AccountData.fetchGet`、`signFetchRequest`、登录过期处理、clipboard/dialog 调用方。

验证：

```bash
rg -n "android\\.|Context|MainActivity|LoginActivity|Toast|AlertDialog|clipboard|SharedPreferences|PREFERENCE_NAME|signFetchRequest|AccountData" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared -g '*.kt'
rg -n "ZhihuPageLoader" .
./gradlew :shared:jvmTest :desktopApp:compileKotlin :app:compileLiteDebugKotlin :app:testLiteDebugUnitTest
```

### 任务 5：迁移核心内容过滤器

状态：

- 2026-05-21：`ContentFilterExtensions.kt` 已从 app 移入 `shared/androidMain`，解除 shared feed ViewModel 对 app 源集的反向引用。NLP 语义 matcher 通过 `AndroidContentFilterRuntime.semanticMatcher` 注入，`MainActivity` 启动时接入现有 full/lite `NlpServiceKeywordSemanticMatcher`，没有把 Android variant NLP 实现迁入 shared。
- 剩余：`ContentFilterExtensions` 仍是 Android wrapper；继续把 `applyContentFilterToDisplayItems` 的详情补齐、关键词/NLP/作者/主题编排和消息/日志回调拆进 `shared/commonMain`，Android 只保留 `Context`、settings/db builder、`ContentDetailCache`、Toast/log 和 variant NLP 注入。

目标：

- `ContentFilterManager` 和 `ContentFilterExtensions` 主体迁入 `shared/commonMain`。
- 保留关键过滤功能：曝光/交互记录、前台已读过滤、重复曝光过滤、广告/知乎学堂/微信公众号/付费内容过滤、关键词/用户/主题过滤、reverseBlock、统计、清理和屏蔽记录保存。
- Android 副作用拆成小 adapter：SharedPreferences/设置读取、Room database builder 和文件路径、Toast/Dialog/log、`Context`、内容详情 fetch/provider、平台生命周期维护触发。
- `ContentFilterSettingsScreen` 必须依赖三块通用能力：`SettingsStore`、`ContentFilterMaintenance`、`UserMessageSink`；后续迁移核心过滤器时，逐步让过滤维护动作委托 shared 的 `ContentFilterManager`/KMP Room，而不是继续在页面 adapter 里扩写逻辑。
- 复用要求：迁移本任务涉及页面、feed ViewModel 或过滤器时，必须先 grep `Toast.makeText`、`getSharedPreferences`、`ContentFilterMaintenance`/统计清理相关重复实现，优先接入已存在的 `UserMessageSink`、`SettingsStore`、`ContentFilterMaintenance`。对仍保留在 Android-only 页面或 ViewModel 中的重复点，要在切片记录中说明为什么暂不替换。
- 不允许把过滤器替换成空实现、只保留 UI 开关，或让 desktop/JVM 绕开过滤语义。

迁移前必须由 subagent 审查：

- `ContentFilterManager.kt`
- `ContentFilterExtensions.kt`
- `ContentFilterSettingsScreen.kt`
- feed viewmodels 中所有过滤调用点。
- `ContentFilterDatabase`、DAO/entity、`BlockedFeedRecord`、`ContentOpenEvent`、`BlocklistManager` 的 shared 边界。

验证：

```bash
rg -n "android\\.|Context|SharedPreferences|Toast|Log\\.|MainActivity|AccountData|getContentDetail" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared -g '*.kt'
rg -n "ContentFilterManager|ContentFilterExtensions" app/src/main/java shared/src/androidMain/kotlin shared/src/commonMain/kotlin -g '*.kt'
./gradlew :shared:jvmTest :desktopApp:compileKotlin :app:compileLiteDebugKotlin :app:testLiteDebugUnitTest
```

### 任务 6：继续拆 AccountData

目标：

- shared 拥有账号 session、cookie、用户基础信息、JSON 持久化结构。
- Android `AccountData` 逐步变成 Android client/provider adapter。
- token refresh、签名、HTTP client 配置能被 shared/JVM 复用，平台只注入必要能力。

迁移前必须由 subagent 审查：

- `AccountData.kt`
- `ZhihuCredentialRefresher`
- shared account/session/client 文件
- desktop account store

验证：

```bash
rg -n "Context|SharedPreferences|File\\(|filesDir|android\\." shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/account shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/data -g '*.kt'
./gradlew :shared:jvmTest :desktopApp:compileKotlin :app:compileLiteDebugKotlin
```

### 任务 7：把 desktop 接入 shared 主 UI

目标：

- `desktopApp/src/main/kotlin/.../Main.kt` 仍保持浅入口。
- QR 登录、cookie、网络、主 UI 状态在 shared/JVM shared adapter 中。
- desktop 复用 Android route model 和 shared 主 UI 壳；不做本次范围外的独立桌面适配。
- JVM/desktop 不引入 WebView。

验证：

```bash
rg -n "WebView|android\\.webkit|androidx\\.webkit" desktopApp/src shared/src
rg -n "DesktopQrLoginScreen|SharedQrLoginPane|ZhihuMain|NavDestination" desktopApp/src shared/src -g '*.kt'
./gradlew :desktopApp:compileKotlin :shared:compileKotlinJvm
```

### 任务 8：本地推荐编排迁移

目标：

- 排序、评分、去重、候选合并等纯逻辑进入 shared。
- Android database/network/Context/Toast/Dialog 留在 adapter。
- 每次移动纯 helper 前先加 shared test 或迁移现有测试。

迁移前必须由 subagent 审查：

- `LocalRecommendationEngine`
- `CrawlingExecutor`
- `TaskScheduler`
- `UserBehaviorAnalyzer`
- `FeedGenerator`
- local Room database/DAO/entity

验证：

```bash
rg -n "android\\.|Context|ConnectivityManager|Toast|AlertDialog|AccountData" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/recommendation shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/local -g '*.kt'
./gradlew :shared:jvmTest :desktopApp:compileKotlin :app:compileLiteDebugKotlin
```

### 任务 9：HTML 解析迁移

目标：

- shared 中只使用 Ksoup 或已验证 KMP 的替代库。
- 先迁移纯文本/纯 HTML parsing。
- WebView document manipulation 不得在未证明跨平台等价前迁入 shared。

迁移前必须由 subagent 审查：

- 所有 `Jsoup` 调用点。
- markdown HTML 解析/导出路径。
- `ArticleScreen` 中 WebView 相关 HTML 操作。

验证：

```bash
rg -n "org\\.jsoup|Jsoup" shared/src/commonMain/kotlin
./gradlew :shared:jvmTest :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin
```

## 最终运行验证门禁

编译与边界：

```bash
./gradlew :shared:compileKotlinJvm :shared:jvmTest :desktopApp:compileKotlin assembleLiteDebug :app:testLiteDebugUnitTest
rg -n "WebView|android\\.webkit|androidx\\.webkit" desktopApp/src shared/src
rg -n "android\\.content|android\\.webkit|androidx\\.webkit|\\bIntent\\b|\\bContext\\b|FileProvider|APK|lite|full" shared/src/commonMain/kotlin shared/src/jvmMain/kotlin desktopApp/src -g '*.kt'
git diff --check
```

Android AVD：

```bash
grep "applicationId" app/build.gradle.kts
./gradlew assembleLiteDebug
adb install -r app/build/outputs/apk/lite/debug/app-lite-debug.apk
adb shell am force-stop com.github.zly2006.zhplus.lite
adb shell monkey -p com.github.zly2006.zhplus.lite -c android.intent.category.LAUNCHER 1
python3 .agents/skills/ui-test/llm_test_helper.py dump
```

JVM/desktop：

```bash
./gradlew :desktopApp:run
terminal-notifier -message "需要扫码登录 JVM 端" -sound default
```

通过标准：

- Android AVD 可登录；覆盖 cookie 后核心操作可用，UI 无非预期变化。
- JVM 可扫码登录；cookie 持久化到 `~/.zhihu-plus-plus/account.json`，后续不反复要求登录。
- desktop/shared 无 WebView。
- iOS target 存在，但本次没有运行 iOS 命令。
- 每个完成 lane 都有聚焦 commit。
