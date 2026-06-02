# Zhihu++ Agent Instructions

本项目是隐私增强的知乎 Android 客户端，支持本地推荐算法、广告屏蔽、内容过滤。

## 构建与测试

```bash
# 验证修改（必须按顺序执行）
./gradlew assembleLiteDebug  # 构建 lite 变体
./gradlew ktlintFormat        # 格式化代码
```

**重要**: 修改后必须先构建验证，再格式化，最后提交。

## 项目结构

- **app**: 主应用（Jetpack Compose UI）
    - `src/main`: 共享代码
    - `src/full`: Full variant（含 NLP）
    - `src/lite`: Lite variant（轻量级）
- **Module**: `sentence_embeddings`（Rust tokenizer，仅 full variant）

### Build Variants
- **lite**: 轻量版 (~4MB)，无 ML 功能，包名 `com.github.zly2006.zhplus.lite`
- **full**: 完整版，含 HanLP NLP，包名 `com.github.zly2006.zhplus`

## 关键约定

### 数据序列化
- **DataHolder** 和 data classes 使用 `camelCase`
- **知乎 API** 返回 `snake_case`
- **自动转换**: `AccountData.fetch*()` 和 `decodeJson()` 内部自动调用 `snake_case2camelCase()`
- 不要手动转换或在 data class 中使用 snake_case

### HTTP 客户端
- 使用 `AccountData.httpClient(context)` 获取配置好的客户端
- Web API 需要 `signFetchRequest(context)` 用于 zse96 v2 签名
- Android API 使用 `AccountData.ANDROID_HEADERS` 和 `ANDROID_USER_AGENT`

### Compose
- Material 3 组件
- 用 `LaunchedEffect` 处理副作用，设置正确的 key
- 用 `collectAsState()` 观察 Flow/StateFlow

### git worktree

新开worktree的时候，记得把 local.properties 复制过去，避免构建问题.

### 导航
- 使用 Jetpack Navigation Compose
- 定义 sealed interface `NavDestination` 表示不同页面，包含 route 和参数
- 在编写导航代码前必须检查 NavDestination.kt

## Android 调试标准流程

注意：
1. 必须使用avd验证，不要使用真机。
2. 时刻注意你是一个LLM，延迟很高。所以大多数情况下不需要你执行sleep指令，你本身的反应就很慢，足够程序响应了。这也是说，如果需要执行双击等复杂手势，必须用&&来串联多个adb指令，不然你的反应太慢就不是双击了。
3. UI 验证时如果启动后看到“下载官方App”“查看协议”“查看设置”这类官方 App/协议确认页，或进入知乎网页登录/安全验证页，不要当成普通业务 UI 问题；这表示当前 AVD 登录态缺失或失效。应先按 `.agents/skills/launch-on-device/SKILL.md` 的 Login JSON Backup and Restore 流程恢复/覆盖 `files/account.json`，确认已登录后再继续 UI 验证；不要反复卡在登录流程里。

### 应用启动与验证
```bash
# 1. 检查包名（必须先做）
grep "applicationId" app/build.gradle.kts
# lite variant: com.github.zly2006.zhplus.lite

# 2. 启动模拟器（如果还没启动）
emulator -avd Medium_Phone_2

# 3. 构建并安装
./gradlew assembleLiteDebug
adb install -r app/build/outputs/apk/lite/debug/app-lite-debug.apk

# 4. 启动
adb shell am force-stop com.github.zly2006.zhplus.lite
adb shell monkey -p com.github.zly2006.zhplus.lite -c android.intent.category.LAUNCHER 1
```

### UI 调试强制清单
修改 UI 代码后**必须**：
1. ✅ 构建 + 格式化
2. ✅ 安装到设备
3. ✅ 正确启动应用（检查包名！）
4. ✅ 等待加载完成（至少 8-10 秒）
5. ✅ 使用 ui-test 技能查看当前页面状态：`python3 .agents/skills/ui-test/llm_test_helper.py dump`
6. ✅ 先 `dump` 再 `tap`，优先通过 `--tag/--text/--desc` 交互，不使用硬编码坐标 tap
7. ✅ 若目标是无标识可点击节点，使用 `--text "" --index N`（N 来自当前页面 dump）
8. ✅ 交互后再次 `dump` 或截图验证状态
9. ✅ 仅在无 tag/文字可用且必须手势操作时，才使用 `adb shell input swipe` 等手势
10. ❌ 异常时检查 logcat：`adb logcat | grep -i error`

