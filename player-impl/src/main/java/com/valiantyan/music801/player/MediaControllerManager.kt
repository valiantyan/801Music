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
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.valiantyan.music801.domain.model.PlaybackState
import com.valiantyan.music801.domain.model.Song
import com.valiantyan.music801.service.MusicPlayerService
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * MediaController 管理器
 *
 * 通过 [MediaController] 连接媒体会话并同步播放状态。
 */
internal class MediaControllerManager(
    context: Context,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : PlayerController {
    /**
     * 统一持有 [Context] 的 application 级引用，避免 [Activity] 泄漏
     */
    private val appContext: Context = context.applicationContext
    /**
     * 用于串行处理媒体会话回调与命令补发的协程作用域
     */
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
    /**
     * 播放状态源，通过 [playbackState] 对外提供只读流
     */
    private val playbackStateFlow: MutableStateFlow<PlaybackState> = MutableStateFlow(PlaybackState())
    /**
     * 连接中标记，防止重复创建 [MediaController]
     */
    private val isConnecting: AtomicBoolean = AtomicBoolean(false)
    /**
     * 已连接的 [MediaController] 实例
     */
    private var controller: MediaController? = null
    /**
     * 连接中的 [MediaController] Future，便于释放时取消
     */
    private var controllerFuture: ListenableFuture<MediaController>? = null
    /**
     * 用于在 [toggleFavorite] 与 [buildPlaybackState] 之间复用收藏状态，避免重复请求会话
     */
    private val favoriteMediaIds: MutableSet<String> = mutableSetOf()
    /**
     * 缓存最后一次播放错误，用于连接断开时仍可展示错误
     */
    private var lastError: PlaybackException? = null
    /**
     * 视图层最新的播放队列缓存，用于构建 [PlaybackState]
     */
    private var currentQueue: List<Song> = emptyList()
    /**
     * 当前队列索引缓存，与 [PlaybackState.currentIndex] 保持同步
     */
    private var currentIndex: Int = C.INDEX_UNSET
    /**
     * 连接完成前的待处理队列
     */
    private var pendingQueue: List<Song>? = null
    /**
     * 连接完成前的待处理起始索引
     */
    private var pendingStartIndex: Int = C.INDEX_UNSET
    /**
     * 连接完成后是否需要自动播放
     */
    private var pendingPlay: Boolean = false
    /**
     * 记录收藏命令需在 [connect] 成功后补发，保证调用时序一致
     */
    private var pendingToggleFavorite: Boolean = false
    /**
     * 监听 [Player] 事件并驱动 [updatePlaybackState]
     */
    private val playerListener: Player.Listener = buildPlayerListener()
    /**
     * 对外暴露的播放状态流
     */
    override val playbackState: StateFlow<PlaybackState> = playbackStateFlow.asStateFlow()

    /**
     * 建立 [MediaController] 连接并处理待补发命令
     */
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
                    applyPendingQueue(controller = result)
                    updatePlaybackState(error = null)
                    if (pendingPlay) {
                        pendingPlay = false
                        result.play()
                    }
                    if (pendingToggleFavorite) {
                        pendingToggleFavorite = false
                        coroutineScope.launch {
                            sendToggleFavoriteCommand(controller = result)
                        }
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

    /**
     * 释放 [MediaController] 与监听，避免资源泄漏
     */
    fun release(): Unit {
        val future: ListenableFuture<MediaController>? = controllerFuture
        if (future != null) {
            future.cancel(true)
        }
        controllerFuture = null
        val player: MediaController? = controller
        if (player != null) {
            player.removeListener(playerListener)
            player.release()
        }
        controller = null
        isConnecting.set(false)
    }

    /**
     * 设置播放队列，连接未完成时先缓存请求
     */
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

    /**
     * 请求播放，未连接时先建立连接
     */
    override fun play(): Unit {
        val player: MediaController? = controller
        if (player != null) {
            player.play()
            return
        }
        pendingPlay = true
        connect()
    }

    /**
     * 请求暂停
     */
    override fun pause(): Unit {
        controller?.pause()
    }

    /**
     * 跳转到目标位置
     */
    override fun seekTo(position: Long): Unit {
        controller?.seekTo(position)
    }

    /**
     * 切换到下一首
     */
    override fun skipToNext(): Unit {
        controller?.seekToNextMediaItem()
    }

    /**
     * 切换到上一首
     */
    override fun skipToPrevious(): Unit {
        controller?.seekToPreviousMediaItem()
    }

    /**
     * 切换收藏状态并返回 [PlayerCommandResult]
     */
    override suspend fun toggleFavorite(): PlayerCommandResult {
        val player: MediaController? = controller
        if (player != null) {
            return sendToggleFavoriteCommand(controller = player)
        }
        pendingToggleFavorite = true
        connect()
        return PlayerCommandResult(
            isSuccess = false,
            errorMessage = "controller-not-connected",
        )
    }

    /**
     * 在 [connect] 成功后补发队列设置请求
     */
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

    /**
     * 将 [Song] 列表转换为 [MediaItem] 并设置到 [MediaController]
     */
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

    /**
     * 通过 [PlayerCommands.ACTION_TOGGLE_FAVORITE] 发送收藏切换并同步 [favoriteMediaIds]
     */
    private suspend fun sendToggleFavoriteCommand(
        controller: MediaController,
    ): PlayerCommandResult {
        val command: androidx.media3.session.SessionCommand = androidx.media3.session.SessionCommand(
            PlayerCommands.ACTION_TOGGLE_FAVORITE,
            android.os.Bundle(),
        )
        val future: ListenableFuture<androidx.media3.session.SessionResult> =
            controller.sendCustomCommand(command, android.os.Bundle())
        val result: androidx.media3.session.SessionResult = awaitResult(future = future)
        val extras: android.os.Bundle = result.extras
        val mediaId: String? = extras.getString(PlayerCommands.EXTRA_MEDIA_ID)
        val isFavorite: Boolean = extras.getBoolean(PlayerCommands.EXTRA_IS_FAVORITE, false)
        if (!mediaId.isNullOrBlank() &&
            result.resultCode == androidx.media3.session.SessionResult.RESULT_SUCCESS
        ) {
            if (isFavorite) {
                favoriteMediaIds.add(mediaId)
            } else {
                favoriteMediaIds.remove(mediaId)
            }
            updatePlaybackState(error = null)
        }
        return PlayerCommandResult(
            isSuccess = result.resultCode == androidx.media3.session.SessionResult.RESULT_SUCCESS,
            errorMessage = extras.getString(PlayerCommands.EXTRA_ERROR_MESSAGE),
            extras = extras,
        )
    }

    /**
     * 构建播放列表条目，统一 [MediaMetadata] 与媒体 ID
     */
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
            .setMediaId(song.id)
            .setMediaMetadata(metadata)
            .build()
    }

    /**
     * 根据 [Song.albumArtPath] 生成封面 Uri
     */
    private fun resolveArtworkUri(song: Song): Uri? {
        val path: String? = song.albumArtPath
        if (path.isNullOrBlank()) {
            return null
        }
        return Uri.fromFile(File(path))
    }

    /**
     * 构建播放器事件监听器，用于驱动 [updatePlaybackState]
     */
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

    /**
     * 根据当前 [MediaController] 状态刷新 [playbackStateFlow]
     */
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

    /**
     * 构建新的 [PlaybackState] 快照
     */
    private fun buildPlaybackState(
        player: Player,
        error: PlaybackException?,
    ): PlaybackState {
        val index: Int = resolveIndexFromPlayer()
        val position: Long = player.currentPosition.coerceAtLeast(0L)
        val duration: Long = if (player.duration >= 0L) player.duration else 0L
        val mediaId: String? = player.currentMediaItem?.mediaId
        val isFavorite: Boolean = if (mediaId.isNullOrBlank()) {
            false
        } else {
            favoriteMediaIds.contains(mediaId)
        }
        return PlaybackState(
            currentSong = resolveCurrentSong(index = index),
            isPlaying = player.isPlaying,
            position = position,
            duration = duration,
            bufferedPosition = player.bufferedPosition.coerceAtLeast(0L),
            playbackState = player.playbackState,
            error = error,
            isFavorite = isFavorite,
            queue = currentQueue,
            currentIndex = index,
        )
    }

    /**
     * 从 [MediaController] 解析当前索引并同步缓存
     */
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

    /**
     * 根据索引查找当前 [Song]
     */
    private fun resolveCurrentSong(index: Int): Song? {
        if (index < 0 || index >= currentQueue.size) {
            return null
        }
        return currentQueue[index]
    }

    /**
     * 规范化起始索引，非法时返回 [C.INDEX_UNSET]
     */
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

    /**
     * 处理播放错误并同步到 [playbackStateFlow]
     */
    private fun handlePlaybackError(error: PlaybackException): Unit {
        controller?.stop()
        updatePlaybackState(error = error)
    }

    /**
     * 清理缓存错误，避免影响后续状态展示
     */
    private fun clearPlaybackError(): Unit {
        lastError = null
    }

    /**
     * 优先返回最新错误，否则复用 [lastError]
     */
    private fun resolvePlaybackError(error: PlaybackException?): PlaybackException? {
        if (error != null) {
            return error
        }
        return lastError
    }

    /**
     * 将 [ListenableFuture] 转为挂起调用，统一处理 [sendToggleFavoriteCommand] 回执
     */
    private suspend fun awaitResult(
        future: ListenableFuture<androidx.media3.session.SessionResult>,
    ): androidx.media3.session.SessionResult {
        return suspendCancellableCoroutine { continuation ->
            Futures.addCallback(
                future,
                object : FutureCallback<androidx.media3.session.SessionResult> {
                    override fun onSuccess(result: androidx.media3.session.SessionResult?) {
                        if (result != null && continuation.isActive) {
                            continuation.resume(result)
                        }
                    }
                    override fun onFailure(t: Throwable) {
                        if (continuation.isActive) {
                            continuation.resume(
                                androidx.media3.session.SessionResult(
                                    androidx.media3.session.SessionResult.RESULT_ERROR_UNKNOWN,
                                ),
                            )
                        }
                    }
                },
                MoreExecutors.directExecutor(),
            )
            continuation.invokeOnCancellation { future.cancel(true) }
        }
    }

    /**
     * 确保 [MusicPlayerService] 处于运行状态以建立会话
     */
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
    }
}
