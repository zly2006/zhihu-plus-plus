# ZhihuMain 按 master 形状回收 shared 边界审查

## 摘要

当前 `ZhihuMain` 的主要问题不是文件位置，而是 route ownership：`shared/commonMain` 只保留了主壳，几乎全部 `composable<...>` route 注册被转移到 app 的 `androidZhihuMainRouteContent`。正确方案是让 shared `ZhihuMain` 恢复 `master` 的大函数形状，自己持有 `NavHost`、`MainTabsPager` 和全部 route 注册；Android/desktop 只保留真正的平台副作用 adapter。

## 输入

- `git show master:app/src/main/java/com/github/zly2006/zhihu/ui/ZhihuMain.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/ZhihuMain.kt`
- `app/src/main/java/com/github/zly2006/zhihu/ui/ZhihuMainAndroidContent.kt`
- `app/src/main/java/com/github/zly2006/zhihu/ui/ZhihuMainAndroidState.kt`
- `docs/kmp-migration-status.md`
- `docs/superpowers/plans/2026-05-21-kmp-migration-remaining.md`

## 关键结论

1. `master` 的 `ZhihuMain` 是单文件大函数结构，route 注册不应由 app 侧 helper 持有。
2. 当前 `routeContent` / `mainTabContent` lambda 已经形成事实上的大注入表雏形，应回收。
3. 必须先回收全部 route 注册 ownership，不要求一次性迁完全部页面实现。
4. `MainActivity`、WebView、`ArticleViewModel`、`ViewModelProvider`、扫码/更新/安装、`SharedPreferences` 持久化都应留平台 adapter。
5. `ColorSchemeScreen` 可直接迁 shared；`DailyScreen`、`CollectionScreen`、`PinScreen`、`NotificationScreen` 更适合先拆 shared 页面壳；`HomeScreen`、`FollowScreen`、`HotListScreen`、`AccountSettingScreen`、`ArticleScreen`、`PeopleScreen` 现阶段仍明显依赖 Android 运行时。

## 最小实现路径

1. 保留 `ZhihuMainPreferenceState` 和 `ZhihuMainNavigationState`。
2. 把 `androidZhihuMainRouteContent` 中的全部 `composable<...>` 按 `master` 顺序收回 `shared/commonMain/.../ZhihuMain.kt`。
3. 对尚未 shared-ready 的页面，不用 screen registry；只新增少量按副作用分组的小 adapter：
   - `ArticleRouteAdapter`
   - `QuestionExternalActions`
   - `AccountRuntimeAdapter`
4. 第一批可直接或接近直接进入 shared 的内容：
   - `ColorSchemeScreen`
   - `DailyScreen` 的 shared 页面壳
   - `CollectionScreen` / `PinScreen` / `NotificationScreen` 的 shared 页面壳
5. `Article` route 暂时继续由 Android adapter 创建 `ArticleViewModel` 并处理 WebView/transition。

## 边界细化

shared：

- `ZhihuMain` 大函数
- bottom bar / pager / `MainTabs` / 全部 route 注册
- 纯 Compose 页面壳
- 导航语义与 shared ViewModel 状态

Android-only：

- `MainActivity`
- `WebviewActivity` / `CustomWebView` / WebView
- `ArticleViewModel.ArticlesSharedData`
- `ViewModelProvider(activity)`
- `SharedPreferences` 实际读写
- 二维码扫码、更新安装、外部 Intent、TTS、clipboard、系统服务

## 必跑验证

```bash
rg -n "android\\.|Context|Intent|WebView|FileProvider|APK|lite|full|MainActivity|Toast|AlertDialog|ViewModelProvider|LocalActivity|LocalContext" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui shared/src/commonMain/kotlin/com/github/zly2006/zhihu/navigation -g '*.kt'

rg -n "getSharedPreferences|rememberLauncherForActivityResult|ActivityResultContracts|context\\.viewModels\\(|viewModel\\(|httpClient|AccountData|WebviewActivity|LoginActivity|FileProvider|ConnectivityManager|clipboardManager|toUri\\(" app/src/main/java/com/github/zly2006/zhihu/ui app/src/main/java/com/github/zly2006/zhihu/ui/subscreens -g '*.kt'

rg -n "navigation-compose|androidx.navigation|org.jetbrains.androidx.navigation" build.gradle.kts app/build.gradle.kts shared/build.gradle.kts desktopApp/build.gradle.kts

./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin
```

## 风险

- 如果只迁 top-level route 而保留 `androidZhihuMainRouteContent` 承接剩余 route，结构仍然不符合 `master`。
- 如果把 WebView 或 `MainActivity` 依赖扩散进 shared，会再次犯“按当前位置判所有权”的旧错误。
- 如果改用 screen registry / 大注入表，会继续偏离 `master` 大函数结构。
