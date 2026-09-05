# Zhihu++ HarmonyOS CPF 迁移探针

这是一个与现有 Android/KMP 工程隔离的迁移验证工程，用来确认 CPF-KMP-CMP（CKC）能够把 Compose Multiplatform 和现有生产代码编译成 HarmonyOS 可加载的原生库，并由 ArkTS 工程打包为 HAP。

之所以暂时独立建工程，是因为 Zhihu++ 当前使用 Kotlin 2.4.0、Compose 1.11.1，而 CPF 1.0.0 对应 Kotlin 2.2.21、Compose 1.9.2。直接改造根工程会先引入整套版本降级风险。

## 已验证的版本

- CPF-KMP-CMP：1.0.0
- Kotlin：2.2.21-1.0.0
- Compose：1.9.2-1.0.0
- Skiko：0.9.22.2-1.0.0
- kotlinx.serialization：1.9.1-1.0.0
- Ktor：3.3.3-1.0.0
- androidx Navigation Compose（OH 变体）：2.9.4-OH.0.1.2-37
- androidx Lifecycle（OH 变体）：2.9.4-OH.0.1.2-37
- androidx SavedState（OH 变体）：1.3.3-OH.0.1.2-37
- Gradle：8.9
- DevEco Studio：26.0.0.621

## 已直接复用的生产代码

探针通过 Gradle source include 直接编译根工程 `shared/commonMain` 的文件，没有复制或维护第二份实现。目前直接纳入 16 个文件、1,314 行（不含第三方阅读器源码）：

