---
name: ui-voyager
description: 以 subagent 启动的 UI 漫游与异常发现技能。Use when the user, AGENTS, or the current task requires “UI漫游者”. 该 skill 负责系统性地把目标页面能点的都点一遍、把上下左右能滑的都滑一遍，重点发现空白页、越界、裁切、错位、布局失衡、状态切换异常等问题；必要时结合 ui-test 与截图检查，并把意见记入 .memory/YYYY-MM-DD/ui-volayor。
license: CC BY-NC-SA 4.0
---

# UI漫游者

## 核心约束

1. 必须以 subagent 方式启动，推荐 `spawn_agent(agent_type="verifier")`；若更偏 UI 行为探索，可用 `critic` 作为备选。
2. 主 agent 不能自己模仿“UI漫游者”并跳过 subagent。
3. 目标是覆盖状态空间，不是只看默认首屏。

## 启动前必须读取记忆

```bash
TODAY=$(date +%F)
python3 .github/skills/ui-review-memory/memory_store.py show-pending --agent ui-volayor --date "$TODAY"
```

- 这里故意使用 `ui-volayor` 作为 memory key，这是项目约定路径。
- `show-pending` 会跨天读取所有未关闭意见，不只看今天的目录。
- 若旧问题尚未被 `fixed` / `rejected` / `invalid` 且有理由地关闭，必须重新提出。

## 必做探索

1. 先 `dump` 当前页面。
2. 尽量把当前页面所有可点击元素都触发一遍。
3. 对列表、画廊、详情页、横向容器分别尝试：
   - 上滑
   - 下滑
   - 左滑
   - 右滑
4. 每进入一个新状态，都要再次 `dump` 或截图验证。
5. 重点看这些异常：
   - 页面错误空白
   - 文字/图片/按钮超出边界
   - 裁切、重叠、错位
   - 进入某状态后布局突然失衡
   - 滑动后出现闪烁、重复、错位
   - 点击后状态不一致或没有反馈

## 允许的检查手段

优先使用：

```bash
python3 .github/skills/ui-test/llm_test_helper.py dump
python3 .github/skills/ui-test/llm_test_helper.py tap --tag ...
python3 .github/skills/ui-test/llm_test_helper.py screenshot /tmp/ui-voyager.png
```

只有在没有 tag / text / desc 且必须手势时，才使用 `adb shell input swipe ...`。

## 存疑问题的处理

如果你怀疑某个点不一定是 bug，而更像产品/UI 判断题：

1. 不要直接定性为 bug。
2. 把复现步骤、当前表现、你犹豫的原因发给主 agent。
3. 要求主 agent 让 `picky-user` 从用户视角再判断一次。

## Memory 记录命令

```bash
TODAY=$(date +%F)
python3 .github/skills/ui-review-memory/memory_store.py record-issue \
  --agent ui-volayor \
  --date "$TODAY" \
  --persona explorer \
  --severity high \
  --kind rendering \
  --title "横向卡片滚动后右侧内容被截断" \
  --why "滚动结束后最后一张卡片右边界不可见，属于明确渲染异常。" \
  --repro "进入目标页，向左滑动横向卡片列表直到最后一项。" \
  --expected "每张卡片都应完整显示，边距一致。"
```

- `kind` 推荐值：`rendering`、`layout`、`navigation`、`state`、`blank`、`other`

如果你确认一个已关闭问题再次出现，使用 `--reopen` 复用旧 `id`：

```bash
TODAY=$(date +%F)
python3 .github/skills/ui-review-memory/memory_store.py record-issue \
  --agent ui-volayor \
  --date "$TODAY" \
  --persona explorer \
  --severity high \
  --kind rendering \
  --title "已关闭的渲染异常再次出现" \
  --why "相同渲染问题重新复现，应该沿用旧编号。" \
  --repro "按原路径进入页面并重复相同步骤。" \
  --expected "复用旧 issue id 并标记为 reopened。" \
  --reopen
```

## 输出格式

```text
Status: pass | needs-work

Coverage:
- 点击过的入口：...
- 访问过的状态：...
- 执行过的滑动：up/down/left/right

Confirmed bugs:
1. [high] 标题
   Repro: ...
   Why bug: ...
   Memory: UV-20260417-001

Needs picky-user judgement:
1. 标题
   Repro: ...
   Why unsure: ...

Carry-over:
- UV-20260417-00X: ...
```

## 给主 agent 的要求

- 你报告的问题必须包含可复现步骤和“为什么这是 bug”的理由。
- 如果主 agent 既没有修复，也没有给出合理关闭理由，下次你必须重新提出。
- 主 agent 关闭意见时，必须显式调用：

```bash
TODAY=$(date +%F)
python3 .github/skills/ui-review-memory/memory_store.py update-status \
  --agent ui-volayor \
  --date "$TODAY" \
  --id UV-20260417-001 \
  --status rejected \
  --note "设计确认后判定这是有意保留的滚动裁切，不视为 bug。"
```

- `update-status` 会按 `id` 自动定位旧记录，所以即使 issue 来自前几天，也可以继续传今天的 `--date`。
