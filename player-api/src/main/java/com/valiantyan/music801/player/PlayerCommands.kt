package com.valiantyan.music801.player

/**
 * 播放器自定义命令定义
 */
object PlayerCommands {
    /**
     * 收藏/取消收藏切换命令
     */
    const val ACTION_TOGGLE_FAVORITE: String = "com.valiantyan.music801.action.TOGGLE_FAVORITE"

    /**
     * 自定义命令返回的媒体 ID
     */
    const val EXTRA_MEDIA_ID: String = "extra_media_id"

    /**
     * 自定义命令返回的收藏状态
     */
    const val EXTRA_IS_FAVORITE: String = "extra_is_favorite"

    /**
     * 自定义命令返回的错误信息
     */
    const val EXTRA_ERROR_MESSAGE: String = "extra_error_message"
}
