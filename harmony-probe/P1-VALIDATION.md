# P1 共享主壳验收记录

日期：2026-09-05。环境：Windows x64 主机，CPF 1.0.0 工具链（Kotlin 2.2.21-1.0.0 / CMP 1.9.2-1.0.0），DevEco CLI 26.0.0.621，模拟器 `ZhihuPlus_API23`（HarmonyOS 6.1.0 API 23，x86_64），bundle `com.github.zly2006.zhplus`。

截图存于 `.validation/`（不入库）。

## 构建证据

- `:probe:publishDebugBinariesToHarmonyApp` 成功产出双架构 `libkn.so`（arm64 约 113.6 MB、x64 约 101.9 MB）与 `libkn_api.h`，包含新导出 `P1HandleBack`/`P1ApplyColorMode`。
- `devecocli build` 产出签名 HAP 并 `hdc install -r` 成功。
- 新增依赖解析自 CPF Maven 仓：`navigation-compose 2.9.4-OH.0.1.2-37`、`lifecycle-*-compose 2.9.4-OH.0.1.2-37`、`savedstate 1.3.3-OH.0.1.2-37`，均含 ohosArm64+ohosX64 klib（`resolutionStrategy.force` 锁定，无修饰符版本无 ohos 变体）。

## 已验证项（模拟器 x86_64，全部截图留档）

| 验收项 | 结果 | 证据 |
| --- | --- | --- |
| 主壳渲染：真实主题（种子色 0xFF2196F3）、三 tab 底栏、内存数据列表、设置入口 | 通过 | p1_home.jpeg |
| 底栏 tab 切换与选中高亮（主页/日报/账号） | 通过 | p1_account.jpeg / p1_daily.jpeg |
| typed Navigation 带字符串参数：`Search(query="鸿蒙 typed route")` 参数序列化、显示与恢复 | 通过 | p1_article.jpeg（误中搜索卡片反而完成该项验证） |
| typed Navigation 自定义 NavType：`Article(type=Answer, id=624949300)` 经 `ArticleTypeNavType` 往返 | 通过 | p1_in.jpeg |
| 嵌套 typed routes：`Account.DeveloperSettings` → `.ColorScheme` 二级嵌套，页内 `LocalNavigator.onNavigateBack` 弹栈 | 通过 | p1_dev2.jpeg / p1_color_back.jpeg |
| 系统返回键消费：`Index.onBackPress` → `P1HandleBack` → `popBackStack`，从文章页弹回主页，底栏恢复 | 通过 | p1_back4.jpeg；hilog `Index.onBackPress consumed=true` |
| 多级返回：ColorScheme 连按两次 Back 回到账号 tab | 通过 | p1_color_back.jpeg |
| 状态恢复：切后台/前台（含 aa start 重入）后 `Search(query)` 参数与返回栈保留 | 通过 | p1_resume.jpeg |
| 深浅色切换：真实 `ThemeManager.setThemeMode` 驱动全壳（背景 #121212、底栏、内容色） | 通过 | p1_dark.jpeg |
| 系统深浅色推送：ArkTS `onConfigurationUpdate` → `P1ApplyColorMode`，账号页显示"当前系统：浅色" | 通过 | hilog `onConfigurationUpdate colorMode: 1` + p1_account.jpeg |
| `onPageShow`/`onPageHide` 转发至 CPF `ArkUIViewController` | 通过 | hilog `Index.onPageShow` |
| P2 日报切片在主壳内共存：打开（NetworkKit + 访客 Cookie 冷启动恢复）、Back 关闭回主壳 | 通过 | p2slice.jpeg / p2back.jpeg |
| 双架构编译：同源 P1 主壳代码编入 ohosArm64 与 ohosX64 | 通过（arm64 仅编译链接） | gradle 构建产物 |

## 过程中发现并修复的问题

1. **返回键回调名错误**：`@Entry` 组件必须实现 `onBackPress()`（而非 `onBackPressed()`）；UIAbility 级 `onBackPressed` 会被系统调用但**不尊重返回值**，无法用于拦截。已修正 `pages/Index.ets`。
2. **CPF klib 参数名问题**：`ImageVector.Builder.path {}` 具名参数 DSL 在 CPF 发布的 ui klib 上解析失败（参数名不可靠），改用 `addPathNodes` + 位置参数 `addPath`。
3. **material-icons 无 ohos 变体**：CPF 仓 `material-icons-extended/core` 均未发布 ohos klib，底部栏图标内联 ImageVector（路径取自 CMP 1.7.3 icons 源码包）。
4. **savedstate 版本线**：`savedstate 1.9.2-OH.0.1.2-01` 只有 arm64；navigation 2.9.4-OH 配套的是 `1.3.3-OH.0.1.2-37`（双架构）。
5. **commonMain 不可见 target 源集源码**：x64 的 `io.ktor.http` 垫片必须做成依赖 klib（独立模块 `:ktor-url-shim`），放在 `ohosX64Main` 源集里 commonMain 编译不可见。

## 未验证项 / 已知限制

- **真机 arm64 未验证**：arm64 只有编译链接证据；模拟器为 x86_64。
- **系统深色跟随**：ArkTS 推送链路已验证，但未在模拟器手动切换系统深色模式做端到端复测。
- **字体缩放、横竖屏旋转**：未自动化验证（需模拟器手动操作）。
- **Compose 资源路径**：P1 未引入 composeResources，资源加载仍是待验证项。
- **material-kolor 动态取色**未纳入；主题用静态 seed 方案替代（真实 ThemeManager 状态驱动）。
- **底栏图标为内联矢量**：与真实 ZhihuMain 使用同一套 Material 图标路径，但不是同一构件。
- **深链解析（resolveContent）**：路由文件已编译，但 URL 解析未端到端验证；x64 使用 `io.ktor.http` 垫片，行为未与 ktor 对齐。
- **启动器出现两个知乎++图标**：模拟器桌面显示新旧两个入口，bundle 查询显示同一 bundleName，疑似 launcher 缓存或历史安装残留，未深究。
- Debug 原生库体积较大（arm64 ≈ 113 MB），Release/符号/包体优化仍属后续工作。
- `P2-VALIDATION.md`（P2 阶段同规格验收记录）仍然缺失，本文件只覆盖 P1。
