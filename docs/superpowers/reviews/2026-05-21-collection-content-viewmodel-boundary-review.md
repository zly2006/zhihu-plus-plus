# Collection Content ViewModel Boundary Review

日期：2026-05-21

## 输入

- `app/src/main/java/com/github/zly2006/zhihu/viewmodel/CollectionsViewModel.kt`
- `app/src/main/java/com/github/zly2006/zhihu/viewmodel/CollectionContentViewModel.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/CollectionContentScreen.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/CollectionScreenData.android.kt`
- 当前 `:shared:compileAndroidMain` 首个阻塞：`CollectionContentViewModel` / `CollectionsViewModel` unresolved。

## 结论

`CollectionsViewModel` 语义属于 `shared/commonMain`，可直接 `git mv`。它只依赖 `Collection`、URL 拼接和 common `PaginationViewModel`，没有 Android API。

`CollectionContentViewModel` 主体也属于 `shared/commonMain`，但不能无拆分直接移动。分页状态、收藏夹标题、`displayItems`、`exportDialogState`、`initialUrl`、`processResponse`、`createDisplayItem`、paging progress 判断属于 shared；`Context`、`Toast`、Android log、`AccountData.fetchGet` 当前落点、`ContentDetailCache`、HTML/ZIP 文件导出、外部文件目录和 cacheDir、Android signed fetch 是平台 adapter。

`CollectionContentScreen` 页面主体应迁入 `shared/commonMain`，当前整页 `expect/actual` 是迁移债。平台层只应提供导出、文章 host、answer navigator repository、必要 back handler 等最小能力。

## 所有权

Shared：

- `CollectionsViewModel`
- `CollectionContentViewModel` 的分页与展示状态主体
- `Collection`、`CollectionItem`、`CollectionHtmlExportDialogState`、`FeedDisplayItem`
- 收藏夹 item 到 `FeedDisplayItem` / nav destination JSON 的映射
- 收藏夹内容页面结构和常规 Compose UI

平台 adapter：

- Android `Context` / `LocalContext`
- Toast、Android `Log`
- `AccountData.fetchGet` / `httpClient` / `decodeJson` 当前 Android 落点
- `signFetchRequest`
- `ContentDetailCache`
- `exportCollectionItemsToZip`、文件路径、cacheDir、outputDir
- `AndroidAnswerNavigatorRepository`
- `articleHost()` 和 Android WebView 预览能力
- Android `BackHandler`
- 当前 `YMDHMS + java.util.Date` 格式化点，后续应优先换成 kotlinx datetime 或 shared 时间格式化

## 最小步骤

1. 先 `git mv CollectionsViewModel.kt` 到 `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/`。
2. 再迁 `CollectionContentViewModel.kt`，移动后拆出 collection info fetch、导出 service、detail provider、message sink，保留 common 状态机。
3. 将 `CollectionContentScreen.kt` 迁入 common，删除整页 `expect/actual`，只保留最小平台能力注入。
4. `CollectionScreenData.android.kt` 继续作为 Android provider，但导入 common `CollectionsViewModel`。
5. `CollectionHtmlExportUtils` 暂留 androidMain，直到文件系统与 ZIP 写入有明确 KMP 抽象。

## 验证

当前已执行：

- `CollectionsViewModel.kt` 已通过 `git mv` 移入 `shared/commonMain`。
- `CollectionContentViewModel.kt` 已通过 `git mv` 移入 `shared/commonMain`，并依赖 `CollectionContentEnvironment` 抽象平台副作用。
- Android 收藏夹 fetch、详情解析、HTML/ZIP 导出、Toast/log、文件路径和 cacheDir 已由 `SharedAndroidPaginationEnvironment` 实现。
- `./gradlew :shared:compileKotlinMetadata`、`./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin --continue` 通过。
- `./gradlew :shared:compileAndroidMain --continue` 的 collection blocker 已清除，当前首个 blocker 后移到 `FollowScreen` 的既有 `MainActivity` 依赖。

```bash
rg -n "android\\.|Context|LocalContext|Toast|Log\\.|MainActivity|AccountData|ContentDetailCache|signFetchRequest|YMDHMS|java\\.|androidx\\.activity|AndroidAnswerNavigatorRepository|articleHost|export|Zip" \
  shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui \
  shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel \
  shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared -g '*.kt'

rg -n "CollectionContentViewModel|CollectionsViewModel|CollectionContentScreen|CollectionScreenData|ZhihuPageLoader" app/src/main/java shared/src -g '*.kt'

./gradlew :shared:compileAndroidMain
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:jvmTest
```
