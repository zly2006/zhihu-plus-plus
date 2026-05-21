# MyModalBottomSheet KMP 边界审查

时间：2026-05-21 15:41:44 CST - 15:49:56 CST

## 输入

- 目标：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/components/MyModalBottomSheet.kt`
- 调用方：`HomeScreen`、`ArticleScreen`、`CollectionDialogComponent`、`CommentScreenComponent` 等。
- 当前依赖：Material3 bottom sheet internal API、`SheetState`、Dialog、Scrim、predictive back、motion tokens。
- 当前问题：`:shared:compileAndroidMain` 在该文件报 `BottomSheetImpl`、`SheetWindowInsets`、`BottomSheetDefaults.modalWindowInsets` / `standardWindowInsets`、`Scrim` 参数和 `MaterialTheme.motionScheme` 相关错误。

## 结论

`MyModalBottomSheet` 是共享 Compose UI 弹层封装，语义所有权属于 `shared/commonMain`。当前停在 `androidMain` 是因为实现复制了 AndroidX / Material3 internal 代码，不代表所有权属于 Android。

最小正确方向是使用公开 KMP API `androidx.compose.material3.ModalBottomSheet` 做薄封装，保留调用方 API；不要继续调用 `BottomSheetImpl`、`ModalBottomSheetDialog`、`Scrim`、`SheetWindowInsets`、`PredictiveBack`、`MotionSchemeKeyTokens` 或 invisible `SheetState` internals。

## 证据

- 原文件声明来自 `androidx.compose.material3:material3:1.5.0-alpha17`，并 suppress invisible API。
- 原文件调用 `BottomSheetImpl`、`SheetWindowInsets`、`ModalBottomSheetDialog`、`Scrim`、`MaterialTheme.motionScheme` 等非稳定 API。
- `shared/build.gradle.kts` 的 commonMain 已依赖 Compose Material3。
- 本地 `org.jetbrains.compose.material3:material3:1.9.0` sources 中公开 `ModalBottomSheet` 已提供相同核心参数，默认 `contentWindowInsets` 是 `BottomSheetDefaults.windowInsets`。

## 依赖边界

- shared：Compose runtime/foundation/material3 公开 KMP API、`SheetState`、`ModalBottomSheetProperties`、`WindowInsets`。
- 平台：不需要新增平台 adapter。Material3 自己的 Dialog / window actual 由库处理。
- 禁止继续依赖：Material3 internal API、invisible `SheetState` internals、版本私有 `BottomSheetImpl` / `Scrim` 签名。

## 风险

原实现有“按一次返回直接关闭”的 internal hack。公开 `ModalBottomSheet` 在存在 partially expanded 状态时可能先收起到 partially expanded。若后续确认这是产品硬要求，应在具体调用方显式使用 `rememberModalBottomSheetState(skipPartiallyExpanded = true)` 或另行设计公开 API 层面的行为，不应恢复 internal copy。

## 最小修复步骤

1. 用公开 `ModalBottomSheet` 实现 `MyModalBottomSheet` 薄 wrapper，保留调用方 API。
2. 默认 `contentWindowInsets` 改为 `BottomSheetDefaults.windowInsets`，兼容当前 shared 解析版本。
3. 删除自定义 `BottomSheet` 函数和 invisible suppress。
4. 将文件迁入 `shared/commonMain`。
5. 运行边界 grep 和 JVM / desktop / AndroidMain 编译验证；AndroidMain 全局失败时确认不再包含本文件错误。

## 验证

```bash
rg -n "INVISIBLE_|BottomSheetImpl|SheetWindowInsets|MaterialTheme\\.motionScheme|MotionSchemeKeyTokens|ModalBottomSheetDialog|androidx\\.compose\\.material3\\.internal|BottomSheetDefaults\\.(modalWindowInsets|standardWindowInsets)" shared/src/commonMain/kotlin shared/src/androidMain/kotlin
rg -n "MyModalBottomSheet\\(|rememberModalBottomSheetState\\(" shared/src app/src -g '*.kt'
./gradlew :shared:compileCommonMainKotlinMetadata :shared:compileKotlinJvm :desktopApp:compileKotlin
./gradlew :shared:compileAndroidMain
./gradlew :app:compileLiteDebugKotlin
```
