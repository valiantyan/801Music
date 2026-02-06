package com.valiantyan.music801.ui.scan

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.valiantyan.music801.R
import com.valiantyan.music801.databinding.FragmentScanDirectorySelectionBinding
import com.valiantyan.music801.domain.model.ScanMode
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanDirectorySelectionFragment : Fragment() {
    private companion object {
        private const val TAG: String = "ScanDirectorySelection"
        private const val FALLBACK_ROOT_PATH: String = "/storage/emulated/0"
    }
    private var _binding: FragmentScanDirectorySelectionBinding? = null
    private val binding: FragmentScanDirectorySelectionBinding
        get() = _binding!!
    private lateinit var adapter: ArrayAdapter<String>
    private val directoryPaths: MutableList<String> = mutableListOf()
    private val selectedIndices: MutableSet<Int> = mutableSetOf()
    internal var directoryProviderForTest: (() -> List<String>)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentScanDirectorySelectionBinding.inflate(
            inflater,
            container,
            false,
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDirectoryList()
        setupActions()
        loadDirectories()
    }

    private fun setupDirectoryList() {
        adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_multiple_choice,
            mutableListOf(),
        )
        binding.directoryListView.adapter = adapter
        binding.directoryListView.setOnItemClickListener { _, _, position, _ ->
            val isChecked: Boolean = binding.directoryListView.isItemChecked(position)
            handleDirectoryToggle(
                position = position,
                isChecked = isChecked,
            )
        }
    }

    private fun setupActions() {
        binding.selectAllButton.setOnClickListener { selectAllDirectories() }
        binding.clearAllButton.setOnClickListener { clearSelectedDirectories() }
        binding.confirmScanButton.setOnClickListener { confirmSelection() }
    }

    /** 异步读取目录，避免阻塞 UI。 */
    private fun loadDirectories() {
        viewLifecycleOwner.lifecycleScope.launch {
            val directories: List<String> = withContext(Dispatchers.IO) { queryDirectories() }
            if (!isAdded) {
                return@launch
            }
            renderDirectories(directories = directories)
        }
    }

    /** 查询 sdcard 一级目录。 */
    private fun queryDirectories(): List<String> {
        val provider: (() -> List<String>)? = directoryProviderForTest
        if (provider != null) {
            return provider().filter { path: String -> path.isNotBlank() }
        }
        val root: File = File(resolveRootPath())
        val children: Array<File> = root.listFiles() ?: return emptyList()
        return children.filter { file: File ->
            file.isDirectory && !file.name.startsWith(".")
        }.sortedBy { file: File ->
            file.name.lowercase()
        }.map { file: File ->
            file.absolutePath
        }
    }

    /** 渲染目录数据与空态。 */
    private fun renderDirectories(directories: List<String>) {
        directoryPaths.clear()
        directoryPaths.addAll(directories)
        selectedIndices.clear()
        val labels: List<String> = directories.map { path: String -> buildDirectoryLabel(path = path) }
        adapter.clear()
        adapter.addAll(labels)
        adapter.notifyDataSetChanged()
        clearCheckedItems()
        updateSelectionState()
        val hasData: Boolean = directories.isNotEmpty()
        binding.directoryListView.visibility = if (hasData) View.VISIBLE else View.GONE
        binding.directoryEmptyText.visibility = if (hasData) View.GONE else View.VISIBLE
    }

    /** 处理勾选变化。 */
    private fun handleDirectoryToggle(position: Int, isChecked: Boolean) {
        if (isChecked) {
            selectedIndices.add(position)
        } else {
            selectedIndices.remove(position)
        }
        updateSelectionState()
    }

    /** 执行全选。 */
    private fun selectAllDirectories() {
        selectedIndices.clear()
        for (index: Int in directoryPaths.indices) {
            selectedIndices.add(index)
            binding.directoryListView.setItemChecked(index, true)
        }
        updateSelectionState()
    }

    /** 执行取消全选。 */
    private fun clearSelectedDirectories() {
        selectedIndices.clear()
        clearCheckedItems()
        updateSelectionState()
    }

    private fun clearCheckedItems() {
        binding.directoryListView.clearChoices()
        adapter.notifyDataSetChanged()
    }

    /** 更新计数与按钮状态。 */
    private fun updateSelectionState() {
        val selectedCount: Int = selectedIndices.size
        binding.selectedCountText.text = getString(R.string.selected_directories_count, selectedCount)
        binding.confirmScanButton.isEnabled = selectedCount > 0
    }

    /** 确认并进入扫描页。 */
    private fun confirmSelection() {
        if (selectedIndices.isEmpty()) {
            return
        }
        val selectedDirectories: List<String> = selectedIndices.toList().sorted().map { index: Int ->
            directoryPaths[index]
        }
        Log.d(TAG, "manual scan entry clicked, selectedCount=${selectedDirectories.size}")
        val args: Bundle = ScanNavigationArgs.createBundle(
            scanMode = ScanMode.MANUAL_FULL,
            selectedDirectories = selectedDirectories,
        )
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.scanDirectorySelectionFragment) {
            return
        }
        navController.navigate(
            resId = R.id.action_scanDirectorySelectionFragment_to_scanProgressFragment,
            args = args,
        )
    }

    private fun buildDirectoryLabel(path: String): String {
        val name: String = File(path).name.ifBlank { path }
        return "$name\n$path"
    }

    private fun resolveRootPath(): String {
        val rootPath: String = Environment.getExternalStorageDirectory().absolutePath
        if (rootPath.isNotBlank()) {
            return rootPath
        }
        return FALLBACK_ROOT_PATH
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
