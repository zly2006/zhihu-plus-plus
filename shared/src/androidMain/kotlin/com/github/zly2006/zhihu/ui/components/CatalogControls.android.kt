package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.theme.AndroidGlassBackdrop
import com.github.zly2006.zhihu.theme.LocalGlassBackdrop
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.backdrop.catalog.components.LiquidSlider
import com.kyant.backdrop.catalog.components.LiquidToggle
import kotlin.math.roundToInt

@Composable
private fun controlBackdrop(): Backdrop {
    val overlay = LocalGlassBackdrop.current as? AndroidGlassBackdrop
    val surface = MaterialTheme.colorScheme.surfaceBright
    // 内联控件不采样正在录制自身的内容层；滑块仍由官方组件采样自身轨道。
    val canvas = rememberCanvasBackdrop { drawRect(surface) }
    return overlay?.nativeBackdrop ?: canvas
}

@Composable
internal actual fun CatalogButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    containerColor: Color,
    contentColor: Color,
    content: @Composable RowScope.() -> Unit,
) {
    val action by rememberUpdatedState(onClick)
    val active by rememberUpdatedState(enabled)
    val backdrop = controlBackdrop()
    OfficialLiquidAppearance {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                Box(modifier.semantics { if (!enabled) disabled() }, propagateMinConstraints = true) {
                    LiquidButton(
                        onClick = { if (active) action() },
                        backdrop = backdrop,
                        modifier = Modifier,
                        isInteractive = enabled,
                        surfaceColor = containerColor.copy(alpha = containerColor.alpha * 0.72f),
                        content = content,
                    )
                    if (!enabled) BlockControlInput(Modifier.matchParentSize())
                }
            }
        }
    }
}

@Composable
actual fun LiquidSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier, enabled: Boolean) {
    val currentChecked by rememberUpdatedState(checked)
    val change by rememberUpdatedState(onCheckedChange)
    val active by rememberUpdatedState(enabled)
    val readValue = remember { { currentChecked } }
    val select = remember {
        { value: Boolean ->
            if (active) change?.invoke(value)
            Unit
        }
    }
    val backdrop = controlBackdrop()
    OfficialLiquidAppearance {
        Box(
            modifier
                .size(64.dp, 48.dp)
                .alpha(if (enabled) 1f else 0.4f)
                .semantics(mergeDescendants = true) {
                    role = Role.Switch
                    toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                    if (!enabled) disabled()
                    if (enabled && onCheckedChange != null) {
                        onClick {
                            onCheckedChange(!checked)
                            true
                        }
                    }
                }
                // 官方 Toggle 的手势观察器与点击共存；消费点击，避免设置行重复触发。
                .clickable(interactionSource = null, indication = null, enabled = enabled, onClick = {}),
            contentAlignment = Alignment.Center,
        ) {
            LiquidToggle(selected = readValue, onSelect = select, backdrop = backdrop, modifier = Modifier.size(64.dp, 48.dp))
            if (!enabled || onCheckedChange == null) BlockControlInput(Modifier.matchParentSize())
        }
    }
}

@Composable
internal actual fun CatalogSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChangeFinished: (() -> Unit)?,
) {
    require(steps >= 0)
    val currentValue by rememberUpdatedState(value)
    val change by rememberUpdatedState(onValueChange)
    val finished by rememberUpdatedState(onValueChangeFinished)
    val active by rememberUpdatedState(enabled)
    val backdrop = controlBackdrop()
    // 官方滑杆按范围计算动画进度；退化范围没有可拖动区间。
    if (valueRange.endInclusive <= valueRange.start) {
        androidx.compose.material3.Slider(value, onValueChange, modifier, enabled = false, valueRange = valueRange)
        return
    }
    val select = remember(valueRange, steps) {
        { proposed: Float ->
            val bounded = proposed.coerceIn(valueRange)
            val next = if (steps == 0) {
                bounded
            } else {
                val increment = (valueRange.endInclusive - valueRange.start) / (steps + 1)
                valueRange.start + ((bounded - valueRange.start) / increment).roundToInt() * increment
            }
            if (active) change(next.coerceIn(valueRange))
        }
    }
    val readValue = remember { { currentValue } }
    OfficialLiquidAppearance {
        Box(
            modifier.heightIn(min = 48.dp).alpha(if (enabled) 1f else 0.4f).semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(valueRange), valueRange, steps)
                if (!enabled) disabled()
                setProgress { proposed ->
                    if (enabled) {
                        select(proposed)
                        finished?.invoke()
                    }
                    enabled
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            key(valueRange, steps) {
                LiquidSlider(
                    value = readValue,
                    onValueChange = select,
                    valueRange = valueRange,
                    visibilityThreshold = (valueRange.endInclusive - valueRange.start) / 1000f,
                    backdrop = backdrop,
                    modifier = Modifier.heightIn(min = 48.dp).pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Final)
                            } while (event.changes.any { it.pressed })
                            if (active) finished?.invoke()
                        }
                    },
                )
            }
            if (!enabled) BlockControlInput(Modifier.matchParentSize())
        }
    }
}

@Composable
private fun BlockControlInput(modifier: Modifier) {
    Box(
        modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) awaitPointerEvent().changes.forEach { it.consume() }
            }
        },
    )
}
