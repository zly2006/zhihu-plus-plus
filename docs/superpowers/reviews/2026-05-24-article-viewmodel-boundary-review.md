# ArticleViewModel KMP 边界审查

日期：2026-05-24

## 输入

- 目标文件：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/ArticleViewModel.kt`
- 关联文件：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/ArticleScreen.kt`
- 当前阻塞：`ArticleScreen` 页面主体仍在 `shared/androidMain`，直接依赖 Android-only `ArticleViewModel`、WebView/Markdown renderer、导出/收藏/总结/投票等平台调用。
- 迁移目标：文章页 UI 主体最终进入 `shared/commonMain`；Android/JVM 只提供 WebView/Markdown/video/export/TTS/share/clipboard/message/permission/http/sign 等最小 adapter，不改变现有 UI。

## 结论

`ArticleViewModel` 是 `ArticleScreen` 迁入 `shared/commonMain` 的直接阻塞点，但当前不能直接 `git mv` 到 common。它同时包含 shared 文章状态机、网络/JSON/SSE 逻辑、回答切换语义，以及 Android-only 权限、Toast、clipboard、WebView 缓存、WebView 导出、MediaStore 保存、`AccountData(context)` 和 Android lifecycle/LiveData。

语义属于 shared 的部分：

- 标题、作者、正文、附件、投票、收藏、评论数、时间、IP、AI summary 等文章展示状态。
- `toCachedContent()`、`isFavorited`、回答/文章 `DataHolder` 到 UI state 的映射。
- 收藏、投票、AI summary 的 endpoint、request body、SSE 解析和状态更新流程。
- `ArticlesSharedData` 中的 navigator/pending navigator/transition 语义。
- 滚动恢复状态语义。

必须留平台 adapter 的部分：

- Android `Context`、`Activity`、存储权限、`Toast`、clipboard。
- 当前 `AccountData.fetch*` 和 `signFetchRequest(context)` 的落地调用。
- `CustomWebView` / `WebView` 预览缓存、图片/HTML 导出的 WebView 渲染。
- `Bitmap`、`Canvas`、`MediaStore`、`Environment`、assets 模板读取。

需要拆分的函数：

- `loadArticle(context)`
- `requestAiSummary(context)`
- `loadCollections(context)`
- `createNewCollection(context)`
- `toggleVoteUp(context, ...)`
- `toggleFavorite(..., context)`

这些函数应尽量保持 master 中的函数名、调用顺序和主体结构，只把平台 fetch/sign/message/history/open-event 能力改成 runtime/environment 注入。

## 最小迁移顺序

1. 新增窄的 article runtime/environment，只承接网络 fetch/sign、消息提示、open event/history、answer navigator repository 等能力。
2. 让 `ArticleViewModel` 的加载、收藏、投票、AI summary 先走 runtime，但暂不移动 `ArticleScreen`，不改 UI。
3. 将 `MutableLiveData` 滚动字段替换为 common 可用状态。
4. 导出、权限、WebView 预览缓存后续再拆成 Android adapter；不要把 WebView/MediaStore 等能力带入 common ViewModel。
5. `ArticleViewModel` core 可编译迁入 common 后，再推进 `ArticleScreen` 主体迁入 common。

## 必须保持的 master 结构

以下函数名和主体结构必须对照 `master` 保持，不应重写或绕开：

- `loadArticle`
- `toggleFavorite`
- `requestAiSummary`
- `cancelAiSummary`
- `loadCollections`
- `createNewCollection`
- `toggleVoteUp`
- `exportToImage`
- `exportToImageWithComments`
- `exportToHtml`
- `convertToMarkdown`
- `exportToClipboard`
- `ArticlesSharedData.reset`
- `ArticlesSharedData.promoteForNavigation`

`ArticleScreen` 侧也必须保留 `ArticleActionsMenu`、`ArticleScreen` 大函数、局部 `DateTexts`、回答切换和导出/收藏/总结调用顺序，不得再抽无语义微组件。

## 推荐下一切片

下一个低风险切片是新增 `ArticleViewModelRuntime` / `ArticleContentRepository` 类窄接口及 Android/JVM 实现，让 `ArticleViewModel` 的加载、收藏、投票、AI summary 走 runtime。写文件范围应限制在：

- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/...ArticleRuntime*.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/...ArticleRuntime.android.kt`
- 必要时 `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/viewmodel/...ArticleRuntime.jvm.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/ArticleViewModel.kt` 的调用注入点

本切片不移动 `ArticleScreen`，不改 UI。

## 验证命令

```bash
rg -n "android\.|Context|Intent|WebView|MediaStore|Environment|Bitmap|Canvas|Activity|MutableLiveData|LiveData|Toast|Handler|Looper|org\.jsoup|java\.io|java\.text|AccountData|signFetchRequest|getContentDetail|clipboardManager" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui -g '*.kt'
rg -n "ArticleViewModel|ArticleScreen|ArticlePreviewWebViewStore|ArticleActionsRuntime|ArticleReadHistoryRecorder|ArticleScreenSettingsState" shared/src app/src/main -g '*.kt'
rg -n "@Composable" shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui -g '*.kt'
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin --continue
```

## 风险

- 直接 `git mv` 会把 Android imports、WebView、MediaStore、Toast、`MutableLiveData` 和 `AccountData(context)` 带入 common，导致 JVM/desktop 编译失败。
- 先重写 UI 或重写 ViewModel 会违反 master 相似度要求，并增加 UI 偏差风险。
- 导出/权限/WebView 缓存不是当前最小 shared core，应作为后续 adapter 拆分。
