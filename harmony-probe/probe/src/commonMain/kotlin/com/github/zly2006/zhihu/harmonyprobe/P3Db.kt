package com.github.zly2006.zhihu.harmonyprobe

/**
 * P3 数据库选型冒烟测试（真实 CPF DB 栈，非占位）。
 * 返回一行人类可读状态；任何失败都如实返回异常信息，不假装成功。
 */
internal expect suspend fun p3DatabaseSmoke(): String
