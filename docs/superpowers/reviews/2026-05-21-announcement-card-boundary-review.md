# AnnouncementCard KMP 边界审查

日期：2026-05-21

## 输入

- 目标文件：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/components/AnnouncementCard.kt`
- 当前问题：组件主体使用 `ExperimentalMaterial3ExpressiveApi` 和 `MaterialTheme.motionScheme.defaultSpatialSpec()`；当前 KMP 迁移分支中同类 expressive API 已造成 `:shared:compileAndroidMain` 阻塞。
- 目标迁移方向：Compose UI 主体应进入 shared；Android 只保留 preview/tooling 等平台开发辅助。

## 结论

`AnnouncementCard` 主体语义属于 shared，当前文件准确边界是“需拆分”：主体是纯 Compose UI，可迁 `shared/commonMain`；尾部 `@Preview` 是 Android tooling，应留 androidMain 或后续删除/单独拆文件。

当前最小正确切片不是把组件退回 app，而是移除 experimental Material3 motion API 依赖，改用稳定 animation spec，降低 KMP 编译风险。

## 证据

- `AnnouncementCard.kt` 主体只依赖 Compose animation/foundation/material3/runtime/ui，没有 `Context`、`Intent`、WebView、文件、偏好、Toast 等平台副作用。
- 当前调用方在 `shared/src/androidMain/.../HomeScreen.kt`，但调用方位置不能决定组件所有权；该组件本身是可共享 UI。
- `@Preview` 和 `androidx.compose.ui.tooling.preview.Preview` 是 tooling，不属于运行时语义。
- `MaterialTheme.motionScheme` 在源码/官方层面是公开实验 API 并有 KMP 变体，但迁移阶段继续绑定 alpha/experimental motion 没有必要；本地编译可用性优先。

## 最小步骤

1. 删除 `ExperimentalMaterial3ExpressiveApi` import 和 opt-in。
2. 将 `expandVertically` / `shrinkVertically` 的 `MaterialTheme.motionScheme.defaultSpatialSpec()` 改为稳定 `tween(...)` 或默认动画 spec。
3. 后续再把主体和 `AnnouncementCardDefaults` / `AnnouncementCardColors` 移入 commonMain，preview 单独留 androidMain。

## 风险

- 稳定动画 spec 会失去 theme-coupled motion 细节，但行为仍是同类淡入/展开、收起/淡出。
- 该切片不能解决 ArticleScreen、WebView、DeveloperSettings 等其他 shared/androidMain 编译阻塞。

## 验证命令

```bash
rg -n "AnnouncementCard\\(|ExperimentalMaterial3ExpressiveApi|MaterialTheme\\.motionScheme" \
  shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/components/AnnouncementCard.kt \
  shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/HomeScreen.kt

rg -n "MaterialTheme\\.motionScheme|ExperimentalMaterial3ExpressiveApi" \
  shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/components

./gradlew :shared:compileAndroidMain
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin
```
