# NotificationViewModel KMP shared 边界审查

日期：2026-05-21

## 输入

- 目标文件：`app/src/main/java/com/github/zly2006/zhihu/viewmodel/NotificationViewModel.kt`
- 当前调用方：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/NotificationScreenData.android.kt`
- 迁移目标：`shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/NotificationViewModel.kt`
- 相关依赖：shared `PaginationViewModel`、Ktor client、`ZhihuNotificationClient`、`NotificationSettingsStore`、common zse96 签名。

## 结论

`NotificationViewModel` 语义所有权为拆分后进入 `shared/commonMain`。通知分页、未读数、过滤显示、自动 readall 策略属于 shared；Android 只保留 `Context`、`AccountData` client lookup、SharedPreferences actual、Toast/Dialog/clipboard/debug UI 和平台 lifecycle 副作用。

## 证据

- `NotificationViewModel` 已继承 shared `PaginationViewModel<NotificationItem>`，主体状态和分页流程不依赖 Android UI。
- 通知网络函数 `fetchZhihuUnreadNotificationCount`、`markAllZhihuNotificationsAsRead` 已在 common `ZhihuNotificationClient`。
- 通知设置已有 common `NotificationSettingsStore`，Android/JVM 分别提供 actual。
- 原实现阻碍为 `Context`、`AccountData.httpClient(context)`、Android `signFetchRequest()`、`androidContext()` 和 stale `NotificationPreferences.get*` 调用。

## 最小迁移

1. 用 `git mv` 保留 ViewModel 本体迁入 shared。
2. 用 `NotificationSettingsStore` 替代 Android `Context` preference 读取。
3. 给 `PaginationEnvironment` 提供签名请求 hook，让 common ViewModel 调用 shared notification client 时不依赖 Android extension。
4. Android actual 只创建 notification pagination environment、settings store，并保留 debug copy button 和 clipboard。
5. JVM actual 仍需后续接入真实 `DesktopAccountStore` / shared HttpClient，删除当前空列表占位。

## 风险

- `markAsRead(notificationId)` 原实现为空，本次保持现状，不新增行为。
- JVM 通知页仍是占位数据，后续必须接入真实 pagination environment 才能满足 desktop 完整复用目标。
- `PaginationEnvironment` 的签名 hook 当前由 Android adapter 真实实现；非 Android 默认 no-op 只用于避免扩大本切片。

## 验证

```bash
rg -n "NotificationViewModel|NotificationPreferences|getDisplayInAppEnabled|getAutoMarkAsReadEnabled|AccountData|signFetchRequest|androidContext|Context|ClipData|ApplicationInfo" app/src/main/java shared/src -g '*.kt'
rg -n "android\\.|Context|AccountData|signFetchRequest|SharedPreferences|Toast|AlertDialog|ClipData" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/notification -g '*.kt'
./gradlew :shared:jvmTest :desktopApp:compileKotlin
./gradlew :shared:compileAndroidMain 2>&1 | rg -n "NotificationViewModel|NotificationScreenData|has no corresponding expected|Actual function cannot have default"
```
