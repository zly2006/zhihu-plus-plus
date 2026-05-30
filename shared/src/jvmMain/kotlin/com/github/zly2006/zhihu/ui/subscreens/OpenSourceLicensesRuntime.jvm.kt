package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mikepenz.aboutlibraries.Libs
import java.io.File

@Composable
actual fun rememberOpenSourceLicensesLibraries(): Libs = remember {
    loadDesktopOpenSourceLicenses()
}

@Composable
actual fun rememberShowFullVariantLicenses(): Boolean = false

private fun loadDesktopOpenSourceLicenses(): Libs =
    loadDesktopAboutLibrariesJson()
        ?.takeIf { it.isNotBlank() }
        ?.let { json ->
            runCatching {
                Libs.Builder().withJson(json).build()
            }.getOrElse { emptyDesktopLibs() }
        } ?: emptyDesktopLibs()

private fun emptyDesktopLibs(): Libs = Libs(emptyList(), emptySet())

private fun loadDesktopAboutLibrariesJson(): String? {
    val resourceJson = Thread
        .currentThread()
        .contextClassLoader
        ?.getResourceAsStream("aboutlibraries.json")
        ?.bufferedReader()
        ?.use { it.readText() }
    if (!resourceJson.isNullOrBlank()) {
        return resourceJson
    }

    return listOf(
        "app/build/generated/aboutLibraries/liteDebug/res/raw/aboutlibraries.json",
        "app/build/generated/aboutLibraries/fullDebug/res/raw/aboutlibraries.json",
        "app/build/intermediates/packaged_res/liteDebug/packageLiteDebugResources/raw/aboutlibraries.json",
        "app/build/intermediates/packaged_res/fullDebug/packageFullDebugResources/raw/aboutlibraries.json",
    ).firstNotNullOfOrNull { path ->
        File(path)
            .takeIf { it.isFile }
            ?.readText()
            ?.takeIf { it.isNotBlank() }
    }
}
