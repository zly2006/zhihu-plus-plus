# DailyScreen commonMain 边界审查

## 输入

- 页面：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/DailyScreen.kt`
- 目标：迁移到 `shared/commonMain`，删除整页 `expect/actual`。
- 新增最小平台能力：`rememberZhihuHttpClient()`。
- 依赖变化：`coil-compose` 从 `androidMain` 上移到 `commonMain`。
- xhigh subagent 审查尝试：已启动，但 CPA 返回 `503 auth_unavailable`，没有产出可采纳结论。本文件记录主 agent 按同一清单完成的本地审查。

## 结论

`DailyScreen` 页面主体应该属于 `shared/commonMain`。它的主要语义是知乎日报页面 UI、日期选择、列表状态、日报卡片、滚动加载和 shared route 跳转；这些都不是 Android 独有语义。

平台边界只保留在最小能力上：

- Android：通过 `AccountData.httpClient(LocalContext.current)` 提供已登录 HTTP client。
- JVM：通过 `DesktopAccountStore` 读取已备份 cookie 并创建 JVM HTTP client。
- 外部打开 URL：使用 Compose `LocalUriHandler`，不引入 Android `Intent` 或 JVM WebView。

## 已拆出的平台触点

- 删除 Android `LocalActivity` / `MainActivity` 强转。
- 删除 Android `Intent.ACTION_VIEW` 和 `androidx.core.net.toUri()`。
- 删除 Android `AccountData.fetchGet` 直接调用，改为 Ktor `HttpClient.get(...).body<JsonObject>()`。
- 删除 JVM 不可用的 `org.jsoup.Jsoup`，改用已在 commonMain 引入的 `com.fleeksoft.ksoup.Ksoup`。
- 删除 `SimpleDateFormat` / `Date` / `Locale`，改用 Kotlin 官方 `Clock.System` 和 `kotlin.time.Instant` 加 `kotlinx.datetime`。

## 风险

- `coil-compose` 上移到 commonMain 后，JVM 编译通过，但 Gradle 报告 Skiko 版本不匹配警告；后续如果桌面实际渲染图片异常，需要单独处理 Coil/Skiko 版本对齐。
- Android 编译当前仍被其它迁移债务阻塞，主要是已搬到 `shared/androidMain` 的页面仍引用 app-only `MainActivity`、`BuildConfig`、`R`、`ArticleViewModel`、`PaginationViewModel` 等。这个阻塞不是 `DailyScreen` 本轮迁移新增。
- `rememberZhihuHttpClient()` 是临时共享 HTTP client 能力边界；后续 AccountData/PaginationViewModel 迁移时应统一到 shared account/client provider，不应扩大为页面级 adapter。

## 验证

已通过：

```bash
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin
```

未通过：

```bash
./gradlew :app:compileLiteDebugKotlin
```

失败原因仍是当前迁移检查点已有的 AndroidMain 债务：`AccountSettingScreen`、`ArticleScreen`、feed viewmodel 等仍在 `shared/androidMain` 里引用 app-only `MainActivity`、`BuildConfig`、`R`、`ArticleViewModel`、`PaginationViewModel` 等。

```bash
./gradlew ktlintFormat
```

失败原因是该聚合任务触发了 iOS Kotlin 编译；本迁移约束不执行 iOS，且失败点是既有 common 代码的 iOS 不兼容引用（`JvmInline`、`Integer`、`System`），不是本次 `DailyScreen` 迁移新增。该命令已完成本切片相关 Kotlin 文件的格式化。

已检查：

```bash
rg -n "actual fun DailyScreen|expect fun DailyScreen|android\.|java\.|org.jsoup|LocalActivity|MainActivity|AccountData|Intent|toUri|System\.currentTimeMillis|SimpleDateFormat|Date\(|Locale|WebView|androidx\.webkit" \
  shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/DailyScreen.kt \
  shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/PlatformHttpClient.kt \
  shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/PlatformHttpClient.android.kt \
  shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/PlatformHttpClient.jvm.kt -g '*.kt'
```

结果：`DailyScreen` common 文件无 Android/Java/Jsoup/WebView 命中；Android actual 中仅保留 `AccountData` HTTP client provider。
