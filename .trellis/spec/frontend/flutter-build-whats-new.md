# Build Whats-New Guide (KMP / Compose)

Every user-facing feature/fix commit must update immersive 「本次更新说明」 content.

## Detection

- Identity = `BuildIdentity.commitHash` + `BuildIdentity.buildTimeUtcMillis`.
- Persist last-seen as `whats_new_seen_commit` + `whats_new_seen_build_time` in `SettingsStore`.
- Show when current identity ≠ last-seen **and** first-install OSS/onboarding already completed.
- First-install path marks current identity seen when onboarding finishes (avoid double gate).

## Content source

- Edit `shared/src/commonMain/kotlin/com/chloemlla/zhplus/onboarding/WhatsNewCatalog.kt`.
- Prefer matching by commit hash prefix; optional `buildTimeUtcMillis` for disambiguation.
- Keep welcome / identity bullets dynamic via getters — never hard-code the live hash/time in static copy.
- Branch-long fork deltas → first-install `ProductOnboarding` pages.
- This-build notes → `WhatsNewCatalog` only.

## Skip

Pure docs / CI / format / lockfile changes with no user-visible app behavior.

## Related

- First-install OSS notice: `OpenSourceNoticeScreen` (not versioned per build).
- Full license dump: existing `OpenSourceLicensesScreen` (aboutlibraries).