### UI 双代理复检

只要修改内容涉及 Compose、布局、样式、导航、交互、可见文案或任何用户可见 UI，主 agent 在完成上面的基础验证后，**必须**再执行以下流程：

1. 必须启动两个 subagent skill，不能由主 agent 自己扮演：
   - `$ui-voyager`（UI漫游者）：系统性探索目标页面，把能点的尽量都点一遍，把上下左右的滑动都试一遍，重点找空白页、越界、裁切、重叠、错位、状态切换异常。
   - `$picky-user`（挑剔的用户）：分别扮演新用户和老用户，对 self explain、明确性、直觉性、效率、布局和操作习惯提出高标准意见。
2. 两个 skill 都必须先读取自己的持久化记忆：
   - `.memory/YYYY-MM-DD/picky-user/`
   - `.memory/YYYY-MM-DD/ui-volayor/`
3. 两个 skill 都允许在 `ui-test` 之外结合截图做视觉判断，但交互仍优先走 `ui-test` 的 `dump` / `tap` / `screenshot` 工作流。
4. `ui-voyager` 遇到拿不准的地方，必须把复现步骤和犹豫原因交给 `$picky-user` 或主 agent，请其再判断，不要含糊带过。
5. 主 agent 只有在以下条件满足后，才能停止工作、宣布 UI 修改完成，或请求我做下一步决策：
   - `$ui-voyager` 没有新的有效问题；
   - `$picky-user` 没有新的有效意见；
   - 或者它们提出的意见都已经被修复，或被明确标记为无效/驳回并留下充分理由。
6. 主 agent 对每条意见都必须写回 memory，至少标记为 `fixed`、`rejected` 或 `invalid`，不能口头略过。

记忆回写命令示例：

```bash
TODAY=$(date +%F)
python3 .agents/skills/ui-review-memory/memory_store.py update-status \
  --agent picky-user \
  --date "$TODAY" \
  --id PU-20260417-001 \
  --status fixed \
  --note "已修复并复测通过。"
```

`update-status` 会按 `id` 自动定位历史记录，所以 issue 即使不是今天创建的，也必须继续回写，而不是新建另一个编号。

## 代码风格
- Kotlin Serialization with `@Serializable`
- 只在必要时注释，不过度注释
- ktlint 格式化（14.0.1）

## Code Review
- 每次修改后，必须进行代码 review，等待批准后才能 commit
- 不仅要进行上述所有检查，还要检查是否有代码重复片段，是否有未使用的变量或函数，是否有潜在的性能问题等
- 不仅要检查当前代码，还要把关键地方都grep一下，检查你写的代码是否和其他地方重复了，是否有类似的代码片段可以复用
- 在不降低注释质量的前提下，代码越短越好，避免过度设计和过度抽象

## ⏰重要提醒，在每次编写代码时必须遵守：
- 不得擅自简化代码实现，如果确实有的功能难以实现，停下来等待我的反馈，不要私自修改设计。
- 必须按照上述流程进行调试验证，尤其是 UI 相关的修改，不能跳过任何一步，确保你写的功能正常可用。
- 每次修改完代码后必须进行review，不能直接提交，必须等待我的反馈和批准后才能合并到主分支。

## Pull Requests

当我要求你发 PR 的时候，PR 的title必须以feat: /fix: /refactor: 开头，标题和内容必须用中文写。
提交PR前，先更新master与远程同步或领先，并确保当前分支基于master，而不包括其他feature branch的内容。
如果一开始给你的提示词包括了issue链接，并且此PR解决了这个issue，应该写上Resolves #issue_number在PR描述里，这样GitHub会自动关联并在PR合并时关闭这个issue。
# AGENTS.md

## Purpose

This file defines repository-specific instructions for coding agents working on **InstallerX Revived**.

Use it to decide:

* where a change belongs,
* which constraints must be preserved,
* what to verify before claiming a task is complete.

Task-specific maintainer instructions take precedence over this file. When the request is narrow, make the smallest coherent change that satisfies it.

For substantial features, invasive refactors, or behavior changes that span several files, sketch a short implementation plan before editing. Keep the plan aligned with the actual implementation as the work proceeds.

