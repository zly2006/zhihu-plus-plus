# AndroidTest Root Cause Report

Date: 2026-04-18

## Summary

- Command run: `./gradlew connectedLiteDebugAndroidTest --console=plain`
- Result: 58 tests executed, 54 failed, 4 passed
- Report: [connected report](/Users/zhaoliyan/IdeaProjects/Zhihu/app/build/reports/androidTests/connected/debug/flavors/lite/index.html)

## Confirmed Non-Root-Cause

The original AVD environment bug you pointed out was real:

- Connected tests were previously starting on an emulator with no `account.json`.
- `MainActivity` loads account state at startup from [AccountData.kt:91](/Users/zhaoliyan/IdeaProjects/Zhihu/app/src/main/java/com/github/zly2006/zhihu/data/AccountData.kt:91).
- That polluted the initial UI state before page tests tried to override content.

This specific issue is now isolated by the custom runner:

- [app/build.gradle.kts:47](/Users/zhaoliyan/IdeaProjects/Zhihu/app/build.gradle.kts:47)
- [ZhihuInstrumentedTestRunner.kt](/Users/zhaoliyan/IdeaProjects/Zhihu/app/src/androidTest/java/com/github/zly2006/zhihu/ZhihuInstrumentedTestRunner.kt)

Evidence:

- `PinScreenInstrumentedTest` passes when run alone after runner seeding.
- Full-suite failures remain, so `account.json` absence was a real blocker but is not the main remaining root cause.

## Root Causes

### 1. Tests are hosted on real `MainActivity`, so page tests still inherit app-shell side effects

Representative files:

- [ComposeScreenTestHost.kt:43](/Users/zhaoliyan/IdeaProjects/Zhihu/app/src/androidTest/java/com/github/zly2006/zhihu/test/ComposeScreenTestHost.kt:43)
- [MainActivity.kt:170](/Users/zhaoliyan/IdeaProjects/Zhihu/app/src/main/java/com/github/zly2006/zhihu/MainActivity.kt:170)
- [MainActivity.kt:224](/Users/zhaoliyan/IdeaProjects/Zhihu/app/src/main/java/com/github/zly2006/zhihu/MainActivity.kt:224)

Why this matters:

- Nearly all instrumented tests use `createAndroidComposeRule<MainActivity>()`.
- `MainActivity` runs startup logic before tests replace content: account loading, refresh token flow, telemetry, cleanup jobs, theme init, history init, and app-shell `ZhihuMain`.
- `activity.setContent { ... }` replaces UI content, but the Activity instance and its side effects still exist.
- Any page that depends on `MainActivity`, activity-scoped ViewModels, history storage, nav state, or startup-initialized services is not actually being tested in isolation.

Impact:

- This is the main structural reason page tests are brittle.
- It explains why tags exist in source but some connected tests still fail to find the expected subtree.

### 2. Global mutable state leaks across tests and across the full suite

Representative test mutations:

- [AccountSettingScreenInstrumentedTest.kt:74](/Users/zhaoliyan/IdeaProjects/Zhihu/app/src/androidTest/java/com/github/zly2006/zhihu/AccountSettingScreenInstrumentedTest.kt:74)
- [HomeScreenInstrumentedTest.kt:69](/Users/zhaoliyan/IdeaProjects/Zhihu/app/src/androidTest/java/com/github/zly2006/zhihu/HomeScreenInstrumentedTest.kt:69)
- [SystemAndUpdateSettingsScreenInstrumentedTest.kt:72](/Users/zhaoliyan/IdeaProjects/Zhihu/app/src/androidTest/java/com/github/zly2006/zhihu/SystemAndUpdateSettingsScreenInstrumentedTest.kt:72)
- [HotListScreenInstrumentedTest.kt:59](/Users/zhaoliyan/IdeaProjects/Zhihu/app/src/androidTest/java/com/github/zly2006/zhihu/HotListScreenInstrumentedTest.kt:59)
- [NotificationScreenInstrumentedTest.kt:137](/Users/zhaoliyan/IdeaProjects/Zhihu/app/src/androidTest/java/com/github/zly2006/zhihu/NotificationScreenInstrumentedTest.kt:137)
- [CollectionContentScreenInstrumentedTest.kt:240](/Users/zhaoliyan/IdeaProjects/Zhihu/app/src/androidTest/java/com/github/zly2006/zhihu/CollectionContentScreenInstrumentedTest.kt:240)

Leaking state includes:

- `account.json` / `AccountData.dataState`
- shared preferences
- `UpdateManager.updateState`
- activity-scoped ViewModels via `ViewModelProvider(activity)`
- `HistoryStorage`

Evidence:

- `PinScreenInstrumentedTest` passes in isolation but fails in full suite.
- `AccountSettingScreenInstrumentedTest` fails even in isolation, so both isolated defects and suite contamination are present.
- The mixed pattern means the suite has at least one cross-test pollution problem in addition to per-page defects.

### 3. A large set of tests assert against contracts that are not actually satisfied by the rendered subtree

