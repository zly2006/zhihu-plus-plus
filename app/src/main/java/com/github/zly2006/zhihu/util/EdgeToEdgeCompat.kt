package com.github.zly2006.zhihu.util

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

@Suppress("NOTHING_TO_INLINE")
inline fun ComponentActivity.enableEdgeToEdgeCompat() {
    enableEdgeToEdge()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Fix for three-button nav not properly going edge-to-edge.
        // TODO: https://issuetracker.google.com/issues/298296168
        window.isNavigationBarContrastEnforced = false
    }
}
