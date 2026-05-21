# Search Screen Boundary Review

## 输入

- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/SearchScreen.kt`
- `SearchViewModel`
- `AccountData` hot-search fetch
- `SettingsStore` / `UserMessageSink`
- `FeedCard` / `PaginatedList` / `FeedPullToRefresh`

## 结论

`SearchScreen` 和 `SearchViewModel` 的语义目标是 `shared/commonMain`。搜索输入、热搜列表、搜索结果列表、导航到 `Search` / 外观设置、`search_v3` URL、`SearchResult -> Feed`、paging 都不是 Android-only。

当前仍不能直接整体迁 common，因为 `BaseFeedViewModel`、`FeedCard`、`FeedPullToRefresh` 等链路仍含 Android `Context`、`AccountData`、Toast/Dialog、SharedPreferences、Intent/open-url、`ContentDetailCache`、`BlocklistManager`、variant 判断等副作用。

## 当前最小修复

`SearchScreen` 里的 `LocalActivity.current as MainActivity` 只用于拿 `Context`，随后传给 `AccountData.fetchGet`、`SearchViewModel.refresh`、`SearchViewModel.loadMore`。这些调用不需要 `MainActivity` 成员。

本切片只做：

- 删除 `LocalActivity` import。
- 删除 app `MainActivity` import。
- 改为 `LocalContext.current`。

不引入新 loader，不重写页面，不改 `SearchViewModel`。

## 后续 shared 方向

- `BaseFeedViewModel` 的 `Context` / SharedPreferences / Toast / `ContentDetailCache` / `BlocklistManager` 拆成 environment。
- `FeedPullToRefresh` 改为接收 `onRefresh` 或环境。
- `FeedCard` 的 prefs、Toast、Intent、variant 判断改为 `SettingsStore` / `UserMessageSink` / open-url / variant adapter。
- URL 编码用 Ktor `URLBuilder` / parameters。
- Toast 用 `UserMessageSink`，prefs 用 `SettingsStore`，HTML 解析优先 Ksoup。

## 验证命令

```bash
rg -n "LocalActivity|MainActivity" shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/SearchScreen.kt
./gradlew :shared:compileAndroidMain --continue
git diff --check
```
