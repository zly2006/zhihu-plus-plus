/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
