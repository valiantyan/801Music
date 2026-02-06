package com.valiantyan.music801.ui.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.valiantyan.music801.R
import com.valiantyan.music801.data.datasource.AndroidMediaStoreIdResolver
import com.valiantyan.music801.data.datasource.AudioFileScanner
import com.valiantyan.music801.data.datasource.MediaMetadataExtractor
import com.valiantyan.music801.data.local.AudioDatabase
import com.valiantyan.music801.data.repository.AudioRepository
import com.valiantyan.music801.databinding.FragmentScanProgressBinding
import com.valiantyan.music801.di.AudioRepositoryProvider
import com.valiantyan.music801.domain.model.ScanMode
import com.valiantyan.music801.viewmodel.ScanUiState
import com.valiantyan.music801.viewmodel.ScanViewModel
import com.valiantyan.music801.viewmodel.ScanViewModelFactory
import kotlinx.coroutines.launch

/**
 * 扫描进度 Fragment
 *
 * 显示音频文件扫描进度，支持取消扫描操作。
 * 扫描完成后自动导航到歌曲列表。
 *
 * 配置变更处理：
 * - 使用 ViewModel 保存扫描进度状态（已扫描数量、当前路径）
 * - 配置变更后从 ViewModel 恢复进度显示
 * - 确保扫描任务在配置变更时不中断（使用 ViewModelScope）
 */
class ScanProgressFragment : Fragment() {
    /**
     * 默认扫描目录兜底路径
     */
    private companion object {
        private const val FALLBACK_ROOT_PATH: String = "/storage/emulated/0"
    }
    /**
     * ViewBinding
     */
    private var _binding: FragmentScanProgressBinding? = null

    /**
     * 视图绑定访问器
     */
    private val binding: FragmentScanProgressBinding
        get() = _binding!!

    /**
     * ViewModel
     */
    private lateinit var viewModel: ScanViewModel

    /**
     * 测试用 ViewModelFactory（仅用于 Robolectric 测试注入）
     */
    internal var viewModelFactoryForTest: ViewModelProvider.Factory? = null

    /**
     * 导航完成标记，避免重复跳转
     */
    private var hasNavigated: Boolean = false

    /**
     * 创建扫描进度视图
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
     * 初始化 [ScanViewModel] 依赖
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory: ViewModelProvider.Factory = viewModelFactoryForTest ?: run {
            val repositoryProvider: AudioRepositoryProvider? = activity as? AudioRepositoryProvider
            val audioRepository: AudioRepository = repositoryProvider?.provideAudioRepository() ?: run {
                val metadataExtractor: MediaMetadataExtractor = MediaMetadataExtractor()
                val mediaStoreIdResolver: AndroidMediaStoreIdResolver =
                    AndroidMediaStoreIdResolver(contentResolver = requireContext().contentResolver)
                val audioFileScanner: AudioFileScanner = AudioFileScanner(
                    metadataExtractor = metadataExtractor,
                    mediaStoreIdResolver = mediaStoreIdResolver,
                )
                val database: AudioDatabase = AudioDatabase.getInstance(context = requireContext().applicationContext)
                AudioRepository(
                    audioFileScanner = audioFileScanner,
                    songDao = database.songDao(),
                    librarySyncStateDao = database.librarySyncStateDao(),
                )
            }
            ScanViewModelFactory(audioRepository = audioRepository)
        }
        viewModel = ViewModelProvider(this, factory)[ScanViewModel::class.java]
    }

    /**
     * 绑定 UI 并启动扫描监听
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
        if (savedInstanceState == null) {
            startScanIfNeeded()
        }
    }

    /**
     * 设置 UI
     */
    private fun setupUI() {
        binding.cancelButton.setOnClickListener {
            viewModel.cancelScan()
            navigateBackToSongList()
        }
    }

    /**
     * 首次进入页面时启动扫描
     */
    private fun startScanIfNeeded() {
        val state: ScanUiState = viewModel.uiState.value
        if (state.isScanning || state.isCompleted || state.error != null) {
            return
        }
        val scanMode: ScanMode = ScanNavigationArgs.parseScanMode(arguments)
        val selectedDirectories: List<String> = resolveSelectedDirectories()
        viewModel.startScan(
            scanMode = scanMode,
            selectedDirectories = selectedDirectories,
        )
    }

    /**
     * 观察 ViewModel 状态
     */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state = state)
                }
            }
        }
    }

    /**
     * 更新 UI
     */
    private fun updateUI(state: ScanUiState) {
        if (state.totalCount != null && state.totalCount > 0) {
            val progress: Int =
                (state.scannedCount * 100 / state.totalCount).coerceIn(0, 100)
            binding.progressBar.progress = progress
            binding.progressBar.isIndeterminate = false
        } else {
            binding.progressBar.isIndeterminate = true
        }
        binding.scannedCountText.text =
            getString(R.string.scanned_files_count, state.scannedCount)
        if (state.currentPath != null) {
            binding.currentPathText.text =
                getString(R.string.current_scanning_path, state.currentPath)
            binding.currentPathText.visibility = View.VISIBLE
        } else {
            binding.currentPathText.visibility = View.GONE
        }
        if (state.hasError) {
            binding.errorText.text = getString(R.string.scan_error, state.error ?: "")
            binding.errorText.visibility = View.VISIBLE
        } else {
            binding.errorText.visibility = View.GONE
        }
        binding.cancelButton.isEnabled = state.isScanning
        handleCompletion(state = state)
    }

    /**
     * 清理视图绑定引用
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 扫描完成后触发导航
     */
    private fun handleCompletion(state: ScanUiState) {
        if (!state.isCompleted || hasNavigated) {
            return
        }
        navigateBackToSongList()
    }

    /**
     * 解析目录参数，缺失时使用兜底根目录
     */
    private fun resolveSelectedDirectories(): List<String> {
        val selectedDirectories: List<String> = ScanNavigationArgs.parseSelectedDirectories(arguments)
        if (selectedDirectories.isNotEmpty()) {
            return selectedDirectories
        }
        return listOf(FALLBACK_ROOT_PATH)
    }

    /**
     * 返回歌曲列表并清理扫描返回栈
     */
    private fun navigateBackToSongList() {
        val navController: NavController = findNavController()
        if (hasNavigated || navController.currentDestination?.id != R.id.scanProgressFragment) {
            return
        }
        val navOptions: NavOptions = NavOptions.Builder()
            .setPopUpTo(
                destinationId = R.id.songListFragment,
                inclusive = false,
            )
            .setLaunchSingleTop(true)
            .build()
        hasNavigated = true
        navController.navigate(
            resId = R.id.action_scanProgressFragment_to_songListFragment,
            args = null,
            navOptions = navOptions,
        )
    }

    /**
     * 统一创建视图绑定
     */
    private fun inflateBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean,
    ): FragmentScanProgressBinding {
        return FragmentScanProgressBinding.inflate(
            inflater,
            parent,
            attachToParent,
        )
    }
}
