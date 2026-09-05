# Android CLI 与 Codex Skills

安装日期：2026-09-04。

## 本机 CLI

- Google 官方 Android CLI：`1.0.16261425`。
- 程序：`%USERPROFILE%\AppData\AndroidCLI\android.exe`，已加入用户 PATH。
- 已识别现有 SDK：`%LOCALAPPDATA%\Android\Sdk`。
- 已运行文档搜索，初始化官方 Knowledge Base。

已经打开的 Codex/终端可能尚未继承新 PATH。重启应用后可直接使用 `android`；当前 PowerShell 可直接调用：

```powershell
& "$env:USERPROFILE\AppData\AndroidCLI\android.exe" --version
& "$env:USERPROFILE\AppData\AndroidCLI\android.exe" skills list --agent=codex --project=.
& "$env:USERPROFILE\AppData\AndroidCLI\android.exe" docs search 'Jetpack Compose state'
```

## 项目 Skills

官方来源：[android/skills](https://github.com/android/skills)。
固定提交：`725364add95396448b0c91c585265dbaf1c36987`，上游插件版本 `1.0.10`。

使用 Codex 的 skill-installer 从该提交安装全部 22 个 Skills 到 `.agents/skills/`，保留原项目 Skills。每个目录包含完整的上游资源以及上游 Apache 2.0 `LICENSE.txt`。

```text
adaptive
agp-9-upgrade
android-cli
android-intent-security
android-profiler
appfunctions
camerax
display-glasses-with-jetpack-compose-glimmer
edge-to-edge
engage-sdk-integration
leanback-to-compose-tv-migration
media3-cast-integration
migrate-xml-views-to-jetpack-compose
navigation-3
play-billing-library-version-upgrade
play-policy-insights
r8-analyzer
restore-credentials
styles
testing-setup
verified-email
wear-compose-m3
```

Codex 会自动发现项目 `.agents/skills/`。下一轮可以指定 `$android-cli`、`$r8-analyzer` 等 Skills；若列表未刷新，重启 Codex。这些 Skills 按任务匹配加载，安装本身不执行 AGP、导航或其他迁移；例如上游 `agp-9-upgrade` 明确不适用于 KMP 项目。

## 后续更新

在项目根目录执行以下命令会获取最新 CLI 和官方 Skills；更新后检查 Skills 差异并更新本文中的版本记录：

```powershell
android update
android skills add --all --agent=codex --project=.
android skills list --agent=codex --project=.
```

项目构建、设备选择和验证仍遵守 `CLAUDE.md`。官方文档当前将 Windows 的 `android emulator` 标记为禁用；需要本地 AVD 时使用现有 Android SDK 的 emulator 工具。

参考：[Android CLI 文档](https://developer.android.com/tools/agents/android-cli)、[Codex Skills 发现规则](https://learn.chatgpt.com/docs/build-skills#where-codex-loads-local-skills)。
