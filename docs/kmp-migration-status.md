# KMP Migration Status

## Completed

- Project has `shared`, `desktopApp`, Android `app`, and Android-only `sentence_embeddings`.
- `desktopApp` is a shallow demo1-style launcher.
- QR login core and QR UI are shared via `SharedQrLoginPane`.
- Android login uses shared QR login; Android WebView stays in app.
- JVM login uses shared QR login and stores cookies under `~/.zhihu-plus-plus/account.json`.
- KMP Room is used for content filter and local content databases.
- 共享导航语义应由 `shared/commonMain` 拥有；`NavDestination`、`LocalNavigator.kt`、`AnswerNavigator.kt` 已迁回 shared。`AnswerNavigator` 的 Android 数据访问通过 `AndroidAnswerNavigatorRepository` 留在 Android 平台适配层。
- `ZhihuMain.kt` 主导航壳已通过 `git mv` 迁入 `shared/commonMain`；但 route 注册仍需按 `master` 的大函数形状收回 shared。Android 只应保留 `MainActivity`、偏好读取、ViewModel 创建和其他运行时副作用 adapter，不得保留完整页面实现。
- `ThemeManager` / `ZhihuTheme` 的主题状态和 Material3 主题壳已迁入 `shared/commonMain`；Android 持久化、system dark、dynamic color 和 system bar 副作用留在 `shared/androidMain` adapter。
- Bottom navigation preference keys and normalization rules are shared in `shared/commonMain`; Android preference screens and `ZhihuMain` adapters reuse that common rule set.
- Account session data and JSON persistence rules have a shared repository; Android and JVM desktop storage are thin file-path adapters over that repository.
- Feed display mapping is shared via `Feed.toDisplayItem`; Android feed view models only pass platform preferences into the shared mapper.
- `PaginationViewModel.kt` 本体已通过 `git mv` 迁入 `shared/commonMain`，当前 common API 依赖 `PaginationEnvironment` 而不是 Android `Context`。Android 侧仍有临时 `Context -> PaginationEnvironment` 薄适配，feed/comment/list 子类还在 Android source set；下一步继续把子类的状态和纯分页流程迁入 shared，并把登录过期 Dialog、Toast/clipboard、Activity 导航、偏好读取、history repository 等副作用压缩到平台 adapter。不得用 `ZhihuPageLoader` 替代本体迁移。
- `CollectionsViewModel` 和 `CollectionContentViewModel` 已迁入 `shared/commonMain`。收藏夹分页、展示状态、导出进度状态和 item 映射属于 shared；收藏夹信息 fetch、详情解析、HTML/ZIP 文件导出、Toast/log、Android 文件路径和 cacheDir 由 `SharedAndroidPaginationEnvironment` 实现为平台 adapter。`CollectionContentScreen` 当前仍在 `shared/androidMain`，后续应继续迁入 common 并删除整页 `expect/actual` 债务。
- `FollowScreen`、`HomeScreen`、`HotListScreen`、`PeopleScreen`、`QuestionScreen`、`SearchScreen` 当前已去掉对 app `MainActivity` / `LoginActivity` / `WebviewActivity` / app `TopLevelReselectAction` typealias 的直接依赖。`HomeScreen` 和这些页面主体仍多在 `shared/androidMain`，后续应继续拆 `Context`、settings、message、WebView、history/openFrom、feed display/filter environment 后迁入 common；本地推荐相关 Android 实现已先移入 `shared/androidMain` 作为临时桥，后续仍需拆 DB/client/connectivity/message/log environment 并迁 shared。
- `CommentScreen.kt` 和 `PinViewModel.kt` 已通过 `git mv` 进入 `shared/androidMain`，清掉 shared 对 app UI/ViewModel 的反向依赖；它们的状态机、评论/想法 UI 和 route/openFrom 语义仍应继续拆副作用后迁 common。
- `WebviewComp` 保持 Android-only WebView 实现留在 `shared/androidMain`，但已去掉 app `MainActivity` / `WebviewActivity` 类型依赖，改用 className 启动和最小 runtime navigation adapter；WebView 本体不得迁 common，URL 到 `NavDestination` 的语义后续可继续抽 shared policy。
- `DeveloperSettingsScreen` 已去掉 app `MainActivity` 直接依赖，改走 common `DeveloperRuntimeInfoProvider` / `DeveloperRuntimeInfo`。TTS 状态语义继续使用 common `TtsState`；Android `TextToSpeech` engine/runtime 细节由 `MainActivity` 平台 adapter 转成展示信息。
- `HistoryViewModel`、`OnlineHistoryViewModel` 和 `OnlineHistoryScreen` 的本地历史读取/清理已改为直接使用 `shared/androidMain` 的 `HistoryStorage`，不再 cast `MainActivity.history`。
- `ContentFilterManager` is in `shared/commonMain`. `ContentFilterExtensions` has moved from app into `shared/androidMain` as the current Android wrapper over shared filter pipelines; its remaining Android `Context`/preferences/Room/log/Toast/detail-fetch dependencies are adapter seams, not ownership. Continue moving the orchestration and strategy into `shared/commonMain` instead of replacing or bypassing filtering logic.
- `ContentFilterSettingsScreen` should not use a mixed page adapter for all platform work. Its common bridge is split into generic `SettingsStore`, `ContentFilterMaintenance`, and `UserMessageSink` capabilities; filter maintenance owns one shared KMP Room-backed implementation instead of copying Android DAO logic in platform actuals.
- Newly extracted generic adapters must be reused in the same migration slice where reasonable. Grep for duplicate `Toast.makeText`, `getSharedPreferences`, and filter maintenance logic; replace low-risk call sites with `UserMessageSink`, `SettingsStore`, and `ContentFilterMaintenance`, or record why a remaining Android-only call site is deliberately left alone.
- `DailyScreen`、`CollectionScreen`、`NotificationScreen`、`OpenSourceLicensesScreen` 页面主体已迁入 `shared/commonMain`；Android 仅保留必要 data/provider/runtime adapter。`SentenceSimilarityTestScreen` 不是普通 shared 页面，full/lite 变体实现必须留在 Android app variant。
- `HotListScreen`、`HotListViewModel`、`BaseFeedViewModel` 的共享分页/展示主体以及 `FeedCard`、`PaginatedList`、`FeedPullToRefresh`、`DraggableRefreshButton`、`AuthorBadge` 已迁入 `shared/commonMain`。Android 屏蔽用户/关键词/主题的详情抓取、Toast、Blocklist 写入保留为 `shared/androidMain` extension；Android/JVM 分别提供分页环境、variant capability、屏幕尺寸、官方徽章图标和 HTML 文本解析 adapter。desktop 登录后已改为调用 shared `HotListScreen`，不再使用单独的 JVM 热榜 UI。
- `FollowViewModel`、`FollowRecommendViewModel`、`RecentMomentsViewModel` 已迁入 `shared/commonMain`，关注页推荐/动态分页状态、recent moments 用户列表状态和 JSON 解码不再依赖 Android `Context`/`AccountData`/`Log`。`FollowScreen` UI 主体仍在 `shared/androidMain`，因为屏蔽用户确认 Dialog、Toast、Blocklist 侧效和部分刷新偏好读取还需要继续拆成 common 可用的小 adapter 后再整体迁入 common。
- `SearchViewModel` 已迁入 `shared/commonMain`，搜索分页状态、搜索结果解码和 `SearchResult -> Feed` 映射复用 common；搜索 query 编码改用 Ktor common `encodeURLParameter(spaceToPlus = true)`，不再依赖 Java `URLEncoder`。
- `HistoryViewModel`、`OnlineHistoryViewModel` 已迁入 `shared/commonMain`；本地历史列表读取通过 `PaginationEnvironment.localHistory()` adapter 提供。Android 仍由 `HistoryStorage(context)` 负责文件路径和持久化，JVM/desktop 暂返回空本地历史，不影响在线历史分页和 common 显示模型。
- `QuestionFeedViewModel` 已迁入 `shared/commonMain`，问题页回答分页、排序状态和 answer feed 显示映射复用 common；关注/取消关注问题的 POST/DELETE 副作用通过 `PaginationEnvironment.followQuestion()` 由 Android adapter 执行。
- `HomeFeedViewModel` 已迁入 `shared/commonMain`，首页 Web 推荐分页、前台/后台过滤编排、read/touch 上报触发、内容交互记录触发和展示列表合并逻辑复用 common。Android adapter 通过 `PaginationEnvironment` 继续承接实际 `ContentFilterExtensions` 调用、`lastread/touch` HTTP 请求、屏蔽/已读数据库写入和偏好读取；本地/Android/Mixed 首页推荐 ViewModel 改为实现 common `HomeFeedInteractionViewModel`，不再依赖 Android UI 文件里的接口。
- `shared/src/androidMain` 中现存任何完整 Compose UI 页面、整页导航壳或整页 `expect/actual` 都只是迁移债务，不是可接受终态。Android source set 只能保留小粒度平台 adapter/provider/slot；页面主体、screen 结构、导航图和跨平台 UI 运行语义必须继续迁入 `shared/commonMain`。
- Shared has feed data models, notification/daily/hot-list/read-history clients, display formatting, ZSE signing, and local recommendation scoring helpers.

