package com.github.zly2006.zhihu.ui

import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Daily
import com.github.zly2006.zhihu.navigation.Follow
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.HotList
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.navigation.TopLevelDestination

const val START_DESTINATION_PREFERENCE_KEY = "startDestination"
const val BOTTOM_BAR_ITEMS_PREFERENCE_KEY = "bottom_bar_items"

val topLevelDestinationsInOrder: List<Pair<String, TopLevelDestination>> = listOf(
    Home.name to Home,
    Follow.name to Follow,
    HotList.name to HotList,
    Daily.name to Daily,
    OnlineHistory.name to OnlineHistory,
    Account.name to Account,
)

fun navDestinationFromName(name: String): TopLevelDestination = topLevelDestinationsInOrder
    .firstOrNull { it.first == name }
    ?.second
    ?: Home

fun resolveValidStartDestinationKey(
    preferredKey: String?,
    availableKeysInOrder: List<String>,
): String = when {
    !preferredKey.isNullOrEmpty() && preferredKey in availableKeysInOrder -> preferredKey
    availableKeysInOrder.isNotEmpty() -> availableKeysInOrder.first()
    else -> Home.name
}

fun defaultBottomBarSelectionKeys(duo3HomeAccount: Boolean): Set<String> = if (duo3HomeAccount) {
    linkedSetOf(Home.name, Follow.name, Daily.name)
} else {
    linkedSetOf(Home.name, Follow.name, Daily.name, OnlineHistory.name, Account.name)
}

fun normalizeBottomBarSelection(
    selectedKeys: Set<String>,
    duo3HomeAccount: Boolean,
    enforceMinimumSelection: Boolean = false,
): Set<String> {
    val allowedKeys = topLevelDestinationsInOrder.map { it.first }.toSet()
    val normalized = selectedKeys
        .filterTo(linkedSetOf()) { it in allowedKeys }
        .ifEmpty { defaultBottomBarSelectionKeys(duo3HomeAccount).toMutableSet() }

    if (duo3HomeAccount) {
        if (Home.name in normalized) {
            normalized.remove(Account.name)
        } else {
            normalized.add(Account.name)
        }
    } else {
        normalized.add(Account.name)
        while (normalized.size > 5) {
            val removableKey = listOf(
                HotList.name,
                OnlineHistory.name,
                Daily.name,
                Follow.name,
                Home.name,
            ).firstOrNull { it in normalized } ?: break
            normalized.remove(removableKey)
        }
    }

    if (enforceMinimumSelection) {
        val fillOrder = if (duo3HomeAccount) {
            if (Home.name in normalized) {
                listOf(Follow.name, Daily.name, HotList.name, OnlineHistory.name)
            } else {
                listOf(Follow.name, Daily.name, HotList.name, OnlineHistory.name, Home.name)
            }
        } else {
            listOf(Home.name, Follow.name, Daily.name, HotList.name, OnlineHistory.name, Account.name)
        }
        fillOrder.forEach { key ->
            if (normalized.size < 3) {
                normalized.add(key)
            }
        }
    }

    return normalized
}

fun shouldShowAccountHistoryShortcut(
    duo3HomeAccount: Boolean,
    selectedKeys: Set<String>,
): Boolean = duo3HomeAccount && OnlineHistory.name !in selectedKeys
