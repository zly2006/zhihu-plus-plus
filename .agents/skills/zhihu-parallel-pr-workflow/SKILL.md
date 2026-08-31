---
name: zhihu-parallel-pr-workflow
description: Coordinate Zhihu++ issue implementation and pull requests when the user explicitly asks for subagents or when multiple independent issues or scopes have real parallel value. Covers issue trust gates, isolated worktrees, ownership-aware process handling, build and AVD validation, review, Chinese PR publication, and final evidence. Do not use merely because one narrow issue needs one PR.
---

# Zhihu++ Parallel PR Workflow

Use this workflow to shorten independent Zhihu++ issue work without weakening evidence or ownership boundaries.

## Decide whether to delegate

- Keep one narrow issue in the main agent unless the user explicitly requests delegation.
- Delegate only independent scopes that can progress concurrently. One cross-platform capability is one scope; do not split its common declaration, callers, and platform implementations among workers.
- Give each worker exactly one issue or tightly coupled capability and one worktree.
- If work becomes serial, end coordination overhead and let the main agent continue, unless the user explicitly assigned implementation or PR publication to the worker.
When the user explicitly requests implementing the latest independent issues in parallel, and those issues contain owner-authored (`to agent:`) directions, parallel delegation is the default. Assign one complete issue/capability to each worker and keep the main agent focused on coordination, review, and final acceptance. This does not relax any trust, version, reproduction, validation, or publication gates below. Mentioning this skill alone is not authorization to use subagents.

## Apply project gates first

1. Record the start time.
2. Read the repository `AGENTS.md` and any instructions under the files in scope.
3. Before creating a branch or worktree, apply the issue trust and information gates from `AGENTS.md`: identify every requirement's author, verify the reported app version and evidence, and ignore unverified solution proposals from non-owner users.
4. Check current work, overlap, and topology:
   - `git status --short --branch`
   - `git worktree list --porcelain`
   - open PRs that may touch the same behavior
   - the current `origin/master`
5. Define the user-reachable success state, minimum data flow, request budget, validation surface, and files owned by each worker.

Do not start implementation when the issue gate requires a warning comment, current-version reproduction, or more information. Follow the comment, close, unsubscribe, and read-back rules in `AGENTS.md` exactly.

## Create isolated worktrees

Create worktrees only under the repository's `.worktrees/` directory and only after the issue passes its gate.

```bash
git fetch origin master --prune
git worktree add .worktrees/<short-name> origin/master -b codex/<short-name>
cp local.properties .worktrees/<short-name>/local.properties 2>/dev/null || true
```

Resolve the absolute target before creation and verify it is inside `<repo>/.worktrees/`. Never edit the user's dirty main checkout for issue implementation.

## Assign ownership

Tell every worker:

- the absolute worktree path, issue, acceptance criteria, and owned files or capability;
- to read `AGENTS.md` before editing;
- that other people and agents share the repository, so it must preserve unrelated changes and never revert work it does not own;
- to read `docs/ai-ui-design-guide.md` and `NavDestination.kt` before UI, navigation, button, or settings changes;
- to keep one complete cross-platform contract in one worker;
- to remove thin forwarding helpers and avoid speculative compatibility branches;
- to return the diff, validation evidence, risks, and screenshot path before committing so the main agent can review.

The main agent coordinates and reviews worker-owned scopes. It must not silently implement, commit, push, or publish a worker-owned scope when the user explicitly assigned those actions to the worker.

## Handle processes by ownership

Process cleanup is an ownership decision, not an executable-name blacklist.

- Never run `gradle --stop` or `./gradlew --stop`; their scope can include builds owned by other agents.
- Never terminate a process merely because it is named Gradle, Java, Kotlin, emulator, or ADB.
- It is valid to interrupt or terminate a process proven to belong to this task by its exact PTY session, PID and parent tree, unique worktree path, or unique command signature.
- Prefer interrupting the exact foreground session. Use `pkill` only when its pattern uniquely identifies this task and cannot match another worker.
- Before and after termination, read back the target process state. Do not claim cleanup from the command exit code alone.
- Do not add build flags, environment overrides, or cache isolation rules without evidence that they solve a real failure. In particular, do not prescribe a Gradle user home, daemon mode, or Kotlin compiler execution strategy in this workflow.

