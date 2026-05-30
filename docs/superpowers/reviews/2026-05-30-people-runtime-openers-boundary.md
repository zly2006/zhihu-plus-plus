# People Runtime Openers Boundary Review

## Input

- Target files: `PeopleScreenRuntime.kt`, `PeopleScreen.kt`, Android/JVM/native People runtime actuals.
- Related wrappers: comment screen, markdown runtime, pin screen, home screen, question screen, and system/update settings open-url or open-image actions.
- Source sets: shared `commonMain`, `androidMain`, `jvmMain`, `nativeMain`.

This review was requested before adding new shared platform expect declarations. A `gpt-5.5 xhigh` subagent reviewed the boundary read-only, then the main agent verified the conclusion against current source.

## Conclusion

Opening a normal external URL, opening a Zhihu URL in the app WebView, and previewing an image URL are shared UI platform capabilities. Common UI should express those actions directly through platform-level expect helpers; Android, JVM, and native should provide the thin actual implementation.

`PeopleScreenRuntime` had no remaining page-specific semantics after earlier cleanup. Its two fields only wrapped opening a Zhihu URL and previewing an image, so the runtime should be deleted instead of kept as a page-level forwarding shell.

Other runtimes should reuse the same helpers internally when their open-url or open-image actions have the same semantics. They should not be deleted when they still own real page behavior such as saving, sharing, font loading, TTS, account/login flow, update installation, WebView lifecycle, or link-card preview loading.

## Evidence

- Android People URL opening used `WebviewActivity`, so it must not be replaced with a generic external URL opener.
- Android People image opening used `OpenImageDialog` with `AccountData.httpClient(context)`, which is a platform image-preview capability rather than People-specific business logic.
- JVM People URL and image opening already both delegated to `openDesktopExternalUrl`.
- Native People runtime had page-specific error stubs even though a general iOS URL opener already exists.

## Implementation Notes

- Add platform helpers for external URL opening, Zhihu in-app Web URL opening, and image preview opening.
- Delete the People page runtime data class and platform actuals.
- Replace People call sites with direct calls to shared platform openers.
- Reuse the helpers inside other runtimes where the action is the same, but keep runtimes that still carry distinct behavior.

## Risks

- Replacing People's Android WebView behavior with a normal external opener would change visible behavior.
- Moving `OpenImageDialog` or `AccountData.httpClient(context)` into common would violate source-set boundaries.
- Deleting the whole People helper file would also remove profile load helpers that still belong to common.

## Verification

```bash
rg -n "rememberPeopleScreenRuntime|PeopleScreenRuntime\\(" shared/src -g '*.kt'
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew --no-daemon :shared:compileKotlinJvm :desktopApp:compileKotlin :shared:compileAndroidMain :app:compileLiteDebugKotlin --continue
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew --no-daemon ktlintFormat
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew --no-daemon ktlintCheck
git diff --check
```
