package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun OpenSourceLicensesContent(
    modifier: Modifier,
    contentPadding: PaddingValues,
    manualLibraries: List<ManualLicenseEntry>,
    onOpenUrl: (String) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        if (manualLibraries.isNotEmpty()) {
            item {
                ManualLicenseEntryGroup(
                    manualLibraries = manualLibraries,
                    onOpenUrl = onOpenUrl,
                )
            }
        }
        item {
            Text(
                text = "Desktop license list is pending migration.",
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
}

@Composable
actual fun rememberShowFullVariantLicenses(): Boolean = false