Representative isolated failure:

- `AccountSettingScreenInstrumentedTest` still fails when run alone.

Representative full-suite failures:

- [AccountSettingScreenInstrumentedTest.html](/Users/zhaoliyan/IdeaProjects/Zhihu/app/build/reports/androidTests/connected/debug/flavors/lite/com.github.zly2006.zhihu.AccountSettingScreenInstrumentedTest.html)
- [AppearanceSettingsScreenInstrumentedTest.html](/Users/zhaoliyan/IdeaProjects/Zhihu/app/build/reports/androidTests/connected/debug/flavors/lite/com.github.zly2006.zhihu.AppearanceSettingsScreenInstrumentedTest.html)
- [CollectionContentScreenInstrumentedTest.html](/Users/zhaoliyan/IdeaProjects/Zhihu/app/build/reports/androidTests/connected/debug/flavors/lite/com.github.zly2006.zhihu.CollectionContentScreenInstrumentedTest.html)
- [DailyScreenInstrumentedTest.html](/Users/zhaoliyan/IdeaProjects/Zhihu/app/build/reports/androidTests/connected/debug/flavors/lite/com.github.zly2006.zhihu.DailyScreenInstrumentedTest.html)
- [PinScreenInstrumentedTest.html](/Users/zhaoliyan/IdeaProjects/Zhihu/app/build/reports/androidTests/connected/debug/flavors/lite/com.github.zly2006.zhihu.PinScreenInstrumentedTest.html)
- [QuestionScreenInstrumentedTest.html](/Users/zhaoliyan/IdeaProjects/Zhihu/app/build/reports/androidTests/connected/debug/flavors/lite/com.github.zly2006.zhihu.QuestionScreenInstrumentedTest.html)

Observed failure pattern:

- testTag exists in source, but node is not found in runtime semantics tree
- text exists in test expectation, but page under test does not render that text under the injected state
- scroll/container assumptions are too strict for the actual node structure

This means many tests currently validate an imagined contract, not the actual runtime contract.

### 4. Several page test seams are incomplete: state injection exists on paper, but the real rendering path is still not fully controlled

Representative partially-seamed files:

- [PeopleScreen.kt](/Users/zhaoliyan/IdeaProjects/Zhihu/app/src/main/java/com/github/zly2006/zhihu/ui/PeopleScreen.kt)
- [FollowScreen.kt](/Users/zhaoliyan/IdeaProjects/Zhihu/app/src/main/java/com/github/zly2006/zhihu/ui/FollowScreen.kt)
- [QuestionScreen.kt](/Users/zhaoliyan/IdeaProjects/Zhihu/app/src/main/java/com/github/zly2006/zhihu/ui/QuestionScreen.kt)
- [PinScreen.kt](/Users/zhaoliyan/IdeaProjects/Zhihu/app/src/main/java/com/github/zly2006/zhihu/ui/PinScreen.kt)

Why this matters:

- Adding `testTag` and `TestOverrides` is necessary but not sufficient.
- If production code still depends on activity state, background effects, merged semantics, or activity-scoped data, the page is not actually deterministic under connected test.

## Representative Evidence

### Isolated failure proves page/test mismatch

Command:

- `./gradlew connectedLiteDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.github.zly2006.zhihu.AccountSettingScreenInstrumentedTest`

Result:

- 4/4 failed

Implication:

- `AccountSettingScreenInstrumentedTest` has intrinsic assertion/seam problems, even without full-suite contamination.

### Isolated pass proves suite contamination exists elsewhere

Command:

- `./gradlew connectedLiteDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.github.zly2006.zhihu.PinScreenInstrumentedTest`

Result:

- 2/2 passed

Implication:

- At least some page tests are viable in isolation.
- Their failure in the full suite indicates state leakage or ordering contamination.

## Severity-Ordered Findings

1. Page tests are not truly isolated from `MainActivity` startup side effects.
   - Primary structural problem.
   - Affects most tests.
2. Test suite shares mutable global state between tests.
   - Causes order-dependent failures.
   - Explains isolated pass vs suite fail.
3. Many test assertions do not match the actual runtime semantics tree.
   - Causes isolated failures even when environment is fixed.
4. Several test seams are partial and do not fully control rendering state.
   - Causes hidden dependencies on activity or production effects.

## Recommended Next Investigation Order

1. Stop using real `MainActivity` as the default host for page-level tests.
2. Introduce a dedicated test host activity for isolated composable screen tests.
3. Reseed account state and shared preferences before every test, not once per instrumentation run.
4. Fix isolated failures first:
   - `AccountSettingScreenInstrumentedTest`
   - `AppearanceSettingsScreenInstrumentedTest`
   - `CollectionContentScreenInstrumentedTest`
5. Only after isolated failures are fixed, rerun the full suite and handle order-dependent pollution.

## Bottom Line

The remaining failures are not caused by missing `account.json`.

The real remaining root causes are:

- wrong test host
- shared mutable state across tests
- incomplete deterministic seams
- assertions that do not match actual runtime semantics
