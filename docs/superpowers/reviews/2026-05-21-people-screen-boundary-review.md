# People Screen Boundary Review

## 输入

- 目标文件：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/PeopleScreen.kt`
- 相关代码：`People*ViewModel`、`PeopleScreenUiState`、`AccountData`、`signFetchRequest`、`BlocklistManager`、`LocalNavigator`、`MainActivity` / `WebviewActivity` usage、history/openFrom、`FeedCard`、`PaginatedList`
- 审查方式：独立 `gpt-5.5` / `xhigh` subagent 只读审查，主 agent 结合当前编译错误和代码复核。

## 结论

`PeopleScreen` 不是 Android-only。当前文件混合了 shared 页面/状态/分页语义和 Android 副作用，短期先清除对 app 层 `MainActivity` / `WebviewActivity` 的反向依赖，长期拆环境后迁入 `shared/commonMain`。

应 shared 的部分：

- `PeopleScreenContent`、tab/pager/header/list UI
- `PeopleScreenUiState`
- `PeopleAnswersViewModel` 等分页 URL/include/sort 状态
- profile/follow/block 的状态和 API 语义
- `DataHolder`、`FeedDisplayItem`、`LocalNavigator`、`PaginationViewModel` / `PaginationEnvironment`

必须平台化或继续拆 adapter 的部分：

- Android `Context`、`Intent`、`Toast`、`Log`
- `WebviewActivity` / WebView
- Android blocklist facade
- Activity-owned history/pending open source
- Android lifecycle/session facade
- `OpenImageDialog`
- `BaseFeedViewModel` 当前 feed display/filter 环境
- `org.jsoup.Jsoup`，后续 common 迁移应替换为 Ksoup

## 当前最小修复

当前 blocker 只来自 app 类反向引用，因此本切片只做小改：

- 删除 `MainActivity` / `WebviewActivity` import。
- `toggleFollow` / `toggleBlock` 改用 `AccountData.httpClient(context)`。
- `load` 中继续用 `AccountData.fetchGet(context)`，history 改为 `(context as? ArticleHost)?.postHistoryDestination(...)`。
- `openInWebview` 改为 `Intent(...).setClassName(context, WEBVIEW_ACTIVITY_CLASS)`，不直接 import app Activity。

这不会把整页迁入 common，也不会引入新 loader。后续要迁 common 时再拆 `PeopleEnvironment` / feed display/filter environment 等小能力。

## 风险

- 直接 `git mv` 到 commonMain 仍会被 `Context`、`Intent`、`Toast`、`Log`、`OpenImageDialog`、`BlocklistManager`、`BaseFeedViewModel`、`Jsoup` 阻塞。
- `ArticleHost` history cast 只有宿主实现该接口时生效；当前 Android `MainActivity` 已承担文章 host/history 角色，若未来宿主变化，需要提供专门 history adapter。

## 验证命令

```bash
rg -n "MainActivity|WebviewActivity" shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/PeopleScreen.kt
./gradlew :shared:compileAndroidMain --continue
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin --continue
git diff --check
```
