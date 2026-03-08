package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.LocalNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSchemeScreen() {
    val navigator = LocalNavigator.current
    val cs = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Color Scheme") },
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                windowInsets = WindowInsets(0),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ColorGroup("Primary") {
                ColorSwatch(cs.primary, cs.onPrimary, "primary", "onPrimary")
                ColorSwatch(cs.primaryContainer, cs.onPrimaryContainer, "primaryContainer", "onPrimaryContainer")
                ColorSwatch(cs.inversePrimary, cs.primary, "inversePrimary", "primary")
            }
            ColorGroup("Secondary") {
                ColorSwatch(cs.secondary, cs.onSecondary, "secondary", "onSecondary")
                ColorSwatch(cs.secondaryContainer, cs.onSecondaryContainer, "secondaryContainer", "onSecondaryContainer")
            }
            ColorGroup("Tertiary") {
                ColorSwatch(cs.tertiary, cs.onTertiary, "tertiary", "onTertiary")
                ColorSwatch(cs.tertiaryContainer, cs.onTertiaryContainer, "tertiaryContainer", "onTertiaryContainer")
            }
            ColorGroup("Error") {
                ColorSwatch(cs.error, cs.onError, "error", "onError")
                ColorSwatch(cs.errorContainer, cs.onErrorContainer, "errorContainer", "onErrorContainer")
            }
            ColorGroup("Surface") {
                ColorSwatch(cs.surface, cs.onSurface, "surface", "onSurface")
                ColorSwatch(cs.surfaceVariant, cs.onSurfaceVariant, "surfaceVariant", "onSurfaceVariant")
                ColorSwatch(cs.surfaceBright, cs.onSurface, "surfaceBright", "onSurface")
                ColorSwatch(cs.surfaceDim, cs.onSurface, "surfaceDim", "onSurface")
                ColorSwatch(cs.inverseSurface, cs.inverseOnSurface, "inverseSurface", "inverseOnSurface")
            }
            ColorGroup("Surface Containers") {
                ColorSwatch(cs.surfaceContainerLowest, cs.onSurface, "surfaceContainerLowest", "onSurface")
                ColorSwatch(cs.surfaceContainerLow, cs.onSurface, "surfaceContainerLow", "onSurface")
                ColorSwatch(cs.surfaceContainer, cs.onSurface, "surfaceContainer", "onSurface")
                ColorSwatch(cs.surfaceContainerHigh, cs.onSurface, "surfaceContainerHigh", "onSurface")
                ColorSwatch(cs.surfaceContainerHighest, cs.onSurface, "surfaceContainerHighest", "onSurface")
            }
            ColorGroup("Background") {
                ColorSwatch(cs.background, cs.onBackground, "background", "onBackground")
            }
            ColorGroup("Outline") {
                ColorSwatch(cs.outline, cs.surface, "outline", "surface")
                ColorSwatch(cs.outlineVariant, cs.onSurface, "outlineVariant", "onSurface")
            }
            ColorGroup("Scrim") {
                ColorSwatch(cs.scrim, Color.White, "scrim", "White")
            }
        }
    }
}

@Composable
private fun ColorGroup(
    name: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        content()
    }
}

@Composable
private fun ColorSwatch(
    bg: Color,
    fg: Color,
    bgName: String,
    fgName: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$bgName / $fgName",
            color = fg,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
