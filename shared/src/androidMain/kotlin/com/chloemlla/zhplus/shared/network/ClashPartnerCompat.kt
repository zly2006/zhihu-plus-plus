/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.chloemlla.zhplus.shared.network

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.edit
import androidx.core.content.getSystemService
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Zero-config ClashMeta VPN partner adapt for Zhihu++.
 * VPN TUN routes traffic system-wide; this module detects status and
 * avoids stacking app-level proxies while Clash VPN is active.
 */
object ClashPartnerCompat {
    private const val PREFS = "clash_partner_compat"
    private const val KEY_AUTO_ADAPT = "clash_auto_adapt"
    private const val METHOD_PARTNER_STATUS = "partnerStatus"

    private val clashPackages = listOf(
        "com.github.metacubex.clash",
        "com.github.metacubex.clash.meta",
        "com.github.metacubex.clash.alpha",
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<(Status) -> Unit>()

    /**
     * Optional hook invoked after status/binding changes so host code can
     * rebuild HTTP clients / image loaders on the new process network.
     */
    @Volatile
    private var networkAdaptHook: (() -> Unit)? = null

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    @Volatile
    private var lastVpnActive: Boolean? = null

    @Volatile
    private var lastVpnNetwork: Network? = null

    @Volatile
    private var boundVpnNetwork: Network? = null

    @Volatile
    var status: Status = Status()
        private set

    data class Status(
        val clashInstalled: Boolean = false,
        val vpnActive: Boolean = false,
        val clashVpnRunning: Boolean = false,
        val partnerAppAutoAdapt: Boolean = true,
        val profileName: String? = null,
        val clashPackage: String? = null,
        /**
         * True when Clash StatusProvider responded. Prefer [clashVpnRunning] over
         * generic VPN heuristics in that case so a non-Clash VPN is not treated
         * as "Clash routing".
         */
        val partnerStatusAvailable: Boolean = false,
    ) {
        val isClashVpnRouting: Boolean
            get() =
                if (partnerStatusAvailable) {
                    clashVpnRunning
                } else {
                    clashInstalled && vpnActive
                }
    }

    fun isAutoAdaptEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_ADAPT, true)

