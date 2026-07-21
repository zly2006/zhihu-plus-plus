---
name: release-latex-fork
description: Release the LaTeX fork used by Zhihu++. Use when merging the latest huarangmeng/latex release into zly2006/latex, preserving approved fork deltas, publishing Maven Central artifacts, updating Zhihu's vendored third_party/markdown and direct LaTeX dependencies, and verifying the app.
---

# Release LaTeX Fork

Use this skill for the Zhihu-local LaTeX fork release workflow.

## Dependency chain

```
huarangmeng/latex (upstream)
    -> zly2006/latex (published fork)
        -> Zhihu/third_party/markdown (vendored hard fork)
        -> Zhihu app and shared modules
```

The old standalone `zly2006/Markdown` Maven release line is retired. Do not clone,
update, or publish a separate Markdown fork. The required order is:

1. Publish the LaTeX fork.
2. Update every `io.github.zly2006:latex-*` dependency in the Zhihu checkout,
   including `third_party/markdown`.
3. Build and format Zhihu.

## Inputs

Required:

- Fork checkout: `/Users/zhaoliyan/IdeaProjects/latex`
- Zhihu checkout: `/Users/zhaoliyan/IdeaProjects/Zhihu`
- Target version derived from the latest upstream tag. If the plain `-zly`
  version already exists for the same upstream tag, increment the fork suffix,
  for example `1.4.7-zly2`.

Optional proxy for Sonatype and repo1 access:

- `https_proxy=http://127.0.0.1:7890`
- `http_proxy=http://127.0.0.1:7890`

## Guardrails

- Publish under `io.github.zly2006`, never `io.github.huarangmeng`.
- `Original version` means the upstream `huarangmeng/latex` tag. `Fork version`
  means the version published by zly2006.
- Never use destructive git commands on a dirty tree.
- Published modules are `latex-base`, `latex-parser`, and `latex-renderer` only.
- Preserve these approved fork deltas:
  - removal of 21 bundled renderer font files;
  - system font fallback in `defaultLatexFontFamilies()`;
  - no bundled OTF loading in `rememberDefaultOtf()`;
  - publishing metadata for `io.github.zly2006`;
  - parser compatibility fixes explicitly requested for Zhihu content, with
    focused parser/visitor/renderer tests and documentation.
- Do not change vendored Markdown behavior while bumping LaTeX. Dependency lines
  are the only expected `third_party/markdown` changes in a routine release.

## Workflow

### 1. Inspect current versions and upstream

```bash
cd /Users/zhaoliyan/IdeaProjects/latex
git fetch origin --tags --prune
git fetch me --prune
git tag -l 'release-*' --sort=-v:refname | head -10

cd /Users/zhaoliyan/IdeaProjects/Zhihu
rg -n 'io.github.zly2006:latex-(base|parser|renderer)' \
  app shared third_party/markdown

curl -s https://repo1.maven.org/maven2/io/github/zly2006/latex-renderer/maven-metadata.xml
```

Record the latest upstream tag, current public fork version, and every consumer
that needs a bump.

### 2. Prepare the release branch

Create the release branch from the latest upstream tag. Carry forward only the
approved fork commits; skip changes already present upstream.

```bash
cd /Users/zhaoliyan/IdeaProjects/latex
git switch -c release-<fork-version> <latest-upstream-tag>
```

Fresh worktrees must copy `local.properties` from the main LaTeX checkout before
Gradle publication.

### 3. Verify the fork diff

```bash
git diff --name-status <latest-upstream-tag>...HEAD
git diff --stat <latest-upstream-tag>...HEAD
```

Review every non-font, non-publishing change as an explicit compatibility delta.
Reject unrelated feature work or changes copied from another development tree.

### 4. Test

Run focused parser tests for compatibility changes, then the release gate:

```bash
./gradlew --no-daemon :latex-parser:jvmTest --tests '*RelevantTest*'
./gradlew --no-daemon :latex-renderer:jvmTest
```

For visible renderer changes, also run the appropriate preview or desktop demo.

### 5. Publish to Maven Central

Publish in dependency order:

```bash
export https_proxy=http://127.0.0.1:7890 http_proxy=http://127.0.0.1:7890

./gradlew --no-daemon --stacktrace :latex-base:publishAndReleaseToMavenCentral
./gradlew --no-daemon --stacktrace :latex-parser:publishAndReleaseToMavenCentral
./gradlew --no-daemon --stacktrace :latex-renderer:publishAndReleaseToMavenCentral
```

If Gradle fails while stopping `maven-central-build-service` after upload, query
the Sonatype deployment and treat `PUBLISHED` or `PUBLISHING` as authoritative.
Retry transient SSL failures with the proxy; do not re-version a deployment that
already exists.

### 6. Verify public artifacts

Check metadata for all three root modules and require HTTP 200 for the Android
renderer POM:

```bash
curl -s https://repo1.maven.org/maven2/io/github/zly2006/latex-base/maven-metadata.xml
curl -s https://repo1.maven.org/maven2/io/github/zly2006/latex-parser/maven-metadata.xml
curl -s https://repo1.maven.org/maven2/io/github/zly2006/latex-renderer/maven-metadata.xml
curl -sI https://repo1.maven.org/maven2/io/github/zly2006/latex-renderer-android/<version>/latex-renderer-android-<version>.pom | head -1
```

### 7. Push the LaTeX fork

After the required code review and approval, commit the release, fast-forward the
fork master, and push:

```bash
git switch master
git merge --ff-only release-<fork-version>
git push me master
```

### 8. Update vendored Markdown and Zhihu

In `/Users/zhaoliyan/IdeaProjects/Zhihu`, update every matching dependency to the
new fork version. The expected current consumers include:

- `third_party/markdown/markdown-renderer/build.gradle.kts`
- `app/build.gradle.kts`
- `shared/build.gradle.kts`

Re-run `rg` after editing so no old zly LaTeX version remains. Then follow the
project-required order:

```bash
./gradlew assembleLiteDebug
./gradlew ktlintFormat
```

Review the complete Zhihu diff and wait for approval before committing. Do not
publish Markdown artifacts; the vendored modules build as part of Zhihu.

## Final report checklist

- [ ] Latest upstream tag and Original version recorded
- [ ] Fork version is new on Maven Central
- [ ] Diff contains only approved fork deltas
- [ ] Focused compatibility tests pass
- [ ] `:latex-renderer:jvmTest` passes
- [ ] All three root modules and renderer Android artifact are public
- [ ] LaTeX fork master is pushed to `me`
- [ ] All Zhihu and `third_party/markdown` consumers use the new version
- [ ] `assembleLiteDebug` passes before `ktlintFormat`
- [ ] Zhihu diff is reviewed before commit
