package com.valiantyan.music801.data.repository

import com.valiantyan.music801.domain.model.PlaybackState
import com.valiantyan.music801.domain.model.Song
import com.valiantyan.music801.player.MediaQueueManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 播放器仓库实现
 *
 * 基于内存状态管理播放队列与播放控制，具体播放引擎逻辑在后续 Story 中接入。
 *
 * @param mediaQueueManager 播放队列管理器
 */
class PlayerRepositoryImpl(
    private val mediaQueueManager: MediaQueueManager,
) : PlayerRepository {
    private val _playbackState: MutableStateFlow<PlaybackState> =
        MutableStateFlow(PlaybackState())

    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    override fun setQueue(
        songs: List<Song>,
        startIndex: Int,
    ): Unit {
        mediaQueueManager.setQueue(songs = songs, startIndex = startIndex)
        val currentSong: Song? = mediaQueueManager.getCurrentSong()
        val resolvedIndex: Int = resolveIndex(queue = songs, currentSong = currentSong)
        _playbackState.value = _playbackState.value.copy(
            currentSong = currentSong,
            isPlaying = false,
            position = 0L,
            duration = currentSong?.duration ?: 0L,
            queue = songs,
            currentIndex = resolvedIndex,
        )
    }

    override fun play(): Unit {
        _playbackState.value = _playbackState.value.copy(isPlaying = true)
    }

    override fun pause(): Unit {
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    override fun seekTo(position: Long): Unit {
        val safePosition: Long = if (position < 0L) 0L else position
        _playbackState.value = _playbackState.value.copy(position = safePosition)
    }

    override fun skipToNext(): Unit {
        val moved: Boolean = mediaQueueManager.moveToNext()
        if (!moved) {
            return
        }
        updateCurrentSong()
    }

    override fun skipToPrevious(): Unit {
        val moved: Boolean = mediaQueueManager.moveToPrevious()
        if (!moved) {
            return
        }
        updateCurrentSong()
    }

    private fun updateCurrentSong(): Unit {
        val currentSong: Song? = mediaQueueManager.getCurrentSong()
        val currentQueue: List<Song> = _playbackState.value.queue
        val resolvedIndex: Int = resolveIndex(queue = currentQueue, currentSong = currentSong)
        _playbackState.value = _playbackState.value.copy(
            currentSong = currentSong,
            position = 0L,
            duration = currentSong?.duration ?: 0L,
            currentIndex = resolvedIndex,
        )
    }

    private fun resolveIndex(
        queue: List<Song>,
        currentSong: Song?,
    ): Int {
        if (currentSong == null) {
            return -1
        }
        return queue.indexOf(currentSong)
    }
}
