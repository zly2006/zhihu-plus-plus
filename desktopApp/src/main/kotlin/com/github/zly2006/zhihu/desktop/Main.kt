package com.github.zly2006.zhihu.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.github.zly2006.zhihu.shared.ZhihuSharedApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Zhihu++",
    ) {
        ZhihuSharedApp()
    }
}
