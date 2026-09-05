# Android Liquid Glass 官方组件

来源：https://github.com/Kyant0/AndroidLiquidGlass/tree/1.0.6/catalog/src/main/java/com/kyant/backdrop/catalog

版本：1.0.6，commit `896a94a3ade1cc1a940b92365f942a34971fecda`。

官方 Maven `io.github.kyant0:backdrop:1.0.6` 只发布底层特效，README 明确说明不含高层组件。这里直接编译官方 `LiquidBottomTabs`、`LiquidBottomTab`、`LiquidButton`、`LiquidToggle`、`LiquidSlider` 及其依赖的原始源码，包名及交互实现保持不变；应用只负责导航、主题环境与系统边距适配。上游源码不参与应用 ktlint 格式化。

授权见 `LICENSE`（Apache-2.0）。`SHA256SUMS` 记录原文件，用于升级时核对。
