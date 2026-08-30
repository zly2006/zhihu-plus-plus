package com.github.zly2006.zhihu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.zly2006.zhihu.platform.macosContentAreaInsetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal actual fun MacosModalBottomSheetDialog(
    onDismissRequest: () -> Unit,
    contentColor: Color,
    properties: ModalBottomSheetProperties,
    predictiveBackProgress: Animatable<Float, AnimationVector1D>,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = properties.shouldDismissOnBackPress,
            dismissOnClickOutside = properties.shouldDismissOnClickOutside,
            usePlatformDefaultWidth = false,
            usePlatformInsets = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = macosContentAreaInsetState.value),
        ) {
            content()
        }
    }
}
