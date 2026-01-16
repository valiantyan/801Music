package com.valiantyan.music801.data.repository

import android.net.Uri
import com.valiantyan.music801.domain.model.PlaybackState
import com.valiantyan.music801.domain.model.Song
import com.valiantyan.music801.player.MediaPlayerManager
import com.valiantyan.music801.player.MediaQueueManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 播放器仓库实现
 *
 * 基于队列状态管理播放控制，并与 [MediaPlayerManager] 同步播放状态。
 *
 * @param mediaQueueManager 播放队列管理器
 * @param mediaPlayerManager 播放引擎管理器
 * @param dispatcher 状态同步所用协程调度器
 */
class PlayerRepositoryImpl(
    private val mediaQueueManager: MediaQueueManager,
    private val mediaPlayerManager: MediaPlayerManager,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : PlayerRepository {
    /**
     * 用于监听播放状态的协程作用域
     */
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
    /**
     * 播放状态监听任务
     */
    private var playbackJob: Job? = null
    private val _playbackState: MutableStateFlow<PlaybackState> =
        MutableStateFlow(PlaybackState())

    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    init {
        observePlaybackState()
    }

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
        val currentSong: Song? = _playbackState.value.currentSong
        if (currentSong == null) {
            return
        }
        val mediaUri: Uri = Uri.parse(currentSong.filePath)
        mediaPlayerManager.play(uri = mediaUri)
    }

    override fun pause(): Unit {
        mediaPlayerManager.pause()
    }

    override fun seekTo(position: Long): Unit {
        val safePosition: Long = if (position < 0L) 0L else position
        mediaPlayerManager.seekTo(position = safePosition)
    }

    override fun skipToNext(): Unit {
        val moved: Boolean = mediaQueueManager.moveToNext()
        if (!moved) {
            return
        }
        val shouldPlay: Boolean = _playbackState.value.isPlaying
        updateCurrentSong()
        if (shouldPlay) {
            play()
        }
    }

    override fun skipToPrevious(): Unit {
        val moved: Boolean = mediaQueueManager.moveToPrevious()
        if (!moved) {
            return
        }
        val shouldPlay: Boolean = _playbackState.value.isPlaying
        updateCurrentSong()
        if (shouldPlay) {
            play()
        }
    }

    /**
     * 释放播放器资源
     */
    override fun release(): Unit {
        playbackJob?.cancel()
        playbackJob = null
        coroutineScope.cancel()
        mediaPlayerManager.release()
        _playbackState.value = PlaybackState()
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

    /**
     * 监听播放引擎状态并合并队列信息
     */
    private fun observePlaybackState(): Unit {
        playbackJob?.cancel()
        playbackJob = coroutineScope.launch {
            mediaPlayerManager.playbackState().collectLatest { state ->
                _playbackState.value = mergePlaybackState(
                    current = _playbackState.value,
                    incoming = state,
                )
            }
        }
    }

    /**
     * 合并播放引擎状态与队列信息，保持上下文一致
     */
    private fun mergePlaybackState(
        current: PlaybackState,
        incoming: PlaybackState,
    ): PlaybackState {
        val resolvedDuration: Long = if (incoming.duration > 0L) {
            incoming.duration
        } else {
            current.duration
        }
        return current.copy(
            isPlaying = incoming.isPlaying,
            position = incoming.position,
            duration = resolvedDuration,
            bufferedPosition = incoming.bufferedPosition,
            playbackState = incoming.playbackState,
            error = incoming.error,
        )
    }
}
