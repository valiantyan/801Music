package com.valiantyan.music801.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager as Media3PlayerNotificationManager
import com.valiantyan.music801.player.impl.R

/**
 * 播放通知管理器
 *
 * 负责创建通知渠道并生成基础播放通知。
 */
internal class PlayerNotificationManager(
    private val context: Context,
    private val serviceController: ForegroundServiceController? = null,
) {
    /**
     * 系统通知管理器，用于创建与更新通知
     */
    private val notificationManager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)
    /**
     * 最近一次生成的通知实例，便于复用
     */
    internal var lastNotification: Notification? = null
    /**
     * 记录通知是否已投递，用于测试与状态判断
     */
    internal var isNotificationPosted: Boolean = false
    /**
     * 标记是否已绑定 [MediaSession]，避免重复初始化
     */
    internal var isManagerAttached: Boolean = false
    /**
     * 标记播放/暂停动作是否启用，便于测试校验
     */
    internal var isPlayPauseActionEnabled: Boolean = false
    /**
     * 标记下一首动作是否启用，便于测试校验
     */
    internal var isNextActionEnabled: Boolean = false
    /**
     * 标记上一首动作是否启用，便于测试校验
     */
    internal var isPreviousActionEnabled: Boolean = false
    /**
     * 标记停止动作是否启用，便于测试校验
     */
    internal var isStopActionEnabled: Boolean = false
    /**
     * 标记紧凑视图下一首动作是否启用，便于测试校验
     */
    internal var isCompactNextActionEnabled: Boolean = false
    /**
     * 标记紧凑视图上一首动作是否启用，便于测试校验
     */
    internal var isCompactPreviousActionEnabled: Boolean = false
    /**
     * Media3 通知管理器实例，负责与 [Player] 联动
     */
    private var media3NotificationManager: Media3PlayerNotificationManager? = null
    /**
     * 记录最近一次通知 ID，用于减少重复日志
     */
    private var lastNotificationId: Int? = null
    /**
     * 记录最近一次通知前台状态，用于减少重复日志
     */
    private var lastNotificationOngoing: Boolean? = null

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
        Log.d(TAG, "notification channel created")
    }

    /**
     * 构建基础播放通知
     *
     * @param title 当前播放标题
     * @param artist 当前播放艺术家
     * @param isPlaying 是否正在播放
     * @return 通知实例
     */
    fun buildNotification(
        title: String?,
        artist: String?,
        isPlaying: Boolean,
    ): Notification {
        val resolvedTitle: String = title ?: DEFAULT_TITLE
        val resolvedArtist: String = artist ?: DEFAULT_ARTIST
        Log.d(TAG, "buildNotification: title=$resolvedTitle isPlaying=$isPlaying")
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_player_notification)
            .setContentTitle(resolvedTitle)
            .setContentText(resolvedArtist)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
        return builder.build()
    }

    /**
     * 构建媒体样式播放通知
     *
     * @param isPlaying 是否正在播放
     * @param mediaSession 媒体会话
     * @return 通知实例
     */
    fun buildMediaStyleNotification(
        isPlaying: Boolean,
        mediaSession: MediaSession,
    ): Notification {
        Log.d(TAG, "buildMediaStyleNotification: isPlaying=$isPlaying")
        attachToSession(mediaSession = mediaSession)
        if (lastNotification == null) {
            lastNotification = buildNotification(
                title = null,
                artist = null,
                isPlaying = isPlaying,
            )
        }
        return lastNotification ?: buildNotification(
            title = null,
            artist = null,
            isPlaying = isPlaying,
        )
    }

    /**
     * 绑定媒体会话并初始化 Media3 通知管理器
     */
    fun attachToSession(mediaSession: MediaSession): Unit {
        if (media3NotificationManager != null) {
            return
        }
        Log.d(TAG, "attachToSession")
        val player: Player = mediaSession.player
        val manager: Media3PlayerNotificationManager = buildMedia3Manager()
        media3NotificationManager = manager
        manager.setMediaSessionToken(mediaSession.getPlatformToken())
        manager.setPlayer(player)
        isManagerAttached = true
    }

    /**
     * 构建 Media3 通知管理器并配置动作按钮
     */
    private fun buildMedia3Manager(): Media3PlayerNotificationManager {
        val adapter: Media3PlayerNotificationManager.MediaDescriptionAdapter =
            MediaDescriptionAdapterImpl()
        val listener: Media3PlayerNotificationManager.NotificationListener =
            NotificationListenerImpl()
        return Media3PlayerNotificationManager.Builder(
            context,
            NOTIFICATION_ID,
            CHANNEL_ID,
        )
            .setSmallIconResourceId(R.drawable.ic_player_notification)
            .setMediaDescriptionAdapter(adapter)
            .setNotificationListener(listener)
            .build()
            .apply {
                setUsePlayPauseActions(true)
                setUseNextAction(true)
                setUsePreviousAction(true)
                setUseStopAction(true)
                setUseNextActionInCompactView(true)
                setUsePreviousActionInCompactView(true)
                isPlayPauseActionEnabled = true
                isNextActionEnabled = true
                isPreviousActionEnabled = true
                isStopActionEnabled = true
                isCompactNextActionEnabled = true
                isCompactPreviousActionEnabled = true
            }
    }

    /**
     * 监听通知投递与取消，用于驱动前台服务状态
     */
    private inner class NotificationListenerImpl :
        Media3PlayerNotificationManager.NotificationListener {
        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean,
        ): Unit {
            lastNotification = notification
            isNotificationPosted = true
            if (ongoing) {
                serviceController?.startForeground(
                    notificationId = notificationId,
                    notification = notification,
                )
            } else {
                serviceController?.stopForeground()
            }
            if (shouldLogNotificationPosted(notificationId = notificationId, ongoing = ongoing)) {
                Log.d(TAG, "notification posted: id=$notificationId ongoing=$ongoing")
            }
        }

        override fun onNotificationCancelled(
            notificationId: Int,
            dismissedByUser: Boolean,
        ): Unit {
            isNotificationPosted = false
            Log.d(TAG, "notification cancelled: id=$notificationId dismissed=$dismissedByUser")
            serviceController?.stopForeground()
            serviceController?.stopSelf()
        }
    }

    /**
     * 避免重复日志刷屏，仅在通知状态变化时输出
     */
    private fun shouldLogNotificationPosted(
        notificationId: Int,
        ongoing: Boolean,
    ): Boolean {
        val shouldLog: Boolean =
            lastNotificationId != notificationId || lastNotificationOngoing != ongoing
        if (!shouldLog) {
            return false
        }
        lastNotificationId = notificationId
        lastNotificationOngoing = ongoing
        return true
    }

    /**
     * 提供通知展示所需的标题与内容描述
     */
    private inner class MediaDescriptionAdapterImpl :
        Media3PlayerNotificationManager.MediaDescriptionAdapter {
        override fun getCurrentContentTitle(player: Player): CharSequence {
            val metadataTitle: CharSequence? = player.mediaMetadata.title
            if (!metadataTitle.isNullOrBlank()) {
                return metadataTitle
            }
            return DEFAULT_TITLE
        }

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            val intent: Intent? = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (intent == null) {
                return null
            }
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        override fun getCurrentContentText(player: Player): CharSequence? {
            val metadataArtist: CharSequence? = player.mediaMetadata.artist
            if (!metadataArtist.isNullOrBlank()) {
                return metadataArtist
            }
            return DEFAULT_ARTIST
        }

        override fun getCurrentLargeIcon(
            player: Player,
            callback: Media3PlayerNotificationManager.BitmapCallback,
        ): android.graphics.Bitmap? {
            return null
        }
    }

    private companion object {
        /**
         * 日志标签
         */
        private const val TAG: String = "MediaNotification"
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

        /**
         * 通知 ID
         */
        private const val NOTIFICATION_ID: Int = 1001
    }

    /**
     * 获取通知 ID
     */
    fun getNotificationId(): Int {
        return NOTIFICATION_ID
    }

}

/**
 * 前台服务控制接口
 */
internal interface ForegroundServiceController {
    /**
     * 启动前台服务
     */
    fun startForeground(
        notificationId: Int,
        notification: Notification,
    ): Unit

    /**
     * 停止前台服务状态
     */
    fun stopForeground(): Unit

    /**
     * 请求停止服务
     */
    fun stopSelf(): Unit
}
