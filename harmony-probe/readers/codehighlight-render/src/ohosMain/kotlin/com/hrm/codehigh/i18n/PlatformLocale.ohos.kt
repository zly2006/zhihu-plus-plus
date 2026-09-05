package com.hrm.codehigh.i18n

import androidx.compose.ui.text.intl.Locale

internal actual object PlatformLocale {
    actual fun current(): LocaleInfo = LocaleInfo(Locale.current.language, Locale.current.region)
}
