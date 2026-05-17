---
name: release-markdown-fork
description: Release the local Markdown fork used by Zhihu++. Use when you need to merge upstream huarangmeng/Markdown into the zly2006 fork master, preserve only approved fork deltas, publish a new Maven Central alpha, update Zhihu's Android dependency versions, and verify the build. Applies specifically to /Users/zhaoliyan/IdeaProjects/Zhihu and its .tmp/Markdown-zly2006 fork checkout.
---

# Release Markdown Fork

Use this skill for the Zhihu-local Markdown fork release workflow.

## Inputs

Required:

- Target version, for example `0.0.1-alpha.N` (check it in gradle.properties and compute the next N)
- Fork checkout: `/Users/zhaoliyan/IdeaProjects/Zhihu/.tmp/Markdown-zly2006`
- Main project checkout: `/Users/zhaoliyan/IdeaProjects/Zhihu`

Optional:

- Proxy for Sonatype and repo1 access:
  - `https_proxy=http://127.0.0.1:7890`
  - `http_proxy=http://127.0.0.1:7890`
  - `all_proxy=socks5://127.0.0.1:7890`

## Guardrails

- Prefer preserving existing published coordinates. Do not switch the app to Android-only coordinates unless publishing KMP root coordinates is impossible.
- Fork-specific behavior should be reduced to the minimum approved delta. Current approved deltas:
  - `NativeBlock` support
  - `mathFont: MathFont` field in `MarkdownTheme` (font CDN support)
  - Switched latex dependency from `io.github.huarangmeng` to `io.github.zly2006`
- Never use destructive git commands on a dirty tree.
- For Zhihu after dependency changes, run:
  1. `./gradlew assembleLiteDebug`
  2. `./gradlew ktlintFormat`

## Dependency order (CRITICAL)

The Markdown fork depends on `io.github.zly2006:latex-*`. **Before publishing Markdown, always verify the latex version it depends on is already published to Maven Central:**

```bash
# Check what latex version Markdown expects
grep 'latex =' gradle/libs.versions.toml

# Check if that version is on Maven Central
curl -s https://repo1.maven.org/maven2/io/github/zly2006/latex-renderer/maven-metadata.xml | grep '<release>'
```

If the expected version is NOT on Maven Central, run the `release-latex-fork` skill first.

## Workflow

### 1. Confirm branch state

In the fork checkout:

- `git fetch origin --prune`
- `git fetch zly --prune`
- Confirm `origin/master` is the upstream head.
- Confirm the release branch contains `origin/master`:
  - `git merge-base --is-ancestor origin/master <release-branch>`

If the release branch is ready, fast-forward the fork master branch that tracks `zly/master`, then push:

- `git checkout push-zly-master`
- `git merge --ff-only <release-branch>`
- `git push zly push-zly-master:master`

Proof to capture:

- `git branch -vv`
- `git log --oneline --decorate --graph --max-count=12 --all`

### 2. Keep only approved fork deltas

Diff against upstream:

- `git diff --name-status origin/master...HEAD`
- `git diff --stat origin/master...HEAD`

The intended final diff should be limited to:

- Publishing metadata changes for `io.github.zly2006`
- Version bump
- `NativeBlock` parser/renderer support and its tests

### 3. Publish to Maven Central

Prefer the normal Gradle route first:

- `./gradlew --no-daemon --stacktrace :markdown-parser:publishAndReleaseToMavenCentral`
- `./gradlew --no-daemon --stacktrace :markdown-runtime:publishAndReleaseToMavenCentral`
- `./gradlew --no-daemon --stacktrace :markdown-renderer:publishAndReleaseToMavenCentral`

If Sonatype communication is flaky, export the proxy first.

If a module uploads successfully but Gradle fails while stopping `maven-central-build-service`, treat the deployment status as the source of truth, not the Gradle exit code.

### 4. Query deployment status directly

Read `mavenCentralUsername` and `mavenCentralPassword` from `~/.gradle/gradle.properties`, then query:

- `POST https://central.sonatype.com/api/v1/publisher/status?id=<deployment-id>`

Use the proxy when needed.

Acceptable states:

- `PUBLISHED`: release is complete
- `PUBLISHING`: uploaded and being synced; keep polling
- `FAILED`: stop and report `errors`

### 5. Verify public availability

Check repo1 metadata and artifact URLs:

- `curl -s https://repo1.maven.org/maven2/io/github/zly2006/markdown-parser/maven-metadata.xml`
- `curl -s https://repo1.maven.org/maven2/io/github/zly2006/markdown-runtime/maven-metadata.xml`
- `curl -s https://repo1.maven.org/maven2/io/github/zly2006/markdown-renderer/maven-metadata.xml`

And confirm the exact POMs return `200`:

- `.../markdown-parser/<version>/markdown-parser-<version>.pom`
- `.../markdown-runtime/<version>/markdown-runtime-<version>.pom`
- `.../markdown-renderer/<version>/markdown-renderer-<version>.pom`

### 6. Update Zhihu dependency versions

Edit:

- `/Users/zhaoliyan/IdeaProjects/Zhihu/app/build.gradle.kts`

Current dependency style is direct Android artifacts:

- `implementation("io.github.zly2006:markdown-parser-android:<version>")`
- `implementation("io.github.zly2006:markdown-renderer-android:<version>")`

Update both to the new version.

### 7. Verify Zhihu

In `/Users/zhaoliyan/IdeaProjects/Zhihu` run:

- `./gradlew assembleLiteDebug`
- `./gradlew ktlintFormat`

Collect:

- success/failure
- the dependency lines after edit
- any blocking error if the new release is not yet visible

### 8. Commit Changes

Then you need to commit the new version. message: `build: bump markdown`. message body:
```text
Original version: (insert original version e.g. 1.x.x)
Fork version: (insert fork version e.g. 0.0.1-alpha.N)
```

DO NOT PUSH!

## Failure handling

### Sonatype upload hangs without returning deployment ID

- Retry with the proxy exported.
- Prefer direct Sonatype Publisher API upload of the generated bundle under `build/publish/*.zip` when Gradle upload is unstable.

### Gradle fails with `maven-central-build-service`

- Query the deployment status API.
- If status is `PUBLISHED` or `PUBLISHING`, do not re-version immediately.
- Only bump to a new alpha if Sonatype shows `FAILED` or the version never created a deployment.

### `renderer` lags while `parser` and `runtime` are already published

- Keep coordinates unchanged.
- Poll deployment status until `PUBLISHED`.
- Only update Zhihu after the renderer root artifact or Android artifact is publicly reachable.

## Final report checklist

- Fork master commit and push proof
- Diff summary vs `origin/master`
- Deployment IDs and Sonatype states
- Public Maven coordinates
- Repo1 availability proof
- Zhihu dependency update proof
- `assembleLiteDebug` result
- `ktlintFormat` result
