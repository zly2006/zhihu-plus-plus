# Webview Component Boundary Review

## 输入

- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/components/WebviewComp.kt`
- `LocalNavigator` / `Navigator`
- `NavDestination.Video` / `resolveContent`
- `MainActivity.navigate`
- `WebviewActivity`
- `luoTianYiUrlLauncher`

## 结论

`WebviewComp` / `CustomWebView` / Android `WebView` 本体必须留在 `shared/androidMain`，不能迁入 common，也不能让 desktop/JVM 引入 WebView。它内部的知乎 URL 解析、`Video` / `resolveContent` 到 `NavDestination` 的语义属于 shared，当前已在 common。

当前 blocker 是 shared/androidMain 反向 import app 层 `MainActivity` / `WebviewActivity`。这不是要把 WebView common 化，而是要把 Android 平台组件从 app class 类型依赖改成最小平台启动/导航 adapter。

## 当前最小修复

- 删除 `MainActivity` / `WebviewActivity` import。
- 用 `MAIN_ACTIVITY_CLASS` / `WEBVIEW_ACTIVITY_CLASS` 字符串启动 Activity。
- 用 `Context.findActivity()` + reflection 保留当前同进程 `MainActivity.navigate(NavDestination, Boolean)` 行为；找不到时 fallback 到 `MAIN_ACTIVITY_CLASS` intent。
- `view.context !is WebviewActivity` 改为 Activity className 判断。
- 不在非 Composable 的 `CustomWebView` 里使用 `LocalNavigator`，也不把 `ArticleHost` 扩成全局导航接口。

## 后续 shared 方向

后续若继续拆分，应抽 common “web content navigation policy”，Android 层保留 WebView、Activity、Intent、CookieManager、CustomTabs、图片保存/分享、文件路径和生命周期。

## 验证命令

```bash
rg -n "MainActivity|WebviewActivity|::class.java|context is MainActivity|is WebviewActivity" shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/components/WebviewComp.kt
./gradlew :shared:compileAndroidMain --continue
git diff --check
```
