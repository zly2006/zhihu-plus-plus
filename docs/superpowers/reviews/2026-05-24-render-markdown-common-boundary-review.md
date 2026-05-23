# 2026-05-24 RenderMarkdown Common Boundary Review

## 输入

- 目标文件：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/markdown/RenderMarkdown.kt`
- 相关文件：`MdAst.kt`、`LatexFontDownloader.kt`、`OpenImageDialog.kt`、`ArticleScreenRuntime.*`、`QuestionScreenRuntime.*`、`PinScreenRuntime.*`
- 目标：把正文 Markdown/HTML 渲染语义迁入 `shared/commonMain`，让 Android 和 desktop 复用同一套正文 UI；desktop/JVM 不引入 WebView。

## 结论

`RenderMarkdown` 不是 Android-only。它的正文结构、图片/视频/公式/表格/划线评论宿主、链接解析和 `htmlToMdAst` 转换属于 shared UI 语义；Android-only 的是 `Context`、`AccountData.httpClient(context)`、`rememberLatexFonts(context, ...)`、`OpenImageDialog`、图片保存/分享、外链打开和 SharedPreferences 读取。

本切片应按 `git mv` 保留主体，迁入 `shared/commonMain` 后只拆平台副作用为小 runtime adapter。必须保留关键函数名与主体形状：`RenderImage`、`RenderVideoBox`、`RenderMarkdown`、`htmlToMdAst`。

## 证据

- `RenderMarkdown` 里直接使用 `Markdown(...)`、`LocalNavigator`、`SegmentCommentHolder`、`CommentScreenComponent`、`SegmentActionSheet` 组织正文和划线评论宿主，这些已经是 common UI 语义。
- `RenderVideoBox` 只依赖 Compose、Coil 和 shared `Video` route，没有 Android API，可以直接进 common。
- `MdAst.kt` 负责把知乎 HTML 转成 markdown parser AST，生成 `Document`、`Paragraph`、`Figure`、`MathBlock`、`NativeBlock`，语义是 shared；但当前使用 `org.jsoup`，迁 common 时必须改为项目已有的 `Ksoup`。
- `LatexFontDownloader` 依赖 Android `Context.cacheDir`、`java.io.File` 和 Android 字体下载路径，必须留 Android adapter。
- `OpenImageDialog` 是 Android `ComponentDialog` + telephoto 实现，必须留 Android adapter；common 只能调用平台图片预览动作。
- JVM 当前 `ArticleMarkdownContent`、`QuestionDetailContent`、`PinHtmlContent` 都把 HTML 剥成纯 `Text`，这与“desktop 复用 Android UI”目标不一致。

## 最小迁移步骤

1. 把 markdown parser/renderer/latex renderer 依赖从 Android-only 移到 `commonMain`，Android 继续保留 telephoto、jsoup 和 Android 网络/图片能力。
2. `git mv` `RenderMarkdown.kt`、`MdAst.kt` 到 `shared/commonMain`，保留原函数名和大体调用顺序。
3. 新增最小 Markdown runtime，只承接图片预览、保存、分享、外链打开、数学字体和 HttpClient。
4. Android runtime 复用现有 `OpenImageDialog`、`LatexFontDownloader`、`luoTianYiUrlLauncher`、`saveImageToGallery`、`shareImage`。
5. JVM runtime 至少提供 common renderer、图片预览降级和浏览器打开，不再退回纯 Text。
6. 不改 `CommentScreen` 主 HTML 渲染；只保证 `RenderMarkdown` 继续作为 segment 评论 sheet 宿主。

## 风险

- `Jsoup -> Ksoup` 可能导致节点空白、`figure`、`video-box`、高亮 span 或 table selector 细节差异，必须编译和 grep 验证，并尽量保持原分支结构。
- 数学字体下载不能从 Android 退化掉；Android runtime 应继续使用现有 `rememberLatexFonts`。
- renderer shared 化不代表 desktop 详情数据加载完成；`QuestionScreenRuntime.jvm`、`PinScreenRuntime.jvm` 和 `DesktopArticleViewModelRuntime` 的数据获取仍需后续迁移。

## 验证命令

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain --continue
git diff --check
rg -n "android\.|Context|SharedPreferences|org\.jsoup|OpenImageDialog|ComponentDialog|MediaStore|FileProvider|luoTianYiUrlLauncher|saveImageToGallery|shareImage" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/markdown -g '*.kt'
rg -n "html.replace\(Regex\(\"<\[\^>\]\+>\"\)|questionDetailPreview\(|compactPreview\(html" shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui -g '*.kt'
```
