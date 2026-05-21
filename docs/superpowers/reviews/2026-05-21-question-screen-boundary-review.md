# Question Screen Boundary Review

## 输入

- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/QuestionScreen.kt`
- `QuestionFeedViewModel`
- `QuestionScreenUiState`
- `ContentOpenEventSupport`
- `ArticleHost`
- `DataHolder.getContentDetail`
- `AccountData`
- `FeedCard` / `PaginatedList` / `CommentScreenComponent` / `WebviewComp`

## 结论

`QuestionScreen` 长期目标是 `shared/commonMain`，但当前实现仍需拆分，不能直接整体 `git mv`。问题详情展示、排序、关注状态、答案列表、评论入口、分享入口、问题打开记录语义属于 shared；Android `Context`、`Intent`、`Toast`、SharedPreferences、WebView、Android 评论/分享实现和 Android 网络适配属于平台副作用。

`QuestionFeedViewModel` 也应拆分后迁 shared：question feeds URL、排序、answer feed 映射、follow/unfollow 业务语义属于 shared；当前仍依赖 `Context`、`AccountData.fetch`、`signFetchRequest`、Android `Log`，且继承的 `BaseFeedViewModel` 仍含 Toast/SharedPreferences/Blocklist/ContentDetailCache。

## 当前最小修复

当前 blocker 来自 shared/androidMain 反向 import app 层 `MainActivity` / `WebviewActivity`：

- `MainActivity` 只用于 `postHistory` 和 `consumePendingContentOpenFrom`，用 common `ArticleHost` 的 `postHistoryDestination` / `consumePendingContentOpenFrom` 替代。
- `WebviewActivity` 只用于“查看日志”按钮显式 Intent，改成本地 className 常量 + `Intent.setClassName(context, WEBVIEW_ACTIVITY_CLASS)`。

不引入新 loader，不重写整页，不把 WebView common 化。

## 后续 shared 方向

- question detail/follow 拆成基于 `HttpClient`/签名配置的 shared client。
- Toast 改 `UserMessageSink`。
- 偏好读写改 `SettingsStore`。
- WebView/打开日志改平台 open-url adapter。
- `QuestionFeedViewModel` 从 `Context` 版 `BaseFeedViewModel` 迁到 `PaginationEnvironment` 路径。
- 纯 HTML 文本解析优先用 Ksoup 替代 Jsoup。

## 验证命令

```bash
rg -n "com\\.github\\.zly2006\\.zhihu\\.(MainActivity|WebviewActivity)" shared/src -g '*.kt'
rg -n "ContentOpenEventSupport|ArticleHost|consumePendingContentOpenFrom|postHistoryDestination" shared/src app/src -g '*.kt'
./gradlew :shared:compileAndroidMain --continue
git diff --check
```
