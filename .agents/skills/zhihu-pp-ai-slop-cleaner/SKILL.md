---
name: zhihu-pp-ai-slop-cleaner
description: Use for Zhihu++ maintenance work that scans Kotlin main sources for low-call functions, structurally similar function bodies, dead code, pure forwarding wrappers, pointless abstractions, repeated helpers, and cross-platform duplicate glue before refactoring or PR cleanup. Trigger when the user asks to clean AI slop, remove low-call wrappers, find similar or duplicated code, review duplicated helper functions, or audit functions with call count at most 2 in /Users/zhaoliyan/IdeaProjects/Zhihu.
---

# Zhihu++ AI Slop Cleaner

## Core Rule

Treat low call count as a queue for review, not proof of deletion.

Delete or inline a function only after inspecting its declaration, every real call site, and the surrounding contract. Keep small functions that are framework entry points, stable UI/test selectors, platform contracts, interface defaults, Room/serialization hooks, navigation hooks, or meaningful domain boundaries.

`Runtime` / UI `State` dependency bundles are migration debt, not contracts. When a screen or shared UI exposes a `*Runtime`, `*State`, `remember*Runtime`, or similar object that only aggregates ViewModels, settings snapshots, environment methods, callbacks, network/database access, or platform helpers, dismantle it unconditionally. Move each responsibility to the real owner: common UI selects common ViewModels directly, common code calls cross-platform network/database helpers directly, and platform-only effects use narrow expect/actual functions or existing composition locals. Do not preserve a runtime field just because it currently carries login, update, install metadata, debug flags, or another “platform service”; split that service into the smallest real platform primitive or inline it at the caller.

When merging duplicated platform implementations, do not stop at moving the duplicated body into an environment/interface default if there is still only one real semantic caller. If a ViewModel is the only place that owns the workflow, put the request logic in that ViewModel and let the environment expose only lower-level capabilities such as authenticated cookies and signed requests. Example: a question follow action should live in the question feed ViewModel that catches its errors, not as a one-call `environment.follow...()` wrapper that only chooses POST or DELETE.

Do not push platform or storage dependencies upward just because the current accessor is only convenient from UI code. If a lower-level navigation or filtering component owns the query, make the cross-platform dependency available at that level instead of threading a database or platform handle through screen and ViewModel calls. Example: answer switching should ask its own support layer for already-opened content, not force the article loading call to accept a database parameter that exists only to be forwarded.

Do not delete runtime guards just to make tests pass or to simplify state. If a throttle, retry limiter, debounce, permission gate, or cache invalidation guard protects production behavior, keep it and make tests reset or inject the relevant state explicitly. Example: an authenticated request refresh throttle should be reset in tests; removing the throttle changes runtime semantics.

When the user asks for a second-stage or broad cleanup, do not continue with tiny one-helper batches. Put the cleanup branch in its own worktree so the main checkout remains available, then process whole-file helper clusters at once. A valid batch should remove or inline at least one complete file's helper cluster and cover at least 30 helper functions unless the user explicitly narrows the scope. If one suspicious helper is found, inspect its surrounding file-level cluster and remove adjacent useless wrappers together instead of deleting one isolated function per commit.

## Workflow

1. Record the current time and inspect `git status --short --branch`.
2. Run the low-call scan:

```bash
python3 .agents/skills/zhihu-pp-ai-slop-cleaner/scripts/scan_low_call_functions.py \
  --root . \
  --max-calls 2 \
  --output /tmp/zhihu_low_functions.tsv
```

3. Run the similar-function scan when looking for repeated code that is not visible from call counts:

```bash
python3 .agents/skills/zhihu-pp-ai-slop-cleaner/scripts/find_similar_kotlin_functions.py \
  --root . \
  --threshold 0.82 \
  --output /tmp/zhihu_similar_functions.tsv
```

4. Review the TSVs from highest-confidence candidates first.
5. For each candidate, verify with `rg` before editing:

```bash
rg -n "\bFUNCTION_NAME\b" app shared desktopApp -g '*.kt'
```

6. Classify each candidate:
   - `delete`: no production caller and no framework/test/runtime entry value.
   - `inline`: function body is a pure forwarding call, local one-use wrapper, or renaming shell.
   - `merge`: repeated helpers perform the same operation; replace with one shared helper or add a parameter.
   - `keep`: function is a real contract, domain boundary, parser step, platform actual, override, DAO method, serializer, stable test tag, or improves readability of a complex expression.
   - Runtime/state glue is not classified as `keep`; delete or inline the whole bundle and all of its actual/expect shells.

7. When a candidate lives in a file that already has several related helpers, switch to a file-level pass before editing. Read the nearby functions and classify the whole helper cluster together instead of deleting one `rg` hit at a time. Example: if one signed request helper in an environment file looks unnecessary, inspect adjacent signed helpers in the same file; keep multi-call primitives, but inline a single-call wrapper that only forwards to the lower-level client and adds no contract.
8. Edit only after classification. Prefer the nearest existing API over creating a new helper.
9. Re-run the relevant scan after each batch and check that deleted or merged names disappeared.
10. Run project verification in the required order before committing:

