package com.github.zly2006.zhihu.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import java.util.Properties

@Composable
actual fun rememberSettingsStore(): SettingsStore = remember {
    val settingsFile = File(System.getProperty("user.home"), ".zhihu-plus/settings.properties")
    val properties = Properties()

    fun load() {
        if (settingsFile.isFile) {
            settingsFile.inputStream().use(properties::load)
        }
    }

    fun save() {
        settingsFile.parentFile?.mkdirs()
        settingsFile.outputStream().use { output ->
            properties.store(output, "Zhihu++ desktop settings")
        }
    }

    load()

    SettingsStore(
        getBoolean = { key, defaultValue ->
            properties.getProperty(key)?.toBooleanStrictOrNull() ?: defaultValue
        },
        putBoolean = { key, value ->
            properties.setProperty(key, value.toString())
            save()
        },
        getString = { key, defaultValue ->
            properties.getProperty(key) ?: defaultValue
        },
        putString = { key, value ->
            properties.setProperty(key, value)
            save()
        },
        getInt = { key, defaultValue ->
            properties.getProperty(key)?.toIntOrNull() ?: defaultValue
        },
        putInt = { key, value ->
            properties.setProperty(key, value.toString())
            save()
        },
        getFloat = { key, defaultValue ->
            properties.getProperty(key)?.toFloatOrNull() ?: defaultValue
        },
        putFloat = { key, value ->
            properties.setProperty(key, value.toString())
            save()
        },
    )
}
