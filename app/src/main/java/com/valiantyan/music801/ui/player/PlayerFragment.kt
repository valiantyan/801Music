package com.valiantyan.music801.ui.player

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.slider.Slider
import com.valiantyan.music801.R
import com.valiantyan.music801.databinding.FragmentPlayerBinding
import com.valiantyan.music801.domain.model.Song
import com.valiantyan.music801.di.PlayerControllerProvider
import com.valiantyan.music801.player.PlayerController
import com.valiantyan.music801.player.PlayerControllerRegistry
import com.valiantyan.music801.viewmodel.PlayerUiState
import com.valiantyan.music801.viewmodel.PlayerViewModel
import com.valiantyan.music801.viewmodel.PlayerViewModelFactory
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 播放器 Fragment
 *
 * 负责展示播放信息与基础控制，订阅 [PlayerViewModel] 状态更新。
 */
class PlayerFragment : Fragment() {
    /**
     * 日志标签
     */
    private companion object {
        private const val TAG: String = "MediaNotification"
        private const val PROGRESS_UPDATE_INTERVAL_MS: Long = 1000L
        private const val PLAYBACK_STATE_BUFFERING: Int = 2
    }
    /**
     * ViewBinding
     */
    private var _binding: FragmentPlayerBinding? = null

    /**
     * 视图绑定访问器
     */
    private val binding: FragmentPlayerBinding
        get() = _binding!!

    /**
     * ViewModel
     */
    private lateinit var viewModel: PlayerViewModel

    /**
     * 测试用 ViewModelFactory（仅用于测试注入）
     */
    internal var viewModelFactoryForTest: ViewModelProvider.Factory? = null
    /**
     * 播放中 UI 进度补间任务
     */
    private var progressTickerJob: Job? = null
    /**
     * 用户是否正在手动拖拽进度条
     */
    private var isUserSeeking: Boolean = false

