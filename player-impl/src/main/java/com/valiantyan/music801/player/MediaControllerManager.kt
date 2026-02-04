package com.valiantyan.music801.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.valiantyan.music801.domain.model.PlaybackState
import com.valiantyan.music801.domain.model.Song
import com.valiantyan.music801.player.PlayerCommands
import com.valiantyan.music801.service.MusicPlayerService
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * MediaController 管理器
 *
 * 通过 [MediaController] 连接媒体会话并同步播放状态。
 */
internal class MediaControllerManager(
    context: Context,
    progressUpdateIntervalMs: Long = DEFAULT_PROGRESS_UPDATE_INTERVAL_MS,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : PlayerController {
    private val appContext: Context = context.applicationContext
    private val progressUpdateIntervalMs: Long = progressUpdateIntervalMs
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val playbackStateFlow: MutableStateFlow<PlaybackState> = MutableStateFlow(PlaybackState())
    private val isConnecting: AtomicBoolean = AtomicBoolean(false)
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var progressJob: Job? = null
    private var lastError: PlaybackException? = null
    private var currentQueue: List<Song> = emptyList()
    private var currentIndex: Int = C.INDEX_UNSET
    private var pendingQueue: List<Song>? = null
    private var pendingStartIndex: Int = C.INDEX_UNSET
    private var pendingPlay: Boolean = false
    private var pendingToggleFavorite: Boolean = false
    private val playerListener: Player.Listener = buildPlayerListener()
    override val playbackState: StateFlow<PlaybackState> = playbackStateFlow.asStateFlow()

    private fun connect(): Unit {
        if (controller != null || isConnecting.get()) {
            return
        }
        isConnecting.set(true)
        ensureServiceStarted()
        // Media3 Java API 不支持命名参数，使用位置参数
        val token: SessionToken = SessionToken(
            appContext,
            ComponentName(appContext, MusicPlayerService::class.java),
        )
        val future: ListenableFuture<MediaController> =
            MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        Futures.addCallback(
            future,
            object : FutureCallback<MediaController> {
                override fun onSuccess(result: MediaController?) {
                    isConnecting.set(false)
                    controllerFuture = null
                    if (result == null) {
                        Log.e(TAG, "media controller create failed: null")
                        return
                    }
                    controller = result
                    result.addListener(playerListener)
                    startProgressUpdates()
                    applyPendingQueue(controller = result)
                    updatePlaybackState(error = null)
                    if (pendingPlay) {
                        pendingPlay = false
                        result.play()
                    }
                    if (pendingToggleFavorite) {
                        pendingToggleFavorite = false
                        sendToggleFavoriteCommand(controller = result)
                    }
                }
                override fun onFailure(t: Throwable) {
                    isConnecting.set(false)
                    controllerFuture = null
                    Log.e(TAG, "media controller create failed", t)
                }
            },
            MoreExecutors.directExecutor(),
        )
    }

    fun release(): Unit {
        val future: ListenableFuture<MediaController>? = controllerFuture
        if (future != null) {
            future.cancel(true)
        }
        controllerFuture = null
        stopProgressUpdates()
        val player: MediaController? = controller
        if (player != null) {
            player.removeListener(playerListener)
            player.release()
        }
        controller = null
        isConnecting.set(false)
    }

    override fun setQueue(
        songs: List<Song>,
        startIndex: Int,
    ): Unit {
        currentQueue = songs
        currentIndex = resolveStartIndex(songs = songs, startIndex = startIndex)
        updatePlaybackState(error = null)
        val player: MediaController? = controller
        if (player == null) {
            pendingQueue = songs
            pendingStartIndex = startIndex
            connect()
            return
        }
        setMediaItems(
            controller = player,
            songs = songs,
            startIndex = startIndex,
        )
    }

    override fun play(): Unit {
        val player: MediaController? = controller
        if (player != null) {
            player.play()
            return
        }
        pendingPlay = true
        connect()
    }

    override fun pause(): Unit {
        controller?.pause()
    }

    override fun seekTo(position: Long): Unit {
        controller?.seekTo(position)
    }

    override fun skipToNext(): Unit {
        controller?.seekToNextMediaItem()
    }

    override fun skipToPrevious(): Unit {
        controller?.seekToPreviousMediaItem()
    }

    override fun toggleFavorite(): Unit {
        val player: MediaController? = controller
        if (player != null) {
            sendToggleFavoriteCommand(controller = player)
            return
        }
        pendingToggleFavorite = true
        connect()
    }

    private fun applyPendingQueue(controller: MediaController): Unit {
        val songs: List<Song>? = pendingQueue
        if (songs == null) {
            return
        }
        val startIndex: Int = pendingStartIndex
        pendingQueue = null
        pendingStartIndex = C.INDEX_UNSET
        setMediaItems(
            controller = controller,
            songs = songs,
            startIndex = startIndex,
        )
    }

    private fun setMediaItems(
        controller: MediaController,
        songs: List<Song>,
        startIndex: Int,
    ): Unit {
        if (songs.isEmpty()) {
            return
        }
        val mediaItems: List<MediaItem> = songs.map { song -> buildMediaItem(song = song) }
        val safeIndex: Int = if (startIndex in songs.indices) startIndex else 0
        controller.setMediaItems(
            mediaItems,
            safeIndex,
            0L,
        )
        controller.prepare()
    }

    private fun sendToggleFavoriteCommand(controller: MediaController): Unit {
        val command: SessionCommand = SessionCommand(
            PlayerCommands.ACTION_TOGGLE_FAVORITE,
            android.os.Bundle(),
        )
        controller.sendCustomCommand(command, android.os.Bundle())
    }

    private fun buildMediaItem(song: Song): MediaItem {
        val metadata: MediaMetadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(resolveArtworkUri(song = song))
            .build()
        val uri: Uri = Uri.fromFile(File(song.filePath))
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun resolveArtworkUri(song: Song): android.net.Uri? {
        val path: String? = song.albumArtPath
        if (path.isNullOrBlank()) {
            return null
        }
        return android.net.Uri.fromFile(File(path))
    }

    private fun buildPlayerListener(): Player.Listener {
        return object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events): Unit {
                if (events.contains(Player.EVENT_IS_PLAYING_CHANGED) && player.isPlaying) {
                    clearPlaybackError()
                }
                updatePlaybackState(error = null)
            }

            override fun onPlayerError(error: PlaybackException): Unit {
                Log.e(TAG, "player error: ${PlaybackException.getErrorCodeName(error.errorCode)}", error)
                handlePlaybackError(error = error)
            }
        }
    }

    private fun startProgressUpdates(): Unit {
        if (progressJob != null) {
            return
        }
        progressJob = coroutineScope.launch {
            while (isActive) {
                updatePlaybackState(error = null)
                delay(progressUpdateIntervalMs)
            }
        }
    }

    private fun stopProgressUpdates(): Unit {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updatePlaybackState(error: PlaybackException?): Unit {
        if (error != null) {
            lastError = error
        }
        val player: MediaController? = controller
        if (player == null) {
            val currentState: PlaybackState = playbackStateFlow.value
            val resolvedSong: Song? = resolveCurrentSong(index = currentIndex)
            playbackStateFlow.value = currentState.copy(
                currentSong = resolvedSong,
                queue = currentQueue,
                currentIndex = currentIndex,
                error = lastError,
            )
            return
        }
        val state: PlaybackState = buildPlaybackState(
            player = player,
            error = resolvePlaybackError(error = error),
        )
        playbackStateFlow.value = state
    }

    private fun buildPlaybackState(
        player: Player,
        error: PlaybackException?,
    ): PlaybackState {
        val index: Int = resolveIndexFromPlayer()
        val position: Long = player.currentPosition.coerceAtLeast(0L)
        val duration: Long = if (player.duration >= 0L) player.duration else 0L
        return PlaybackState(
            currentSong = resolveCurrentSong(index = index),
            isPlaying = player.isPlaying,
            position = position,
            duration = duration,
            bufferedPosition = player.bufferedPosition.coerceAtLeast(0L),
            playbackState = player.playbackState,
            error = error,
            queue = currentQueue,
            currentIndex = index,
        )
    }

    private fun resolveIndexFromPlayer(): Int {
        val player: MediaController? = controller
        if (player == null) {
            return currentIndex
        }
        val index: Int = player.currentMediaItemIndex
        if (index == C.INDEX_UNSET) {
            return currentIndex
        }
        if (index < 0 || index >= currentQueue.size) {
            return currentIndex
        }
        currentIndex = index
        return index
    }

    private fun resolveCurrentSong(index: Int): Song? {
        if (index < 0 || index >= currentQueue.size) {
            return null
        }
        return currentQueue[index]
    }

    private fun resolveStartIndex(
        songs: List<Song>,
        startIndex: Int,
    ): Int {
        if (songs.isEmpty()) {
            return C.INDEX_UNSET
        }
        if (startIndex < 0 || startIndex >= songs.size) {
            return C.INDEX_UNSET
        }
        return startIndex
    }

    private fun handlePlaybackError(error: PlaybackException): Unit {
        controller?.stop()
        updatePlaybackState(error = error)
    }

    private fun clearPlaybackError(): Unit {
        lastError = null
    }

    private fun resolvePlaybackError(error: PlaybackException?): PlaybackException? {
        if (error != null) {
            return error
        }
        return lastError
    }

    private fun ensureServiceStarted(): Unit {
        val intent: Intent = Intent(appContext, MusicPlayerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(appContext, intent)
            return
        }
        appContext.startService(intent)
    }

    private companion object {
        /**
         * 日志标签
         */
        private const val TAG: String = "MediaControllerManager"

        /**
         * 默认进度更新间隔（500ms）
         */
        private const val DEFAULT_PROGRESS_UPDATE_INTERVAL_MS: Long = 500L
    }
}
