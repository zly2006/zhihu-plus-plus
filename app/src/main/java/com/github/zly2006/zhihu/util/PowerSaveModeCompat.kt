package com.github.zly2006.zhihu.util

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

enum class PowerSaveModeCompat {
    NORMAL,
    POWER_SAVE,
    HUAWEI_POWER_SAVE,
    ;

    val isPowerSaveMode: Boolean
        get() = this != NORMAL

    companion object {
        fun getPowerSaveMode(context: Context): PowerSaveModeCompat {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context.getSystemService(PowerManager::class.java).isPowerSaveMode) {
                return POWER_SAVE
            }
            if (Settings.System.getInt(context.contentResolver, "SmartModeStatus", 0) == 4) {
                return HUAWEI_POWER_SAVE
            }
            return NORMAL
        }
    }
}
