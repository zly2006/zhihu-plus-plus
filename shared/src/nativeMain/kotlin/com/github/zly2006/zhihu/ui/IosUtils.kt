package com.github.zly2006.zhihu.ui

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

internal fun openIosUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}
