# CommentScreen Common Boundary Review

日期：2026-05-22

## 输入

- 目标文件：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/CommentScreen.kt`
- 当前状态：`BaseCommentViewModel`、`RootCommentViewModel` 和 `ChildCommentViewModel` 已迁入 `shared/commonMain`，通过 `PaginationEnvironment` 获取 HttpClient 和签名配置。
- 调用方：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/components/CommentScreenComponent.kt`
- 被调用方：comment ViewModel、`OpenImageDialog`、`createEmojiInlineContent`、`saveImageToGallery`、`shareImage`、`luoTianYiUrlLauncher`、`LocalNavigator`。
- 预期迁移目标：页面主体进入 `shared/commonMain`，Android/JVM 只保留图片、emoji、外链和平台 workaround 等小粒度 adapter。

## 结论

`CommentScreen.kt` 的语义所有权是 split，但页面主体应迁入 `shared/commonMain`。评论列表、排序、回复栏、输入框、滑动回复、作者跳转、点赞状态、test tags、root/child 评论编排和评论图片菜单 UI 属于 shared UI/运行语义。

不能无编辑直接 `git mv` 后收工。当前阻碍是 `LocalContext`、`androidx.core.net.toUri`、`OpenImageDialog`、图片保存/分享/外链、`Jsoup`、`java.text`/`Date`/`Calendar`、emoji inline content 和 Honor 选择菜单 workaround 等平台或 JVM-only 依赖。

## 证据

- `CommentScreen.kt` 仍 import `LocalContext`、`toUri`、`OpenImageDialog`、`saveImageToGallery`、`shareImage`、`Jsoup` 和 `java.text/java.util`。
- `CommentScreen` 已使用 common comment ViewModel，并通过 `rememberPaginationEnvironment` 驱动加载、排序、提交和点赞。
- `CommentScreenSheetContent` 已在 common，Android `CommentScreenComponent` 目前只负责注入 Android `CommentScreen` 和 `AccountData.httpClient(context)`。
- `shared/build.gradle.kts` 已有 common `Ksoup` 和 `kotlinx-datetime`，可替换 Jsoup 和 Java 时间格式化。
- `OpenImagePreviewContent` 已在 common，可复用评论图片菜单 UI；Android 保留 `OpenImageDialog` 和保存/分享副作用。

## 边界

应迁入 shared：

- `CommentScreen` 页面主体、`SwipeToReplyContainer`、`CommentItem` 视觉结构。
- `commentViewModelKey`、排序/回复/发送交互和测试 tag。
- HTML 到 `AnnotatedString` 的纯解析和评论时间展示格式。
- 评论图片菜单的文案、顺序、test tag 和布局。

必须留平台 adapter：

- Android `OpenImageDialog` / `ComponentDialog` / Telephoto renderer。
- 保存图片、系统分享、系统浏览器打开、Toast、FileProvider、MediaStore。
- emoji 本地缓存和 bitmap 解码。
- Honor context-menu 过滤的实际实现。

## 风险

- 生命周期：必须保留 ViewModel key 逻辑，避免恢复历史上的 content mismatch。
- 线程：图片保存/分享 adapter 仍需由平台层处理 suspend/CoroutineScope。
- 持久化：emoji cache、分享临时文件、相册写入不能进入 common。
- 网络：评论提交/点赞继续走 `PaginationEnvironment` 签名，不把 Android `AccountData` 拉回 common。
- 导航：作者和链接跳转继续走 shared `LocalNavigator`/`NavDestination` 语义。
- 主题：仅使用 `MaterialTheme`，无新增主题状态。

## 最小迁移步骤

1. 新增 common 小 runtime/slot，承接评论图片打开、浏览器打开、保存、分享和 emoji inline content。
2. 将 `CommentScreen.kt` 通过 `git mv` 移入 `shared/commonMain`，保留 UI 结构和 test tag。
3. 用 Ksoup / kotlinx-datetime 替换 Jsoup / Java 时间格式化。
4. 删除 UI 参数中的 Android `HttpClient` 依赖；图片副作用由 runtime 或平台 adapter 自行处理。
5. Android actual 复用现有 `OpenImageDialog`、`saveImageToGallery`、`shareImage` 和 `luoTianYiUrlLauncher`；JVM actual 不引入 WebView。
6. 更新 `CommentScreenComponent` 为薄 wrapper 或直接调用 common 页面。

## 验证命令

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin assembleLiteDebug --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet
rg -n "LocalContext|Context|Intent|FileProvider|Toast|OpenImageDialog|org\\.jsoup|java\\.text|java\\.util\\.Date|java\\.util\\.Calendar|BitmapFactory|MediaStore|WebView" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/CommentScreen.kt
rg -n "CommentScreen\\(|CommentScreenComponent|createEmojiInlineContent|fuckHonorService" shared/src app/src -g '*.kt'
```

## 不应混入

不改 UI 文案、布局、test tag、排序默认值、评论发送/点赞接口、签名路径、root/child sheet 切换、作者/链接导航、图片菜单项、评论文本可选择性、ViewModel key、Android lite/full 发行语义；desktop/JVM 不能引入 WebView。
