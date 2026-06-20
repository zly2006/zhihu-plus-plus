---
name: github-pr-assets
description: Manage screenshots and other visual assets for GitHub pull requests. Use when a PR body needs real screenshots, when local /tmp paths must be replaced with durable links, when deciding whether GitHub web drag-and-drop uploads can be reproduced with gh api, or when updating a PR without disturbing the user's browser tabs.
---

# GitHub PR Assets

## Overview

Use this skill to attach verified screenshots or other review assets to GitHub PRs without relying on local-only paths or unverified upload behavior.

Core rule: PR descriptions must contain durable, publicly accessible Markdown links or images. A local path such as `/tmp/foo.png` is evidence for the agent, not a usable PR asset. Do not store transient PR screenshots in the product repository unless the user explicitly asks for that; use the dedicated `agent-image` repository.

## Workflow

1. Inspect the PR body with `gh pr view <number> --json title,body,url,headRefName,baseRefName`.
2. Verify every screenshot or asset mentioned in the PR:
   - local existence: `ls -lh <path>`
   - image type and dimensions: `file <path>`
   - optional checksum: `shasum -a 256 <path>`
3. Choose the least disruptive hosting method:
   - Prefer existing durable URLs if the image is already hosted and can be verified.
   - For generated agent screenshots, use `zly2006/agent-image` by default. Check it with `gh repo view zly2006/agent-image`; if it does not exist, create it instead of claiming upload is blocked.
   - Put PR assets under a namespaced path such as `zhihu-plus-plus/pr-<number>/<file>.png` inside `agent-image`.
   - Use raw GitHub URLs for PR Markdown, for example `https://raw.githubusercontent.com/zly2006/agent-image/main/zhihu-plus-plus/pr-<number>/<file>.png`.
4. Push the branch before updating the PR body.
5. Verify hosted URLs with `curl -I -L '<url>'` and require `HTTP 200` plus the expected content type.
6. Update the PR body with `gh pr edit <number> --body-file -`.
7. Re-read the PR body and re-check `git status --short --branch`.

## GitHub Upload Boundary

Do not claim that GitHub's web drag-and-drop attachment upload can be reproduced with ordinary `gh api` unless a real endpoint has been verified in the current environment.

Known boundary:

- Public REST issue/comment APIs accept Markdown text; they do not expose a general "upload this screenshot attachment for a PR body" endpoint.
- `uploads.github.com` is for documented upload surfaces such as release assets, not a generic PR screenshot endpoint.
- The browser drag-and-drop flow depends on GitHub Web's authenticated session and internal request flow. A token-only `gh api` call is not equivalent.

When the user forbids browser disruption, do not open, focus, navigate, or upload through their active tabs. Use CLI/API inspection and raw-link hosting instead, or ask before using a separate browser context.

## PR Body Requirements

For UI PRs, include:

- A short note that screenshots came from the actual app, device, emulator, or reproducible UI test output.
- Markdown image links, not local paths.
- Enough context to identify what each screenshot proves.

Good pattern:

```markdown
截图来自实际运行的 Lite Debug APK + AVD `Medium_Phone_2`。

![回答详情赞同者弹层](https://raw.githubusercontent.com/owner/repo/branch/docs/pr-assets/pr-415/answer-voters-sheet.png)
```

Avoid:

```markdown
- 回答详情赞同者弹层：`/tmp/answer-voters-sheet.png`
```

## Review Discipline

Keep asset commits narrow:

- In the product repository, commit only skill/content changes needed for the requested PR.
- In `agent-image`, commit only the PR asset files needed for the requested PR.
- Do not touch unrelated dirty files in the user's worktree.
- Avoid committing large or reusable generated assets unless the PR requirement needs them.
- Prefer descriptive names like `answer-voters-sheet.png` over timestamp-only names.

## Troubleshooting

### 创建 skill 的指令不能降级成普通记录

当用户明确说要“创建 skill”或纠正先前误写的 skill 触发词时，必须实际创建或更新一个 skill 目录，并按 skill 创建流程验证。不能只把经验写进现有文档、回复一句“记住了”，或把它当成任务已经完成。例子：用户要求把某类 PR 截图收尾流程沉淀成 skill 时，应该生成 `SKILL.md`、必要的 metadata，并跑 validator；不是只在原任务技能里加一条失败经验。

### agent-image 不存在就创建

当流程要求用 `agent-image` 承载截图资产时，必须真实检查仓库是否存在。如果不存在，就用 `gh repo create zly2006/agent-image --public` 创建并继续上传；不能把截图提交到业务仓库，也不能说“没有仓库所以做不了”。例子：PR 需要两张 AVD 截图时，应把图片提交到 `agent-image` 的项目/PR 子目录，然后把 PR 正文改成该仓库 raw 链接。

### raw 链接返回 404

先确认分支已推送，并且 URL 中的 owner、repo、branch、path 都精确匹配。路径大小写必须一致。用 `gh pr view` 确认 head branch，再用 `curl -I -L` 验证。

### PR 仍显示旧截图或旧正文

先用 `gh pr view <number> --json body` 验证 GitHub 端正文是否已更新。浏览器缓存或 Markdown 渲染延迟不能替代 CLI/API 复查。

### UI PR 截图设备优先级不能跳级

为 UI PR 准备效果截图时，必须先沿项目指定的远端 AVD 路径执行到明确不可用，再考虑本地 AVD 回退；不能因为本地截图更直接就绕过远端优先级。例子：一个功能已经在远端 AVD 上跑过目标 UI 测试，但还缺 PR 截图时，应该继续在同一类远端短生命周期设备里抓屏，只有远端启动、安装、截图链路被证明不可用后，才启动本地模拟器。
