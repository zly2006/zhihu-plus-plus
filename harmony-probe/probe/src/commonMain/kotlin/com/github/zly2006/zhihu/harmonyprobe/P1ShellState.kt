package com.github.zly2006.zhihu.harmonyprobe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * P1 共享主壳的宿主侧状态。
 *
 * [backHandler] 由 P1Shell 注册（navController.popBackStack），供原生导出 [handleBack] 统一消费返回键。
 * [systemDarkMode] 由 ArkTS 壳在 onConfigurationUpdate 中推送，作为 currentSystemInDarkTheme 的 OHOS actual 数据源。
 * [p2SliceOpen] 打开 P2 日报切片时置位。
 * [p3SliceOpen] 打开 P3 数据库选型切片时置位。
 */
object P1ShellState {
    var backHandler: (() -> Boolean)? = null

    var p2SliceOpen by mutableStateOf(false)

    var p3SliceOpen by mutableStateOf(false)

    var systemDarkMode by mutableStateOf(false)
        private set

    fun applyColorMode(dark: Boolean) {
        systemDarkMode = dark
    }

    /**
     * 统一返回键分发：P2 内部（文章/压力样本）→ 关闭 P2 切片 → P1 导航返回栈。
     * 返回 false 表示主壳没有消费，宿主应继续默认行为（退出 Ability）。
     */
    fun handleBack(): Boolean {
        if (p3SliceOpen) {
            p3SliceOpen = false
            return true
        }
        if (p2SliceOpen) {
            if (P2State.showArticle || P2State.showStress) {
                P2State.showArticle = false
                P2State.showStress = false
            } else {
                p2SliceOpen = false
            }
            return true
        }
        return backHandler?.invoke() ?: false
    }
}
