package com.github.zly2006.zhihu.v2.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun DraggableRefreshButton(
    context: Context,
    onRefresh: () -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val preferences = remember {
        context.getSharedPreferences(
            "com.github.zly2006.zhihu_preferences",
            Context.MODE_PRIVATE
        )
    }

    var offsetX by remember { mutableStateOf(preferences.getFloat("fabRefresh_x", Float.MAX_VALUE)) }
    var offsetY by remember { mutableStateOf(preferences.getFloat("fabRefresh_y", Float.MAX_VALUE)) }
    var pressing by remember { mutableStateOf(false) }

    fun adjustFabPosition() {
        with(density) {
            offsetX = offsetX.coerceIn(0f, configuration.screenWidthDp.dp.toPx() - 56.dp.toPx())
            offsetY = offsetY.coerceIn(0f, configuration.screenHeightDp.dp.toPx() - 160.dp.toPx())
        }
    }

    adjustFabPosition()

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(if (pressing) 1 else 300),
        label = "offsetX"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = tween(if (pressing) 1 else 300),
        label = "offsetY"
    )

    FloatingActionButton(
        onClick = onRefresh,
        shape = CircleShape,
        modifier = Modifier
            .offset { IntOffset(animatedOffsetX.roundToInt(), animatedOffsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        pressing = true
                        // 震动反馈
                        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                        } else {
                            @Suppress("DEPRECATION")
                            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(
                                VibrationEffect.createOneShot(
                                    50,
                                    VibrationEffect.DEFAULT_AMPLITUDE
                                )
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(50)
                        }
                    },
                    onDragEnd = {
                        pressing = false
                        adjustFabPosition()
                        with(density) {
                            val screenWidth = configuration.screenWidthDp.dp.toPx()
                            offsetX =
                                if (offsetX < screenWidth / 2) 0f
                                else screenWidth - 56.dp.toPx()
                        }
                        preferences.edit()
                            .putFloat("fabRefresh_x", offsetX)
                            .putFloat("fabRefresh_y", offsetY)
                            .apply()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (pressing) {
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                            adjustFabPosition()
                        }
                    }
                )
            }
    ) {
        Icon(Icons.Default.Refresh, contentDescription = "刷新")
    }
}
