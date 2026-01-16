package com.valiantyan.music801.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.valiantyan.music801.domain.model.PlaybackState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Media3 播放器管理实现
 *
 * 负责初始化 ExoPlayer 与音频属性配置，提供统一播放接口。
 */
class Media3PlayerManager(
    context: Context,
) : MediaPlayerManager {
    internal val exoPlayer: ExoPlayer = buildExoPlayer(
        context = context,
        audioAttributes = buildAudioAttributes(),
    )
    private val playbackStateFlow: MutableStateFlow<PlaybackState> =
        MutableStateFlow(PlaybackState())

    override fun play(uri: Uri): Unit {
        val mediaItem: MediaItem = MediaItem.fromUri(uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    override fun pause(): Unit {
        exoPlayer.pause()
    }

    override fun resume(): Unit {
        exoPlayer.play()
    }

    override fun stop(): Unit {
        exoPlayer.stop()
    }

    override fun seekTo(position: Long): Unit {
        exoPlayer.seekTo(position)
    }

    override fun playbackState(): Flow<PlaybackState> = playbackStateFlow

    override fun release(): Unit {
        exoPlayer.release()
    }

    private fun buildExoPlayer(
        context: Context,
        audioAttributes: AudioAttributes,
    ): ExoPlayer {
        val player: ExoPlayer = ExoPlayer.Builder(context)
            // Media3 Java API 不支持命名参数，使用位置参数
            .setAudioAttributes(audioAttributes, true)
            .build()
        return player
    }

    private fun buildAudioAttributes(): AudioAttributes {
        val attributes: AudioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        return attributes
    }
}
