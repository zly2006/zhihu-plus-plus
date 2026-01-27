package com.github.zly2006.zhihu.ui.components

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import kotlin.math.roundToInt

@Composable
fun DraggableRefreshButton(
    modifier: Modifier = Modifier,
    preferenceName: String = "fabRefresh",
    onClick: () -> Unit,
    content: @Composable () -> Unit = {
        Icon(Icons.Default.Refresh, contentDescription = "刷新")
    },
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val preferences = remember {
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    var offsetX by remember { mutableFloatStateOf(preferences.getFloat("$preferenceName-x", Float.MAX_VALUE)) }
    var offsetY by remember { mutableFloatStateOf(preferences.getFloat("$preferenceName-y", Float.MAX_VALUE)) }
    var pressing by remember { mutableStateOf(false) }

    fun adjustFabPosition() {
        with(density) {
            offsetX = offsetX.coerceIn(0f, configuration.screenWidthDp.dp.toPx() - 56.dp.toPx())
            offsetY = offsetY.coerceIn(0f, configuration.screenHeightDp.dp.toPx() - 250.dp.toPx())
        }
    }

    adjustFabPosition()

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(if (pressing) 1 else 300),
        label = "offsetX",
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = tween(if (pressing) 1 else 300),
        label = "offsetY",
    )

    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        modifier = modifier
            .offset { IntOffset(animatedOffsetX.roundToInt(), animatedOffsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        pressing = true
                        // 震动反馈
                        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            (context.getSystemService(VibratorManager::class.java) as VibratorManager).defaultVibrator
                        } else {
                            @Suppress("DEPRECATION")
                            context.getSystemService(Vibrator::class.java) as Vibrator
                        }
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                50,
                                VibrationEffect.DEFAULT_AMPLITUDE,
                            ),
                        )
                    },
                    onDragEnd = {
                        pressing = false
                        adjustFabPosition()
                        with(density) {
                            val screenWidth = configuration.screenWidthDp.dp.toPx()
                            offsetX =
                                if (offsetX < screenWidth / 2) {
                                    0f
                                } else {
                                    screenWidth - 56.dp.toPx()
                                }
                        }
                        preferences.edit {
                            putFloat("$preferenceName-x", offsetX)
                            putFloat("$preferenceName-y", offsetY)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (pressing) {
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                            adjustFabPosition()
                        }
                    },
                )
            },
        content = content,
    )
}
