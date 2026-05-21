# Home Screen Local Feed Boundary Review

日期：2026-05-21

## 输入

- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/HomeScreen.kt`
- `app/src/main/java/com/github/zly2006/zhihu/viewmodel/local/LocalHomeFeedViewModel.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/feed/BaseFeedViewModel.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/feed/HomeFeedViewModel.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/ui/TopLevelReselectAction.kt`
- 当前 `:shared:compileAndroidMain` 首个 blocker：`HomeScreen.kt` 仍引用 app `MainActivity`、`LoginActivity`、`LocalHomeFeedViewModel` 和 app typealias reselect helper。

## 结论

`HomeScreen` 页面主体应迁入 `shared/commonMain`，当前整页 `expect/actual` 是迁移债务。Android actual 中混入的是平台副作用，不是页面所有权。

`BaseFeedViewModel` 的分页展示状态、`displayItems`、flatten 和去重属于 shared；`Context` preference、Toast、`ContentDetailCache.getOrFetch(context)`、`BlocklistManager` 属于平台 adapter。

`HomeFeedViewModel` 的 feed/read/touch/filter 编排语义应进 shared；`AccountData`、signed fetch、`ContentFilterExtensions(context)` 和 Android log 是平台 adapter。

`LocalHomeFeedViewModel` 和本地推荐排序、反馈、候选池语义也应进 shared，但不能单文件直接进入 common。它当前依赖 Android `AlertDialog`、`Context`、`androidContext()` 和本地推荐 engine。短期为解除 `shared/androidMain -> app` 反向依赖，可把整组实现先迁到 `shared/androidMain`，但必须记录为临时桥，后续继续拆 DB/client/connectivity/message/log environment 后迁入 common。

`TopLevelReselectAction` 已经在 common，Home 应直接导入 common helper，不能依赖 app 下的 typealias。

## 所有权

Shared：

- Home 页面布局和 tab/feed UI 结构
- top-level reselect 语义
- feed 分页状态、展示 item flatten、去重
- 本地推荐排序、反馈、候选池、LocalContent KMP Room 语义

平台 adapter：

- Android `Context`、`Intent`、`MainActivity`、`LoginActivity`
- `Activity.viewModels` / platform ViewModel owner
- Toast、Android `AlertDialog`
- `PackageManager.firstInstallTime`
- clipboard、外链 launcher、`UpdateManager` APK/update 语义
- Android SharedPreferences 实现
- Android cookie/header/client、ConnectivityManager、Room builder 文件路径

## 最小步骤

1. 先让 `HomeScreen.kt` 不再 import app `MainActivity` / `LoginActivity` / app typealias。
2. 改用 `LocalContext.current`、Compose `viewModel()`、common `TopLevelReselectAction` helper。
3. 临时把 `LocalHomeFeedViewModel` 及其必需 Android-only local engine 实现移到 `shared/androidMain`，解除 `shared` 反向依赖 app。
4. 后续切片再把 Home 页面主体 `git mv` 到 common，并把 settings、login launcher、message sink、debug clipboard、update/system-link、local recommendation environment 拆成小 adapter。

## 验证

当前已执行：

- `HomeScreen` 改为直接使用 common `TopLevelReselectAction` helper。
- `HomeScreen` 不再 import app `MainActivity` / `LoginActivity`；登录启动改为 Android class name intent，系统更新跳转改走 shared navigator。
- `LocalHomeFeedViewModel` 及本地推荐 Android 实现已先 `git mv` 到 `shared/androidMain`，解除 `shared/androidMain -> app` 反向依赖。此为临时桥，后续仍需拆 environment 后迁入 common。
- `./gradlew :shared:compileAndroidMain --continue` 的 Home blocker 已清除，首个 blocker 后移到 `HotListScreen` 的既有 `MainActivity` 依赖。

```bash
rg -n "MainActivity|LoginActivity|LocalActivity|viewModels<|getSharedPreferences|Toast|AlertDialog|PackageManager|ApplicationInfo|UpdateManager|clipboard|androidContext\\(" shared/src/commonMain/kotlin shared/src/androidMain/kotlin -g '*.kt'
rg -n "LocalHomeFeedViewModel|BaseFeedViewModel|HomeFeedViewModel|AndroidHomeFeedViewModel|MixedHomeFeedViewModel|TopLevelReselectAction" app/src/main/java shared/src -g '*.kt'
rg -n "WebView|android\\.webkit|androidx\\.webkit" desktopApp shared/src/commonMain shared/src/jvmMain -g '*.kt'

./gradlew :shared:compileAndroidMain --continue
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin --continue
./gradlew :app:compileLiteDebugKotlin
```
