# JVM NotificationScreenData 边界审查

日期：2026-05-21

## 输入

- 目标文件：`shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/ui/NotificationScreenData.jvm.kt`
- 调用方：common `NotificationScreen()` 和 shared `ZhihuMain` 的 `Notification` route。
- 被调用方：目标应调用 common `NotificationViewModel`、`NotificationSettingsStore`、`PaginationEnvironment`、`ZhihuNotificationClient`、`signZhihuFetchRequest`，并在 JVM 侧使用 `DesktopAccountStore` 与 Ktor CIO client。
- source set：JVM actual，属于平台 adapter。

## 结论

JVM actual 应从空列表/no-op 改为接入 shared `NotificationViewModel`。通知分页状态、URL/JSON、签名、未读数和 readall 策略属于 shared；`DesktopAccountStore`、`~/.zhihu-plus-plus/account.json`、Ktor CIO client、cookie 文件持久化、JVM settings actual、桌面消息/日志属于 JVM adapter。

## 证据

- `NotificationViewModel` 已迁入 `shared/commonMain`，只需要平台提供 `NotificationPaginationEnvironment`。
- `ZhihuNotificationClient`、`NotificationItem`、`NotificationType`、`signZhihuFetchRequest` 都已在 common。
- 现有 JVM actual 只返回空数据，绕开了 shared 通知逻辑。
- `DesktopAccountStore` 已提供 cookie 备份与 `createHttpClient(cookies)`。

## 最小迁移

1. JVM actual 使用 `viewModel<NotificationViewModel>()`、`rememberNotificationSettingsStore()`、`rememberCoroutineScope()`。
2. 使用 `DesktopAccountStore` load 已备份 cookie，并创建一个 remembered `HttpClient`。
3. 用 `DisposableEffect(client)` 关闭 client，避免每次分页请求新建 client。
4. 新增 private JVM `NotificationPaginationEnvironment`，实现 fetch JSON、签名、错误提示和日志。
5. 保持 JVM debug copy hidden/no-op，后续若需要再抽桌面 clipboard adapter。

## 风险

- JVM notification settings 当前是 in-memory actual，重启后恢复默认值。
- JVM 没有 Android 登录过期 Dialog；失败时只提示加载失败，后续应复用现有 QR 登录流程处理登录过期。
- 签名必须在 `include` query 写入后执行，否则 zse96 URL 不匹配。

## 验证

```bash
rg -n "android\\.|Context|Intent|WebView|Toast|AlertDialog|AccountData|signFetchRequest\\(" shared/src/jvmMain shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/NotificationScreen.kt shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/NotificationViewModel.kt -g '*.kt'
rg -n "ZhihuPageLoader|NotificationPaginationEnvironment|rememberNotificationScreenData|signZhihuFetchRequest" shared/src -g '*.kt'
./gradlew :shared:jvmTest :desktopApp:compileKotlin :shared:compileKotlinJvm
```