- [`ZhihuImageUploadModels.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/editor/ZhihuImageUploadModels.kt)：知乎图片上传结果与异常模型
- [`OnlineHistory.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/data/OnlineHistory.kt)：在线浏览历史响应模型及序列化
- [`RecommendationMode.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/data/RecommendationMode.kt)：推荐模式业务枚举
- [`SegmentInfo.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/data/SegmentInfo.kt)：内容分段模型及布尔值兼容序列化
- [`NlpSupport.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/nlp/NlpSupport.kt)：NLP 文本加权、关键词去重与过滤核心
- [`ThemeMode.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/theme/ThemeMode.kt)：主题模式业务枚举
- [`AnswerDoubleTapAction.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/ui/AnswerDoubleTapAction.kt)：回答页双击动作偏好解析
- [`TopLevelReselectAction.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/ui/TopLevelReselectAction.kt)：顶层页签重复选择策略
- [`SchematicVersion.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/updater/SchematicVersion.kt)：版本解析、比较和序列化
- [`AnswerSwitchSensitivity.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/components/AnswerSwitchSensitivity.kt)：回答切换灵敏度边界与默认值
- [`SmoothGradient.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/util/SmoothGradient.kt)：Compose 颜色渐变计算
- [`ZhidaSummary.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/util/ZhidaSummary.kt)：知答请求序列化、Base64、SSE 解析和流式合并
- [`ZhihuPolicy.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/util/ZhihuPolicy.kt)：连续使用提醒策略及 `expect Log`
- [`ZseSigner.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/util/ZseSigner.kt)：ZSE v4 加密算法
- [`DailyStory.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/data/DailyStory.kt)：知乎日报响应模型及序列化
- [`ZhihuDailyClient.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/data/ZhihuDailyClient.kt)：基于 Ktor 的知乎日报请求与分页合并
- [`NavDestination.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/navigation/NavDestination.kt)：P1 新增；全部共享 typed routes（MainTabs/Article/Search/Account.* 等）与知乎深链解析
- [`ArticleTypeNavType.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/navigation/ArticleTypeNavType.kt)：P1 新增；ArticleType 自定义 NavType
- [`LocalNavigator.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/navigation/LocalNavigator.kt)：P1 新增；Navigator 与 LocalNavigator
- [`Color.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/theme/Color.kt)、[`Type.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/theme/Type.kt)、[`ThemeManager.kt`](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/theme/ThemeManager.kt)：P1 新增；真实主题状态与 Typography

19 个文件参与双架构编译；`ZhihuDailyClient.kt` 仅编入 arm64，因为 CPF Ktor 3.3.3-1.0.0 未发布 `ohosX64` 变体。arm64 入口实际调用共享 Ktor 日报客户端，并由 CPF Coil 3.3.0-1.0.0 加载图片；x64 使用 ArkTS NetworkKit 传输 JSON/图片，再交给同一 Kotlin 模型与 Compose 阅读器。arm64 目前只有编译链接证据，不代表原生网络已通过真机验收。

x64 另有 `io.ktor.http` 最小编译垫片（`ktor-url-shim` 独立 klib 模块）：CPF Ktor 没有 ohosX64 变体，而共享 `NavDestination.kt` 使用 `io.ktor.http.Url`。垫片只实现该文件用到的只读 API，深链解析尚未在 x64 上验收。

## 与 Android 对齐的应用信息

Harmony 应用壳直接沿用 Android 正式版元数据：

- bundle/application ID：`com.github.zly2006.zhplus`
- versionName：`0.24.4`
- versionCode：`222`
- 默认名称：`Zhihu++`；中文名称：`知乎++`
- vendor：`zly2006`
- 图标：直接复制 `app/src/main/ic_launcher-playstore.png`，AppScope、Entry 与 Android 原文件的 SHA-256 均为 `3F8EBAA76968451AF1A8344E8713225F8FCA12DEC4D12D3BBC72F70BC561559B`

## 本机配置

本机依赖两个用户环境变量（已在系统设置中配置，新开的终端会自动继承）：

- `DEVECO_CLI_STUDIO_PATH`：DevEco Studio 安装目录，`devecocli` 据此定位 CLI 与 Hvigor。
- `DEVECO_SDK_HOME`：DevEco 安装内的 `sdk` 目录，Hvigor 构建与 `hdc` 都从这里取工具链。

PowerShell 中可随时确认取值：

```powershell
[Environment]::GetEnvironmentVariable('DEVECO_CLI_STUDIO_PATH', 'User')
[Environment]::GetEnvironmentVariable('DEVECO_SDK_HOME', 'User')
```

探针的调试签名配置引用本机 `.ohos/config` 中由 DevEco 生成的加密凭据，换机器后需要重新生成。

`harmonyApp/build-profile.json5` 包含本机签名信息，已从版本控制排除。首次配置可以复制 `harmonyApp/build-profile.json5.example`，然后通过 DevEco Studio 生成或填写本机签名配置。

若 bundleName 改为 Android 正式包名，需要为该包名重新生成调试 profile：

```powershell
devecocli auth login
devecocli signature generate --force --product default
```

## 构建 Kotlin/Native 库

在本目录执行：

```powershell
.\gradlew.bat :probe:linkDebugSharedOhosArm64
.\gradlew.bat :probe:linkDebugSharedOhosX64
.\gradlew.bat :probe:publishDebugBinariesToHarmonyApp
```

发布任务会生成并复制以下文件，但它们被 `.gitignore` 排除：

- `harmonyApp/entry/libs/arm64-v8a/libkn.so`
- `harmonyApp/entry/libs/x86_64/libkn.so`
- `harmonyApp/entry/src/main/cpp/include/<ABI>/libkn_api.h`

## 安装 OHPM 依赖并构建 HAP

~~~powershell
Push-Location harmonyApp
devecocli build
Pop-Location
~~~

签名产物位于 `harmonyApp/entry/build/default/outputs/default/entry-default-signed.hap`。DevEco CLI 使用随安装提供的 SDK/Hvigor；不需要打开 Studio GUI。根 Android 工程使用 Android CLI 管理 SDK/设备，仍用 Gradle 构建。

## 模拟器安装验证

```powershell
devecocli emulator start ZhihuPlus_API23

$hdc = "$env:DEVECO_SDK_HOME\default\openharmony\toolchains\hdc.exe"
& $hdc list targets
& $hdc install -r 'harmonyApp\entry\build\default\outputs\default\entry-default-signed.hap'
& $hdc shell aa start -a EntryAbility -b com.github.zly2006.zhplus -m entry
```

本轮已在 `ZhihuPlus_API23` 上完成正式包名 HAP 的安装、冷启动、系统 bundle 信息读取和截图验证。系统登记的 bundleName、版本、vendor 与图标引用均符合上述 Android 元数据，Ability 位于前台，Compose UI 成功从 x86_64 `libkn.so` 渲染。日志中仍有 CPF 资源名冲突、模拟器 OpenGL/系统服务警告，以及窗口装饰能力不支持异常，但未导致崩溃或白屏。

## P2 阅读器接入

八个与 Android 同版本的第三方模块通过 sources.jar 重编译为 OHOS 双架构，详见 [构建配置](reader-module.gradle.kts) 和 [许可证及适配范围](THIRD_PARTY_NOTICES.md)。解析与渲染并非自制占位实现。

- Markdown：parser / runtime / renderer，0.0.1-alpha.11。
- LaTeX：base / parser / renderer，1.4.6-zly。
- CodeHighlight：parser / render，1.1.1。
- CPF Coroutines 1.10.2-1.0.0、Serialization 1.9.1-1.0.0、Ktor 3.3.3-1.0.0、Coil 3.3.0-1.0.0。

当前范围是知乎日报公开游客首页 → 按“分钟阅读”选取一篇文章 → 封面、正文图片和 Markdown；首页未预载条目明确关闭。另有固定的 80 公式 / 300 列表项 / 200 表格行 / Kotlin 高亮压力样本。HTML 只适配日报基本标签，尚未迁移 Android 的完整 Ksoup 转换器。

访客 Cookie 通过 Preferences 持久化与冷启动恢复，只允许 _xsrf / BEC / d_c0，不导入或声称支持登录凭据。NetworkKit 的 Netscape Cookie jar 会转换为合法请求头；请求及图片有大小、超时、重定向限制。

宿主机解析回归测试：

~~~powershell
.\gradlew.bat :reader-checks:jvmTest
~~~

[验收记录](P2-VALIDATION.md) 区分构建、模拟器运行与未验证项。P2 切片不等于完整主壳迁移、全部 PoC 门槛或可发布的 HarmonyOS MVP；Navigation、数据库、登录、系统分享等仍属于后续工作。

CPF 仍报告未使用的 Fusion Renderer 符号未链接警告，当前仅测试 Skia 渲染后端。原生 Debug 库较大，正式版本需要单独做 Release、符号和包体优化。

## P1 共享主壳

P1 阶段把真实共享主壳的最小裁剪入口搬进了探针，验收详见 [P1 验收记录](P1-VALIDATION.md)。

- 真实共享部件：`NavDestination` 全部 typed routes（含 `ArticleTypeNavType` 自定义 NavType）、`Navigator`/`LocalNavigator`、`ThemeManager`（真实种子色 0xFF2196F3 与背景色）、真实 `Typography`。
- 主壳结构：`MainTabs` 是 NavHost 唯一顶层 route，`HorizontalPager` 承载 主页/日报/账号 三个 tab（对齐真实 `ZhihuMain` 的 tab 模型，testTag 同名 `nav_tab_*`）；页面内容为内存数据。
- 宿主能力：ArkTS 壳在 `onWindowStageCreate`/`onConfigurationUpdate` 推送系统深浅色；返回键统一分发为 P2 内部返回 → 关闭 P2 切片 → `navController.popBackStack()`（`@Entry` 组件的 `onBackPress()`，注意 UIAbility 级 `onBackPressed` 不生效）。
- 版本锁定：CPF 的 `-OH.x` 变体在 Gradle 语义比较中可能排在无修饰符版本之后（后者没有 ohos klib），probe 的 `configurations.all { resolutionStrategy.force(...) }` 强制锁定 OH 变体。
- 已知限制：material-icons 构件无 ohos 变体，底部栏三个图标以 ImageVector 内联（路径数据取自 CMP 1.7.3 icons 源码包）；material-kolor 动态取色未进探针，主题用静态 seed 方案替代；系统深色跟随、字体缩放、横竖屏尚未自动化验证。

构建与 P2 相同：`.\gradlew.bat :probe:publishDebugBinariesToHarmonyApp` 后在 `harmonyApp` 执行 `devecocli build`。

## P3 数据库选型

选型结论：**HarmonyOS 端采用 CPF SQLDelight（app.cash.sqldelight 2.2.1-1.0.0 OH 变体线）**；CPF Room3 的 OH 变体（room3-runtime-ohosarm64 3.0.0-alpha01-0.3.0）被 fork 改为非 suspend API 但配套 room3-compiler 未发布，上游编译器（alpha01～3.0.2）生成的代码全部无法编译，按可行性报告停止条件不承担私有 compiler fork。详见 [P3 验收记录](P3-VALIDATION.md)。

四个新增模块（`settings.gradle.kts`）：

- `db-room3`：Room3 纯 JVM 验证模块（上游 room3 3.0.0-alpha01 + sqlite-bundled-jvm 2.7.0-alpha01），KSP schema 导出到 `db-room3/schemas`。
- `db-sqldelight`：SQLDelight KMP 模块（jvm + ohosArm64），`.sq`/`.sqm` 与生产表名对齐；OH 侧随 `:probe` 进入 libkn.so。
- `db-legacy-room2`：生产 Room 2.8.4 基线 fixture（Sync 方式引入 shared-local-db 的 7 个实体源文件），导出生产 v6 schema JSON。
- `db-checks`：双栈同项测试 + 格式兼容实验，`.\gradlew.bat :db-checks:test` 运行（A1-A8 / B1-B7 / C1-C5 全部通过）。

关键格式兼容结论：SQLDelight 可零迁移直接读取生产 Room 2.8.4 数据库文件；Room3 3.0.0-alpha01 与 Room 2.8.4 的 identity hash 兼容（同 DDL 文件可直接接管，数据零丢失）。

SQLDelight OHOS 侧适配注意点（详见验收记录）：dialect 的 `AS Boolean` 代码生成有缺陷（用 INTEGER 存布尔）、CREATE TABLE 内不能写注释（会进 sqlite_master DDL）、`NativeSqliteDriver` 路径随 `name` 传完整沙箱路径（ArkTS 壳经 `P3SetDatabasePath` 注入 `filesDir/databases`，napi 入口 `setDatabasePath`）。

模拟器（x86_64）上两个 DB 栈均无 ohosX64 变体，P3 切片页面如实展示该能力边界；arm64 设备端冒烟待真机。
