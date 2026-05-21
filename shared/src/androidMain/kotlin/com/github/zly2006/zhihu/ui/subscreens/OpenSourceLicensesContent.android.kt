package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.R
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer

@Composable
actual fun OpenSourceLicensesContent(
    modifier: Modifier,
    contentPadding: PaddingValues,
    manualLibraries: List<ManualLicenseEntry>,
    onOpenUrl: (String) -> Unit,
) {
    val libraries by produceLibraries(R.raw.aboutlibraries)
    LibrariesContainer(
        libraries = libraries,
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
actual fun rememberShowFullVariantLicenses(): Boolean = !BuildConfig.IS_LITE
