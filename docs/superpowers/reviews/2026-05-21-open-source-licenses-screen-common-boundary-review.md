# OpenSourceLicensesScreen shared 边界审查

## 审查对象

- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/subscreens/OpenSourceLicensesScreen.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/subscreens/OpenSourceLicensesContent.android.kt`
- `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/subscreens/OpenSourceLicensesContent.jvm.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/subscreens/PlatformSubscreenEntrypoints.kt`
- `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/subscreens/PlatformSubscreenEntrypoints.jvm.kt`

当前会话 agent 线程已满，无法启动新的 `gpt-5.5 xhigh` subagent。本审查按 AGENTS 的 shared 边界清单本地执行。

## 结论

`OpenSourceLicensesScreen` 页面壳属于 `shared/commonMain`。标题栏、返回行为、Material3 外壳、full 变体手工许可条目和 URL 打开语义不是 Android 独有，应由 shared 复用。

Android-only 的部分只保留在小 adapter：

- `R.raw.aboutlibraries`
- `BuildConfig.IS_LITE`
- `produceLibraries(...)`
- `LibrariesContainer(...)`

JVM 端当前提供最小 license list 占位内容，保证 desktop 编译和 shared 页面壳可复用；完整 desktop license 数据后续再迁移，不在本切片扩展。

## 行为保留点

- Android 仍使用原 `aboutlibraries` 资源和 `LibrariesContainer`。
- full 变体特有的 `sentence_embeddings` / Rust tokenizer 手工许可条目仍只在 `!BuildConfig.IS_LITE` 时展示。
- 原 Android URL launcher 改为 common `LocalUriHandler.openUri`，避免把 Android `Context`/`Uri` launcher 带入 shared。

## 验证

已通过：

```bash
./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet :shared:compileKotlinJvm :desktopApp:compileKotlin
git diff --check
```

已执行但仍失败：

```bash
./gradlew :shared:compileAndroidMain
```

失败仍集中在既有迁移债务，例如 `AccountSettingScreen.kt`、`ArticleScreen.kt`、feed viewmodels 和 `PaginationViewModel` 缺口；本切片新增的 `OpenSourceLicensesContent.android.kt` 未出现在可见错误列表中。

边界 grep 确认：common `OpenSourceLicensesScreen.kt` 中没有 Android `Context`、`Intent`、`FileProvider`、`BuildConfig`、`R`、`produceLibraries`、`LibrariesContainer` 等依赖；这些只留在 Android actual content adapter。
