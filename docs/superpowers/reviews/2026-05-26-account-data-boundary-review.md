# AccountData KMP Boundary Review

## Input

- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/data/AccountData.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/account/*`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/data/ZhihuAccount.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/data/ZhihuCookieStorage.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/util/ZhihuFetchSignature.kt`
- `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/shared/desktop/DesktopAccountStore.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/util/ZhihuCredentialRefresher.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/util/ZhihuCredentialRefresherPlatform.android.kt`
- `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/util/ZhihuCredentialRefresherPlatform.jvm.kt`
- `master:app/src/main/java/com/github/zly2006/zhihu/data/AccountData.kt`

## Conclusion

账号 session 结构、JSON 持久化规则、cookie storage、通用 Ktor client 配置、`/me` 校验、ZSE 签名和 token refresh 业务逻辑属于 `shared/commonMain`。`AccountData` 和 `DesktopAccountStore` 应保留为平台 wrapper：Android 负责 `Context`、生命周期、`filesDir/account.json`、Android client cache、Android mobile headers 和 UI 登录态桥接；JVM 负责 `Path`、`~/.zhihu-plus-plus/account.json`、CIO engine 和桌面文件 API。

当前不适合整文件 `git mv AccountData`。正确切法是保留 `AccountData` 的 master 函数名和公开顺序，继续把共用主体下沉到 shared。

## Evidence

- `ZhihuAccountSession` / `ZhihuAccountRepository` / `ZhihuAccountSessionStore` 已在 common，证明 session 结构和持久化协议可共享。
- `installZhihuCommonClientConfig()`、`ZhihuCookieStorage`、`fetchVerifiedZhihuAccount()` 已在 common，证明 cookie/client 和 `/me` 基础请求可共享。
- `ZhihuFetchSignature` 和 `ZhihuCredentialRefresher` 已在 common，HMAC 只用小粒度平台 actual。
- `AccountData` 仍依赖 `Context`、Compose state、`LifecycleOwner`、`ContextCompat`、`filesDir` 和 Android `Log`，这些必须留 Android。
- `DesktopAccountStore` 仍依赖 `Path`、`kotlin.io.path.*`、`System.getProperty("user.home")` 和 CIO engine，这些必须留 JVM。

## Risks

- Android `AccountData.fetch()` 有 401 refresh/retry 和 10 秒节流；JVM `DesktopPaginationEnvironment.fetchJson()` / `DesktopArticleViewModelRuntime.fetchGet()` 仍没有同等语义，后续需要再抽 shared authenticated fetch。
- Desktop `verifyAndSave()` 当前只保存 `profile`，没有保存 `self`，会导致 shared account/home UI 在 JVM 上缺头像等完整账号信息。
- 该 lane 涉及生命周期、持久化、序列化和网络；不直接涉及数据库或主题状态，导航只通过平台 runtime 回调间接相关。

## Recommended First Slice

第一刀做共用 verified session 构造：

- 在 common `ZhihuAccount.kt` 中新增基于 `/api/v4/me` 的 `ZhihuAccountSession` 构造 helper，同时填充 `profile` 和 raw `self`。
- Android `AccountData.verifyLogin()` 保留函数名和外层流程，改为调用 shared helper 后 `saveData(context, session.toAndroidData())`。
- JVM `DesktopAccountStore.verifyAndSave()` 保留函数名和外层流程，改为调用同一 shared helper 后 `save(session)`。

验证：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:jvmTest --tests com.github.zly2006.zhihu.shared.account.ZhihuAccountRepositoryTest --tests com.github.zly2006.zhihu.shared.data.ZhihuAccountTest :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin --continue
```

Boundary grep：

```bash
rg -n "Context|SharedPreferences|File\\(|filesDir|android\\." shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/account shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/data -g '*.kt'
rg -n "fetchRefreshToken|refreshZhihuToken|signZhihuFetchRequest" shared/src/commonMain shared/src/androidMain shared/src/jvmMain -g '*.kt'
rg -n "verifyLogin\\(|verifyAndSave\\(|createHttpClient\\(|load\\(|save\\(" shared/src/androidMain/kotlin/com/github/zly2006/zhihu/data/AccountData.kt shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/shared/desktop/DesktopAccountStore.kt -g '*.kt'
```

## Master Shape Requirements

`AccountData` 必须继续保留这些关键函数名和公开顺序：

`loadData`, `asState`, `saveData`, `cookieStorage`, `createConfiguredHttpClient`, `overrideHttpClientFactoryForTesting`, `httpClient`, `verifyLogin`, `delete`, `decodeJson`, `snake_case2camelCase`, `fetch`, `fetchGet`, `fetchPost`, `addReadHistory`。

不得引入一次性账号 facade，不得把 Android mobile headers/user-agent 或 Android `signFetchRequest()` wrapper 判为 shared 账号语义。