## Do Not Redo

- 不得留下仍存活的 subagent 继续自行决策。只要本轮启动、接手上下文、摘要或环境中列出的任何 subagent 仍然存活，主 agent 必须继续 `wait_agent` 等待完成；subagent 是前台阻塞任务，不是可遗留后台任务。在此之前不能提交、不能宣布完成、不能发送最终回复、不能把本地判断当成最终结论，不能修改代码或文档，也不能继续推进任何实现或迁移工作。等待完成并消费结论是唯一默认路径，且优先级高于并行推进、节省时间、本地判断、继续实现或提交切片；不能因为本地判断似乎足够、等待较久、或认为待返回结论大概率不重要而绕过。`wait_agent` 超时、返回空状态、或没有明确 `completed` 结论时，一律按 subagent 仍存活处理，应继续等待、补充输入或调整任务，不能一边等待一边做会影响结论的本地工作。主动关闭不是常规退路；只有用户明确取消、任务已作废、或书面说明该结果已不再可能影响当前工作时才能关闭，并必须记录原因。
- Do not keep navigation semantics or the main navigation shell Android-only. Move shared route/destination semantics plus `ZhihuMain.kt`, `LocalNavigator.kt`, and `AnswerNavigator.kt` toward `shared/commonMain`; keep only Android runtime side effects (`Context`, `Intent`, WebView, APK/update/install semantics, platform-only callbacks) in app.
- Do not move `ZhihuMain` route registration into an Android-only helper such as `androidZhihuMainRouteContent`. Match `master`: the shared `ZhihuMain` big function owns `NavHost` and all `composable<...>` route registrations; platforms inject runtime side effects only, not whole page implementations.
- Do not keep complete UI code in `shared/src/androidMain`. Full screens, complete Compose page bodies, whole navigation shells, and page-level `expect/actual` implementations must be moved to `shared/commonMain`; `expect/actual` is only for small platform capabilities such as Context/Intent/WebView/Activity, Toast/Dialog/notifications, file pickers, link opening, database builders/file paths, settings persistence, dynamic color/system dark, and TTS engines.
- Use `org.jetbrains.androidx.navigation:navigation-compose` as the preferred KMP navigation runtime. The current Android module already depends on `org.jetbrains.androidx.navigation:navigation-compose:2.9.2`; continue by moving that dependency to shared/commonMain and validating JVM/desktop compilation before introducing any custom route adapter.
- Do not assume desktop needs a separate route model. Desktop should reuse the shared Android UI/navigation semantics for this migration; only introduce a thin runtime adapter if the current Navigation Compose dependency cannot compile for JVM/desktop.
- Do not recreate `Time.android.kt` / `Time.jvm.kt`; use `Clock.System`.
- Do not put APK/lite/full/update/install semantics into `shared`.
- Do not make `desktopApp` contain QR login, cookie persistence, networking, or main UI state.
- Do not recreate the removed `.codex/hooks.json`; that deletion is intentional for this worktree.
- Do not introduce `ZhihuPageLoader` or another one-off loader layer as a substitute for migrating `PaginationViewModel`. Split side effects into small cross-platform interfaces/adapters and move the ViewModel.
- Do not move theme mode/custom color/background color state back to app just because Android currently owns persistence or dynamic color. Only platform environment and side effects stay in adapters.
- Do not treat `ContentFilterManager` or `ContentFilterExtensions` as Android-only just because they currently read Android preferences, Room builders, or `Context`; move the filtering orchestration and strategy to shared and keep only platform access in adapters.
- Do not grow a page-specific content-filter-settings aggregate back into a mixed adapter. Keep `SettingsStore`, `ContentFilterMaintenance`, and `UserMessageSink` separate and generically named; page-level adapters must not become a replacement for the shared filtering manager.
- Do not leave newly extracted generic adapters unused outside their first page when adjacent low-risk duplicates exist.

