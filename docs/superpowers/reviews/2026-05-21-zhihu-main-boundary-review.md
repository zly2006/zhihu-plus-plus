# ZhihuMain shared 边界审查

## 输入

- 审查对象：
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/ZhihuMain.kt`
  - `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/ZhihuMainState.kt`
  - `app/src/main/java/com/github/zly2006/zhihu/ui/ZhihuMainAndroidContent.kt`
  - `app/src/main/java/com/github/zly2006/zhihu/ui/ZhihuMainAndroidState.kt`
  - `app/src/main/java/com/github/zly2006/zhihu/MainActivity.kt`
- 目标：确认 `ZhihuMain` 迁入 shared 后，哪些语义应共享、哪些必须留在 Android adapter。
- 审查方式：独立 `gpt-5.5` / `xhigh` subagent 只读审查，主 agent 结合编译结果复核。

## 结论

- 应进入 shared：
  - `ZhihuMain` 主导航壳、底栏、pager、`MainTabs` / `TopLevelDestination` 选择语义、`openFrom` 映射。
  - bottom bar 偏好规则、`ZhihuMainPreferenceSnapshot` / `ZhihuMainPreferenceState` 这类纯状态。
- 必须留在平台 adapter：
  - Android 页面注册、`MainActivity`、`LocalActivity` / `LocalContext`、`SharedPreferences`、`ViewModelProvider`、`ArticleViewModel` 创建。
  - `Toast`、Dialog、`Intent`、clipboard、TTS、更新安装、WebView、APK / lite / full 发行语义。
- 当前路径是正确的最小迁移路径：
  - 原 `ZhihuMain.kt` 以 `git mv` 保留主体到 shared。
  - Android screen 注册和 Android state 读写拆到 app wrapper。
  - 后续可以继续缩窄 Android route adapter 的参数，但这不是当前边界错误。

## 必要修正

- `isDarkTheme` 不应塞进 `ZhihuMainPreferenceSnapshot`；主题状态应由后续 shared theme core 处理，当前 Android wrapper 只作为显式参数传入。
- shared 需要 KMP Compose material icons 依赖，因为底栏图标属于共享 UI 壳。
- Android-only `testTagsAsResourceId` 不应放在 shared；Android 外层已有 resource-id 语义设置。
- app / androidTest 旧 `ZhihuMain(navController = ...)` 调用应改走 Android wrapper。

## Theme 下一步

- 不要整搬 `ThemeManager`。
- shared 先承载 theme core：主题模式、主题 snapshot、自定义色、深浅色解析纯逻辑。
- Android adapter 保留 SharedPreferences、系统深色探测、dynamic color、系统栏副作用。

## 验证命令

```bash
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin
rg -n "android\\.|Context|Intent|WebView|FileProvider|APK|lite|full|MainActivity|Toast|AlertDialog|ViewModelProvider|LocalActivity|LocalContext|SharedPreferences" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui shared/src/commonMain/kotlin/com/github/zly2006/zhihu/navigation -g '*.kt'
rg -n "ZhihuMain\\(navController = rememberNavController\\(\\)\\)|AndroidZhihuMain\\(" app/src/androidTest app/src/main/java -g '*.kt'
```
