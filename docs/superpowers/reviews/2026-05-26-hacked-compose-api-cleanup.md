# HackedComposeApi Cleanup

## Scope

- Deleted file: `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/components/HackedComposeApi.kt`.

## Evidence

- `rg` found no references to `HackedComposeApi`.
- The file only defined an opt-in annotation and did not contain runtime behavior, UI, navigation, or platform adapter code.

## Master Shape

- This is a dead-code cleanup, not a migration rewrite.
- No function body, route registration, UI structure, platform side effect, or visible text changed.
- Removing this unused Android-only marker reduces `shared/androidMain` residual UI-component debt without introducing a new abstraction.