---

## Read these first when relevant

* `README.md` — product scope, supported install flows, user-facing feature boundaries.
* `CONTRIBUTING.md` — translation policy, build prerequisites, contribution expectations.
* `.github/workflows/pr-check.yml` — the default CI build matrix used for pull requests.
* `settings.gradle.kts`, `app/build.gradle.kts`, and
  `gradle/libs.versions.toml` — before touching Gradle, repositories, flavors, versions, or dependencies.

Do not duplicate or contradict those files casually. Update this file only for stable, repository-wide rules that agents should repeatedly follow.

---

## Repository overview

InstallerX Revived is a community-maintained Android installer with:

* dialog, notification, and automatic installation flows,
* support for APK, APKS, APKM, XAPK, APKs inside ZIP files, and batch APK installation,
* profile-driven install options and install flags,
* privileged workflows involving Root, Shizuku, Dhizuku, and hidden APIs,
* switchable UI families based on Material 3 Expressive and Miuix.

Several product behaviors are intentionally flow-specific. Do **not** assume a feature supported in dialog installation is also valid for notification or automatic installation unless the existing code and docs already establish that.

---

## Critical project constraints

* Preserve the **online/offline** product boundary. The offline flavor must not silently gain network-only behavior or permissions.
* Prefer the repository’s existing **native API** paths and abstractions. Do not introduce shell-command implementations as a shortcut unless the maintainer explicitly requests it.
* Treat flow-specific behavior as flow-specific. A capability that exists for dialog installation is not automatically valid for notification or automatic installation.
* When changing behavior that is described in `README.md`, update it or call out the documentation impact in the handoff.

---

## Project layout

### Top-level areas

* `app/` — main Android application.
* `hidden-api/` — hidden API declarations/helpers consumed by the app.
* `build-plugins/` — shared Gradle convention plugins.
* `baselineprofile/` — Android baseline profile generation.
* `.github/workflows/` — CI and release automation.

Do not assume every top-level directory is an included Gradle module. Confirm active modules in
`settings.gradle.kts` before making module-level assumptions.

### Main Kotlin package map

Under `app/src/main/java/com/rosan/installer/`:

* `core/` — shared low-level app infrastructure.
* `data/` — persistence, concrete providers, repositories, and mappers.
* `di/` — Koin modules and initialization wiring.
* `domain/` — domain models, repository contracts, providers, use cases, and business rules.
* `framework/` — Android/platform-facing integration code.
* `ui/` — screens, widgets, navigation, themes, and UI-specific models.
* `util/` — utility helpers.

Preserve this separation. Do not move behavior into a convenient but wrong layer just to finish faster.

---

## Build prerequisites

### Toolchain

* Use the repository Gradle Wrapper: `./gradlew ...`.
* The project requires **JDK 25**.
* Kotlin/JVM toolchains and Android compile settings are centrally defined; do not downgrade or loosen them unless the task explicitly requires it.

### GitHub Packages authentication

The project resolves snapshot `miuix` artifacts from GitHub Packages.

For local builds, credentials are expected outside the repository, typically in the global Gradle properties file:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_PERSONAL_ACCESS_TOKEN
```

The token needs `read:packages` access. CI may instead use `GITHUB_ACTOR` and `GITHUB_TOKEN`.

Never commit credentials, inline them into tracked files, or weaken the existing credential handling.

---

## Default verification

### Standard smoke build

The default repository-level verification target is the same pair used by pull request CI:

```bash
./gradlew assembleOnlinePreviewDebug assembleOfflinePreviewDebug \
  -PAPP_ID="com.rosan.installer.x.revived.test"
