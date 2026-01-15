package com.valiantyan.aidemo.ui.scan

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
import com.valiantyan.aidemo.R
import com.valiantyan.aidemo.data.datasource.AudioFileScanner
import com.valiantyan.aidemo.data.datasource.MediaMetadataExtractor
import com.valiantyan.aidemo.data.repository.AudioRepository
import com.valiantyan.aidemo.databinding.FragmentScanProgressBinding
import com.valiantyan.aidemo.viewmodel.ScanViewModel
import com.valiantyan.aidemo.viewmodel.ScanViewModelFactory
import kotlinx.coroutines.launch

/**
 * 扫描进度 Fragment
 * 
 * 显示音频文件扫描进度，支持取消扫描操作。
 * 扫描完成后自动导航到歌曲列表（将在后续实现）。
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
    private val binding get() = _binding!!

    /**
     * ViewModel
     */
    private lateinit var viewModel: ScanViewModel
    /**
     * 测试用 ViewModelFactory（仅用于 Robolectric 测试注入）
     */
    internal var viewModelFactoryForTest: ViewModelProvider.Factory? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 创建依赖（手动依赖注入，v1.0）
        val metadataExtractor = MediaMetadataExtractor()
        val audioFileScanner = AudioFileScanner(metadataExtractor)
        val audioRepository = AudioRepository(audioFileScanner)
        // 创建 ViewModel
        val factory = viewModelFactoryForTest ?: ScanViewModelFactory(audioRepository)
        viewModel = ViewModelProvider(this, factory)[ScanViewModel::class.java]
    }

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
                    updateUI(state)
                }
            }
        }
    }

    /**
     * 更新 UI
     */
    private fun updateUI(state: com.valiantyan.aidemo.viewmodel.ScanUiState) {
        // 更新进度条
        if (state.totalCount != null && state.totalCount > 0) {
            val progress = (state.scannedCount * 100 / state.totalCount).coerceIn(0, 100)
            binding.progressBar.progress = progress
            binding.progressBar.isIndeterminate = false
        } else {
            // 未知总数时显示不确定进度
            binding.progressBar.isIndeterminate = true
        }

        // 更新已扫描文件数
        binding.scannedCountText.text = 
            getString(R.string.scanned_files_count, state.scannedCount)

        // 更新当前扫描路径
        if (state.currentPath != null) {
            binding.currentPathText.text = 
                getString(R.string.current_scanning_path, state.currentPath)
            binding.currentPathText.visibility = View.VISIBLE
        } else {
            binding.currentPathText.visibility = View.GONE
        }

        // 更新错误信息
        if (state.hasError) {
            binding.errorText.text = getString(R.string.scan_error, state.error ?: "")
            binding.errorText.visibility = View.VISIBLE
        } else {
            binding.errorText.visibility = View.GONE
        }

        // 更新取消按钮状态
        binding.cancelButton.isEnabled = state.isScanning

        // 扫描完成后的处理（将在后续 Task 中实现导航）
        if (state.isCompleted) {
            // TODO: 导航到歌曲列表（将在后续 Task 中实现）
            // navigateToSongList()
        }
    }

    /**
     * 开始扫描
     * 
     * 注意：实际扫描路径应该从外部传入或从配置中读取。
     * 这里暂时使用默认路径，后续会完善。
     */
    private fun startScan() {
        // 获取外部存储根目录
        // 注意：实际应用中应该使用 Environment.getExternalStorageDirectory()
        // 或从 SharedPreferences 中读取用户选择的路径
        val rootPath = try {
            Environment.getExternalStorageDirectory().absolutePath
        } catch (e: Exception) {
            // 如果无法获取，使用备用路径
            "/storage/emulated/0"
        }
        
        viewModel.startScan(rootPath)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
