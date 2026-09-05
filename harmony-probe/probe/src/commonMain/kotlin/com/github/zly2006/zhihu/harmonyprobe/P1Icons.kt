package com.github.zly2006.zhihu.harmonyprobe

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * P1 内联的 Material Filled 图标，路径数据逐行取自 CMP 1.7.3 material-icons-extended/core 源码包
 * （androidx.compose.material.icons.filled.Home/Newspaper/ManageAccounts），与真实 ZhihuMain 使用同一套图标。
 *
 * CPF 未发布带 ohos 变体的 material-icons 构件，因此不能以 Gradle 依赖方式引入 icons 库，这里手动内联。
 * addPath 按位置传参：CPF 发布的 ui klib 参数名不可靠（具名参数解析失败）。
 */
private fun iconBuilder(name: String): ImageVector.Builder =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )

private fun ImageVector.Builder.fillPath(pathData: String): ImageVector.Builder =
    addPath(addPathNodes(pathData), PathFillType.NonZero, "p", SolidColor(Color(0xFF000000)))

private var _home: ImageVector? = null

val P1IconHome: ImageVector
    get() {
        if (_home != null) return _home!!
        _home = iconBuilder("P1Filled.Home")
            .fillPath("M10,20 v-6 h4 v6 h5 v-8 h3 L12,3 L2,12 h3 v8 z")
            .build()
        return _home!!
    }

private var _newspaper: ImageVector? = null

val P1IconNewspaper: ImageVector
    get() {
        if (_newspaper != null) return _newspaper!!
        _newspaper = iconBuilder("P1Filled.Newspaper")
            .fillPath(
                "M22,3 l-1.67,1.67 L18.67,3 L17,4.67 L15.33,3 l-1.66,1.67 L12,3 l-1.67,1.67 " +
                    "L8.67,3 L7,4.67 L5.33,3 L3.67,4.67 L2,3 v16 c0,1.1,0.9,2,2,2 l16,0 " +
                    "c1.1,0,2,-0.9,2,-2 V3 z " +
                    "M11,19 H4 v-6 h7 V19 z " +
                    "M20,19 h-7 v-2 h7 V19 z " +
                    "M20,15 h-7 v-2 h7 V15 z " +
                    "M20,11 H4 V8 h16 V11 z",
            )
            .build()
        return _newspaper!!
    }

private var _manageAccounts: ImageVector? = null

val P1IconManageAccounts: ImageVector
    get() {
        if (_manageAccounts != null) return _manageAccounts!!
        _manageAccounts = iconBuilder("P1Filled.ManageAccounts")
            .fillPath("M10,8 m-4,0 a4,4 0,1,1 8,0 a4,4 0,1,1 -8,0")
            .fillPath(
                "M10.67,13.02 C10.45,13.01,10.23,13,10,13 c-2.42,0,-4.68,0.67,-6.61,1.82 " +
                    "C2.51,15.34,2,16.32,2,17.35 V20 h9.26 " +
                    "C10.47,18.87,10,17.49,10,16 C10,14.93,10.25,13.93,10.67,13.02 z",
            )
            .fillPath(
                "M20.75,16 c0,-0.22,-0.03,-0.42,-0.06,-0.63 l1.14,-1.01 l-1,-1.73 l-1.45,0.49 " +
                    "c-0.32,-0.27,-0.68,-0.48,-1.08,-0.63 L18,11 h-2 l-0.3,1.49 " +
                    "c-0.4,0.15,-0.76,0.36,-1.08,0.63 l-1.45,-0.49 l-1,1.73 l1.14,1.01 " +
                    "c-0.03,0.21,-0.06,0.41,-0.06,0.63 s0.03,0.42,0.06,0.63 l-1.14,1.01 l1,1.73 l1.45,-0.49 " +
                    "c0.32,0.27,0.68,0.48,1.08,0.63 L16,21 h2 l0.3,-1.49 " +
                    "c0.4,-0.15,0.76,-0.36,1.08,-0.63 l1.45,0.49 l1,-1.73 l-1.14,-1.01 " +
                    "C20.72,16.42,20.75,16.22,20.75,16 z " +
                    "M17,18 c-1.1,0,-2,-0.9,-2,-2 s0.9,-2,2,-2 s2,0.9,2,2 S18.1,18,17,18 z",
            )
            .build()
        return _manageAccounts!!
    }
