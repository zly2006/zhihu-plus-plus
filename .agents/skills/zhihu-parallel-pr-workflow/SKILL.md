---
name: zhihu-parallel-pr-workflow
description: Coordinate Zhihu++ issue and PR implementation through subagents with isolated git worktrees, appropriately selected Android AVD validation, real screenshots, Chinese PR creation, and main-agent-only review/coordination. Use when the user asks to fan out issues, maximize parallel subagent work, implement multiple Zhihu++ features or fixes, optionally validate on `$off-android-avd-ci-debug`, or automatically open PRs from worker branches.
---

# Zhihu Parallel PR Workflow

## Trigger Boundary

This workflow is only for work with real parallel value: multiple independent issues or PRs, an explicit user request for subagents/delegation, or independently executable scopes that materially reduce wall-clock time. Do not trigger it merely because a task references one GitHub issue or will end in one PR. A single narrow issue should stay with the main agent, including implementation, validation, screenshots, and PR publication, unless the user explicitly asks to delegate it.

Example: adding one menu action and adjusting that menu's sheet behavior is one bounded UI change. Spawning a worker only adds handoff and review overhead, so the main agent should complete it directly.

## Core Contract

Use this skill for high-throughput Zhihu++ issue work. The main agent must coordinate; worker subagents must implement.

- The main agent may triage issues/PRs, assign scopes, create worktrees, track remote AVD capacity, review returned diffs, and summarize status.
- The main agent must not directly implement code, commit, push, or create PRs for a worker-owned issue unless the user explicitly overrides this boundary.
- Each worker owns exactly one issue or one tightly related issue bundle, in exactly one branch/worktree.
- Workers are not alone in the codebase: tell them not to revert unrelated changes, and to keep write scopes disjoint from other workers.
- Prefer many independent workers over one broad worker, but do not assign overlapping files or features to multiple workers.

## Daemon Ban

Do not use any Gradle, Kotlin compiler, build, watch, or long-lived helper daemon in the main checkout or any worker worktree. Daemon processes retain heap across parallel workers and can freeze the machine.

- Every Gradle command in this workflow must use `--no-daemon`.
- Every Gradle command that compiles Kotlin must also pass `-Dkotlin.compiler.execution.strategy=in-process` so it does not start a Kotlin compiler daemon.
- Prefer bounded one-shot commands only. Do not run `--continuous`, watch mode, dev servers, background Gradle processes, or any command intended to stay resident.
- Before a batch and after heavy validation, stop existing Gradle daemons from a writable isolated worktree with `./gradlew --stop || true`. This cleanup command is allowed because it terminates daemons rather than relying on them; never run it in a main checkout rejected by **Main Checkout Drift Guard**.
- Worker prompts must repeat this rule explicitly; if a worker reports validation without `--no-daemon`, send it back to rerun validation correctly.

## Startup

1. Record start time with `date '+%Y-%m-%d %H:%M:%S %Z'`.
2. Read repo instructions that apply to `/Users/zhaoliyan/IdeaProjects/Zhihu`, especially `AGENTS.md`.
3. Choose the lowest-cost healthy AVD that matches the target API. Read `$off-android-avd-ci-debug` only when remote validation is selected.
4. Capture the main checkout baseline as required by **Main Checkout Drift Guard**, then inspect worktrees, open PRs, and open issues:
   - `git worktree list --porcelain`
   - `gh pr list --state open --limit 80 --json number,title,headRefName,baseRefName,isDraft,url`
   - `gh issue list --state open --limit 80 --json number,title,labels,updatedAt,url`
5. If remote validation is selected, run its health checks once per batch:
   - `/Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/scripts/off-avd-ci-debug.sh status`
   - `/Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/scripts/off-avd-ci-debug.sh boot-check`
   - If boot-check fails, inspect remote logs before assigning UI verification.
6. Stop any existing Gradle daemons from a writable isolated worktree before assigning workers:
   - `./gradlew --stop || true`

