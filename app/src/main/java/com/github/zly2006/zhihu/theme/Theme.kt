package com.github.zly2006.zhihu.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.dynamicColorScheme

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
     */
)

@Composable
fun ZhihuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useDynamicColor = ThemeManager.getUseDynamicColor()

    // Get custom background color from preferences
    val preferences = context.getSharedPreferences(
        com.github.zly2006.zhihu.ui.PREFERENCE_NAME,
        android.content.Context.MODE_PRIVATE,
    )
    val backgroundColorKey = if (darkTheme) "backgroundColorDark" else "backgroundColorLight"
    val defaultBackgroundColor = if (darkTheme) 0xFF121212.toInt() else 0xFFFFFFFF.toInt()
    val customBackgroundColor = androidx.compose.ui.graphics.Color(
        preferences.getInt(backgroundColorKey, defaultBackgroundColor),
    )

    val baseColorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        !useDynamicColor -> {
            dynamicColorScheme(
                seedColor = ThemeManager.getCustomColor(),
                isDark = darkTheme,
                isAmoled = false,
            )
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Apply custom background color
    val colorScheme = baseColorScheme.copy(
        background = customBackgroundColor,
        surface = customBackgroundColor,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