```

Run this when the change can reasonably affect app compilation, resources, dependency wiring, or variant-sensitive behavior.

### Report verification honestly

When summarizing work:

* state which commands were run,
* state whether they passed,
* say explicitly when verification was not run or could not be completed.

Do not imply a build or test passed unless it actually did.

---

## Gradle, variants, and dependency rules

### Flavors and build levels

The app currently uses two flavor dimensions:

* connectivity:

    * `online`
    * `offline`
* level:

    * `Unstable`
    * `Preview`
    * `Stable`

Important build behavior includes:

* connectivity-specific `INTERNET_ACCESS_ENABLED`,
* build-level-specific `BUILD_LEVEL`,
* git-hash version suffixes for unstable/preview outputs,
* an optional `VERSION_NAME` Gradle override for release automation.

Do not flatten, rename, or silently bypass flavor logic. If behavior differs by variant, make that relationship explicit.

### Dependencies

* Prefer `gradle/libs.versions.toml` for dependency and plugin version changes.
* Follow the existing version catalog naming style.
* Do not scatter raw dependency coordinates or versions across module build files without a strong reason.
* Respect the current centralized repository setup and `RepositoriesMode.FAIL_ON_PROJECT_REPOS`.
* The GitHub Packages `miuix` repository is intentionally configured in `settings.gradle.kts`; do not duplicate it in subprojects.

### Signing and release flow

* Release and debug signing behavior is handled centrally in `app/build.gradle.kts`.
* Local builds may fall back to the debug keystore when custom signing material is absent.
* Stable release packaging is automated in `.github/workflows/manual-stable-release.yml`.

Do not alter signing fallback, artifact naming, release workflow commands, version generation, or stable-release logic unless the maintainer explicitly asks for that.

---

## Architecture conventions

### Dependency injection

Use the existing Koin structure in `app/src/main/java/com/rosan/installer/di/`.

Relevant modules already include areas such as:

* core,
* device,
* engine,
* initialization,
* installer sessions,
* privileged behavior,
* serialization,
* settings,
* view models.

When introducing a new injectable dependency:

* place it in the most relevant existing module,
* avoid ad-hoc global singletons,
* keep initialization wiring explicit.

### Application initialization

`App.kt` performs important global startup work, including:

* crash handling initialization,
* hidden API exemptions on supported Android versions,
* Monet setup for older platform versions,
* conditional logging setup,
* Koin bootstrap,
* privileged auto-lock service initialization.

Treat the order and presence of this startup logic as sensitive. Do not reorder or remove initialization steps unless the task requires it and the consequences are understood.

### Domain and data boundaries

Keep business rules and platform/data access separate:

* domain models and use cases should not become Android UI utilities,
* data repositories/providers should not grow presentation logic,
* UI code should not bypass established repository/use-case boundaries when a domain path already exists.

Prefer extending the existing pattern used by the nearest comparable feature.

---

## Settings-related changes

Settings in this repository are layered and should stay that way.

### Typical settings layout

Domain side:

* `domain/settings/model/`
* `domain/settings/provider/`
* `domain/settings/repository/`
* `domain/settings/usecase/`
* `domain/settings/util/`

Data side:

* `data/settings/local/`
* `data/settings/mapper/`
* `data/settings/provider/`
* `data/settings/repository/`

### Checklist for adding or changing a setting

When a setting is persisted or exposed through app state, verify whether the change needs:

1. a domain model or state update,
2. provider/repository contract changes,
3. data-layer storage or mapping updates,
4. DI wiring updates,
5. UI state/action/view-model changes where that feature is presented,
6. both Material 3 and Miuix screen updates when both UI families expose the same setting,
7. English and Simplified Chinese string updates.

Do not implement only the visible switch while leaving persistence, mapping, or downstream behavior inconsistent.

---

## UI conventions

### Material 3 Expressive and Miuix are separate UI families

The repository keeps page implementations under distinct paths such as:

* `ui/page/main/`
* `ui/page/miuix/`

Respect that split.

Do not:

* leak Miuix-only components into Material 3 screens without intent,
* rebuild Material 3 screens with Miuix assumptions,
* change shared logic while checking only one UI family.

When a feature exists in both design systems, preserve semantic consistency while allowing implementation details to remain native to each UI family.

### Reusable UI components

For common widgets and shared components:

* keep dependencies as narrow as the existing component boundary allows,
* do not hardcode behavior that should be supplied by the caller,
* avoid locking reusable components to one screen-specific style or workflow,
* prefer composable APIs that remain extensible rather than baking product decisions into generic widgets.

Follow nearby component patterns before inventing a new style.

---

## Text, translation, and wording

### Translation policy

Per repository contribution policy:

* English and Simplified Chinese strings are maintained by developers.
* Other languages should go through Weblate rather than direct translation PRs.

### When changing user-visible strings

* Update English and Simplified Chinese together when the changed text belongs to both maintained locales.
* Preserve established product terminology unless the task is explicitly a wording cleanup.
* Keep safety caveats and compatibility warnings precise. Do not soften them just to make copy shorter.
* When text distinguishes concepts such as global authorization, profile authorization, install initiator, requester, or system limitations, keep those distinctions intact.

---

## Privileged and installation behavior

This codebase interacts with highly sensitive platform behavior, including:

* app installation flows,
* install flags and profile inheritance,
* user/all-user installation handling,
* Root, Shizuku, Dhizuku, and hidden API paths,
* ROM-specific compatibility workarounds,
* attempts to bypass OEM interception only where the project already supports that behavior.

### Rules for sensitive changes

Before changing privileged or install behavior:

1. identify the exact flow being modified: dialog, notification, automatic, profile, or system integration,
2. check whether the feature is already documented as flow-specific,
3. preserve permission boundaries and do not imply capabilities the current authorizer cannot provide,
4. avoid widening behavior from one privileged backend to another without explicit evidence,
5. keep ROM compatibility behavior explicit instead of hiding it behind vague generic logic.

If a change affects data safety, install success, system-API fallbacks, or ROM-specific behavior, explain the tradeoff clearly in the final summary.

---

## Source and API discipline

* Prefer native APIs and the repository’s existing abstractions over ad-hoc shell-command workflows.
* Reuse existing platform wrappers, providers, and repositories before adding parallel paths.
* Avoid introducing reflection, hidden API access, or privileged shortcuts where an existing maintained path already exists.
* When adding compatibility logic, keep version checks and backend checks local and readable.

---

## CI and workflow boundaries

The workflow directory contains purpose-specific automation such as:

* pull request build checks,
* preview/dev automation,
* release automation,
* CodeQL or other maintenance workflows.

When editing workflows:

* change only the workflow relevant to the request,
* preserve least-privilege token permissions unless explicitly required,
* keep package-auth requirements intact,
* do not conflate PR validation with release packaging.

Release automation deserves extra caution because it may involve version inputs, signing material, artifact renaming, and draft release creation.

---

## Recommended agent workflow

For implementation tasks, follow this order:

1. Restate the concrete behavior being changed.
2. Locate the smallest relevant area of the repository.
3. Find the nearest existing pattern and extend it.
4. Update all affected layers, not only the most visible file.
5. Run the narrowest meaningful verification, defaulting to the CI-equivalent smoke build when compilation impact is plausible.
6. Summarize:

    * what changed,
    * why this shape was chosen,
    * what was verified,
    * what remains unverified.

Prefer targeted, reviewable edits over sweeping refactors.

---

## Common mistakes to avoid

* Editing one UI family and forgetting the parallel Material 3 or Miuix surface.
* Adding a setting toggle without updating persistence or state propagation.
* Adding repositories to module Gradle files despite centralized repository management.
* Hardcoding dependency versions outside the version catalog.
* Modifying release/signing/version automation during unrelated work.
* Treating dialog-only install behavior as universally available.
* Weakening or deleting compatibility warnings from strings without understanding why they exist.
* Reordering app initialization code as a “cleanup.”
* Claiming a build passed when no verification was run.

---

## Maintainer-facing handoff format

When finishing a task, give a compact handoff that includes:

* **Changed:** files or areas updated.
* **Behavior:** what users or maintainers should expect now.
* **Verification:** commands run and result.
* **Notes:** migration concerns, unverified scenarios, or follow-up risks only when genuinely relevant.

Keep the report factual and specific.
## Miuix 适配工作手册

本仓库正在做 Material 3 / miuix 双主题适配，采用**页面级分流**模式（参考
[InstallerX-Revived](https://github.com/wxxsfxyzm/InstallerX-Revived)）。
如果用户要求你 port 某个页面到 miuix 风格，严格按下面执行。

### 架构

- `ui/` 下是 Material 3 现有页面，**不动**。
- `ui/miuix/` 下是 miuix 风格的对应页面，文件名加 `Miuix` 前缀。
- 主题切换由 `theme/ThemeManager.getThemeStyle()` 控制；顶层 `theme/Theme.kt`
  的 `ZhihuTheme` 自动分流，子页面不需要关心。
- 路由分流写在 `ui/ZhihuMain.kt` 的 NavHost 里，每个 `composable<>` 加
  `if useMiuix ... else ...`。
- 数据层、ViewModel、navigation/、theme/ 都**不归 miuix 适配工作管**，不要碰。

### 参考实现

模仿这个文件的结构和风格：
**`app/src/main/java/com/github/zly2006/zhihu/ui/miuix/MiuixAppearanceSettingsScreen.kt`**

它演示了 Scaffold + TopAppBar + LazyColumn 骨架、`SuperSwitch` / `SuperArrow`
设置项、`LocalNavigator` 返回、`ThemeManager` 读写。

### 强制约定（不要违反）

1. **只引用六个 miuix 包**：`basic` / `preference` / `theme` / `utils` / `window` / `blur`。
   `window` 用于 `WindowBottomSheet`（已确认可用）；`blur` 用于四段式纹理模糊管线。
   需要 `extra` / `icon` 时**停下来跟用户确认**，不要自己引。
2. **图标继续用 `androidx.compose.material.icons.*`**（已在 deps 中），
   不要引 `miuix-icons` 的图标对象。
3. **不要新建 ViewModel**。miuix 页面必须**复用** `ui/` 下同名页面的 ViewModel。
   数据层和业务逻辑零修改，只换表现层。
4. **不要修改这些目录下的文件**，除非用户明确要求：
   `theme/`、`navigation/`、`viewmodel/`、`data/`、`util/`、`nlp/`。
5. **不要引入新的依赖**（Koin、DataStore、Hilt 等），沿用 `ThemeManager` +
   `SharedPreferences` + `mutableState` 的现有模式。
6. **不要升级到 Navigation 3**，保持 `navigation-compose:2.9.2`。
7. **miuix 页面的函数签名必须跟原版完全一致**（参数名、类型、默认值），
   否则 NavHost 里的分流没法直接换。

### 标准工作流

收到 "port 某某页面" 的任务时：

1. 读 `ui/XxxScreen.kt`（原 M3 版），摸清功能、参数、依赖的 ViewModel。
2. 在 `ui/miuix/` 下新建 `MiuixXxxScreen.kt`，**函数签名与原版完全一致**。
3. 用 miuix 组件重写 UI 部分，业务调用（ViewModel 方法、navigator 跳转）
   原样照抄。
4. 在 `ui/ZhihuMain.kt` 对应的 `composable<>` 加 if/else 分支：
   ```kotlin
   composable<Xxx> { entry ->
       if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
           MiuixXxxScreen(...)
       } else {
           XxxScreen(...)
       }
   }
   ```
   如果 NavHost 里已经存在 `themedComposable<>` helper，优先用它。
5. 按现有的构建/调试/复检流程验证（见上面的 "Android 调试标准流程" 和
   "UI 双代理复检"）。**必须验证两个主题下都正常**，不是只测一个。

### 设置项语义映射

| 语义 | M3 控件 | miuix 等价 | 注意 |
|---|---|---|---|
| 开关 | `SettingItemWithSwitch` | `SwitchPreference` | 1:1 |
| 跳子页 | `SettingItem + onClick` | `ArrowPreference` | 1:1 |
| 单选（少选项） | `SettingItem + Dropdown` | `WindowSpinnerPreference` | 内联弹出 |
| 多选 | 自定义 Dialog | `MiuixMultiSelectExpandable` | 内联折叠 |
| 数值滑块 | `Slider` 行 | 待定（ArrowPreference + 展开 Slider） | |
| 颜色选择 | 自定义 `ColorPickerDialog` | `MiuixColorPickerSheet` | BottomSheet |
| 输入文本 | `TextField` | `TextField` + Card 容器 | miuix 无专门 pref |
| 纯展示 | `SettingItem` 不可点 | `ArrowPreference` summary 不传 onClick | |
| BottomSheet | `MyModalBottomSheet` | `WindowBottomSheet` + `MutableState<Boolean>` | sheet 始终在树里 |

### 设置项映射进度

| 设置 key | M3 控件 | miuix 控件 | 状态 |
|---|---|---|---|
| `themeStyle` | `SettingItemWithSwitch` | `WindowSpinnerPreference` | ✅ |
| `useDynamicColor` | `SettingItemWithSwitch` | `SwitchPreference` | ✅ |
| `customThemeColor` | 自定义 Dialog | `MiuixColorPickerSheet` | ✅ |
| `backgroundColorLight/Dark` | 自定义 Dialog | `MiuixColorPickerSheet` | ✅ |
| `bottomBarItems` | 自定义多选 | `MiuixMultiSelectExpandable` | ✅ |
| `fontSize` | Slider | ArrowPreference + 展开 Slider | ✅ |
| `lineHeight` | Slider | ArrowPreference + 展开 Slider | ✅ |
| `feedCardStyle` | Dropdown | `WindowSpinnerPreference` | ✅ |
| `answerSwitchMode` | Dropdown | `WindowSpinnerPreference` | ✅ |
| `answerDoubleTapAction` | Dropdown | `WindowSpinnerPreference` | ✅ |
| `showFeedThumbnail` | `SettingItemWithSwitch` | `SwitchPreference` | ✅ |
| `showRefreshFab` | `SettingItemWithSwitch` | `SwitchPreference` | ✅ |
| `duo3_*` 系列 | `SettingItemWithSwitch` | `SwitchPreference` | ✅ |
| `autoHideBottomBar` | `SettingItemWithSwitch` | `SwitchPreference` | ✅ |
| `bottomBarTapScrollToTop` | `SettingItemWithSwitch` | `SwitchPreference` | ✅ |

### 设置项 port 规则

1. 新增/修改设置项时，先在映射进度表登记 key + 控件类型
2. SharedPreferences key 和默认值从 `SettingsKeys` object 读，**不许在页面 hardcode 字符串 key**
3. miuix 端功能必须等价 M3 端：同样 key、同样可选值范围、同样默认值
4. M3 写法奇葩（如两个 Switch 拼出单选逻辑）→ miuix 端做语义重构，一个 `WindowSpinnerPreference` 解决
5. M3 没有的功能 → miuix 不主动加，先对齐再增强
6. miuix 表达力不够 → 退回基础组件在 Card 里自己拼，不强套 preference
7. 复杂 Dialog → 优先 `WindowBottomSheet`

### 常见坑

- miuix `Scaffold` 没有 `contentWindowInsets` 参数，需要 inset 时在内部
  用 `WindowInsets.safeDrawing`。
- miuix `Button` 的 content lambda 是 `BoxScope`，不是 `RowScope`，图标+文字
  横排要自己包一层 `Row`。
- 状态栏图标颜色由顶层 `ZhihuTheme` 统一处理，子页面**不要**再写
  `WindowCompat` 相关代码。
- `androidx.compose.material3.Text` 和 `top.yukonga.miuix.kmp.basic.Text`
  同名，**不要在一个文件里混引**。miuix 页面里只引 miuix 的 Text。
- 一个 miuix 页面里如果不小心引了 `androidx.compose.material3.*` 的组件，
  视觉风格会瞬间穿帮（M3 的圆角、阴影、密度跟 miuix 完全不一样）。**review
  自己写的代码时第一件事：检查 import 列表**。
- **BottomSheet 只能用 WindowBottomSheet + MutableState 模式**，不要用 if 包裹。sheet 始终留在树里，`show` 参数用 `MutableState<Boolean>`，内部 `LaunchedEffect(show.value)` 在打开时重置状态。参考 `MiuixColorPickerSheet.kt`。
- 所有 miuix 页面的 TopAppBar 模糊必须走四段式管线（`theme/Backdrop.kt`）：
  1. `val backdrop = rememberMiuixBlurBackdrop(blurEnabled)` 创建模糊源
  2. LazyColumn 加 `.layerBackdrop(backdrop)` 指定采样源
  **关键**：`layerBackdrop` 必须在 `overScrollVertical` / `nestedScroll` 之前，
  否则 overscroll 变换会导致 backdrop 捕获到错误像素，模糊无效。
  正确顺序：`fillMaxSize → layerBackdrop → overScrollVertical → nestedScroll`
  3. TopAppBar 加 `.installerMiuixBlurEffect(backdrop)` 应用纹理模糊
  4. TopAppBar 的 containerColor 用 `backdrop.getMiuixAppBarColor()`（透明）
  设置项 key `"blurEnabled"`，默认 `true`。
- **More 菜单（FeedCard 右上角弹出菜单）暂未实现**。`WindowDropdownMenu` 语义是锚点浮动菜单，跟 `WindowSpinnerPreference` 不同。遇到时参照 InstallerX `WindowListPopup`。

### 反面教材（看到立刻停下）

```kotlin
// ❌ 不要引这些 miuix 包，先跟用户确认
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.blur.layerBackdrop