## Remaining Work

- Continue shrinking the Android `ZhihuMain` adapter only where it removes real platform side effects; do not rewrite the shared main shell.
- Audit and eliminate all remaining complete UI bodies in `shared/src/androidMain`, including page-level `expect/actual` bridges; move UI bodies to `shared/commonMain` and leave only minimal platform capability adapters.
- Split account fetch/token refresh orchestration from Android `AccountData`.
- Continue migrating `PaginationViewModel` subclasses and Android-only pagination call sites into shared after splitting their remaining platform side effects into small adapters.
- Move `CollectionContentScreen` body into `shared/commonMain`; keep only export, article host, answer navigator repository, and Android back-handler/runtime pieces as small platform adapters.
- Continue migrating the remaining feed screens (`HomeScreen`、`FollowScreen`、`QuestionScreen`、`SearchScreen`、history screens) now that `BaseFeedViewModel` and shared feed card/list components are common; keep their Android block-list/content-detail side effects as small adapters until the filter pipeline is shared.
- Move `ContentFilterManager` and `ContentFilterExtensions` into shared after splitting preferences, database builder, logging/toast, and content-detail fetch side effects into adapters.
- Add a shared hot-list/home shell that Android and desktop can both invoke.
- Move local recommendation orchestration behind platform adapters.
- Move pure HTML/text parsing only after replacing Jsoup with Ksoup-compatible shared code.
- Run Android AVD and JVM QR login runtime validation before final completion.