## Implement and validate

Use the repository's required order:

```bash
./gradlew assembleLiteDebug
./gradlew ktlintFormat
```

- Add only the smallest focused compile or test task needed for the changed behavior.
- Do not run the complete instrument test suite locally unless the user explicitly requests it. Use targeted device tests or CI for device-only behavior.
- Separate one-time acceptance evidence from durable regression coverage. Verify low-risk visual styling once with the real UI and a screenshot; add an instrument test only when repeated device execution protects a stable behavior contract whose regression risk justifies emulator cost and timing fragility.
- Do not add Gradle flags as ceremony. Use a flag only when current evidence requires it, and report its effect.
- For API-dependent features, trigger `zhihu-reproduce` and obtain the real request/response and decode matrix required by `AGENTS.md` before designing fallbacks.
- For UI changes, use a healthy matching AVD, install the built APK, restore the approved test login state when needed, dump semantics before interaction, verify the changed state after interaction, and capture a real final screenshot.
- Read `off-android-avd-ci-debug` only if the remote AVD is actually selected. Keep all ADB work on the selected host and clean up only the emulator session owned by this task.
- Do not substitute build success, a non-empty file, process liveness, health checks, or a single HTTP 200 for the requested product success state.

## Review before commit

Review the complete diff against `origin/master` before approving a commit:

Never amend or rebase a commit that is already present on a pull-request branch. Fix follow-ups with a new commit so the PR history remains auditable; only rewrite an unshared local commit when no PR or remote branch contains it.

- intended behavior and default values;
- duplicated logic, unused code, and thin helpers;
- settings keys with real runtime reads and searchable/highlightable UI entries;
- navigation and back-stack semantics;
- visible labels or counts whose destination URL was dropped;
- title or content truncation introduced while adding controls;
- cross-platform implementations required by a common contract;
- tests that prove the changed behavior rather than only setup or persistence;
- unexpected binaries, generated artifacts, secrets, or unrelated files.

When merging overlapping visual regression tests, reconcile their asserted pixel regions with the final layout before keeping both assertions. An overlay such as a badge may intentionally cover a corner of its parent content box; the parent-background assertion must exclude the covered region while the overlay gets its own geometry and pixel assertions. Do not combine test bodies mechanically and claim the feature is preserved without checking the final bounds relationship.

Return blocking findings to the same worker. After approval, let the owner commit and continue publication.

## Publish the PR

- Base the branch on current `origin/master` and keep unrelated feature branches out.
- Use a Chinese title and body. Prefix the title with `feat:`, `fix:`, or `refactor:` according to the product change relative to baseline.
- Include `Resolves #<issue>` when the PR resolves the supplied issue.
- For visible UI changes, include a screenshot from the actual app, AVD, or reproducible UI render.
- Describe the behavior, reason, scope, validation commands and results, screenshot source, and any genuine boundary.
- Read the PR back and verify title prefix and language, head/base, issue linkage, screenshot rendering, and check status. Never describe checks that are still running as green.
- For every added or changed test, the PR body must explicitly record whether red-to-green verification against the pre-fix and fixed implementations was completed, including commands and terminal results or a precise blocker. Do not describe compilation or a pending CI check as red-to-green evidence.

Workers create draft PRs themselves.

## Finish

1. Clean up only task-owned AVD sessions and foreground processes; preserve other agents' work.
2. Recheck the worker worktree, published commit, remote branch, PR body, and validation evidence.
3. Record the end time and calculate elapsed runtime.
4. If runtime exceeds five minutes, send the repository-required `terminal-notifier` message.
5. Report each branch and PR, validation terminal state, screenshot, and any unresolved evidence gap.
