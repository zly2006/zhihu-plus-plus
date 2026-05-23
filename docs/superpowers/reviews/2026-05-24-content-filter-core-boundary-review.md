# 2026-05-24 Content Filter Core Boundary Review

审查时间：2026-05-24 03:59:01 CST - 2026-05-24 04:04:12 CST
总耗时：5 分 11 秒

## 输入

本次只读审查的目标是确认下一步把 `ContentFilterManager` / `ContentFilterExtensions` / feed 过滤调用链继续迁入 `shared/commonMain` 时的真实 shared 边界，避免再次把“当前在 Android source set、由 Android 调用、import 看起来像 Android”误判成“语义属于 Android”。

重点检查对象：

- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterManager.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterExtensions.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/subscreens/ContentFilterSettingsScreen.kt`
- feed 过滤调用点：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/AndroidPaginationEnvironment.android.kt`、`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/za/AndroidHomeFeedViewModel.kt`
- KMP Room：`shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterDatabase.kt` 及各 DAO/entity
- `BlockedFeedRecord`、`ContentOpenEvent`、`BlocklistManager`、`BlockedKeywordRepository`

## 结论

内容过滤核心已经大面积进入 shared。下一步不应该“再迁一次过滤算法”，而应该把 Android wrapper 继续压薄。`ContentFilterManager`、`FeedFilterSettings`、`ForegroundReadFilterPipeline`、`FeedDisplayFilterPipeline`、`FeedContentFilterPipeline`、`FeedAdFilter`、`FeedFilterContent`、KMP Room `ContentFilterDatabase`/DAO/entity 都已经在 `shared/commonMain`，common 侧没有 `Context` / `Toast` / `SharedPreferences` / `Log` 泄漏。

真正还留在 Android 的，是 `ContentFilterExtensions` 这层入口包装，以及少数 facade。`ContentFilterExtensions` 现在同时承担设置读取、数据库实例获取、详情拉取、NLP matcher 注入、Toast/Log 回调，所以不能原样 `git mv` 到 common；但它的方法主体已经被拆进 common pipeline。最小迁移路径是保留同名公开入口和关键函数名，把实现改成 common 入口 + Android/JVM 薄 adapter，而不是重写过滤器。

## 当前边界

- `ContentFilterManager` 的所有权是 shared。它负责曝光记录、交互记录、统计、清理和 reset，不应回退到 Android，也不需要再拆。
- `ContentFilterExtensions` 的语义所有权是需拆分。应迁 shared 的是公开入口语义和 feed 过滤编排；必须留 Android 的是 `Context`、SharedPreferences actual、详情拉取、Toast/Log、`mainExecutor`、variant NLP 注入。
- `ContentFilterSettingsScreen` 的所有权是 shared。页面主体已经依赖 `SettingsStore`、`ContentFilterMaintenance`、`UserMessageSink`，后续不要把页面重新绑回 Android。
- `ContentFilterDatabase`、DAO 和 entity 已经按 KMP Room 方向共享。数据库 builder 和文件路径保留平台 adapter。
- `ContentViewRecord`、`BlockedFeedRecord`、`ContentOpenEvent` 职责分离正确，下一步不能把 feed 已读和内容已打开重新揉回一个 API。
- `BlocklistService` 和 `BlockedKeywordService` 是 shared 核心；`BlocklistManager`、`BlockedKeywordRepository` 是 Android facade。
- Android feed 入口还在 wrapper；desktop `applyHomeFeedFilters()` 仍是 no-op。后续如果声称过滤入口 shared，desktop 至少要通过同一 common 入口接 no-op/薄 adapter，而不是永久绕开。

## 最小迁移步骤

1. 保留 `ContentFilterManager`、各 pipeline、Room schema/DAO/entity 现状，不要再重写或回退。
2. 在 `shared/commonMain` 补齐同职责 common 过滤入口，优先保留 `ContentFilterExtensions` 公开函数名：`recordContentDisplay`、`recordContentInteraction`、`performMaintenanceCleanup`、`applyForegroundReadFilterToDisplayItems`、`applyContentFilterToDisplayItems`。
3. common 入口只接收 shared 依赖：`SettingsStore`、`ContentFilterDatabase`、`ContentDetailProvider`、`KeywordSemanticMatcher` 和消息/日志回调。
4. Android source set 保留薄 wrapper，把 `Context` 转成 settings/database/detail provider/NLP matcher/message callbacks。
5. JVM 侧如果本轮接入过滤，补同样薄的 desktop runtime；若暂时 no-op，也要挂在 shared 入口之后。
6. `BlocklistManager` 不要作为 filter core 继续扩散；过滤核心继续依赖 common `BlocklistService`。
7. `BlockedKeywordRepository` 本轮可不动，它是 full UI 管理 facade，不是 feed 过滤迁移 blocker。
8. 完成后先改 `AndroidPaginationEnvironment.applyHomeFeedFilters()`，必要时再改旧的 `AndroidHomeFeedViewModel` 链路。

## 需要保留的结构和名称

必须尽量保留：

- `ContentFilterManager` 的 7 个公开方法名和当前 common 主体顺序。
- `ContentFilterExtensions` 的 8 个公开开关/阈值读取函数和 5 个公开过滤入口函数名。
- `ContentFilterSettingsScreen` 的页面结构和现有 testTag。
- `BlockedFeedRecord` / `ContentViewRecord` / `ContentOpenEvent` 的表与职责命名。
- `FeedDisplayFilterPipeline.filter()`、`FeedContentFilterPipeline.filter()`、`saveBlockedFeedRecords()` 这些现有 common 主体函数名。

不要新增无业务语义的临时 loader、helper registry、page-only adapter。现有 common pipeline 已经足够，下一步只是把 Android wrapper 继续压薄。

## 风险

1. 最大风险是重复抽象。common 过滤骨架已经存在，不能再造一层新 facade 把已有 pipeline 包一遍。
2. `:shared:jvmTest` 当前会先被 `FeedFilterSettingsTest` 的 `SettingsStore` fake 参数漂移挡住，需要先修这个假红门禁。
3. desktop 当前 `applyHomeFeedFilters()` 是 no-op；如果过滤入口 shared 化后 desktop 仍绕开，会留下语义不一致。
4. `ContentFilterExtensions.getNLPSimilarityThreshold(context)` 仍被 full UI 直接调用，修改签名时要同步考虑 full 管理页。

## 验证命令

```bash
rg -n "android\.|Context|SharedPreferences|Toast|Log\.|MainActivity|ContentDetailCache|Build\.VERSION|FileProvider|Intent|Uri" \
  shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/filter \
  shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared \
  -g '*.kt'

rg -n "ContentFilterExtensions|ContentFilterManager|BlocklistManager|BlockedKeywordRepository|ContentOpenEventSupport|getContentFilterDatabase" \
  shared/src/androidMain/kotlin \
  shared/src/commonMain/kotlin \
  shared/src/jvmMain/kotlin \
  app/src/main/java \
  app/src/full/java \
  -g '*.kt'

JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:jvmTest --tests com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabaseTest --tests com.github.zly2006.zhihu.viewmodel.filter.FeedFilterSettingsTest --tests com.github.zly2006.zhihu.viewmodel.filter.ForegroundReadFilterPipelineTest --tests com.github.zly2006.zhihu.viewmodel.filter.FeedContentFilterPipelineTest --tests com.github.zly2006.zhihu.viewmodel.filter.FeedDisplayFilterPipelineTest
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :app:compileLiteDebugKotlin --continue
```