## Latest Verification

- 2026-05-21：`./gradlew :shared:compileAndroidMain --continue` 通过。
- 2026-05-21：`./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin --continue` 通过。
- 2026-05-21：`./gradlew assembleLiteDebug` 通过。
- 2026-05-21：`./gradlew ktlintFormat` 已执行 app/main、desktop、shared/commonMain、shared/androidMain 格式化任务，但最终被 iOS 编译任务拦下；失败点仍是本迁移期已知的 iOS/common 不兼容项（`JvmInline`、`Integer`、`System`、`java`、`String.format` 等）。本阶段仍不执行 iOS 修复。
- 2026-05-22：HotList common UI 切片通过 `./gradlew assembleLiteDebug`。
- 2026-05-22：HotList common UI 切片通过 `./gradlew :shared:jvmTest :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin --continue`。
- 2026-05-22：HotList common UI 切片通过 `./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet :app:runKtlintFormatOverMainSourceSet :desktopApp:runKtlintFormatOverMainSourceSet`。总任务 `./gradlew ktlintFormat` 仍会继续触发并失败于既有 iOS 编译问题（同 2026-05-21 记录），本阶段不处理 iOS。
- 2026-05-22：Follow ViewModel common 切片通过 `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin --continue`，并通过 `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet`。当前 shell 默认 JDK 26 会触发 buildSrc Java/Kotlin target 不一致，需显式使用 JDK 25。
- 2026-05-22：Search ViewModel common 切片通过 `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin --continue`，并通过 `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet`。
- 2026-05-22：History ViewModel common 切片通过 `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin --continue`，并通过 `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet`。
- 2026-05-22：QuestionFeedViewModel common 切片通过 `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin --continue`，并通过 `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet`。
- 2026-05-22：HomeFeedViewModel common 切片通过 `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin --continue`，并通过 `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet`。
