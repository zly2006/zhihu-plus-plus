package com.github.zly2006.zhihu.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.ui.subscreens.LIST_PANE_DEFAULT_WIDTH_DP_PREFERENCE_KEY

@Composable
fun rememberListPaneDefaultWidthDp(): Int {
    val context = LocalContext.current
    return rememberListPaneDefaultWidthDp(context)
}

@Composable
fun rememberListPaneDefaultWidthDp(context: Context): Int {
    val preferences = remember {
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }
    var widthDp by remember {
        mutableIntStateOf(preferences.getInt(LIST_PANE_DEFAULT_WIDTH_DP_PREFERENCE_KEY, 320))
    }

    DisposableEffect(preferences) {
        val listener =
            android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (key == LIST_PANE_DEFAULT_WIDTH_DP_PREFERENCE_KEY) {
                    widthDp = sharedPreferences.getInt(LIST_PANE_DEFAULT_WIDTH_DP_PREFERENCE_KEY, 320)
                }
            }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    return widthDp
}
