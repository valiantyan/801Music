package com.valiantyan.music801.ui.songlist

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.os.BundleCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.valiantyan.music801.R
import com.valiantyan.music801.data.datasource.AudioFileScanner
import com.valiantyan.music801.data.datasource.MediaMetadataExtractor
import com.valiantyan.music801.data.repository.AudioRepository
import com.valiantyan.music801.databinding.FragmentSongListBinding
import com.valiantyan.music801.di.AudioRepositoryProvider
import com.valiantyan.music801.domain.model.Song
import com.valiantyan.music801.viewmodel.SongListUiState
import com.valiantyan.music801.viewmodel.SongListViewModel
import com.valiantyan.music801.viewmodel.SongListViewModelFactory
import kotlinx.coroutines.launch

/**
 * 歌曲列表 Fragment
 *
 * 展示扫描结果的歌曲列表，支持空状态与加载状态。
 */
class SongListFragment : Fragment() {
    /**
     * 视图绑定缓存
     */
    private var _binding: FragmentSongListBinding? = null
    /**
     * 视图绑定访问器
     */
    private val binding: FragmentSongListBinding
        get() = _binding!!
    /**
     * 列表页面状态管理
     */
    private lateinit var viewModel: SongListViewModel
    /**
     * 列表适配器实例
     */
    private lateinit var adapter: SongListAdapter
    /**
     * 列表滚动状态缓存
     */
    private var pendingListState: Parcelable? = null
    /**
     * 测试注入的 ViewModel 工厂
     */
    internal var viewModelFactoryForTest: ViewModelProvider.Factory? = null
    /**
     * 上一次返回键时间戳
     */
    private var lastBackPressedTime: Long = 0L

    /**
     * 初始化 [SongListViewModel] 依赖
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
            SongListViewModelFactory(audioRepository = audioRepository)
        }
        viewModel = ViewModelProvider(owner = this, factory = factory)[SongListViewModel::class.java]
    }

    /**
     * 创建并绑定列表视图
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
     * 绑定视图后进行 UI 初始化
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        setupBackPressHandler()
    }

    /**
     * 保存列表滚动状态
     */
    override fun onSaveInstanceState(outState: Bundle) {
        val listState: Parcelable? = binding.songListRecyclerView.layoutManager?.onSaveInstanceState()
        outState.putParcelable(KEY_LIST_STATE, listState)
        super.onSaveInstanceState(outState)
    }

    /**
     * 恢复列表滚动状态
     */
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        pendingListState = getListState(bundle = savedInstanceState)
        restoreListState()
    }

    /**
     * 初始化列表组件
     */
    private fun setupRecyclerView() {
        val layoutManager: LinearLayoutManager = LinearLayoutManager(requireContext())
        adapter = SongListAdapter(
            onItemClick = { song: Song ->
                handleSongClick(song = song)
            },
            onItemLongClick = { song: Song ->
                handleSongLongClick(song = song)
            },
        )
        binding.songListRecyclerView.layoutManager = layoutManager
        binding.songListRecyclerView.adapter = adapter
        binding.songListRecyclerView.setHasFixedSize(true)
        restoreListState()
    }

    /**
     * 订阅列表状态变化
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
     * 渲染列表状态
     */
    private fun updateUI(state: SongListUiState) {
        adapter.submitList(songs = state.songs)
        binding.songListLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.songListEmptyText.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
        binding.songListRecyclerView.visibility = if (state.isLoading) View.INVISIBLE else View.VISIBLE
    }

    /**
     * 处理列表项点击
     */
    private fun handleSongClick(song: Song) {
        // TODO: To be implemented in Story STORY-003
    }

    /**
     * 处理列表项长按
     */
    private fun handleSongLongClick(song: Song) {
        // TODO: To be implemented in Story STORY-003
    }

    /**
     * 恢复列表滚动位置
     */
    private fun restoreListState() {
        val listState: Parcelable? = pendingListState
        if (listState == null) {
            return
        }
        binding.songListRecyclerView.layoutManager?.onRestoreInstanceState(listState)
        pendingListState = null
    }

    /**
     * 从 Bundle 中读取列表状态
     *
     * @param bundle 状态容器
     * @return 列表滚动状态
     */
    private fun getListState(bundle: Bundle?): Parcelable? {
        if (bundle == null) {
            return null
        }
        return getParcelableCompat(
            bundle = bundle,
            key = KEY_LIST_STATE,
            clazz = Parcelable::class.java,
        )
    }

    /**
     * 兼容不同版本的 Parcelable 读取
     *
     * @param bundle 状态容器
     * @param key 存储键
     * @param clazz 类型
     * @return 读取到的值
     */
    private fun getParcelableCompat(
        bundle: Bundle,
        key: String,
        clazz: Class<Parcelable>,
    ): Parcelable? {
        return BundleCompat.getParcelable(bundle, key, clazz)
    }

    /**
     * 统一创建视图绑定
     */
    private fun inflateBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean,
    ): FragmentSongListBinding {
        return FragmentSongListBinding.inflate(
            inflater,
            parent,
            attachToParent,
        )
    }

    /**
     * 处理双击返回退出逻辑
     */
    private fun setupBackPressHandler() {
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentTime: Long = System.currentTimeMillis()
                if (currentTime - lastBackPressedTime <= EXIT_INTERVAL_MS) {
                    requireActivity().finish()
                    return
                }
                // 防止误触退出，提示用户再次点击返回键
                lastBackPressedTime = currentTime
                showExitToast()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            owner = viewLifecycleOwner,
            onBackPressedCallback = callback,
        )
    }

    /**
     * 显示退出提示
     */
    private fun showExitToast() {
        val toast: Toast = Toast(requireContext())
        // 使用统一文案，避免硬编码
        toast.setText(getString(R.string.exit_app_tip))
        toast.duration = Toast.LENGTH_SHORT
        toast.show()
    }

    /**
     * 清理视图绑定引用
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        /**
         * 列表滚动状态存储键
         */
        private const val KEY_LIST_STATE: String = "song_list_state"
        /**
         * 退出应用的双击间隔(2000ms)
         */
        private const val EXIT_INTERVAL_MS: Long = 2000L
    }
}
