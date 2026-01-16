package com.valiantyan.music801.ui.scan

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.valiantyan.music801.R
import com.valiantyan.music801.data.datasource.AudioFileScanner
import com.valiantyan.music801.data.datasource.MediaMetadataExtractor
import com.valiantyan.music801.data.repository.AudioRepository
import com.valiantyan.music801.databinding.FragmentScanProgressBinding
import com.valiantyan.music801.di.AudioRepositoryProvider
import com.valiantyan.music801.viewmodel.ScanViewModel
import com.valiantyan.music801.viewmodel.ScanViewModelFactory
import com.valiantyan.music801.viewmodel.ScanUiState
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
                val audioFileScanner: AudioFileScanner = AudioFileScanner(metadataExtractor = metadataExtractor)
                AudioRepository(audioFileScanner = audioFileScanner)
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
        // 如果 ViewModel 中没有扫描任务，且没有错误，则开始扫描
        // 注意：实际扫描路径应该从外部传入或从配置中读取
        // 这里暂时使用默认路径，后续会完善
        // 配置变更后，ViewModel 会保持状态，所以这里只在首次创建时启动扫描
        if (savedInstanceState == null) {
            if (!viewModel.uiState.value.isScanning &&
                !viewModel.uiState.value.isCompleted &&
                viewModel.uiState.value.error == null) {
                startScan()
            }
        }
        // 如果 savedInstanceState != null，说明是配置变更恢复，不需要重新启动扫描
        // ViewModel 会自动保持扫描状态，UI 会通过 observeViewModel() 自动更新
    }

    /**
     * 设置 UI
     */
    private fun setupUI() {
        binding.cancelButton.setOnClickListener {
            viewModel.cancelScan()
        }
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
     * 开始扫描
     * 
     * 注意：实际扫描路径应该从外部传入或从配置中读取。
     * 这里暂时使用默认路径，后续会完善。
     */
    private fun startScan() {
        val defaultPath: String = Environment.getExternalStorageDirectory().absolutePath
        val rootPath: String = if (defaultPath.isNotBlank()) {
            defaultPath
        } else {
            "/storage/emulated/0"
        }
        viewModel.startScan(rootPath = rootPath)
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
        val navController: NavController = findNavController()
        if (navController.currentDestination?.id != R.id.scanProgressFragment) {
            return
        }
        // 清除扫描页返回栈，避免返回键回到扫描页
        val navOptions: NavOptions = NavOptions.Builder()
            .setPopUpTo(
                destinationId = R.id.scanProgressFragment,
                inclusive = true,
            )
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
