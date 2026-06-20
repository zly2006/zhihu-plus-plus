---
name: zhihu-parallel-pr-workflow
description: Coordinate Zhihu++ issue and PR implementation through subagents with isolated git worktrees, off-host Android AVD validation, real screenshots, Chinese PR creation, and main-agent-only review/coordination. Use when the user asks to fan out issues, maximize parallel subagent work, implement multiple Zhihu++ features or fixes, validate on `$off-android-avd-ci-debug`, or automatically open PRs from worker branches.
---

# Zhihu Parallel PR Workflow

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
- Before a batch and after heavy validation, stop existing Gradle daemons with `./gradlew --stop || true`. This cleanup command is allowed because it terminates daemons rather than relying on them.
- Worker prompts must repeat this rule explicitly; if a worker reports validation without `--no-daemon`, send it back to rerun validation correctly.

## Startup

1. Record start time with `date '+%Y-%m-%d %H:%M:%S %Z'`.
2. Read repo instructions that apply to `/Users/zhaoliyan/IdeaProjects/Zhihu`, especially `AGENTS.md`.
3. Read `$off-android-avd-ci-debug` before any UI or AVD validation.
4. Inspect current checkout, open PRs, and open issues:
   - `git status --short --branch`
   - `git worktree list --porcelain`
   - `gh pr list --state open --limit 80 --json number,title,headRefName,baseRefName,isDraft,url`
   - `gh issue list --state open --limit 80 --json number,title,labels,updatedAt,url`
5. Run remote AVD health checks once per batch:
   - `/Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/scripts/off-avd-ci-debug.sh status`
   - `/Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/scripts/off-avd-ci-debug.sh boot-check`
   - If boot-check fails, inspect remote logs before assigning UI verification.
6. Stop any existing Gradle daemons before assigning workers:
   - `./gradlew --stop || true`

## Candidate Selection

Choose issues critically, not literally. Rank by user impact, feasibility, current PR overlap, blast radius, and testability.

- Skip or defer issues already covered by open PRs unless the task is to review or replace that PR.
- Split unrelated issues across workers.
- Bundle only when the same files and UX path are clearly shared.
- Prefer bug fixes and narrow UX wins before vague platform rewrites.
- For UI/nav/settings changes, require workers to read `docs/ai-ui-design-guide.md` and `NavDestination.kt` before editing.

## Worktree Rules

Create one worktree per worker from current `origin/master`.

```bash
git fetch origin master --prune
git worktree add .worktrees/<short-name> origin/master -b codex/<short-name>
cp local.properties .worktrees/<short-name>/local.properties 2>/dev/null || true
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
- If UI changed, use $off-android-avd-ci-debug remote AVD for install/launch/UI dump or screenshot. Do not use local bare adb after choosing off.
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

## Remote AVD Validation

Use `off` as a short-lived runner only. Do not run multiple emulator sessions concurrently on `off`; coordinate AVD access in the main agent.

Minimum UI validation for each UI worker:

1. Build APK locally in the worker worktree with:
   `./gradlew --no-daemon -Dkotlin.compiler.execution.strategy=in-process assembleLiteDebug`
2. Start a short-lived remote AVD session on `off` using the environment from `$off-android-avd-ci-debug`.
3. Install `app/build/outputs/apk/lite/debug/app-lite-debug.apk`.
4. Launch `com.github.zly2006.zhplus.lite`.
5. Wait for content; if login/disclaimer blocks the path, restore account JSON using the project launch-on-device instructions or state the blocker precisely.
6. Use `.agents/skills/ui-test/llm_test_helper.py dump` before taps when practical.
7. Capture a screenshot from the actual AVD and place it in the worker worktree, for example `artifacts/issue-<n>-final.png`.
8. Kill the remote emulator after validation:
   `/Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/scripts/off-avd-ci-debug.sh kill`

If no remote interactive runner exists for a needed step, the worker must either add a small reusable runner script in its branch or mark remote UI validation blocked and still run build/format.

## PR Requirements

Workers create draft PRs themselves.

- Base must be `master`.
- Sync from latest `origin/master` before branch creation.
- PR title/body must be Chinese.
- Title prefix must be `feat:`, `fix:`, or `refactor:`.
- Include `Resolves #<issue>` when the PR closes the issue.
- UI PR descriptions must include final effect screenshots from a real app run, AVD, or reproducible UI render.
- Do not create one mega PR for unrelated issue work.

## Main-Agent Review

After a worker returns:

1. Inspect `git status`, `git show --stat`, and the PR diff.
2. Check for overlap with other worker branches and open PRs.
3. Review for:
   - thin helper/wrapper regressions
   - duplicated logic
   - stale comments or wrong doc-comment style
   - broken navigation semantics
   - settings keys without runtime reads
   - UI text truncation or layout regressions
4. Confirm validation evidence and screenshot are real.
5. If a worker missed requirements, send it back to fix in the same worktree.

## Shutdown

1. Ensure remote AVD cleanup:
   `/Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/scripts/off-avd-ci-debug.sh kill`
2. Stop Gradle daemons from the main checkout and every worker worktree:
   `./gradlew --stop || true`
3. Record end time with `date '+%Y-%m-%d %H:%M:%S %Z'`.
4. If total runtime exceeds 5 minutes, notify:
   `terminal-notifier -message "已完成 Zhihu++ 并行 PR 工作" -sound default`
5. Final response must list each worker branch/PR, validation state, screenshots, and any blocked items.
