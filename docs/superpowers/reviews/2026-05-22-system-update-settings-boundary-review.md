# SystemAndUpdateSettingsScreen KMP 边界审查

日期：2026-05-22

## 输入

- 目标文件：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/subscreens/SystemAndUpdateSettingsScreen.kt`
- 调用方：`shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/ZhihuMain.kt` 已在 common route 中直接调用 `SystemAndUpdateSettingsScreen()`。
- 当前平台触点：Android `Context`、`Intent`、`LocalContext`、`SharedPreferences`、`toUri`、`painterResource(R.drawable...)`、`UpdateManager`、`ContinuousUsageReminderManager`、`luoTianYiUrlLauncher`。
- 目标：页面 UI 主体和设置语义进入 `shared/commonMain`；Android APK 下载/安装、系统外链打开、资源落地、更新 runtime 和文件路径留平台 adapter；desktop/JVM 复用同一页面 UI，不能引入 WebView。

## 结论

`SystemAndUpdateSettingsScreen` 是 split ownership。完整 Compose 页面、设置项、文案、滚动结构、更新状态展示、GitHub Token / Nightly / 遥测 / 防沉迷间隔设置语义属于 shared；Android `Context`/`Intent`、APK 下载与安装、Custom Tabs、SharedPreferences 落地、Android 资源和 app 发行语义属于平台 adapter。

整页 `expect/actual` 是迁移债务，应删除。JVM 端不能继续显示 unsupported 占位页，应复用 common 页面，并通过 JVM runtime 暴露无更新或不可用更新能力。

## 证据

- 目标文件本身是完整 Compose screen，当前位于 `shared/src/androidMain`。
- `ZhihuMain.kt` 的 common route 已直接调用 `SystemAndUpdateSettingsScreen()`，说明导航语义和页面入口已在 shared 壳中。
- `PlatformSubscreenEntrypoints.jvm.kt` 仍把该页面实现为 `UnsupportedDesktopSubscreen()`，与桌面/安卓复用 UI 的目标冲突。
- `SettingsStore` 和 `UserMessageSink` 已是 common 平台能力，可替代页面中的直接 SharedPreferences 和 Toast/消息通道。
- 更新 release 数据模型和版本解析已有 shared 代码；APK 下载/安装仍是 Android 发行能力，不能迁 common。

## 最小步骤

1. `git mv` 页面到 `shared/commonMain`，保留 UI 主体和文案。
2. 把 `actual fun SystemAndUpdateSettingsScreen()` 改成普通 common `fun`，删除整页 expect 与 JVM unsupported actual。
3. 新增 `SystemAndUpdateSettingsRuntime`：提供 `SettingsStore`、更新状态、检查/跳过/下载/安装动作、外链打开动作。
4. Android actual 包装当前 `UpdateManager`、`luoTianYiUrlLauncher`、Intent 浏览器打开和 APK 安装能力。
5. JVM actual 复用同一页面 UI，更新状态返回 `NoUpdate`，下载/安装能力置为不可用或 no-op，不引入 WebView。
6. 把 Discord / Telegram / GitHub 图标从 Android drawable 迁到 Compose Multiplatform common resources，common 页面用 `org.jetbrains.compose.resources.painterResource`。

## 风险

- 不要改变 UI 文案、布局、按钮顺序、滚动行为或更新状态按钮语义。
- 不要把 APK/lite/full/download/install 语义放进 common。
- `UpdateManager.updateState` 的状态流转要保持：跳过版本后进入 Latest，Latest 按钮再回 NoUpdate。
- 防沉迷设置当前只写 preference；不要误加新的生命周期行为。
- desktop/JVM 不得引入 WebView、JxBrowser、CEF、JavaFX WebView 等内嵌浏览器。

## 验证

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet
rg -n "android\\.|Context|Intent|LocalContext|FileProvider|SharedPreferences|PREFERENCE_NAME|java\\.io\\.File|com\\.github\\.zly2006\\.zhihu\\.shared\\.R|androidx\\.compose\\.ui\\.res\\.painterResource|UpdateManager|ContinuousUsageReminderManager|WebView|android\\.webkit|androidx\\.webkit" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/subscreens -g '*.kt'
rg -n "expect fun SystemAndUpdateSettingsScreen|actual fun SystemAndUpdateSettingsScreen" shared/src -g '*.kt'
rg -n "WebView|android\\.webkit|androidx\\.webkit|javafx|jxbrowser|cef" desktopApp shared/src/jvmMain shared/src/commonMain -g '*.kt'
```
