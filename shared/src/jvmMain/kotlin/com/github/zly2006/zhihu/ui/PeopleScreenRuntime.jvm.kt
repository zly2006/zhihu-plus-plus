package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.DesktopHistoryStorage
import com.github.zly2006.zhihu.shared.desktop.openDesktopExternalUrl
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.viewmodel.filter.createBlocklistManager
import com.github.zly2006.zhihu.viewmodel.filter.desktopContentFilterDatabaseFile
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase

@Composable
actual fun rememberPeopleScreenRuntime(): PeopleScreenRuntime {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        val store = DesktopAccountStore()
        val historyStorage = DesktopHistoryStorage()
        val databaseFile = desktopContentFilterDatabaseFile()
        databaseFile.parentFile?.mkdirs()
        val database = getContentFilterDatabase(databaseFile)
        val blocklistManager = database.createBlocklistManager()
        PeopleScreenRuntime(
            toggleRecommendationBlock = { request ->
                if (request.isBlocked) {
                    blocklistManager.removeBlockedUser(request.userId)
                    false
                } else {
                    blocklistManager.addBlockedUser(
                        userId = request.userId,
                        userName = request.userName,
                        urlToken = request.urlToken,
                        avatarUrl = request.avatarUrl,
                    )
                    true
                }
            },
            showShortMessage = { message ->
                userMessages.showShortMessage(message)
            },
            openWebUrl = { url ->
                openDesktopExternalUrl(url)
            },
            openImage = { url ->
                openDesktopExternalUrl(url)
            },
        )
    }
}
