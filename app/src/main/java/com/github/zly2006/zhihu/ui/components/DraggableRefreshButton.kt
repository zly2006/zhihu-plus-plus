package com.github.zly2006.zhihu.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * For internal use copied from [androidx.compose.animation.core.animateValueAsState]
 */
@Composable
private fun <T, V : AnimationVector> animateValue(
    targetValue: T,
    typeConverter: TwoWayConverter<T, V>,
    animationSpec: AnimationSpec<T> = remember { spring() },
    visibilityThreshold: T? = null,
    label: String = "ValueAnimation",
    finishedListener: ((T) -> Unit)? = null
): Animatable<T, V> {
    val animatable = remember { Animatable(targetValue, typeConverter, visibilityThreshold, label) }
    val listener by rememberUpdatedState(finishedListener)
    val animSpec: AnimationSpec<T> by
        rememberUpdatedState(
            animationSpec.run {
                if (
                    visibilityThreshold != null &&
                    this is SpringSpec &&
                    this.visibilityThreshold != visibilityThreshold
                ) {
                    spring(dampingRatio, stiffness, visibilityThreshold)
                } else {
                    this
                }
            }
        )
    val channel = remember { Channel<T>(Channel.CONFLATED) }
    SideEffect { channel.trySend(targetValue) }
    LaunchedEffect(channel) {
        for (target in channel) {
            // This additional poll is needed because when the channel suspends on receive and
            // two values are produced before consumers' dispatcher resumes, only the first value
            // will be received.
            // It may not be an issue elsewhere, but in animation we want to avoid being one
            // frame late.
            val newTarget = channel.tryReceive().getOrNull() ?: target
            launch {
                if (newTarget != animatable.targetValue) {
                    animatable.animateTo(newTarget, animSpec)
                    listener?.invoke(animatable.value)
                }
            }
        }
    }
    return animatable
}

@Composable
fun animateFloatAsState(
    targetValue: Float,
    immediatelyTransitionToTarget: Boolean = false,
    animationSpec: AnimationSpec<Float>,
    visibilityThreshold: Float = 0.01f,
    label: String = "FloatAnimation",
    finishedListener: ((Float) -> Unit)? = null
): State<Float> {
    val state = animateValue(
        targetValue,
        Float.VectorConverter,
        animationSpec,
        visibilityThreshold,
        label,
        finishedListener,
    )
    LaunchedEffect(immediatelyTransitionToTarget, targetValue) {
        if (immediatelyTransitionToTarget) {
            state.snapTo(targetValue)
        }
    }
    return state.asState()
}

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
            offsetY = offsetY.coerceIn(0f, configuration.screenHeightDp.dp.toPx() - 160.dp.toPx())
        }
    }

    adjustFabPosition()

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        immediatelyTransitionToTarget = pressing,
        animationSpec = tween(300),
        label = "offsetX",
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        immediatelyTransitionToTarget = pressing,
        animationSpec = tween(300),
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
                            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                        } else {
                            @Suppress("DEPRECATION")
                            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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
