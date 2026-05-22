package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpOffset

@Composable
fun OpenImagePreviewContent(
    url: String,
    onDismiss: () -> Unit,
    onSaveImage: () -> Unit,
    onShareImage: () -> Unit,
    onOpenInBrowser: () -> Unit,
    imageContent: @Composable (
        url: String,
        onClick: () -> Unit,
        onLongClick: (Offset) -> Unit,
    ) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // 禁用图片查看器自带的震动反馈，保持长按菜单手感稳定。
        CompositionLocalProvider(LocalHapticFeedback provides NoopHapticFeedback) {
            imageContent(
                url,
                onDismiss,
            ) { offset ->
                menuOffset = offset
                showMenu = true
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = with(density) {
                DpOffset(
                    menuOffset.x.toDp(),
                    menuOffset.y.toDp(),
                )
            },
        ) {
            DropdownMenuItem(
                text = { Text("保存图片") },
                onClick = {
                    showMenu = false
                    onSaveImage()
                },
            )
            DropdownMenuItem(
                text = { Text("分享图片") },
                onClick = {
                    showMenu = false
                    onShareImage()
                },
            )
            DropdownMenuItem(
                text = { Text("在浏览器中打开") },
                onClick = {
                    showMenu = false
                    onOpenInBrowser()
                },
            )
        }
    }
}

private object NoopHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        // noop
    }
}
