package com.valiantyan.music801.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.ui.PlayerNotificationManager as Media3PlayerNotificationManager
import androidx.media3.session.MediaSession
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
    internal var lastNotification: Notification? = null
    internal var isNotificationPosted: Boolean = false
    internal var isManagerAttached: Boolean = false
    internal var isPlayPauseActionEnabled: Boolean = false
    internal var isNextActionEnabled: Boolean = false
    internal var isPreviousActionEnabled: Boolean = false
    internal var isStopActionEnabled: Boolean = false
    internal var isCompactNextActionEnabled: Boolean = false
    internal var isCompactPreviousActionEnabled: Boolean = false
    private var media3NotificationManager: Media3PlayerNotificationManager? = null
    private var currentSong: Song? = null
    private var currentSongId: String? = null
    private var lastNotificationId: Int? = null
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
        Log.d(TAG, "buildNotification: title=$title isPlaying=$isPlaying")
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(artist)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
        return builder.build()
    }

    /**
     * 构建媒体样式播放通知
     *
     * @param song 当前播放歌曲
     * @param isPlaying 是否正在播放
     * @param mediaSession 媒体会话
     * @return 通知实例
     */
    fun buildMediaStyleNotification(
        song: Song?,
        isPlaying: Boolean,
        mediaSession: MediaSession,
    ): Notification {
        Log.d(TAG, "buildMediaStyleNotification: isPlaying=$isPlaying")
        updateCurrentSong(song = song)
        attachToSession(mediaSession = mediaSession)
        if (lastNotification == null) {
            lastNotification = buildNotification(
                song = song,
                isPlaying = isPlaying,
            )
        }
        return lastNotification ?: buildNotification(
            song = song,
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
     * 更新当前歌曲信息用于通知标题回退
     */
    fun updateCurrentSong(song: Song?): Unit {
        val newSongId: String? = song?.id
        if (newSongId == currentSongId) {
            return
        }
        currentSongId = newSongId
        currentSong = song
        Log.d(TAG, "updateCurrentSong: title=${song?.title}")
    }

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
            .setSmallIconResourceId(R.mipmap.ic_launcher)
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

    private inner class NotificationListenerImpl :
        Media3PlayerNotificationManager.NotificationListener {
        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean,
        ): Unit {
            lastNotification = notification
            isNotificationPosted = true
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
        }
    }

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

    private inner class MediaDescriptionAdapterImpl :
        Media3PlayerNotificationManager.MediaDescriptionAdapter {
        override fun getCurrentContentTitle(player: Player): CharSequence {
            val metadataTitle: CharSequence? = player.mediaMetadata.title
            if (!metadataTitle.isNullOrBlank()) {
                return metadataTitle
            }
            return currentSong?.title ?: DEFAULT_TITLE
        }

        override fun createCurrentContentIntent(player: Player): android.app.PendingIntent? {
            return null
        }

        override fun getCurrentContentText(player: Player): CharSequence? {
            val metadataArtist: CharSequence? = player.mediaMetadata.artist
            if (!metadataArtist.isNullOrBlank()) {
                return metadataArtist
            }
            return currentSong?.artist ?: DEFAULT_ARTIST
        }

        override fun getCurrentLargeIcon(
            player: Player,
            callback: Media3PlayerNotificationManager.BitmapCallback,
        ): android.graphics.Bitmap? {
            return null
        }
    }

    private companion object {
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
