---
name: release-latex-fork
description: Release the local LaTeX fork used by Zhihu++. Use when you need to merge upstream huarangmeng/latex into the zly2006 fork, preserve only approved fork deltas (font removal), publish a new Maven Central release, update Markdown's latex dependency, and verify the build. Applies specifically to /Users/zhaoliyan/IdeaProjects/latex as the fork checkout.
---

# Release LaTeX Fork

Use this skill for the Zhihu-local LaTeX fork release workflow.

## Dependency chain

```
huarangmeng/latex (upstream)
    → zly2006/latex (fork, this repo)
        → zly2006/Markdown (markdown fork, depends on latex)
            → Zhihu app
```

**CRITICAL**: LaTeX must be published BEFORE Markdown. When bumping both, follow this order:
1. Publish LaTeX fork (this skill)
2. Update Markdown fork's latex version and publish (release-markdown-fork skill)
3. Update Zhihu app

## Inputs

Required:

- Target version, e.g. `1.4.5-zly` (check upstream tags and bump the zly suffix)
- Fork checkout: `/Users/zhaoliyan/IdeaProjects/latex`
- Markdown fork checkout: `/Users/zhaoliyan/IdeaProjects/Zhihu/.tmp/Markdown-zly2006`

Optional:

- Proxy for Sonatype and repo1 access:
  - `https_proxy=http://127.0.0.1:7890`
  - `http_proxy=http://127.0.0.1:7890`

## Guardrails

- The fork publishes under `io.github.zly2006` coordinates (NOT `io.github.huarangmeng`).
- Fork-specific approved deltas:
  - **Font removal**: 21 font files removed from latex-renderer's Compose Resources
  - **System fallback**: `defaultLatexFontFamilies()` returns `FontFamily.Default`
  - **No OTF loading**: `rememberDefaultOtf()` skips bundled OTF, returns `MathFont.KaTeXTTF`
- Never use destructive git commands on a dirty tree.
- Published modules: `latex-base`, `latex-parser`, `latex-renderer` (not `latex-preview` or `androidapp`/`composeApp`).

## Workflow

### 1. Check if release is needed

Before publishing Markdown, always check whether LaTeX needs a new release:

```bash
# Compare the latex version in Markdown's catalog with what's on Maven Central
grep 'latex =' .tmp/Markdown-zly2006/gradle/libs.versions.toml
curl -s https://repo1.maven.org/maven2/io/github/zly2006/latex-renderer/maven-metadata.xml | grep latest
```

If the version in Markdown's catalog is NOT published to Maven Central, publish it first.

Also check: are there upstream changes that should be merged? New upstream tags?

### 2. Confirm branch state

In the fork checkout:

```bash
cd /Users/zhaoliyan/IdeaProjects/latex
git fetch origin --tags --prune
git fetch me --prune
```

- `origin` = huarangmeng/latex (upstream)
- `me` = zly2006/latex (fork)

Check what's new upstream:
```bash
git tag -l 'release-*' --sort=-v:refname | head -10
```

Create a release branch from the latest upstream tag (e.g. `release-1.4.3`):
```bash
git checkout -b release-<version>-zly <latest-upstream-tag>
```

If there are fork commits on a previous release branch that need to be carried forward, cherry-pick or merge them. If upstream already includes equivalent changes, skip.

### 3. Apply approved fork deltas

Verify the diff against upstream is limited to:

- Font files deleted from `latex-renderer/src/commonMain/composeResources/font/`
- `LatexFontFamily.kt`: system font fallback, no Res.font imports
- `RememberResolvedMathFont.kt`: no Res.font.latinmodern_math import, `rememberDefaultOtf()` skips OTF
- Publishing metadata: `io.github.zly2006` coordinates in latex-base, latex-parser, latex-renderer
- Version bump in `gradle.properties`

Check with:
```bash
git diff --name-status origin/master...HEAD
```

### 4. Verify build and tests

```bash
./gradlew --no-daemon :latex-renderer:jvmTest
```

### 5. Publish to Maven Central

Modules in dependency order:

```bash
export https_proxy=http://127.0.0.1:7890 http_proxy=http://127.0.0.1:7890

./gradlew --no-daemon --stacktrace :latex-base:publishAndReleaseToMavenCentral
./gradlew --no-daemon --stacktrace :latex-parser:publishAndReleaseToMavenCentral
./gradlew --no-daemon --stacktrace :latex-renderer:publishAndReleaseToMavenCentral
```

If Sonatype communication is flaky, export the proxy first.

### 6. Verify public availability

Check repo1 metadata:

```bash
curl -s https://repo1.maven.org/maven2/io/github/zly2006/latex-base/maven-metadata.xml
curl -s https://repo1.maven.org/maven2/io/github/zly2006/latex-parser/maven-metadata.xml
curl -s https://repo1.maven.org/maven2/io/github/zly2006/latex-renderer/maven-metadata.xml
```

Also check Android artifacts exist:
```bash
curl -sI https://repo1.maven.org/maven2/io/github/zly2006/latex-renderer-android/<version>/latex-renderer-android-<version>.pom | head -1
```

### 7. Push fork master

Fast-forward the fork's master branch and push:

```bash
git checkout master
git merge --ff-only release-<version>-zly
git push me master
```

### 8. Update Markdown fork's latex version

In the Markdown fork:

```bash
cd /Users/zhaoliyan/IdeaProjects/Zhihu/.tmp/Markdown-zly2006
```

Edit `gradle/libs.versions.toml`:
```toml
latex = '<new-version>'
```

Then publish Markdown following the `release-markdown-fork` skill.

## Failure handling

### Sonatype upload hangs

- Retry with the proxy exported.
- If a module uploads successfully but Gradle fails while stopping `maven-central-build-service`, treat the deployment status as the source of truth.

### Gradle fails with `maven-central-build-service`

- Query deployment status directly via Sonatype API.
- If `PUBLISHED` or `PUBLISHING`, do not re-version.

### SSL errors during publish

- Retry. The Sonatype endpoint occasionally drops connections.
- Successive retries usually succeed (seen with latex-renderer specifically).

## Final report checklist

- [ ] Upstream base tag confirmed
- [ ] Fork deltas match approved list (font removal only)
- [ ] `jvmTest` passes
- [ ] latex-base published and verified on Maven Central
- [ ] latex-parser published and verified on Maven Central
- [ ] latex-renderer published and verified on Maven Central (both root and Android artifact)
- [ ] Fork master pushed to `me` remote
- [ ] Markdown fork's latex version updated
