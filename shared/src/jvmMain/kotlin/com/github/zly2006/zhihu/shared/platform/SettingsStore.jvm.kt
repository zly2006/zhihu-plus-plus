package com.github.zly2006.zhihu.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.desktop.desktopZhihuDataFile
import java.util.Properties

@Composable
actual fun rememberSettingsStore(): SettingsStore = remember {
    desktopSettingsStore()
}

fun desktopSettingsStore(): SettingsStore {
    val settingsFile = desktopZhihuDataFile("settings.properties")
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

    return SettingsStore(
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
        getStringOrNull = { key ->
            properties.getProperty(key)
        },
        putStringSet = { key, value ->
            properties.setProperty(key, value.joinToString("\u001F"))
            save()
        },
        getStringSet = { key, defaultValue ->
            properties
                .getProperty(key)
                ?.split("\u001F")
                ?.filter { it.isNotEmpty() }
                ?.toSet() ?: defaultValue
        },
        getInt = { key, defaultValue ->
            properties.getProperty(key)?.toIntOrNull() ?: defaultValue
        },
        putInt = { key, value ->
            properties.setProperty(key, value.toString())
            save()
        },
        getLong = { key, defaultValue ->
            properties.getProperty(key)?.toLongOrNull() ?: defaultValue
        },
        putLong = { key, value ->
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
        remove = { key ->
            properties.remove(key)
            save()
        },
    )
}
