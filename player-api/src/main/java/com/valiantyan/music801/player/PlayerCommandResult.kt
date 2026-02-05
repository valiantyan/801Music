package com.valiantyan.music801.player

import android.os.Bundle

/**
 * 播放命令执行结果
 *
 * @property isSuccess 是否执行成功
 * @property errorMessage 失败原因（可为空）
 * @property extras 附加数据（可为空）
 */
data class PlayerCommandResult(
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val extras: Bundle? = null,
)
