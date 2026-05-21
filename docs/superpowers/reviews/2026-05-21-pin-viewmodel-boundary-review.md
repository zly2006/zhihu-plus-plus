# Pin ViewModel Boundary Review

## 输入

- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/PinScreen.kt`
- `app/src/main/java/com/github/zly2006/zhihu/viewmodel/PinViewModel.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/pin/PinScreenState.kt`
- 相关能力：`DataHolder.getContentDetail`、`ContentOpenEventSupport`、`ArticleHost`、openFrom、`AccountData`、`signFetchRequest`、Toast/Log。

## 结论

`PinScreen` 和 `PinViewModel` 都是需拆分的 shared 语义，不是 Android-only。页面主体、`PinScreenUiState`、route model、详情/点赞状态机、like count/isLiked 更新、openFrom 记录语义属于 shared；Android `Context`、Toast/Log、当前 `AccountData` / `getContentDetail` adapter、Room 写入落地、WebView/share/browser 是平台副作用。

当前 blocker 是 `PinScreen` 已在 shared/androidMain，但 `PinViewModel` 还留在 app，导致 shared 编译不可见。最小修复应先 `git mv` 到 `shared/androidMain`，再移除 `MainActivity` cast。不要直接 common 化，因为它仍依赖 Android `Context`、`Toast`、`Log`、`AccountData`、`getContentDetail(context, ...)`、`ContentOpenEventSupport.recordOpenEvent(context, ...)`。

## 当前最小修复

- `git mv app/src/main/java/com/github/zly2006/zhihu/viewmodel/PinViewModel.kt shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/PinViewModel.kt`
- 删除 `MainActivity` import。
- 用 `context.articleHost()?.consumePendingContentOpenFrom(pin) ?: ContentOpenFrom.UNKNOWN` 替代 `MainActivity` cast。
- 保持 `PinScreen` / `PinScreenUiState` / test override 不重写，不引入 `PinLoader`。

## 后续 shared 方向

- Toast 改 `UserMessageSink`。
- Log 改 shared logger/no-op adapter。
- `getContentDetail` 收敛成 shared content detail provider/client。
- open-event recorder 拆成 shared interface + Room KMP/platform builder。
- `Jsoup` 纯解析替换为 Ksoup；WebView 分支留 Android adapter。

## 验证命令

```bash
rg -n "PinViewModel|MainActivity|Toast|Log\\.|getContentDetail|signFetchRequest|ContentOpenEventSupport|ArticleHost|Jsoup" app/src/main/java shared/src -g '*.kt'
./gradlew :shared:compileAndroidMain --continue
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin --continue
git diff --check
```
