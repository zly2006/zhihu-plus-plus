package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mikepenz.aboutlibraries.Libs

@Composable
actual fun rememberOpenSourceLicensesLibraries(): Libs = remember { Libs(emptyList(), emptySet()) }

@Composable
actual fun rememberShowFullVariantLicenses(): Boolean = false
