# AppearanceSettingsScreen KMP 边界审查

日期：2026-05-21

## 输入

- 目标文件：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/subscreens/AppearanceSettingsScreen.kt`
- 当前错误：internal `ExperimentalMaterial3ExpressiveApi`、app alias `AnswerDoubleTapAction` / `ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY` unresolved、Android actual 签名与 common expect `String?` 不匹配，并引发双击动作字段的级联类型错误。
- 目标迁移方向：页面 UI 和主题/导航/双击动作设置语义进入 shared；Android 只保留 SharedPreferences、文件选择、Toast、WebView 字体、dynamic color 等平台能力。

## 结论

`AppearanceSettingsScreen.kt` 的语义所有权是“需拆分”，不是 Android-only。主题模式、自定义色、底栏/启动页规则、双击回答动作和页面 UI 结构都属于 shared；当前 Android actual 仍混有 SharedPreferences、Toast、文件选择、contentResolver、WebView 字体路径等平台副作用，所以不能整文件直接 `git mv` 到 `commonMain`。

本次最小正确切片是修复 Android actual 的契约漂移和错误依赖：actual 参数恢复 `String?`，移除无实际用途的 internal expressive opt-in，改用 shared `AnswerDoubleTapAction` 定义。

## 证据

- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/subscreens/PlatformSubscreenEntrypoints.kt` 的 expect 是 `setting: String? = null`；JVM actual 也保持 nullable，Android actual 当前偏离为 `String = ""`。
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/theme/ThemeManager.kt` 和 `Theme.kt` 已拥有主题状态和主题壳；Android `AndroidThemeSettings` 只是持久化 adapter。
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/ui/AnswerDoubleTapAction.kt` 已提供双击回答动作枚举和 preference key；app 侧同名文件只是 alias，不应被 shared 反向依赖。
- `AppearanceSettingsScreen.kt` 仍直接使用 `Context`、`Toast`、`SharedPreferences.edit`、Activity Result 文件选择、`contentResolver`、WebView 字体文件路径等 Android 副作用。
- 底栏选择和启动页规则已由 `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/ZhihuMainPreferences.kt` 提供，说明该页面已有 shared 设置语义。

## 最小步骤

1. Android actual 签名改为 `setting: String?`，默认值只保留在 common expect；函数内部用 `settingKey = setting.orEmpty()` 统一处理高亮和滚动。
2. 删除 `ExperimentalMaterial3ExpressiveApi` import 和 opt-in；当前文件没有实际 expressive API 调用。
3. `AnswerDoubleTapAction` 和 `ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY` 改为从 `com.github.zly2006.zhihu.shared.ui` 引入。
4. 后续迁 commonMain 前，先扩 `SettingsStore` 支持 `StringSet`、nullable string、remove，并用 `UserMessageSink` 替代 Toast；文件选择和 WebView 字体路径继续留 Android adapter。

## 风险

- 该切片只能消除 Appearance 设置页自身的编译错误，不能单独让 `:shared:compileAndroidMain` 变绿；`ArticleScreen`、`AnnouncementCard`、WebView/export 等仍有独立阻塞。
- 不应为了短期编译把整页退回 app，否则会重新违背“主题状态和页面 UI 语义应 shared”的迁移方向。

## 验证命令

```bash
rg -n "AppearanceSettingsScreen\\(|ExperimentalMaterial3ExpressiveApi|ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY|AnswerDoubleTapAction" shared/src app/src -g '*.kt'
rg -n "rememberSettingsStore\\(|rememberUserMessageSink\\(|getStringSet|putStringSet|remove\\(" shared/src -g '*.kt'
rg -n "AndroidThemeSettings|rememberLauncherForActivityResult|ActivityResultContracts|Toast\\.makeText|filesDir|contentResolver|android\\.graphics\\.Color" shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/subscreens/AppearanceSettingsScreen.kt
./gradlew :shared:compileAndroidMain
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin
```
