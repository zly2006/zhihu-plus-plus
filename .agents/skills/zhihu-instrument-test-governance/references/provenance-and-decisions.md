# Provenance and Decision Reference

## Provenance audit command

Run this from the repository root after editing. It reports each `@Test` whose preceding twelve lines do not contain both a repository issue URL and a repository pull-request URL.

```bash
failed=0
while IFS= read -r file_path; do
  awk '
    { source[NR] = $0 }
    /^[[:space:]]*@Test([[:space:]]|$)/ {
      issue = 0
      pull = 0
      start = NR > 12 ? NR - 12 : 1
      for (line = start; line < NR; line++) {
        if (source[line] ~ /https:\/\/github.com\/zly2006\/zhihu-plus-plus\/issues\/[0-9]+/) issue = 1
        if (source[line] ~ /https:\/\/github.com\/zly2006\/zhihu-plus-plus\/pull\/[0-9]+/) pull = 1
      }
      if (!issue || !pull) {
        printf "%s:%d missing%s%s provenance\n", FILENAME, NR, issue ? "" : " issue", pull ? "" : " PR"
        failed = 1
      }
    }
    END { exit failed }
  ' "$file_path" || failed=1
done < <(rg --files app/src/androidTest -g '*.kt')
exit "$failed"
```

This is a structural gate only. It cannot prove that URLs are real or relevant; the audit must read both pages before accepting them.

The cleanup PR must include a retained-test table with the test name, classification, issue URL, PR URL, and device-only reason. Review that table against the linked pages; passing the command alone is never acceptance.

## Classification

| Class | Meaning | Default decision |
| --- | --- | --- |
| `REGRESSION` | Reproduces a bug that occurred and was fixed | Keep permanently |
| `CONTRACT` | Protects an explicitly accepted feature behavior | Keep only at the cheapest sufficient layer |
| `SMOKE` | Broadly proves startup or integration without a narrow past failure | Keep only with a verified issue/PR pair documenting its unique device value |
| `UNVERIFIED` | Historical reason or linked behavior has not been proven | Investigate; never delete by assumption |

## Device-necessity questions

A permanent instrument test should answer yes to at least one question that cannot be covered faithfully below Android instrumentation:

- Does it depend on Android window, system bars, predictive back, permissions, activity lifecycle, or platform services?
- Does it verify Compose semantics, focus, scrolling, gesture arbitration, save/restore, or actual pixel/layout behavior tied to Android?
- Does it protect an Android-only integration or generated implementation?
- Did a historical bug escape cheaper coverage specifically because device behavior differed?

Pure reducers, parsers, model mapping, URL construction, list transformation, settings-key lookup, and deterministic ViewModel state transitions normally belong in common/JVM tests.

## Deletion evidence

For every deletion, record one of:

- exact duplicate of a retained test, including the same historical provenance and assertion strength;
- migrated to a cheaper test that asserts the same contract;
- one-time visual or implementation-detail check with no historical regression;
- obsolete product behavior, with the PR that removed the behavior;
- fixture-only test whose behavior is already covered by retained consumers.

“Slow”, “flaky”, “old”, or “hard to understand” is never sufficient evidence by itself.
