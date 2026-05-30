package com.github.zly2006.zhihu.shared.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.desktop.desktopZhihuDataFile
import java.util.Properties

@Composable
actual fun rememberNotificationSettingsStore(): NotificationSettingsStore = remember {
    desktopNotificationSettingsStore()
}

fun desktopNotificationSettingsStore(): NotificationSettingsStore = DesktopNotificationSettingsStore()

private class DesktopNotificationSettingsStore : NotificationSettingsStore {
    private val settingsFile = desktopZhihuDataFile("notification_settings.properties")
    private val properties = Properties()

    init {
        load()
    }

    private fun load() {
        if (settingsFile.isFile) {
            settingsFile.inputStream().use(properties::load)
        }
    }

    private fun save() {
        settingsFile.parentFile?.mkdirs()
        settingsFile.outputStream().use { output ->
            properties.store(output, "Zhihu++ desktop notification settings")
        }
    }

    override fun getSystemNotificationEnabled(type: NotificationType): Boolean =
        properties.getProperty("$KEY_SYSTEM_NOTIFICATION${type.name}")?.toBooleanStrictOrNull() ?: false

    override fun setSystemNotificationEnabled(type: NotificationType, enabled: Boolean) {
        properties.setProperty("$KEY_SYSTEM_NOTIFICATION${type.name}", enabled.toString())
        save()
    }

    override fun getDisplayInAppEnabled(type: NotificationType): Boolean =
        properties.getProperty("$KEY_DISPLAY_IN_APP${type.name}")?.toBooleanStrictOrNull() ?: type.defaultValue

    override fun setDisplayInAppEnabled(type: NotificationType, enabled: Boolean) {
        properties.setProperty("$KEY_DISPLAY_IN_APP${type.name}", enabled.toString())
        save()
    }

    override fun getAutoMarkAsReadEnabled(): Boolean =
        properties.getProperty(KEY_AUTO_MARK_AS_READ)?.toBooleanStrictOrNull() ?: true

    override fun setAutoMarkAsReadEnabled(enabled: Boolean) {
        properties.setProperty(KEY_AUTO_MARK_AS_READ, enabled.toString())
        save()
    }
}

private const val KEY_SYSTEM_NOTIFICATION = "system_notification_"
private const val KEY_DISPLAY_IN_APP = "display_in_app_"
private const val KEY_AUTO_MARK_AS_READ = "auto_mark_notifications_read"
