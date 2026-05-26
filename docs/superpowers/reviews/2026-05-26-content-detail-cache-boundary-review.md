# ContentDetailCache Boundary Review

## Scope

- Target files:
  - `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/data/ContentDetailCache.kt`
  - `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/data/DataHolder.kt`
- Goal: move the content-detail cache state and lookup flow to `shared/commonMain` without moving Android network/session side effects.
- UI impact: none.

## Conclusion

`ContentDetailCache` is a split file: the cache key, in-memory map, mutex, expiry/LRU policy, `NavDestination` type extraction, `getOrFetch` flow, `clearExpired`, and `clearAll` belong in `shared/commonMain`. Android `Context`, `AccountData.fetchGet()`, Android `signFetchRequest()`, and the concrete content-detail network calls remain platform adapter code.

`DataHolder.getContentDetail(context, ...)` must stay in `shared/androidMain` for this slice because it still owns Android account/client/signing access. Moving it to common now would mix Android session and cookie lifecycle into common code.

## Evidence

- `ContentDetailCache` only needs `DataHolder.Content`, `NavDestination`, `Article`, `Question`, `Pin`, `ArticleType`, `Mutex`, logging, and current time for its cache flow; those dependencies are available from common code or existing common adapters.
- The existing Android fetch path in `DataHolder.getContentDetail(context, ...)` still depends on `Context`, `AccountData.fetchGet()`, `AccountData.decodeJson()`, and `signFetchRequest()`.
- Current callers can keep the old Android call shape through a thin Android extension adapter.

## Required Shape

Keep the original function names and ordering:

- `getOrFetch`
- `extractContentInfo`
- `fetchContent`
- `clearExpired`
- `clearAll`

The main flow must remain: extract cache key, check cache, fetch on miss, evict oldest entry if needed, store fresh content, return content.

## Risks

- Do not expand this slice into moving full content-detail network fetches to common.
- Do not create a large service abstraction for this small boundary; a provider function is enough.
- Moving the cache itself does not make desktop filtering fetch details; that requires a separate desktop provider slice.

## Verification

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :shared:runKtlintFormatOverCommonMainSourceSet :shared:runKtlintFormatOverAndroidMainSourceSet
git diff --check
rg -n "android\\.content|android\\.util|AccountData|signFetchRequest" shared/src/commonMain/kotlin/com/github/zly2006/zhihu/data/ContentDetailCache.kt
rg -n "ContentDetailCache\\.getOrFetch|DataHolder\\.getContentDetail" shared/src/androidMain shared/src/jvmMain shared/src/commonMain -g '*.kt'
```
