package com.github.zly2006.zhihu.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val LiquidGlassShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private fun glassText(size: Int, height: Int, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f) = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = size.sp,
    lineHeight = height.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
)

internal val LiquidGlassTypography = Typography(
    displayLarge = glassText(48, 54, FontWeight.Bold, -0.6f),
    displayMedium = glassText(40, 46, FontWeight.Bold, -0.5f),
    displaySmall = glassText(34, 40, FontWeight.Bold, -0.4f),
    headlineLarge = glassText(32, 38, FontWeight.Bold, -0.4f),
    headlineMedium = glassText(28, 34, FontWeight.Bold, -0.3f),
    headlineSmall = glassText(24, 30, FontWeight.SemiBold, -0.2f),
    titleLarge = glassText(21, 28, FontWeight.SemiBold),
    titleMedium = glassText(17, 24, FontWeight.SemiBold),
    titleSmall = glassText(15, 21, FontWeight.SemiBold),
    bodyLarge = glassText(17, 25),
    bodyMedium = glassText(15, 22),
    bodySmall = glassText(13, 19),
    labelLarge = glassText(15, 20, FontWeight.SemiBold),
    labelMedium = glassText(12, 16, FontWeight.Medium),
    labelSmall = glassText(11, 15, FontWeight.Medium),
)