// ❌ 不要新建 ViewModel
class MiuixBlocklistViewModel : ViewModel() { ... }
// 直接 viewModel<BlocklistViewModel>() 复用

// ❌ 不要改原 M3 页面去"统一"什么
// ui/BlocklistSettingsScreen.kt 必须保持完全不变

// ❌ 不要改路由定义
// navigation/NavDestination.kt 不归 miuix 适配管

// ❌ 不要在 miuix 页面里写状态栏处理
SideEffect {
    WindowCompat.getInsetsController(...).isAppearanceLightStatusBars = ...
}
// 顶层 ZhihuTheme 已经处理
```

### 双向开关约束

每个能切换 miuix 风格的全局入口（目前只有外观设置页）必须在 M3 版本和 miuix
版本里都写一遍，否则会出现单向不可达（切得过去回不来）。**这是脚手架阶段就应
满足的约束，不是 port 任务里要补的。**

### 范围控制

每个 port 任务**只 port 一个页面**。即使发现了其他页面的 bug、性能问题、
代码重复，**只记 TODO 注释**，不顺手改。一个 PR 一个页面方便 review 和 revert。

### 同步 AGENTS.md

本仓库的 `CLAUDE.md` 和 `AGENTS.md` 是同一份内容（diff 为空）。修改其中一份后
必须同步到另一份，可以用 `cp CLAUDE.md AGENTS.md` 或建立 symlink。

### miuix 设置页标准布局

- 每个分组 = `SmallTitle` + `Card`
- Card 标准 padding：`.padding(horizontal = 12.dp).padding(bottom = 12.dp)`
- Card 内部 preference 之间自动有分隔线，不要手动加 Divider
- 顶部第一项前留一个 `item { Spacer(Modifier.size(12.dp)) }`
- TopAppBar 配 `MiuixScrollBehavior()` 实现大字折叠，LazyColumn 用 `nestedScroll`

### API 映射追加

| 单选下拉（Material 3 / Miuix / 系统主题这类） | `WindowSpinnerPreference` + `DropdownItem` |
| 分组容器 | `Card`（basic 包），自带 surface 色和圆角 |
| TopAppBar 滚动行为 | `MiuixScrollBehavior()`（无参） |

### 包白名单更新

允许的 miuix 包：
- top.yukonga.miuix.kmp.basic.*
- top.yukonga.miuix.kmp.preference.*
- top.yukonga.miuix.kmp.theme.*
- top.yukonga.miuix.kmp.utils.* (新增：overScrollVertical、scrollEndHaptic 等修饰符)
- top.yukonga.miuix.kmp.icon.* (新增：MiuixIcons 图标资源)
- top.yukonga.miuix.kmp.window.* (新增：WindowBottomSheet、WindowDialog、WindowSpinnerPreference 配套用)
- top.yukonga.miuix.kmp.blur.* (新增：仅当按完整链路使用——LayerBackdrop + textureBlur + layerBackdrop 三件套都要用上)

### Token 映射表（已确认）

| Material 3 | miuix 映射 |
|---|---|
| surface | MiuixTheme.colorScheme.surface |
| onSurface | MiuixTheme.colorScheme.onSurface |
| background | MiuixTheme.colorScheme.background |
| primary | MiuixTheme.colorScheme.primary |
| surfaceContainerLow | MiuixTheme.colorScheme.surface（近似） |
| surfaceBright | MiuixTheme.colorScheme.surface（近似） |
| tertiary | MiuixTheme.colorScheme.tertiaryContainer（近似） |
| inversePrimary | MiuixTheme.colorScheme.primary.copy(alpha=0.5f)（近似） |

涉及这 4 个近似 token 的代码**优先封装到共享组件**（MiuixFeedCard、MiuixStatusBadge 等），不要在使用点重复翻译。

### BottomSheet / Dialog 标准模式

- 用 `WindowBottomSheet(show: MutableState<Boolean>, onDismissRequest, content)`
- show 传 MutableState 引用，不是 Boolean
- 永远跟 Dialog 同级，不要嵌套
- 内容组件拆出来不带 host（如 ColorPickerContent），调用方决定用 Dialog 还是 BottomSheet