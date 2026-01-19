package com.valiantyan.music801.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.valiantyan.music801.R
import com.valiantyan.music801.domain.model.Song

/**
 * 播放通知管理器
 *
 * 负责创建通知渠道并生成基础播放通知。
 */
class PlayerNotificationManager(
    private val context: Context,
) {
    private val notificationManager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    /**
     * 创建通知渠道（Android 8+）
     */
    fun createNotificationChannel(): Unit {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val channel: NotificationChannel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        )
        channel.description = CHANNEL_DESCRIPTION
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * 构建基础播放通知
     *
     * @param song 当前播放歌曲
     * @param isPlaying 是否正在播放
     * @return 通知实例
     */
    fun buildNotification(
        song: Song?,
        isPlaying: Boolean,
    ): Notification {
        val title: String = song?.title ?: DEFAULT_TITLE
        val artist: String = song?.artist ?: DEFAULT_ARTIST
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(artist)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
        return builder.build()
    }

    private companion object {
        /**
         * 通知渠道 ID
         */
        private const val CHANNEL_ID: String = "playback"

        /**
         * 通知渠道名称
         */
        private const val CHANNEL_NAME: String = "播放通知"

        /**
         * 通知渠道描述
         */
        private const val CHANNEL_DESCRIPTION: String = "用于展示播放状态"

        /**
         * 默认标题
         */
        private const val DEFAULT_TITLE: String = "未知歌曲"

        /**
         * 默认艺术家
         */
        private const val DEFAULT_ARTIST: String = "未知艺术家"
    }
}