```bash
./gradlew assembleLiteDebug
./gradlew ktlintFormat
```

11. Run a final review pass:

```bash
git diff --check
git diff --stat
rg -n "TODO|unused|deprecated wrapper|pure forwarding" app shared desktopApp -g '*.kt'
```

## What To Remove Aggressively

- Private or local functions with one call and a body under 10 lines that simply call another function.
- Public/internal functions used only by tests when production should instead test the lower-level encoder, decoder, URL builder, or data type directly.
- Same-file helper names that only rename an environment/platform method with no branch, state, permission, or lifecycle boundary.
- Cross-platform duplicate helpers that do identical formatting, parsing, URL normalization, or ID construction.
- Dead debug helpers, stale reset hooks, stale bulk methods, and comment-referenced methods with no production caller.
- `*Runtime`, `*State`, `remember*Runtime`, and UI dependency-bundle fields, even when they still have multiple call sites. Treat each field as a forwarding layer to eliminate, not as a surface to preserve.

## Merge Conflict Boundary

During slop cleanup, do not reintroduce platform/environment state just because `master` has a newer conflict-side implementation. If the cleanup intentionally pushed a cache/throttle into a lower-level helper to keep API surface small, preserve that direction when resolving conflicts. Example: when a lower-level authenticated request helper owns a short refresh throttle, do not add per-platform `last...` getters/setters back onto the environment interface merely to satisfy one merge side; keep the throttle at the lower layer and adapt the conflicting code around it.

After a user points out one bad conflict direction, audit the whole merge intersection before continuing. Check every file changed on both sides, search for deleted helper families that may have been reintroduced under the same or similar names, and verify new master-side behavior is still present. Example: if a conflict around an environment interface accidentally brings back one state accessor, also inspect adjacent files for old repositories, URL helpers, wrapper methods, and newly added feature interfaces instead of fixing only the named accessor.

## What To Keep

- `override`, `expect`, `actual`, `@Serializable` serializer methods, `NavType`, Room DAO, Room converter, migration, lifecycle callback, and JavaScript bridge entry points unless source inspection proves they are unused and removable.
- Interface default implementations that are part of a capability contract.
- Stable UI test tags and semantic tag builders, even if call count is one.
- Parser helpers where the name explains a non-obvious grammar or Markdown/HTML rule.
- UI composables that isolate a meaningful visual unit rather than only forwarding parameters.
- Platform code that intentionally differs by Android/JVM/native behavior.
- These keep rules do not protect runtime/state dependency bundles or their expect/actual constructors.

## Scan Script Notes

`scripts/scan_low_call_functions.py` is intentionally conservative:

- It scans main source roots only by default.
- It excludes `test`, `androidTest`, build output, and worktrees unless options change that.
- It groups `expect`/`actual` declarations by name and arity so the multiplatform declaration family is counted once.
- It masks comments and strings before counting references.
- It still cannot know Kotlin reflection, framework dispatch, Java interop, or generated-code calls. Always inspect candidates manually.

Useful options:

```bash
python3 .agents/skills/zhihu-pp-ai-slop-cleaner/scripts/scan_low_call_functions.py --help
python3 .agents/skills/zhihu-pp-ai-slop-cleaner/scripts/scan_low_call_functions.py --include-tests
python3 .agents/skills/zhihu-pp-ai-slop-cleaner/scripts/scan_low_call_functions.py --max-calls 0
```

`scripts/find_similar_kotlin_functions.py` finds similar bodies by normalized token shingles:

- It normalizes non-keyword identifiers to `ID`, numbers to `NUM`, and string/char literals to `STR`/`CHAR`.
- It compares function bodies with shingled Jaccard similarity.
- It defaults to main source roots, excludes tests, ignores tiny bodies, and outputs only high-score pairs.
- It is useful for repeated formatters, URL builders, platform stubs, and copied UI glue.
- It can false-positive on framework callbacks and similar Compose layout skeletons; inspect before merging.

Useful options:

```bash
python3 .agents/skills/zhihu-pp-ai-slop-cleaner/scripts/find_similar_kotlin_functions.py --help
python3 .agents/skills/zhihu-pp-ai-slop-cleaner/scripts/find_similar_kotlin_functions.py --threshold 0.9
python3 .agents/skills/zhihu-pp-ai-slop-cleaner/scripts/find_similar_kotlin_functions.py --min-lines 8 --min-tokens 60
```

## PR Hygiene

When the cleanup becomes a PR:

- Do not use broad `git add .`; stage explicit paths.
- Do not commit unrelated user changes.
- Keep PR title/body in Chinese and use `refactor:` unless the user asks otherwise.
- Include the scan command, validation commands, and any deliberately kept categories in the PR body.
