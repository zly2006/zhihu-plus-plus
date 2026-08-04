package com.hrm.markdown.parser.log


/**
 * 日志门面，初始化时注入实现
 */
object HLog {
    @PublishedApi
    internal var loggerImpl: ILogger? = null

    /**
     * 设置日志实现，由 SDK 初始化时调用
     */
    fun setLogger(logger: ILogger?) {
        loggerImpl = logger
    }

    /** 是否有日志实现（用于调用方提前判断，避免无谓的参数构造） */
    val isEnabled: Boolean get() = loggerImpl != null

    fun v(tag: String, message: String) {
        loggerImpl?.v(tag, message)
    }

    fun d(tag: String, message: String) {
        loggerImpl?.d(tag, message)
    }

    fun i(tag: String, message: String) {
        loggerImpl?.i(tag, message)
    }

    fun w(tag: String, message: String) {
        loggerImpl?.w(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        loggerImpl?.e(tag, message, throwable)
    }

    // --- Lazy 重载：message lambda 仅在日志启用时求值 ---
    // 解决热路径中 HLog.d(TAG, "...$variable...") 即使日志关闭也会执行字符串模板的问题

    inline fun v(tag: String, message: () -> String) {
        loggerImpl?.v(tag, message())
    }

    inline fun d(tag: String, message: () -> String) {
        loggerImpl?.d(tag, message())
    }

    inline fun i(tag: String, message: () -> String) {
        loggerImpl?.i(tag, message())
    }

    inline fun w(tag: String, message: () -> String) {
        loggerImpl?.w(tag, message())
    }

    inline fun e(tag: String, throwable: Throwable? = null, message: () -> String) {
        loggerImpl?.e(tag, message(), throwable)
    }
}
