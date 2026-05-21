# HotList Boundary Review

## 输入

- 目标文件：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/HotListScreen.kt`
- 相关文件：`HotListViewModel`、hot-list shared client、`MainActivity` usage、settings/showRefreshFab、top-level reselect、`BlockUserConfirmDialog`、`FeedCard`、`PaginatedList`
- 审查方式：独立 `gpt-5.5` / `xhigh` subagent 只读审查，主 agent 结合当前代码复核。

## 结论

`HotListScreen` / `HotListViewModel` 的目标所有权是 `shared/commonMain`，当前状态是“需拆分后迁移”，不是 Android-only。热榜列表、分页状态、刷新、展示 `FeedDisplayItem`、热榜 URL、JSON decode、去掉作者头像等属于跨平台业务和 UI 语义；Android 依赖主要来自 `Context`、Activity-scoped ViewModel、SharedPreferences、Toast/Dialog、Intent、blocklist、Room builder/file path 等副作用。

当前最小 blocker 是 `HotListScreen` import 了 app 模块 `MainActivity`，通过 `LocalActivity.current as MainActivity` 只把它当 `Context` 和 `ViewModelStoreOwner` 使用。这里不需要 Activity 专属能力，应该先改成 `LocalContext.current` 和 Compose `viewModel()`，清掉 shared 对 app 的反向依赖。

## 边界

应迁入或保持 shared 的能力：

- `Feed` / `HotListFeed` / `FeedDisplayItem`
- `zhihuHotListUrl` / `fetchHotListPage` / decode
- `PaginationViewModel` / `PaginationEnvironment`
- `LocalNavigator` / feed navigation
- Compose 页面主体、Material3、Ktor、Kotlinx serialization
- JetBrains lifecycle ViewModel Compose、JetBrains Navigation Compose、Coil3 Compose 公共层

必须平台化或拆 adapter 的能力：

- Android `Context`、`LocalActivity`、`androidx.activity.viewModels`、`MainActivity`
- SharedPreferences 直接读写、Toast/Dialog、Intent/Uri external open
- `packageName.endsWith(".lite")` variant 判断
- `BlocklistManager.getInstance(context)`、`ContentDetailCache.getOrFetch(context, ...)`
- Android-only HTML parser/theme helper
- Room builder/file path

已有 KMP 或跨平台方向：

- `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose`
- `org.jetbrains.androidx.navigation:navigation-compose`
- Ktor、Room KMP、Coil3 Compose
- HTML/text parser 后续优先 Ksoup

## 最小步骤

1. 先小改 `HotListScreen`，去掉 `MainActivity` / `LocalActivity` / `androidx.activity.viewModels`，换成 `LocalContext.current` + `viewModel<HotListViewModel>()`。
2. 后续把 `HotListViewModel` 的 `Context` 需求从 `BaseFeedViewModel` 的 display/filter/blocking 环境中拆出。
3. 逐步迁移 `PaginatedList`、热榜页面主体、`FeedCard` 纯 UI；打开外链、消息、settings、variant、blocklist 留小 adapter。
4. 最后删除 `HotListScreen` 整页 `expect/actual`，让 JVM actual 不再是 placeholder。

## 风险

- 直接整体 `git mv` 到 commonMain 仍会被 `Context`、`FeedPullToRefresh`、`FeedCard`、`DraggableRefreshButton`、`BlockUserConfirmDialog`、`BaseFeedViewModel` 的 Android 副作用阻塞。
- 使用 Compose `viewModel()` 可能改变与旧 instrumentation test 中 Activity-scoped `ViewModelProvider(activity)` 的实例 owner 对齐方式；若测试暴露问题，应加最小 owner adapter，不恢复 `MainActivity` 类型依赖。

## 验证命令

```bash
rg -n "MainActivity|LocalActivity|androidx.activity.viewModels|context.viewModels" shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/HotListScreen.kt
rg -n "HotListScreen|HotListViewModel|zhihuHotListUrl|fetchHotListPage|TopLevelReselectAction" shared/src app/src -g '*.kt'
./gradlew :shared:compileAndroidMain --continue
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin --continue
git diff --check
```
