package com.github.zly2006.zhihu.util

object ZseSigner {
    fun encryptZseV4(input: String): String =
        com.github.zly2006.zhihu.shared.util.ZseSigner
            .encryptZseV4(input)
}
