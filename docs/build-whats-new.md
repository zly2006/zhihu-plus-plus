# Build Whats-New Guide

Tracked copy of the Trellis frontend contract.

See also: `.trellis/spec/frontend/flutter-build-whats-new.md`.

## Rule

Every user-facing feature/fix commit must update immersive 「本次更新说明」:

1. Edit `shared/src/commonMain/kotlin/com/chloemlla/zhplus/onboarding/WhatsNewCatalog.kt`.
2. Detection uses `BuildIdentity.commitHash` + `BuildIdentity.buildTimeUtcMillis`.
3. Dynamic labels for hash/time; never hard-code live identity into static strings.
4. First-install long-form education stays in product onboarding pages; per-build notes go to WhatsNew only.

Skip for pure docs/CI/format/lockfile-only changes.