## Main Checkout Drift Guard

Treat the canonical main checkout as concurrently owned state, not as a stable workspace. At startup, record its exact branch, HEAD, and status outputs; an empty status output is the only clean state:

```bash
git -C /Users/zhaoliyan/IdeaProjects/Zhihu branch --show-current
git -C /Users/zhaoliyan/IdeaProjects/Zhihu rev-parse HEAD
git -C /Users/zhaoliyan/IdeaProjects/Zhihu status --porcelain=v1 --untracked-files=all
```

Re-run and compare all three commands after triage, immediately before assigning a worker or starting implementation, immediately before every intended write in the main checkout, at the start of main-agent review, and again during shutdown. An initially clean checkout is not a lasting guarantee; each check authorizes only the next action.

If the status is non-empty at any check, branch/HEAD/status differs from the baseline, or another person/process modified the checkout, do not edit, format, stage, stash, reset, clean, switch, commit, or otherwise write there. Preserve all user and other-agent changes, and immediately create or use a fresh isolated worktree from the current `origin/master` using **Worktree Rules**. Existing workers always remain in their assigned worktrees; main-checkout drift never moves or redirects them.

## Candidate Selection

Choose issues critically, not literally. Rank by user impact, feasibility, current PR overlap, blast radius, and testability.

- Skip or defer issues already covered by open PRs unless the task is to review or replace that PR.
- Split unrelated issues across workers.
- Bundle only when the same files and UX path are clearly shared.
- Prefer bug fixes and narrow UX wins before vague platform rewrites.
- For UI/nav/settings changes, require workers to read `docs/ai-ui-design-guide.md` and `NavDestination.kt` before editing.

## Worktree Rules

Create one worktree per worker from current `origin/master`. These commands update repository/worktree metadata without changing files in the main checkout:

```bash
ZH_MAIN=/Users/zhaoliyan/IdeaProjects/Zhihu
git -C "$ZH_MAIN" fetch origin master --prune
git -C "$ZH_MAIN" worktree add -b codex/<short-name> "$ZH_MAIN/.worktrees/<short-name>" origin/master
cp "$ZH_MAIN/local.properties" "$ZH_MAIN/.worktrees/<short-name>/local.properties" 2>/dev/null || true
```

Use unique branch names such as:

- `codex/issue-444-account-history`
- `codex/issue-445-main-tab-reselect`
- `codex/issue-440-content-block-spacing`

Never give two workers the same worktree. Never let a worker edit the main checkout unless the user explicitly says so.

## Worker Prompt Template

Give each worker a self-contained prompt:

```text
Use the Zhihu++ repo at <absolute worktree path>. You own issue #<number>: <title>.

Hard rules:
- Read AGENTS.md before editing.
- For UI/nav/settings work, read docs/ai-ui-design-guide.md and NavDestination.kt before editing.
- Do not touch unrelated files or revert changes you did not make.
- Keep helpers only when they carry real behavior; remove thin wrappers.
- Implement in this worktree only.
- Do not use any daemon: no Gradle daemon, Kotlin compiler daemon, build daemon, watch mode, dev server, or long-lived helper process.
- Validate with ./gradlew --no-daemon -Dkotlin.compiler.execution.strategy=in-process assembleLiteDebug, then ./gradlew --no-daemon -Dkotlin.compiler.execution.strategy=in-process ktlintFormat.
- If you run any additional Gradle command, it must also include --no-daemon and -Dkotlin.compiler.execution.strategy=in-process.
- If UI changed, use a healthy local or remote AVD for install/launch/UI dump or screenshot. After choosing off, keep all ADB commands in the remote environment.
- Produce a real screenshot for PR description if the UI is visible.
- Commit, push, and open a draft PR with Chinese title/body. Title must start with feat:, fix:, or refactor:. Include Resolves #<number> when appropriate.

Task:
<issue body, product judgement, acceptance criteria, known open PR overlaps, disjoint write scope>

Return:
- branch, commit, PR URL
- files changed
- validation commands and results
- screenshot path or PR asset URL
- risks or intentionally deferred parts
```

