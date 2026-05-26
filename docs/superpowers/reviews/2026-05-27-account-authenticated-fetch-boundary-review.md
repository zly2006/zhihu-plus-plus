# Account Authenticated Fetch Boundary Review

Date: 2026-05-27

## Inputs

- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/data/AccountData.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/data/ZhihuAuthenticatedFetch.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/util/ZhihuCredentialRefresher.kt`
- `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/shared/desktop/DesktopAccountStore.kt`
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/AndroidPaginationEnvironment.android.kt`
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/viewmodel/PaginationEnvironment.kt`

## Ownership

- Shared: account session JSON shape, cookie persistence protocol, `ZhihuJson`, authenticated request retry/refresh orchestration, ZSE signing helpers, and token refresh protocol.
- Android-only: `Context`, `filesDir/account.json`, lifecycle-owned `HttpClient` cache, Android API headers/user-agent wrappers, Android cookie storage hookup, Android logging and Activity/Dialog side effects.
- JVM-only: `DesktopAccountStore` file path compatibility, `Path` storage adapter, CIO engine, desktop cookie store wiring.

`AccountData.kt` should not be moved directly to common in the next slice. It still owns Android runtime wiring. The common boundary already exists at `executeZhihuAuthenticatedRequest()` and `fetchZhihuAuthenticatedJson()`.

## Evidence

- Android `AccountData.fetch()` already delegates JSON requests to common `fetchZhihuAuthenticatedJson()`, so JSON fetch retry/refresh is shared.
- JVM `DesktopAccountStore.withAuthenticatedResponse()` delegates raw-response requests to common `executeZhihuAuthenticatedRequest()`.
- Android `markItemsAsTouched()` still uses `AccountData.httpClient(context).post(...)` directly, so it bypasses the common 401 refresh/retry path used by JVM for the same last-read touch endpoint.

## Recommended Slice

Add a thin Android `AccountData.withAuthenticatedResponse()` wrapper matching the JVM store shape. Use it only in `AndroidPaginationEnvironment.markItemsAsTouched()` for this slice.

Do not rename `AccountData`, move it to common, batch-convert all Android network calls, or change any UI/navigation code in this slice.

## Verification

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:jvmTest --tests com.github.zly2006.zhihu.shared.data.ZhihuAccountTest --tests com.github.zly2006.zhihu.shared.account.ZhihuAccountRepositoryTest :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew assembleLiteDebug
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew ktlintFormat
git diff --check
```

Boundary grep:

```bash
rg -n "android\\.|Context|SharedPreferences|filesDir|CookieManager|Toast|Activity" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/data shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/account shared/src/commonMain/kotlin/com/github/zly2006/zhihu/util -g '*.kt'
rg -n "lastread/touch|withAuthenticatedResponse|httpClient\\(context\\).*post|\\.post\\(\" shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/AndroidPaginationEnvironment.android.kt shared/src/androidMain/kotlin/com/github/zly2006/zhihu/data/AccountData.kt
```

## Notes

The iOS source set is out of scope for this migration run. If iOS is later enabled, `hmacSha1Hex` and platform HTTP/session adapters need separate actual implementations.
