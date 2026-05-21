package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer

@Suppress("DEPRECATION")
@Composable
actual fun OpenSourceLicensesContent(
    modifier: Modifier,
    contentPadding: PaddingValues,
    manualLibraries: List<ManualLicenseEntry>,
    onOpenUrl: (String) -> Unit,
) {
    LibrariesContainer(
        modifier = modifier,
        contentPadding = contentPadding,
        showDescription = false,
        header = {
            if (manualLibraries.isNotEmpty()) {
                item {
                    ManualLicenseEntryGroup(
                        manualLibraries = manualLibraries,
                        onOpenUrl = onOpenUrl,
                    )
                }
            }
        },
    )
}

@Composable
actual fun rememberShowFullVariantLicenses(): Boolean = !LocalContext.current.packageName.endsWith(".lite")
