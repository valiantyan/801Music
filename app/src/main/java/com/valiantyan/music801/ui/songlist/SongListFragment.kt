package com.valiantyan.music801.ui.songlist

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.os.BundleCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.valiantyan.music801.data.datasource.AudioFileScanner
import com.valiantyan.music801.data.datasource.MediaMetadataExtractor
import com.valiantyan.music801.data.repository.AudioRepository
import com.valiantyan.music801.databinding.FragmentSongListBinding
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
    private var _binding: FragmentSongListBinding? = null
    private val binding: FragmentSongListBinding
        get() = _binding!!
    private lateinit var viewModel: SongListViewModel
    private lateinit var adapter: SongListAdapter
    private var pendingListState: Parcelable? = null
    internal var viewModelFactoryForTest: ViewModelProvider.Factory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val metadataExtractor: MediaMetadataExtractor = MediaMetadataExtractor()
        val audioFileScanner: AudioFileScanner = AudioFileScanner(metadataExtractor = metadataExtractor)
        val audioRepository: AudioRepository = AudioRepository(audioFileScanner = audioFileScanner)
        val factory: ViewModelProvider.Factory = viewModelFactoryForTest
            ?: SongListViewModelFactory(audioRepository = audioRepository)
        viewModel = ViewModelProvider(owner = this, factory = factory)[SongListViewModel::class.java]
    }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val listState: Parcelable? = binding.songListRecyclerView.layoutManager?.onSaveInstanceState()
        outState.putParcelable(KEY_LIST_STATE, listState)
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        pendingListState = getListState(bundle = savedInstanceState)
        restoreListState()
    }

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

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state = state)
                }
            }
        }
    }

    private fun updateUI(state: SongListUiState) {
        adapter.submitList(songs = state.songs)
        binding.songListLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.songListEmptyText.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
        binding.songListRecyclerView.visibility = if (state.isLoading) View.INVISIBLE else View.VISIBLE
    }

    private fun handleSongClick(song: Song) {
        // TODO: To be implemented in Story STORY-003
    }

    private fun handleSongLongClick(song: Song) {
        // TODO: To be implemented in Story STORY-003
    }

    private fun restoreListState() {
        val listState: Parcelable? = pendingListState
        if (listState == null) {
            return
        }
        binding.songListRecyclerView.layoutManager?.onRestoreInstanceState(listState)
        pendingListState = null
    }

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

    private fun getParcelableCompat(
        bundle: Bundle,
        key: String,
        clazz: Class<Parcelable>,
    ): Parcelable? {
        return BundleCompat.getParcelable(bundle, key, clazz)
    }

    private fun inflateBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean,
    ): FragmentSongListBinding {
        return FragmentSongListBinding.inflate(inflater, parent, attachToParent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        /**
         * 列表滚动状态存储键
         */
        private const val KEY_LIST_STATE: String = "song_list_state"
    }
}
