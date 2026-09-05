---
name: zhihu-instrument-test-governance
description: Audit, add, migrate, or remove Zhihu++ Android instrument tests under app/src/androidTest. Use for instrument-test cleanup, flaky device-test reduction, regression-test provenance, deciding whether a UI test belongs on an emulator, or reviewing a PR that changes permanent Android instrument coverage.
---

# Zhihu++ Instrument Test Governance

## 回归与数据边界

新增回归用例前必须在基线得到可复现失败，再在修复版本用同一用例验证通过。不得为注入时间或伪造后端数据新增一次性生产 helper；线上数据变更必须走真实服务端写入与读回，不能用 seed 冒充。

Treat emulator time as a recurring maintenance cost. Keep it only where a real product contract or historical regression needs Android, Compose, window, lifecycle, accessibility, or device integration to prove the behavior.

## Non-negotiable Rules

1. Never delete a test that protects a bug which actually occurred.
2. Every retained `@Test` must have verified issue and fixing/introducing PR links next to it. Never invent provenance.
3. Keep test cleanup out of feature and bug-fix PRs. Use a dedicated branch, worktree, commit, and PR.
4. Do not add a permanent instrument test merely to confirm a low-risk visual choice once. Build the APK and capture a real before/after screenshot instead.
5. Do not run the complete instrument suite locally unless the user explicitly asks. Prefer compilation, the smallest relevant test, or GitHub CI.
6. Flakiness, runtime, or inconvenience never justify deleting a proven regression test. Repair its synchronization, fixture, or execution boundary.
7. A test must assert user-observable behavior or a proven regression, not copy an implementation constant list into assertions. Delete tests that only restate the current source shape without an independent contract; validate the behavior through real interaction, build, or focused regression evidence instead.
8. Every retained regression test must be red against the pre-fix implementation. Temporarily restore the old behavior (or run the test against the parent revision) and record the failing assertion before accepting the test.
9. Every retained test must document both the related issue and the fixing or introducing PR with full URLs in its provenance KDoc.
10. Every retained test must state the target state, what it verifies, and why the assertion protects that behavior; a bare test name or implementation-detail assertion is insufficient.
11. Before stopping, execute every changed regression test against both the pre-fix implementation (must fail) and the fixed implementation (must pass). Record the exact red/green evidence; compilation alone is not sufficient.
12. Every PR that adds or changes tests must state in its body, per test, whether red-to-green verification was completed, including the commands and terminal results or an explicit blocker.

Read [references/provenance-and-decisions.md](references/provenance-and-decisions.md) before changing tests.

## Required Provenance Block

Place a KDoc block immediately above the annotations for every permanent test:

```kotlin
/**
 * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/123
 * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/456
 */
@Test
fun restoredBehaviorStaysStable() = Unit
```

For a feature contract rather than a reported regression, use:

```kotlin
/**
 * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/123
 * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/456
 */
@Test
fun featureContractStaysStable() = Unit
```

Both URLs are required. A commit hash, branch name, issue number without a URL, current cleanup PR, or a guessed related report is not historical provenance.

An introducing PR without an issue is not enough. A smoke test has no exemption. If no verified pair exists, record the test as `UNVERIFIED` in the audit. Do not add a fake block merely to make a checker pass, and do not create a retrospective issue solely to legalize an existing test.

## Workflow

### 1. Establish an isolated cleanup surface

- Fetch the intended base and create a dedicated worktree from it.
- Record the base SHA.
- Inventory staged, unstaged, and untracked files before editing.
- Do not touch a feature worktree or a dirty main checkout.
- Never set or change `GRADLE_USER_HOME`.
- Never run `gradle --stop` or `./gradlew --stop`.
- If a task-owned Gradle process must be stopped, first prove the exact PID, cwd, and command belong to this worktree; terminate only that process.

### 2. Build a per-test audit

Inventory every `@Test` under `app/src/androidTest`. For each test record:

- file and test name;
- verified issue URL;
- verified fixing or introducing PR URL;
- classification: `REGRESSION`, `CONTRACT`, `SMOKE`, or `UNVERIFIED`;
- why an Android device is necessary;
- overlapping cheaper coverage;
- decision: keep, migrate, consolidate, or delete;
- evidence for that decision.

Use git history as the starting point, then read the linked issue and PR. Search by the test name, nearby production symbols, commit SHA, and user-visible behavior. A filename containing an issue number is a clue, not proof.

### 3. Apply the deletion gate

Use this order for every test:

1. If it covers a real historical bug, keep it. Consolidation is allowed only when another retained test proves the same regression at least as strongly and carries the same provenance.
2. If it protects a current product contract, keep it only when the assertion genuinely needs Android/device behavior.
3. If the contract is valuable but device execution is unnecessary, migrate the coverage to the cheapest appropriate JVM/common test before removing the instrument version.
4. If it only captures a one-time visual preference, implementation detail, duplicate fixture plumbing, or behavior already proven more strongly elsewhere, remove it.
5. If provenance or equivalence is uncertain, keep it pending more evidence. Uncertainty is not permission to delete.

`SMOKE` is an audit classification, not an exemption. A smoke test can remain permanent only when a verified issue and PR document why that device-level signal matters. A pure reducer, mapping, or list transform cannot be retained by renaming it a smoke test.

### 4. Review test quality without weakening coverage

For retained tests:

- add the verified provenance block;
- assert user-observable behavior, not only internal state or that composition did not crash;
- replace fixed sleeps with semantics, idling, clock control, or explicit state waits;
- keep fixtures deterministic and local when the behavior does not require the network;
- split unrelated behaviors so a failure identifies one contract;
- remove duplicate setup only when the remaining support object carries a real shared contract.

Do not create helper layers that merely rename a single Compose assertion or navigation call.

If a proven regression remains flaky after a scoped repair attempt, keep its code and provenance. Temporary isolation is allowed only with a new follow-up issue that records the failure mode, owner, restoration condition, and affected CI lane; never silently disable it or delete it to make checks green.

### 5. Validate proportionally

After edits:

1. Run the provenance audit command from the reference.
2. Run `git diff --check`.
3. Run formatting.
4. Compile the affected androidTest source set if the project exposes a suitable task.
5. Run only targeted instrument tests when device-specific behavior or a changed fixture needs execution.
6. Let GitHub CI run the complete suite and follow it to a terminal result when CI is the acceptance boundary.

Do not describe an in-progress check as green or a compiled test as behaviorally executed.

### 6. Publish an auditable cleanup PR

The Chinese PR body must include:

- base SHA and the exact instrument-test inventory before/after;
- a keep/migrate/consolidate/delete count;
- every deleted test and why it passed the deletion gate;
- the issue/PR pair and classification for every retained test;
- confirmation that no historical regression coverage was deleted;
- the validation actually completed and checks still running;
- a note that the PR contains no product behavior change.

Read the PR back after creation and verify title/body language, head/base, diff scope, and mergeability.

## Stop Conditions

Stop deletion and gather more evidence when:

- an issue or PR link cannot be verified;
- a test name suggests a regression but history is unclear;
- cheaper coverage looks similar but does not assert the same user-visible outcome;
- removing a fixture would silently reduce several retained tests;
- the only reason to delete is suite duration or flakiness.

When blocked, keep the test and document the unresolved provenance. Conservative retention is cheaper than silently reintroducing a known bug.