## AVD Validation

Choose an existing healthy local AVD when it is faster or better matches the target API. Choose `off` when remote isolation, KVM resources, API 35, or reduced local pressure materially helps. If `off` is selected, use it as a short-lived runner only and do not run multiple emulator sessions concurrently.

Minimum UI validation for each UI worker:

1. Build APK locally in the worker worktree with:
   `./gradlew --no-daemon -Dkotlin.compiler.execution.strategy=in-process assembleLiteDebug`
2. Start the selected local AVD, or a short-lived remote AVD session using `$off-android-avd-ci-debug`.
3. Install `app/build/outputs/apk/lite/debug/app-lite-debug.apk`.
4. Launch `com.github.zly2006.zhplus.lite`.
5. Wait for content; if login/disclaimer blocks the path, restore account JSON using the project launch-on-device instructions or state the blocker precisely.
6. Use `.agents/skills/ui-test/llm_test_helper.py dump` before taps when practical.
7. Capture a screenshot from the actual AVD and place it in the worker worktree, for example `artifacts/issue-<n>-final.png`.
8. If `off` was selected, kill the remote emulator after validation:
   `/Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/scripts/off-avd-ci-debug.sh kill`

If the selected runner cannot perform a needed step, switch to another healthy AVD when practical or report the exact validation gap; do not add remote infrastructure solely to satisfy a preference rule.

## PR Requirements

Workers create draft PRs themselves.

- Base must be `master`.
- Sync from latest `origin/master` before branch creation.
- PR title/body must be Chinese.
- Title prefix must be `feat:`, `fix:`, or `refactor:`.
- After creating or editing a PR, read it back with `gh pr view` and explicitly verify the title prefix instead of trusting the worker's summary.
- Include `Resolves #<issue>` when the PR closes the issue.
- UI PR descriptions must include final effect screenshots from a real app run, AVD, or reproducible UI render.
- Do not create one mega PR for unrelated issue work.

When an upstream UI module supplies both display data and a destination URL, review the state projection and interaction together. Do not preserve only the visible label or count while silently discarding the action target; keep the URL in UI state, make the relevant container actionable through the project's established URL opener, and verify click semantics. For example, a social profile row with a count and profile link is incomplete if it renders the count but cannot open the profile.

## Main-Agent Review

After a worker returns:

1. Re-run **Main Checkout Drift Guard** before review; if it fails, preserve the main checkout, create or use a fresh isolated coordination worktree from current `origin/master`, and inspect each worker only in its assigned worktree.
2. Inspect `git status`, `git show --stat`, and the PR diff.
3. Check for overlap with other worker branches and open PRs.
4. Review for:
   - thin helper/wrapper regressions
   - duplicated logic
   - stale comments or wrong doc-comment style
   - broken navigation semantics
   - settings keys without runtime reads
   - UI text truncation or layout regressions
5. Confirm validation evidence and screenshot are real.
6. Read the published PR back and verify its title matches `^(feat|fix|refactor): `, its body language and issue linkage are correct, and any visible data carrying a destination URL remains actionable.
7. If a worker missed requirements, send it back to fix in the same worktree.

## Shutdown

1. Re-run **Main Checkout Drift Guard** and preserve any observed drift; never clean the main checkout to make the final check pass.
2. If `off` was used, ensure remote AVD cleanup:
   `/Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/scripts/off-avd-ci-debug.sh kill`
3. Stop Gradle daemons from every worker worktree; include the main checkout only when its final drift check is still clean and unchanged:
   `./gradlew --stop || true`
4. Record end time with `date '+%Y-%m-%d %H:%M:%S %Z'`.
5. If total runtime exceeds 5 minutes, notify:
   `terminal-notifier -message "已完成 Zhihu++ 并行 PR 工作" -sound default`
6. Final response must list each worker branch/PR, validation state, screenshots, and any blocked items.
