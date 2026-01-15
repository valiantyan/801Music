package com.valiantyan.music801.ui.songlist

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.valiantyan.music801.R
import com.valiantyan.music801.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 测试 SongListAdapter 基础功能
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SongListAdapterTest {
    @Test
    fun `提交列表后返回正确数量`() : Unit {
        // Arrange
        val adapter: SongListAdapter = createAdapter()
        val song: Song = createSong(
            id = "/storage/music/song1.mp3",
            title = "Song 1",
            artist = "Artist 1",
            durationMs = 180000L,
        )
        val songs: List<Song> = listOf(song)
        // Act
        adapter.submitList(songs = songs)
        val actualCount: Int = adapter.itemCount
        // Assert
        assertEquals(1, actualCount)
    }

    @Test
    fun `绑定数据后显示标题艺术家与时长`() : Unit {
        // Arrange
        val adapter: SongListAdapter = createAdapter()
        val song: Song = createSong(
            id = "/storage/music/song2.mp3",
            title = "Song 2",
            artist = "Artist 2",
            durationMs = 180000L,
        )
        val songs: List<Song> = listOf(song)
        val baseContext: Context = ApplicationProvider.getApplicationContext()
        val themedContext: Context = createThemedContext(baseContext = baseContext)
        val parent: FrameLayout = FrameLayout(themedContext)
        // Act
        adapter.submitList(songs = songs)
        val viewHolder: SongListAdapter.SongViewHolder = adapter.onCreateViewHolder(
            parent = parent,
            viewType = 0,
        )
        adapter.onBindViewHolder(
            holder = viewHolder,
            position = 0,
        )
        val titleText: TextView = viewHolder.itemView.findViewById(R.id.songTitleText)
        val artistText: TextView = viewHolder.itemView.findViewById(R.id.songArtistText)
        val durationText: TextView = viewHolder.itemView.findViewById(R.id.songDurationText)
        // Assert
        assertEquals("Song 2", titleText.text.toString())
        assertEquals("Artist 2", artistText.text.toString())
        assertEquals("03:00", durationText.text.toString())
    }

    private fun createAdapter(): SongListAdapter {
        return SongListAdapter()
    }

    private fun createThemedContext(baseContext: Context): Context {
        return ContextThemeWrapper(baseContext, R.style.Theme_music801)
    }

    private fun createSong(
        id: String,
        title: String,
        artist: String,
        durationMs: Long,
    ): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = null,
            duration = durationMs,
            filePath = id,
            fileSize = 1024L,
            dateAdded = 1700000000000L,
            albumArtPath = null,
        )
    }
}
