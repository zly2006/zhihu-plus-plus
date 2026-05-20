# Task 3 Theme shared 边界审查

## 输入范围

- `app/src/main/java/com/github/zly2006/zhihu/theme/*`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/theme/ThemeMode.kt`
- 所有 `ThemeManager`、`ZhihuTheme`、dynamic color、system dark、system bar 调用方。
- 审查方式：独立 `gpt-5.5` / `xhigh` subagent 只读审查，主 agent 结合当前代码和编译结果复核。

## shared 所有权

- `ThemeMode`、主题模式、自定义主题色、亮/暗背景色状态属于 shared。
- `ThemeSnapshot`、`resolveDarkTheme()`、`ThemeManager` 内存状态属于 shared。
- `ZhihuTheme` Material3 主题壳、`Color.kt`、`Type.kt` 属于 shared。
- `material-kolor:4.1.1` 有 KMP metadata/common 变体，可以作为 shared 依赖。

## 平台 adapter

- Android `SharedPreferences` 读写、`Context`、系统深色探测、Android 12 dynamic color、系统栏 light/dark flag 必须留在 `androidMain` adapter。
- WebView HTML dark class/js 注入仍是 Android-only UI/WebView 逻辑，不能迁入 shared。
- JVM/desktop 目前只提供默认系统深色状态和无 dynamic color / system bar 副作用；桌面适配不在本阶段范围。

## 当前实现方向

- `Color.kt`、`Type.kt`、`Theme.kt`、`ThemeManager.kt` 通过 `git mv` 迁入 `shared/commonMain`。
- `AndroidThemeSettings` 在 `shared/androidMain` 处理 Android 持久化。
- `PlatformTheme.android.kt` 提供 Android dynamic color、system dark 和 system bar effect。
- `PlatformTheme.jvm.kt` / `PlatformTheme.native.kt` 提供无副作用默认实现，避免 commonMain 直接依赖 Android API。

## 风险

- `ThemeManager.isDarkTheme` 仍有非 Compose 缓存读调用，主要在 Android-only `WebviewComp`。这是现有 WebView 逻辑耦合，后续可用显式 theme snapshot/callback 收敛。
- `ktlintFormat` 会触发 iOS 编译，而当前项目仍有其他 common 代码对 iOS 不可用；本阶段仍按约束不执行 iOS 验证。
- 曾出现 Kotlin daemon 增量缓存冲突；重跑后 fallback 编译通过，未证明为主题迁移问题。

## 验证命令

```bash
rg -n "android\\.|Context|SharedPreferences|WindowCompat|LocalContext|LocalView|Build\\.VERSION|dynamicDarkColorScheme|dynamicLightColorScheme|isSystemInDarkTheme|PREFERENCE_NAME" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/theme shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui -g '*.kt'
rg -n "ThemeManager\\.initialize|ThemeManager\\.setUseDynamicColor|ThemeManager\\.setCustomColor|ThemeManager\\.setBackgroundColor|ThemeManager\\.setThemeMode" app/src shared/src -g '*.kt'
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin
./gradlew :app:compileLiteDebugAndroidTestKotlin
./gradlew assembleLiteDebug
```
