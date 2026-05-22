# AccountSettingScreen KMP 边界审查

日期：2026-05-22

## 输入

- 目标文件：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/AccountSettingScreen.kt`
- 调用方：`shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/ZhihuMain.kt` 主 UI 和账号 route；`PlatformScreenEntrypoints.kt` 仍有整页 `expect fun AccountSettingScreen(innerPadding)`。
- 当前平台触点：Android `Context`/`Intent`/`ActivityResultContracts`/`LocalContext`、`Toast`、clipboard、`PackageManager`/`ApplicationInfo`、Android `AccountData`、`UpdateManager`、`signFetchRequest`、`LoginActivity`/`QRCodeScanActivity`/`WebviewActivity`、Activity 反射切主 tab、Android drawable resources。
- 目标：账号页 UI 主体、快捷入口、设置导航、关于区、版本/开发者入口语义进入 `shared/commonMain`；Android/JVM 只保留登录、扫码、账号持久化、剪贴板、外链、版本信息、更新消息和主 tab 选择等小粒度 runtime。

## 结论

`AccountSettingScreen` 是 split ownership，但页面主体必须迁入 `shared/commonMain`。账号页布局、导航语义、Duo3 快捷入口、开发者 5 连点、关于区和退出登录确认属于 shared UI；Android `LoginActivity`、扫码 Activity、WebView 风控处理、Toast、clipboard、版本读取、`AccountData` 和 `UpdateManager` 是平台 adapter。

JVM 端不能继续显示 `UnsupportedDesktopScreen`，应复用同一账号页 UI；登录入口走已有 shared/JVM QR 登录能力或 runtime 提示，不得引入 WebView。

## 证据

- 目标文件当前是完整 Compose screen，位于 `shared/src/androidMain`，并以整页 `actual fun AccountSettingScreen` 暴露。
- common `PlatformScreenEntrypoints.kt` 仍声明整页 expect，JVM actual 仍为 unsupported 占位，与桌面/安卓复用 UI 的目标冲突。
- `SettingsStore` / `UserMessageSink` / `SystemUpdateRuntime` 已提供 common 能力，可替换页面中的直接偏好、提示和更新状态读取。
- `ZhihuAccountRepository`、`DesktopAccountStore` 和 shared QR 登录流程已覆盖账号 JSON 持久化与 JVM 登录方向。

## 最小步骤

1. 新增 `AccountSettingsRuntime` common model/expect，包含账号 UI 状态、刷新 profile、登录/扫码、退出、版本信息、复制文本、外链打开、主 tab 选择和 update state。
2. `git mv` 页面到 `shared/commonMain`，保留 UI 主体、文案、testTag 和布局。
3. 页面改用 `SettingsStore`、`UserMessageSink`、`AccountSettingsRuntime`，移除 Android imports 和 `AccountData.Data` 直接依赖。
4. 删除整页 `expect/actual AccountSettingScreen` 与 JVM unsupported actual。
5. Android actual 包装 `AccountData`、Activity Result、Intent/WebView、clipboard、package info、`UpdateManager` 和 MainActivity tab 选择。
6. JVM actual 包装 desktop account store/shared 登录能力、desktop 外链 opener、空 APK 更新能力，不引入 WebView。
7. 账号页需要的 launcher/license/github 图标迁到 Compose common resources；Android 侧保留仍被 Android-only 页面引用的 drawable。

## 风险

- 不要改 UI 文案、布局、testTag、Duo3 快捷入口显示规则、开发者 5 连点、长按复制版本号、退出登录确认或关于区链接。
- `/api/v4/me` 刷新要避免因 common state 映射导致反复刷新。
- 不要让 common 页面依赖 Android `AccountData.Data`；使用 common UI model，Android/JVM 各自映射。
- 历史 shortcut 不应继续依赖 Context 反射，应通过 runtime 的 `selectMainTab(OnlineHistory)`。
- 桌面端不得用 WebView 或嵌入式浏览器实现登录。

## 验证

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet :shared:runKtlintFormatOverJvmMainSourceSet :app:runKtlintFormatOverAndroidTestSourceSet
rg -n "android\\.|Context|Intent|WebView|ActivityResultContracts|LocalContext|Toast|AccountData|UpdateManager|clipboardManager|signFetchRequest|PackageManager|ApplicationInfo" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/AccountSettingScreen.kt
rg -n "expect fun AccountSettingScreen|actual fun AccountSettingScreen|UnsupportedDesktopScreen|HomeAccountSettingSheetContent" shared/src
rg -n "WebView|android\\.webkit|androidx\\.webkit|javafx|jxbrowser|cef|browser" desktopApp shared/src/jvmMain shared/src/commonMain -g '*.kt'
```
