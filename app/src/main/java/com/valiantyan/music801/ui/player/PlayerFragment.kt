package com.valiantyan.music801.ui.player

import android.os.Bundle
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
import com.valiantyan.music801.data.repository.PlayerRepository
import com.valiantyan.music801.databinding.FragmentPlayerBinding
import com.valiantyan.music801.di.PlayerRepositoryProvider
import com.valiantyan.music801.domain.model.Song
import com.valiantyan.music801.viewmodel.PlayerUiState
import com.valiantyan.music801.viewmodel.PlayerViewModel
import com.valiantyan.music801.viewmodel.PlayerViewModelFactory
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * 播放器 Fragment
 *
 * 负责展示播放信息与基础控制，订阅 [PlayerViewModel] 状态更新。
 */
class PlayerFragment : Fragment() {
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
            val repository: PlayerRepository = resolvePlayerRepository()
            PlayerViewModelFactory(playerRepository = repository)
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
                // 拖拽开始无需额外处理
            }

            override fun onStopTrackingTouch(slider: Slider) {
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
        updatePlayPauseIcon(isPlaying = state.isPlaying)
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
     * 处理播放/暂停切换
     */
    private fun handlePlayPause(): Unit {
        val isPlaying: Boolean = viewModel.uiState.value.isPlaying
        if (isPlaying) {
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
     * 获取播放器仓库
     */
    private fun resolvePlayerRepository(): PlayerRepository {
        val provider: PlayerRepositoryProvider? = activity as? PlayerRepositoryProvider
        if (provider != null) {
            return provider.providePlayerRepository()
        }
        throw IllegalStateException("PlayerRepositoryProvider not found")
    }

}
