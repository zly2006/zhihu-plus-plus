# CollectionScreen commonMain 边界审查

## 输入

- 页面：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/CollectionScreen.kt`
- 目标：迁移页面主体到 `shared/commonMain`，删除整页 `expect/actual`。
- 保留平台能力：`rememberCollectionScreenData()`。
- 参考审查：subagent 已确认 `CollectionScreen` 的页面主体应 shared，但 `CollectionsViewModel`/`PaginationViewModel` 的 Android `Context`、网络、登录过期弹窗、Toast 等副作用不应在本切片迁入 common。

## 结论

`CollectionScreen` 页面主体属于 `shared/commonMain`。它的核心语义是收藏夹列表 UI、返回按钮、列表 item 渲染和 `CollectionContent` 导航，这些都不是 Android 独有。

本切片删除了整页 `CollectionScreen` 的 `expect/actual`，改为 common 直接注册页面函数；平台差异只保留在 `rememberCollectionScreenData()`：

- Android actual：继续复用现有 `CollectionsViewModel` 和 `LocalContext`，负责刷新、加载更多和读取分页状态。
- JVM actual：当前返回测试数据或空列表，保持桌面端 route 可编译；后续在 `PaginationViewModel`/account client 进入 shared 后再接真实数据源。

## 边界

本切片没有迁移 `PaginationViewModel`。原因是它当前仍绑定 Android `Context`、`AccountData.fetchGet`、`signFetchRequest`、登录过期弹窗、Toast、clipboard、SharedPreferences 和 `MainActivity.httpClient`。这属于单独的大切片，不应混入一个页面迁移提交。

`PaginatedList` 也没有在本切片迁入 common，因为它的 `ProgressIndicatorFooter` 隐式依赖 Android-side `LocalPullToRefreshViewModel`。本页暂时使用 common `LazyColumn` 实现同等 UI 行为，保留测试需要的稳定 tag 和“已经到底啦”文案。

## 验证

已通过：

```bash
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin
./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet
```

已检查：

```bash
rg -n "expect fun CollectionScreen|actual fun CollectionScreen|android\.|Context|LocalContext|MainActivity|Toast|AlertDialog|PaginationViewModel|CollectionsViewModel|PaginatedList|ProgressIndicatorFooter" \
  shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/CollectionScreen.kt \
  shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/PlatformScreenEntrypoints.kt \
  shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/PlatformScreenEntrypoints.jvm.kt \
  shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/CollectionScreenData.android.kt \
  shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/CollectionScreenData.jvm.kt -g '*.kt'
```

结果：`CollectionScreen` common 文件无 Android/API/ViewModel 命中；Android actual 中仅保留数据适配。

未通过：

```bash
./gradlew :shared:compileAndroidMain
```

失败原因仍是当前迁移检查点已有的 AndroidMain 债务，主要是 `AccountSettingScreen`、`ArticleScreen`、feed viewmodel 等还引用 app-only `MainActivity`、`BuildConfig`、`R`、`ArticleViewModel`、`PaginationViewModel`。输出中未出现 `CollectionScreen` / `CollectionScreenData` 新错误。
