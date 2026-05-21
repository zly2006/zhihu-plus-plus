# Article Host Boundary Review

日期：2026-05-21

## 输入

- 新增：`shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/ArticleHost.kt`
- 新增：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/ArticleHost.android.kt`
- 移动：`ArticleViewModel.kt`、`AndroidAnswerNavigatorRepository.kt`、`ContentOpenEventSupport.kt` 从 app 迁到 `shared/androidMain`
- 修改：`ArticleScreen.kt`、`ShareDialogComponent.kt`、`CollectionContentScreen.kt` 改为通过 article host 访问导航、剪贴板、TTS、历史、openFrom 和回答切换状态
- 修改：`MainActivity.kt` 实现 article host

## 结论

保留当前方向，但接口需要收窄。`ArticleScreen` 去掉对 `MainActivity` 的直接依赖是正确切片；`AndroidAnswerNavigatorRepository` 和 Android 版 `ContentOpenEventSupport` 放在 `shared/androidMain` 也合理，因为它们依赖的 `AccountData`、`ContentDetailCache`、Android Room wrapper 已在 `shared/androidMain`。

`ArticleHost` 不能直接暴露 `ArticleViewModel.ArticlesSharedData`。当前已改为暴露 `ArticleAnswerSwitchState`，把 host 边界从具体 ViewModel 类型上解开，并把 host/TTS/回答切换状态移动到 `commonMain`。`ArticlePreviewWebViewStore` 继续留在 `androidMain`，因为它返回 Android `CustomWebView` 并需要 `Context`。

补充纠错：不能因为 desktop 暂时没有 TTS 或预览 WebView，就把 `ArticleHost`、`ArticleAnswerSwitchState`、`TtsState` 命名成 Android 或留在 `androidMain`。Android 和 iOS 都有 TTS 能力；TTS 状态、朗读请求语义、回答切换方向、pending 内容、历史/openFrom/复制目标语义属于 shared。Android `Context`、TTS engine、WebView cache、clipboard 写入和 Activity/history 落地才是平台 adapter。

`ArticleViewModel` 目前仍是 Android-heavy 运行时对象，虽然移动到 `shared/androidMain` 是当前解除 shared 反向依赖的最小切片，但不能把它当作 shared/common 本体迁移完成。权限、WebView、导出、MediaStore、Toast、Activity 级副作用后续仍需拆。

## 所有权

Shared 语义：

- `Article` route、`ArticleHost`、`ArticleAnswerSwitchState`、回答切换语义、历史/openFrom 语义、TTS UI 状态、复制目标语义。
- `ContentOpenEventSupport` 纯内容身份和 openFrom 推断已经在 `shared/commonMain`。

Android 平台 adapter：

- Android TTS engine、clipboard、`MainActivity` 历史写入和 pending openFrom 落地。
- `ArticlesSharedData` 中的 WebView cache。
- `AndroidAnswerNavigatorRepository` 的 Android detail fetch、signed fetch、Room 查询。
- Android 版 `ContentOpenEventSupport` 的 database builder 和 DAO 访问。

## 风险

- `ArticleHost` 仍合并了多类文章页能力，只是当前为 Article 切片临时收敛反向依赖。后续不要继续把新平台能力塞进这个接口。
- `ArticleViewModel` 名字容易误导为 shared ViewModel；实际它仍在 `androidMain`，只能算 Android runtime ViewModel。
- `:shared:compileAndroidMain` 当前仍失败，但失败点已转移到 `CollectionContentScreen`、`FollowScreen`、`HomeScreen`、`PinScreen`、`WebviewComp` 等既有迁移债。

## 验证

已运行：

```bash
./gradlew :shared:compileAndroidMain --continue
```

结果：`/tmp/zhihu-shared-compile-article-4.log` 中不再出现 `ArticleScreen`、`ArticleViewModel`、`AndroidAnswerNavigatorRepository`、`ContentOpenEventSupport` 的编译错误；首个错误转为 `CollectionContentScreen.kt:69` 的 `CollectionContentViewModel` unresolved。后续已把 `AndroidArticleHost` 改名并移动为 common `ArticleHost`。

继续验证命令：

```bash
rg -n 'MainActivity|context as\?? MainActivity|context as MainActivity|viewModels<ArticlesSharedData>|ttsState|consumePendingContentOpenFrom|postHistory\(' \
  shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/ArticleScreen.kt \
  shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/ArticleViewModel.kt

rg -n 'ArticleScreen|ArticleViewModel|ArticleHost|AndroidAnswerNavigatorRepository|ContentOpenEventSupport' \
  /tmp/zhihu-shared-compile-article-4.log

./gradlew :shared:compileAndroidMain --continue
```
