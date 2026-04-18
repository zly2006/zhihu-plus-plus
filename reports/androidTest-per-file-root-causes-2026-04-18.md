# AndroidTest Per-File Root Causes

Date: 2026-04-18

## Suite Snapshot

- Full suite command: `./gradlew connectedLiteDebugAndroidTest --console=plain`
- Result observed: 58 executed, 54 failed
- Primary suite report path: [connected report](/Users/zhaoliyan/IdeaProjects/Zhihu/app/build/reports/androidTests/connected/debug/flavors/lite/index.html)

## Interpretation Rules

- `Isolated fail`: a class was rerun alone and still failed. This proves the class has intrinsic test/seam/assertion issues.
- `Suite-only fail`: the class failed in the full suite, but was not isolated yet. This means root cause may be either local test mismatch or state pollution.
- `Root cause summary`: best current diagnosis from logs plus source inspection. It is evidence-based, but some rows remain provisional until isolated reruns are done.

## Per File

| Test file | Current status | Root cause summary |
|---|---|---|
| `AccountSettingScreenInstrumentedTest.kt` | Isolated fail | Intrinsic failure. Test expectations do not match the actual rendered subtree under the current seam. Logged-in and logged-out branches are both not rendering the expected rows/text during connected test. |
| `AppearanceSettingsScreenInstrumentedTest.kt` | Suite-only fail | Runtime tree does not expose the expected `appearanceSettings.scroll` node. Either the screen is not actually mounted under the test host, or the tagged scroll container is not the node receiving semantics in connected runs. |
| `BlockedFeedHistoryScreenInstrumentedTest.kt` | Suite-only fail | Seeded placeholder/row texts are not visible in runtime output. Likely test seam does not fully replace production source of truth or assertions target text that is transformed at render time. |
| `BlocklistSettingsScreenInstrumentedTest.kt` | Suite-only fail | Runtime tree does not expose expected tab/list/stat nodes. The screen has tags in source, so this points to host/render-path mismatch rather than missing constants. |
| `CollectionContentScreenInstrumentedTest.kt` | Suite-only fail | Expected tagged title/list/menu nodes are absent in connected run. The injected `CollectionContentScreenTestOverrides` does not appear to fully dominate the production render path in device execution. |
| `CollectionScreenInstrumentedTest.kt` | Suite-only fail | Expected title/list tags are not visible. Similar pattern to `CollectionContent`: source contains tags, but connected semantics tree does not expose them. |
| `ColorSchemeScreenInstrumentedTest.kt` | Suite-only fail | Expected visible title text is missing. Likely same host/render mismatch affecting developer/settings subscreens. |
| `ContentFilterSettingsScreenInstrumentedTest.kt` | Suite-only fail | Tagged fields/scroll node not found. The screen likely renders a different subtree than the test assumes, or the test is asserting pre-scroll content before the screen reaches stable state. |
| `DailyScreenInstrumentedTest.kt` | Suite-only fail | Injected `DailyScreenUiState` is not surfacing the expected tagged nodes during connected run. This points to incomplete isolation from production `DailyScreen` setup or semantics mismatch. |
| `DeveloperSettingsScreenInstrumentedTest.kt` | Suite-only fail | Expected text and back-button tag are not visible. The screen likely depends on `MainActivity` state in ways not covered by the test harness. |
| `HistoryScreenInstrumentedTest.kt` | Suite-only fail | Timeout while waiting for empty-state condition. Strong signal that activity-scoped history state or startup content is not fully reset between tests. |
| `HomeScreenInstrumentedTest.kt` | Suite-only fail | Root tag `home_top_actions` absent in connected run. `HomeScreen` is highly coupled to `MainActivity`, activity ViewModels, update state, and login/account state, so current harness is not isolating it enough. |
| `HotListScreenInstrumentedTest.kt` | Suite-only fail | Mix of missing tags and wrong navigation count (`expected 0 but was 7`). This is a strong sign of leaked navigator/viewmodel state across tests in addition to local seam mismatch. |
| `NotificationScreenInstrumentedTest.kt` | Suite-only fail | Toolbar text and action content descriptions are not visible. Likely the test is not actually landing on the intended screen subtree under connected execution. |
| `NotificationSettingsScreenInstrumentedTest.kt` | Suite-only fail | Title text and expected rows not found. Same pattern as other settings screens: screen host and runtime subtree are misaligned with assertions. |
| `OnlineHistoryScreenInstrumentedTest.kt` | Suite-only fail | Overflow/menu/list nodes missing. The screen uses activity history plus activity-scoped ViewModel state, so test isolation is incomplete. |
| `OpenSourceLicensesScreenInstrumentedTest.kt` | Suite-only fail | Timeout waiting for license dialog condition. Likely caused by the screen not reaching the assumed stable state or the test targeting the wrong semantics node after render. |
| `PinScreenInstrumentedTest.kt` | Isolated pass, suite fail | Proven cross-test contamination candidate. The class passes when run alone after the runner fix, but fails in the full suite. Root cause is not the page alone; shared state/order pollution is involved. |
| `QuestionScreenInstrumentedTest.kt` | Suite-only fail | Tagged title/list nodes missing in full suite. Given the new seam compiles and tags exist, likely influenced by shared host state or incomplete override dominance. Needs isolated rerun. |
| `SearchScreenInstrumentedTest.kt` | Suite-only fail | Search input/hot list tags missing. `SearchScreen` depends on `MainActivity` and startup state; current connected harness likely mounts a different subtree than expected. |
| `SystemAndUpdateSettingsScreenInstrumentedTest.kt` | Suite-only fail | Timeouts and missing banner text. The test mutates `UpdateManager.updateState`, so cross-test global-state ordering is a likely contributor. |
| `ZhihuMainNavigationInstrumentedTest.kt` | Suite-only fail | Missing bottom-tab tag and selection timeout. This is app-shell navigation coverage; unlike page tests, it legitimately depends on real `MainActivity`, so it is especially vulnerable to shared app state and startup preferences. |

## Files With Strongest Evidence Of Cross-Test Pollution

- `PinScreenInstrumentedTest.kt`
- `HotListScreenInstrumentedTest.kt`
- `HistoryScreenInstrumentedTest.kt`
- `SystemAndUpdateSettingsScreenInstrumentedTest.kt`
- `ZhihuMainNavigationInstrumentedTest.kt`

Reason:

- These failures include timeouts, wrong shared counters, or global singleton/viewmodel/state behavior.
- They are not explained well by missing tags alone.

## Files With Strongest Evidence Of Intrinsic Test/Seam Mismatch

- `AccountSettingScreenInstrumentedTest.kt`

Reason:

- It fails even when run alone.
- The expected rows/text are not rendered under the test’s own seeded conditions.

## Highest-Level Root Cause By Area

1. Test host problem:
   - Most page tests are hosted on real `MainActivity`.
2. Shared mutable state:
   - Preferences, `AccountData`, activity-scoped ViewModels, `UpdateManager`, and history leak across tests.
3. Partial test seams:
   - Several screens have tags and overrides, but the injected state does not fully control the rendered subtree.
4. Assertion drift:
   - Some tests assert strings/tags that are present in source but not actually on the visible semantics node used in connected runs.

## Recommended Fix Order

1. Fix host isolation for page tests.
2. Add hard per-test reset for global state, not just per-run seeding.
3. Re-run `AccountSettingScreenInstrumentedTest` alone and fix it first.
4. Re-run `PinScreenInstrumentedTest` and `QuestionScreenInstrumentedTest` alone to separate pollution from intrinsic defects.
5. After isolated classes are stable, rerun the full suite and address ordering pollution.
