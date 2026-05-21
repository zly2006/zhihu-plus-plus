# ArticleScreen KMP 边界审查

日期：2026-05-21

## 输入

- 目标文件：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/ArticleScreen.kt`
- 目标问题：`:shared:compileAndroidMain` 当前首先在 `ArticleScreen.kt` 报错，包括 internal Material3 expressive API、Preview tooling、app `MainActivity`、app markdown、`ArticleViewModel`、TTS、share/export/webview、`AnswerDoubleTapAction` alias 等。
- 目标迁移方向：页面 UI 主体进入 `shared/commonMain`；整页 `expect/actual` 只是临时债；Android/desktop 只保留最小平台能力 adapter。

## 结论

`ArticleScreen.kt` 的语义所有权是“需拆分”：页面 UI 主体、阅读状态、回答切换语义、双击回答动作、标题/作者/日期/阅读进度等应进入 `shared/commonMain`；当前 Android `Context`、`MainActivity`、`ArticleViewModel`、TTS、WebView、分享、导出、权限、SharedPreferences、Toast、app markdown renderer 等必须拆成平台 adapter 或 slot。

当前不能把整文件直接 `git mv` 到 `commonMain`。阻碍不是 Article 页面语义属于 Android，而是页面主体仍混入 app-owned 运行时对象和 Android 副作用。

## 证据

- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/ZhihuMain.kt` 已在 shared 注册 `Article` route，但仍通过 `platformAdapter.article(...)` 注入当前平台实现。
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/ArticleScreen.kt` 直接引用 `MainActivity`、`ArticleViewModel`、`com.github.zly2006.zhihu.markdown.*`、Preview tooling、internal Material3 expressive API、TTS、WebView、Toast、SharedPreferences、Intent。
- `app/src/main/java/com/github/zly2006/zhihu/viewmodel/ArticleViewModel.kt` 当前仍导入 Android `Activity`/`Context`/`WebView`/`MediaStore`/`MainActivity`，并包含导出、权限、WebView 预渲染、网络加载等平台副作用。
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/ui/AnswerDoubleTapAction.kt` 已经提供 shared 双击回答动作枚举和 preference key；app 侧 `AnswerDoubleTapAction.kt` 只是 alias。

## 依赖与替代

- Material3 `TwoRowsTopAppBar` 在官方 API 层面存在 common/JVM 维度，但当前项目依赖解析下在 `shared` 编译为 internal，不应继续直接依赖；先用公开 Material3 `TopAppBar` 组合替代，后续再根据依赖版本决定是否恢复两行 app bar。
- Preview 是 tooling，不是运行时能力；shared 当前不需要引入 `ui-tooling-preview`，应删除或移出本迁移切片。
- Markdown 上游存在 KMP 产物，长期方向可以 shared；当前阻碍是仓库里的 renderer 仍在 app 且混有 Android 副作用。短期可把正文 renderer 作为 slot 注入，后续再迁 markdown renderer。
- `AnswerDoubleTapAction` 已在 shared，应直接引用 shared 定义，不再走 app alias。

## 最小步骤

1. 先清纯编译债：删除 `@Preview`、移除 internal `ExperimentalMaterial3ExpressiveApi` / `TwoRowsTopAppBar` 依赖、改用 shared `AnswerDoubleTapAction`。
2. 再拆 `ArticleScreen(article, viewModel)` 签名：shared 页面主体吃 state/callback/slot，Android wrapper 负责把 `ArticleViewModel`、TTS、share/export/webview 接回去。
3. 把 `CollectionDialogComponent`、`ExportDialogComponent`、`ShareDialogComponent` 这类直接绑定 Android runtime 或 `ArticleViewModel` 的组件从 shared 页面主体中剥离成 slot 或 Android wrapper。
4. 最后再把 Article 页面主体移入 `shared/commonMain`，并保留 Android runtime adapter。

## 风险

- 用公开 `TopAppBar` 替代 internal `TwoRowsTopAppBar` 会暂时弱化折叠两行标题/副标题的表现；这是编译债清理，不应被误当成最终 UI 迁移完成。
- 如果先迁 `ArticleViewModel`，会把 WebView、导出、权限、TTS 等平台副作用一起拖入 shared，风险高且方向错误。
- 如果把整页放回 app，会违反当前“页面主体进 shared，整页 expect 只是临时债”的关键约束。

## 验证命令

```bash
rg -n "MainActivity|ArticleViewModel|com\\.github\\.zly2006\\.zhihu\\.markdown|ui\\.tooling\\.preview|ExperimentalMaterial3ExpressiveApi|TwoRowsTopAppBar" \
  shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/ArticleScreen.kt \
  shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/components \
  app/src/main/java/com/github/zly2006/zhihu/ui

rg -n "platformAdapter\\.article|composable<Article>|ArticleScreen\\(" \
  shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/ZhihuMain.kt \
  app/src/main/java/com/github/zly2006/zhihu/ui/ZhihuMainAndroidContent.kt \
  shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/ArticleScreen.kt

rg -n "ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY|AnswerDoubleTapAction" shared/src app/src -g '*.kt'

./gradlew :shared:compileAndroidMain
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin
```
