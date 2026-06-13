---
name: zhihu-pp-ai-slop-cleaner
description: Use for Zhihu++ maintenance work that scans Kotlin main sources for low-call functions, structurally similar function bodies, dead code, pure forwarding wrappers, pointless abstractions, repeated helpers, and cross-platform duplicate glue before refactoring or PR cleanup. Trigger when the user asks to clean AI slop, remove low-call wrappers, find similar or duplicated code, review duplicated helper functions, or audit functions with call count at most 2 in /Users/zhaoliyan/IdeaProjects/Zhihu.
---

# Zhihu++ AI Slop Cleaner

## Core Rule

Treat low call count as a queue for review, not proof of deletion.

Delete or inline a function only after inspecting its declaration, every real call site, and the surrounding contract. Keep small functions that are framework entry points, stable UI/test selectors, platform contracts, interface defaults, Room/serialization hooks, navigation hooks, or meaningful domain boundaries.

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

## What To Keep

- `override`, `expect`, `actual`, `@Serializable` serializer methods, `NavType`, Room DAO, Room converter, migration, lifecycle callback, and JavaScript bridge entry points unless source inspection proves they are unused and removable.
- Interface default implementations that are part of a capability contract.
- Stable UI test tags and semantic tag builders, even if call count is one.
- Parser helpers where the name explains a non-obvious grammar or Markdown/HTML rule.
- UI composables that isolate a meaningful visual unit rather than only forwarding parameters.
- Platform code that intentionally differs by Android/JVM/native behavior.

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
