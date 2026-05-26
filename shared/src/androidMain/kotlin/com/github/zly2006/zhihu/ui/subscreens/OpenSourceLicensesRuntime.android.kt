package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.shared.platform.rememberIsLiteVariant
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withContext

@Composable
actual fun rememberOpenSourceLicensesLibraries(): Libs {
    val context = LocalContext.current
    return remember(context) {
        Libs
            .Builder()
            .withContext(context)
            .build()
    }
}

@Composable
actual fun rememberShowFullVariantLicenses(): Boolean = !rememberIsLiteVariant()
