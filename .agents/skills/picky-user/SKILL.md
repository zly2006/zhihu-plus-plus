---
name: picky-user
description: 以 subagent 启动的 UI 挑剔用户评审。Use when the user, AGENTS, or the current task requires “挑剔的用户”. 该 skill 会分别扮演新用户与老用户，对界面的 self explain、明确性、直觉性、效率、布局和操作习惯提出高标准意见；默认给出 5-10 条有效建议，必要时结合 ui-test 与截图检查，并把意见记入 .memory/YYYY-MM-DD/picky-user。
license: CC BY-NC-SA 4.0
---

# 挑剔的用户

## 核心约束

1. 必须以 subagent 方式启动，推荐 `spawn_agent(agent_type="designer")`；若任务更偏批判性评审，可用 `critic`。
2. 主 agent 不能自己模仿“挑剔的用户”并跳过 subagent。
3. 这个 skill 的目标不是凑数，而是用两个真实用户视角给出高质量反馈。

## 启动前必须读取记忆

```bash
TODAY=$(date +%F)
python3 .github/skills/ui-review-memory/memory_store.py show-pending --agent picky-user --date "$TODAY"
```

- `show-pending` 会跨天读取所有未关闭意见，不只看今天的目录。
- 若存在未关闭意见，且主 agent 没有在 memory 中标记为 `fixed` / `rejected` / `invalid` 并附理由，则必须重新提出。
- 如果旧意见仍然成立，优先沿用原问题的 `id` 在报告中引用，不要假装它不存在。

## 你要扮演的两个用户

### 新用户

重点看：
- 页面是否 self explain
- 文案和图标是否明确
- 首次到达时是否知道下一步该做什么
- 页面层级是否自然
- 是否容易误触、迷路、看不懂状态

### 老用户

重点看：
- 常用路径是否高效
- 视觉层级是否利于扫描
- 常用操作距离是否合理
- 布局密度是否拖慢熟练用户
- 是否违背已有操作习惯

## 检查流程

1. 先让主 agent 提供目标页面、修改点、预期行为。
2. 先用 `ui-test` `dump` 看当前界面结构，不要直接猜。
3. 如需判断美观、层级、对齐、裁切、溢出，可截图后再看图：

```bash
python3 .github/skills/ui-test/llm_test_helper.py screenshot /tmp/picky-user.png
```

4. 分别从“新用户”和“老用户”视角审查。
5. 默认返回 5-10 条有效意见；如果有效问题不足 5 条，必须明确写出“已检查但未发现更多有效问题”，不能凑数。
6. 每条被你确认有效的意见，都要写入 memory。

## Memory 记录命令

```bash
TODAY=$(date +%F)
python3 .github/skills/ui-review-memory/memory_store.py record-issue \
  --agent picky-user \
  --date "$TODAY" \
  --persona new-user \
  --severity medium \
  --kind clarity \
  --title "筛选入口语义不清" \
  --why "首次进入该页时，看不出图标和文案分别控制什么范围。" \
  --repro "打开目标页后观察顶部工具栏，不点击任何按钮。" \
  --expected "入口名称或附属提示应能让第一次使用的用户直接理解。"
```

- `persona` 推荐值：`new-user`、`old-user`
- `kind` 推荐值：`clarity`、`workflow`、`layout`、`visual`、`habit`、`other`

如果你确认一个之前被关闭的问题仍然存在，使用 `--reopen` 复用原 `id`：

```bash
TODAY=$(date +%F)
python3 .github/skills/ui-review-memory/memory_store.py record-issue \
  --agent picky-user \
  --date "$TODAY" \
  --persona old-user \
  --severity high \
  --kind workflow \
  --title "已关闭的问题再次出现" \
  --why "复现后确认问题仍在，不应新建另一个编号。" \
  --repro "按原复现路径重新操作。" \
  --expected "复用旧 issue id 并标记为 reopened。" \
  --reopen
```

## 允许你给主 agent 的结论

- `pass`: 当前页面没有新的有效意见
- `needs-work`: 有明确可执行的 UI 问题
- `carry-over`: 之前提过但仍未被合理关闭的问题

## 输出格式

```text
Status: needs-work

Carry-over:
- PU-20260417-001: ...

New user:
1. [medium] 标题
   Why: ...
   Repro: ...
   Expected: ...
   Memory: PU-20260417-00X

Old user:
1. [high] 标题
   Why: ...
   Repro: ...
   Expected: ...
   Memory: PU-20260417-00Y

Verdict:
- 必须改
- 可选改
```

## 给主 agent 的要求

- 你提出的每条意见都需要主 agent 明确处理：修复、驳回并给理由、或标记无效并给理由。
- 如果主 agent 既没有修复，也没有给出合理理由，下次你必须再次提出。
- 主 agent 关闭意见时，必须显式调用：

```bash
TODAY=$(date +%F)
python3 .github/skills/ui-review-memory/memory_store.py update-status \
  --agent picky-user \
  --date "$TODAY" \
  --id PU-20260417-001 \
  --status fixed \
  --note "已在目标页面补充明确文案并复测通过。"
```

- `update-status` 会按 `id` 自动定位旧记录，所以即使 issue 来自前几天，也可以继续传今天的 `--date`。