    /**
     * 创建播放器视图
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = inflateBinding(
            inflater = inflater,
            parent = container,
            attachToParent = false,
        )
        return binding.root
    }

    /**
     * 初始化 [PlayerViewModel] 依赖
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory: ViewModelProvider.Factory = viewModelFactoryForTest ?: run {
            val controller: PlayerController = resolvePlayerController()
            PlayerViewModelFactory(playerController = controller)
        }
        viewModel = ViewModelProvider(this, factory)[PlayerViewModel::class.java]
    }

    /**
     * 绑定 UI 与状态订阅
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
        observeViewModel()
    }

    /**
     * 设置播放控制交互
     */
    private fun setupUi(): Unit {
        binding.playerPlayPause.setOnClickListener {
            handlePlayPause()
        }
        binding.playerNext.setOnClickListener {
            viewModel.skipToNext()
        }
        binding.playerPrevious.setOnClickListener {
            viewModel.skipToPrevious()
        }
        binding.playerProgress.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(slider: Slider) {
                isUserSeeking = false
                handleSeek(position = slider.value.toLong())
            }
        })
    }

    /**
     * 观察 [PlayerViewModel] 状态
     */
    private fun observeViewModel(): Unit {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state = state)
                }
            }
        }
    }

    /**
     * 更新 UI
     *
     * @param state 播放器 UI 状态
     */
    private fun updateUi(state: PlayerUiState): Unit {
        val currentSong: Song? = state.currentSong
        val titleText: String = currentSong?.title ?: getString(R.string.player_song_title_placeholder)
        val artistText: String = currentSong?.artist ?: getString(R.string.player_song_artist_placeholder)
        binding.playerSongTitle.text = titleText
        binding.playerSongArtist.text = artistText
        updateProgress(state = state)
        val isBufferingWithPlayIntent: Boolean = isBufferingWithPlayIntent(state = state)
        updateBufferingIndicator(isVisible = isBufferingWithPlayIntent)
        updatePlayPauseIcon(isPlaying = isPlaybackActiveForUi(state = state))
        syncProgressTicker(state = state)
    }

    /**
     * 更新进度显示
     *
     * @param state 播放器 UI 状态
     */
    private fun updateProgress(state: PlayerUiState): Unit {
        val duration: Long = if (state.duration > 0L) state.duration else 0L
        val position: Long = state.position.coerceIn(
            minimumValue = 0L,
            maximumValue = duration,
        )
        binding.playerProgress.valueFrom = 0f
        binding.playerProgress.valueTo = duration.toFloat().coerceAtLeast(1f)
        binding.playerProgress.value = position.toFloat()
        binding.playerPosition.text = formatTime(milliseconds = position)
        binding.playerDuration.text = formatTime(milliseconds = duration)
    }

    /**
     * 更新播放按钮图标
     *
     * @param isPlaying 是否正在播放
     */
    private fun updatePlayPauseIcon(isPlaying: Boolean): Unit {
        val iconResId: Int = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        binding.playerPlayPause.setImageResource(iconResId)
    }

    /**
     * 更新缓冲外环可见性
     */
    private fun updateBufferingIndicator(isVisible: Boolean): Unit {
        binding.playerBufferingIndicator.visibility = if (isVisible) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    /**
     * 基于播放意图和缓冲态计算 UI 是否应保持“播放中”视觉
     */
    private fun isPlaybackActiveForUi(state: PlayerUiState): Boolean {
        return state.isPlaying || isBufferingWithPlayIntent(state = state)
    }

    /**
     * 判断是否处于“有播放意图且正在缓冲”的状态
     */
    private fun isBufferingWithPlayIntent(state: PlayerUiState): Boolean {
        return state.isPlayWhenReady && state.playbackState == PLAYBACK_STATE_BUFFERING
    }

    /**
     * 播放中使用本地定时器补间进度，避免状态流仅事件触发导致进度条静止
     */
    private fun syncProgressTicker(state: PlayerUiState): Unit {
        if (!state.isPlaying) {
            progressTickerJob?.cancel()
            progressTickerJob = null
            return
        }
        if (progressTickerJob?.isActive == true) {
            return
        }
        progressTickerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                delay(PROGRESS_UPDATE_INTERVAL_MS)
                if (isUserSeeking) {
                    continue
                }
                val duration: Long = binding.playerProgress.valueTo.toLong().coerceAtLeast(0L)
                val currentPosition: Long = binding.playerProgress.value.toLong().coerceAtLeast(0L)
                val nextPosition: Long = (currentPosition + PROGRESS_UPDATE_INTERVAL_MS).coerceAtMost(duration)
                binding.playerProgress.value = nextPosition.toFloat()
                binding.playerPosition.text = formatTime(milliseconds = nextPosition)
            }
        }
    }

    /**
     * 处理播放/暂停切换
     */
    private fun handlePlayPause(): Unit {
        val state: PlayerUiState = viewModel.uiState.value
        Log.d(
            TAG,
            "playPause click: isPlaying=${state.isPlaying}, isPlayWhenReady=${state.isPlayWhenReady}",
        )
        if (state.isPlayWhenReady) {
            viewModel.pause()
        } else {
            viewModel.play()
        }
    }


    /**
     * 处理拖拽进度跳转
     *
     * @param position 目标位置（毫秒）
     */
    private fun handleSeek(position: Long): Unit {
        viewModel.seekTo(position = position)
    }

    /**
     * 格式化时间显示
     *
     * @param milliseconds 时长（毫秒）
     * @return mm:ss 格式字符串
     */
    private fun formatTime(milliseconds: Long): String {
        val totalSeconds: Long = milliseconds / 1000L
        val minutes: Long = totalSeconds / 60L
        val seconds: Long = totalSeconds % 60L
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    /**
     * 清理视图绑定引用
     */
    override fun onDestroyView() {
        progressTickerJob?.cancel()
        progressTickerJob = null
        super.onDestroyView()
        _binding = null
    }

    /**
     * 统一创建视图绑定
     */
    private fun inflateBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean,
    ): FragmentPlayerBinding {
        return FragmentPlayerBinding.inflate(
            inflater,
            parent,
            attachToParent,
        )
    }

    /**
     * 获取播放控制器
     */
    private fun resolvePlayerController(): PlayerController {
        val provider: PlayerControllerProvider? = activity as? PlayerControllerProvider
        if (provider != null) {
            return provider.providePlayerController()
        }
        return PlayerControllerRegistry.getOrCreate(context = requireContext())
    }

}
