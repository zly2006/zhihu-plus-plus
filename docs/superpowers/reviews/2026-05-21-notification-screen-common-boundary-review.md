# NotificationScreen shared 边界审查

## 审查对象

- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/NotificationScreen.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/NotificationScreenData.android.kt`
- `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/NotificationScreenData.jvm.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/PlatformScreenEntrypoints.kt`
- `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/PlatformScreenEntrypoints.jvm.kt`

按 AGENTS 规则尝试启动 `gpt-5.5 xhigh` subagent 做独立审查，但当前会话 agent 线程已满，启动失败。本文件为按同一清单执行的本地审查记录。

## 结论

`NotificationScreen` 页面主体属于 `shared/commonMain`。它的主要语义是通知列表 UI、通知 item 渲染、已读入口、通知设置入口、通知目标到 shared navigation model 的跳转映射，这些不是 Android 独有语义。

平台侧只保留当前无法在本切片迁入 common 的能力：

- Android `NotificationViewModel` 创建和状态读取。
- `Context`、`Toast`、clipboard、`BuildConfig.DEBUG`。
- `viewModel.shouldShowNotification(context, item)` 依赖 Android preference/context，暂留 Android data adapter。
- 原 Android 调试复制按钮使用 `DraggableRefreshButton(preferenceName = "copyAll")`，这是带 Android preference 的拖拽 FAB 行为，保留在 Android 小 slot 中；JVM 为 no-op。

已删除整页 `NotificationScreen` 的 `expect/actual`。common 里只保留两个小平台边界：`rememberNotificationScreenData()` 和 `NotificationDebugCopyButton()`。这是临时迁移形态，后续 `PaginationViewModel`/notification preference 迁入 shared 后，应继续缩小或删除这些 adapter。

## 行为保留点

- 原页面基于 `viewModel.allData` 分页、显示时再按 `shouldShowNotification` 过滤。本次 common 数据模型保留 `totalItemCount = viewModel.allData.size`，避免过滤后列表为空时误判没有数据或停止加载。
- 如果当前批次全部被过滤但服务端仍未到底，common 会继续触发 `loadMore()`，避免隐藏通知导致列表卡空。
- 调试复制按钮保持 Android 原来的 `DraggableRefreshButton` 和 `copyAll` 位置偏好，不降级成普通 `IconButton`。

## 风险和后续

- `rememberNotificationScreenData()` 仍然是偏大的临时 data adapter，因为 `NotificationViewModel` 和 preference/filter 副作用还在 Android 侧。它不是长期边界。
- common 本轮没有迁移 `PaginatedList`，因为现有 `ProgressIndicatorFooter` 依赖 Android-side `LocalPullToRefreshViewModel`。后续应在 `PaginationViewModel` 迁移时统一处理 shared pagination component。
- JVM 端当前返回空列表，只用于保持 desktop 编译；真实通知数据要等 account/http/pagination 继续迁移。

## 验证

已通过：

```bash
./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet :shared:compileKotlinJvm :desktopApp:compileKotlin
git diff --check
```

已执行但失败，失败为既有迁移债务，不是本页面新增：

```bash
./gradlew :shared:compileAndroidMain
```

失败集中在 `AccountSettingScreen.kt`、`ArticleScreen.kt`、feed viewmodels 等 app-only 引用和 `PaginationViewModel`/filter 迁移缺口。

边界 grep 确认：`NotificationScreen.kt` common 文件中没有 Android `Context`、`Toast`、`ClipData`、`BuildConfig`、`NotificationViewModel`、`DraggableRefreshButton`、`PaginatedList`、`ProgressIndicatorFooter` 等平台或 Android-only 依赖；这些只出现在 `NotificationScreenData.android.kt`。