    fun setAutoAdaptEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_AUTO_ADAPT, enabled) }
        refresh(context.applicationContext)
    }

    fun statusLabel(context: Context): String {
        val enabled = isAutoAdaptEnabled(context)
        val s = status
        return when {
            !enabled -> "已关闭自动适配"
            !s.clashInstalled -> "未检测到 Clash Meta"
            s.isClashVpnRouting -> {
                val profile = s.profileName
                if (!profile.isNullOrBlank()) {
                    "VPN 已连接 · $profile"
                } else {
                    "VPN 已连接 · 流量自动经 Clash"
                }
            }
            else -> "已安装 Clash · 等待开启 VPN"
        }
    }

    fun start(context: Context) {
        val app = context.applicationContext
        appContext = app
        refresh(app)
        startNetworkWatch(app)
    }

    fun setNetworkAdaptHook(hook: (() -> Unit)?) {
        networkAdaptHook = hook
    }

    fun refresh(context: Context? = null) {
        val ctx = context?.applicationContext ?: appContext ?: return
        val previousBound = boundVpnNetwork
        val next = buildStatus(ctx)
        status = next
        // Keep this process on the VPN network while Clash is routing so
        // apps cannot "escape" via ConnectivityManager request-network /
        // allowBypass paths. Clear binding when Clash is off.
        applyVpnProcessBinding(ctx, next)
        if (previousBound != boundVpnNetwork) {
            runCatching { networkAdaptHook?.invoke() }
        }
        listeners.forEach { listener ->
            mainHandler.post { listener(next) }
        }
    }

    fun addListener(listener: (Status) -> Unit) {
        listeners.add(listener)
        listener(status)
    }

    fun removeListener(listener: (Status) -> Unit) {
        listeners.remove(listener)
    }

    fun shouldSkipManualProxy(context: Context): Boolean =
        isAutoAdaptEnabled(context) && status.isClashVpnRouting

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun buildStatus(context: Context): Status {
        val clashInstalled = isClashInstalled(context)
        val vpnActive = isVpnActive(context)
        val partner = queryPartnerStatus(context)
        val partnerStatusAvailable = partner != null
        val clashVpnRunning =
            if (partnerStatusAvailable) {
                partner?.get("vpnRunning") as? Boolean ?: false
            } else {
                clashInstalled && vpnActive
            }
        return Status(
            clashInstalled = clashInstalled,
            vpnActive = vpnActive,
            clashVpnRunning = clashVpnRunning,
            partnerAppAutoAdapt = (partner?.get("partnerAppAutoAdapt") as? Boolean)
                ?: (partner?.get("piliPlusAutoAdapt") as? Boolean)
                ?: true,
            profileName = partner?.get("name") as? String,
            clashPackage = partner?.get("package") as? String,
            partnerStatusAvailable = partnerStatusAvailable,
        )
    }

    private fun startNetworkWatch(context: Context) {
        if (networkCallback != null) return
        val cm = context.getSystemService<ConnectivityManager>() ?: return
        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = onNetworkMaybeChanged()

                override fun onLost(network: Network) = onNetworkMaybeChanged()

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) = onNetworkMaybeChanged()
            }
        networkCallback = callback
        val request =
            NetworkRequest
                .Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                .build()
        runCatching { cm.registerNetworkCallback(request, callback) }
            .onFailure {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        cm.registerDefaultNetworkCallback(callback)
                    }
                }
            }
    }

    private fun onNetworkMaybeChanged() {
        val context = appContext ?: return
        val cm = context.getSystemService<ConnectivityManager>()
        val vpnActive = isVpnActive(context)
        val vpnNetwork = cm?.let { findVpnNetwork(it) }
        // Re-evaluate when VPN goes up/down *or* the underlying Network handle
        // is replaced (Clash restart / re-establish) so process binding follows.
        if (lastVpnActive == vpnActive && lastVpnNetwork == vpnNetwork) return
        lastVpnActive = vpnActive
        lastVpnNetwork = vpnNetwork
        refresh(context)
    }

    private fun isVpnActive(context: Context): Boolean {
        val cm = context.getSystemService<ConnectivityManager>() ?: return false
        for (network in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(network) ?: continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return true
        }
        return false
    }

    private fun isClashInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return clashPackages.any { pkg ->
            try {
                pm.getApplicationInfo(pkg, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    private fun queryPartnerStatus(context: Context): Map<String, Any?>? {
        val resolver = context.contentResolver
        for (pkg in clashPackages) {
            val uri =
                Uri
                    .Builder()
                    .scheme("content")
                    .authority("$pkg.status")
                    .build()
            val bundle = runCatching {
                resolver.call(uri, METHOD_PARTNER_STATUS, null, null)
            }.getOrNull() ?: continue
            return mapOf(
                "running" to bundle.getBoolean("running", false),
                "vpnRunning" to bundle.getBoolean("vpnRunning", false),
                "partnerAppAutoAdapt" to bundle.getBoolean(
                    "partnerAppAutoAdapt",
                    bundle.getBoolean("piliPlusAutoAdapt", true),
                ),
                "piliPlusAutoAdapt" to bundle.getBoolean("piliPlusAutoAdapt", true),
                "name" to bundle.getString("name"),
                "package" to (bundle.getString("package") ?: pkg),
            )
        }
        return null
    }

    /**
     * Bind (or unbind) this process to the active VPN network while Clash is
     * routing. Without this, [NetworkCapabilities.NET_CAPABILITY_NOT_VPN]
     * requests and [VpnService.allowBypass] can let Ktor/WebView leave the
     * tunnel even though Clash is "on".
     */
    private fun applyVpnProcessBinding(context: Context, status: Status) {
        val cm = context.getSystemService<ConnectivityManager>() ?: return
        if (!isAutoAdaptEnabled(context) || !status.isClashVpnRouting) {
            clearProcessNetworkBinding(cm)
            return
        }
        val vpn = findVpnNetwork(cm)
        if (vpn == null) {
            // Status says routing but no VPN Network is visible yet — drop any
            // stale binding so we do not stick to a dead Network handle.
            clearProcessNetworkBinding(cm)
            return
        }
        if (boundVpnNetwork == vpn) return
        runCatching {
            cm.bindProcessToNetwork(vpn)
            boundVpnNetwork = vpn
        }.onFailure {
            boundVpnNetwork = null
        }
    }

    private fun clearProcessNetworkBinding(cm: ConnectivityManager) {
        if (boundVpnNetwork == null && cm.boundNetworkForProcess == null) return
        runCatching { cm.bindProcessToNetwork(null) }
        boundVpnNetwork = null
    }

    private fun findVpnNetwork(cm: ConnectivityManager): Network? {
        for (network in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(network) ?: continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                return network
            }
        }
        return null
    }
}
