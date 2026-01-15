package com.valiantyan.music801.ui.songlist

import android.view.LayoutInflater
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
    private val items: MutableList<Song> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val binding: ItemSongBinding = ItemSongBinding.inflate(inflater, parent, false)
        return SongViewHolder(binding = binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song: Song = items[position]
        holder.bind(
            song = song,
            onItemClick = onItemClick,
            onItemLongClick = onItemLongClick,
        )
    }

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
     * 列表项 ViewHolder
     */
    class SongViewHolder(
        private val binding: ItemSongBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            song: Song,
            onItemClick: (Song) -> Unit,
            onItemLongClick: (Song) -> Unit,
        ) {
            binding.songTitleText.text = song.title
            binding.songArtistText.text = song.artist
            binding.songDurationText.text = formatDuration(durationMs = song.duration)
            binding.root.setOnClickListener { onItemClick(song) }
            binding.root.setOnLongClickListener {
                onItemLongClick(song)
                true
            }
        }

        private fun formatDuration(durationMs: Long): String {
            val totalSeconds: Long = TimeUnit.MILLISECONDS.toSeconds(durationMs)
            val minutes: Long = totalSeconds / 60
            val seconds: Long = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    }

    private class SongDiffCallback(
        private val oldItems: List<Song>,
        private val newItems: List<Song>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition].id == newItems[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}
