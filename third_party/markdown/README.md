# Vendored Markdown fork

This directory contains the Markdown source used by Zhihu++ instead of the
published `io.github.zly2006:markdown-*` artifacts.

- Fork: `zly2006/Markdown`
- Base commit: `9203d3e4e855309c5dc35064841765e60e7670b8`
- Fork version at copy time: `0.0.1-alpha.14`
- Upstream lineage recorded by the fork: `huarangmeng/Markdown` 1.4.1
- Snapshot date: 2026-07-21
- Production source snapshot SHA-256:
  `be537dbd30f40eca2c0587b1e315d10b28b6ee3dcb7438d5c15e68126089ae5c`

The snapshot also includes the uncommitted issue #495 first-viewport and
selection-performance work that was present in the fork worktree on the
snapshot date. Only the runtime, parser, and renderer production source sets
needed by Zhihu++ are included. Demo, preview, benchmark, publishing, JS, and
Wasm modules are intentionally excluded.

Updates are manual and deliberate: copy the required production sources from
the hard fork, review the diff in this directory, then run the Zhihu++ build
and formatting checks. Do not replace these modules with a floating Maven
version.

Verify the production source snapshot from the repository root with:

```bash
find third_party/markdown/markdown-parser/src \
    third_party/markdown/markdown-runtime/src \
    third_party/markdown/markdown-renderer/src \
    -type f -print0 \
  | LC_ALL=C sort -z \
  | xargs -0 shasum -a 256 \
  | shasum -a 256
```
