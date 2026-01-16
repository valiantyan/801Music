package com.valiantyan.music801.ui.songlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.valiantyan.music801.databinding.ItemSongBinding
import com.valiantyan.music801.domain.model.Song
import java.util.concurrent.TimeUnit

/**
 * 歌曲列表适配器
 *
 * @param onItemClick 列表项点击回调
 * @param onItemLongClick 列表项长按回调
 */
class SongListAdapter(
    private val onItemClick: (Song) -> Unit = {},
    private val onItemLongClick: (Song) -> Unit = {},
) : RecyclerView.Adapter<SongListAdapter.SongViewHolder>() {
    /**
     * 当前渲染的歌曲数据
     */
    private val items: MutableList<Song> = mutableListOf()

    /**
     * 当前播放歌曲标识
     */
    private var currentSongId: String? = null

    /**
     * 当前播放状态
     */
    private var isPlaying: Boolean = false

    /**
     * 创建列表项的 [SongViewHolder]
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val binding: ItemSongBinding = ItemSongBinding.inflate(inflater, parent, false)
        return SongViewHolder(binding = binding)
    }

    /**
     * 绑定 [SongViewHolder] 的展示数据
     */
    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song: Song = items[position]
        holder.bind(
            song = song,
            onItemClick = onItemClick,
            onItemLongClick = onItemLongClick,
            currentSongId = currentSongId,
            isPlaying = isPlaying,
        )
    }

    /**
     * 返回当前列表项数量
     */
    override fun getItemCount(): Int = items.size

    /**
     * 更新列表数据
     *
     * @param songs 最新歌曲列表
     */
    fun submitList(songs: List<Song>) {
        val oldItems: List<Song> = items.toList()
        val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(
            SongDiffCallback(
                oldItems = oldItems,
                newItems = songs,
            ),
        )
        items.clear()
        items.addAll(songs)
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * 更新播放状态
     *
     * @param currentSongId 当前播放歌曲 ID
     * @param isPlaying 是否正在播放
     */
    fun updatePlaybackState(
        currentSongId: String?,
        isPlaying: Boolean,
    ) {
        if (this.currentSongId == currentSongId && this.isPlaying == isPlaying) {
            return
        }
        this.currentSongId = currentSongId
        this.isPlaying = isPlaying
        notifyDataSetChanged()
    }

    /**
     * 列表项 ViewHolder
     */
    class SongViewHolder(
        private val binding: ItemSongBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        /**
         * 绑定 [Song] 数据到视图
         *
         * @param song 当前歌曲数据
         * @param onItemClick 点击回调
         * @param onItemLongClick 长按回调
         */
        fun bind(
            song: Song,
            onItemClick: (Song) -> Unit,
            onItemLongClick: (Song) -> Unit,
            currentSongId: String?,
            isPlaying: Boolean,
        ) {
            binding.songTitleText.text = song.title
            binding.songArtistText.text = song.artist
            binding.songDurationText.text = formatDuration(durationMs = song.duration)
            val isCurrentSong: Boolean = currentSongId != null && currentSongId == song.id
            binding.songPlayingIndicator.visibility = if (isCurrentSong && isPlaying) {
                View.VISIBLE
            } else {
                View.GONE
            }
            binding.root.setOnClickListener { onItemClick(song) }
            binding.root.setOnLongClickListener {
                onItemLongClick(song)
                true
            }
        }

        /**
         * 格式化时长为 mm:ss
         *
         * @param durationMs 时长（毫秒）
         * @return 格式化后的时长字符串
         */
        private fun formatDuration(durationMs: Long): String {
            val totalSeconds: Long = TimeUnit.MILLISECONDS.toSeconds(durationMs)
            val minutes: Long = totalSeconds / 60
            val seconds: Long = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * 列表 Diff 计算回调
     */
    private class SongDiffCallback(
        private val oldItems: List<Song>,
        private val newItems: List<Song>,
    ) : DiffUtil.Callback() {
        /**
         * 旧列表大小
         */
        override fun getOldListSize(): Int = oldItems.size

        /**
         * 新列表大小
         */
        override fun getNewListSize(): Int = newItems.size

        /**
         * 比较是否为同一项
         */
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition].id == newItems[newItemPosition].id
        }

        /**
         * 比较内容是否一致
         */
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}
