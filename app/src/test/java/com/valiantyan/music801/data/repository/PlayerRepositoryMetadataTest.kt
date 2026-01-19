package com.valiantyan.music801.data.repository

import android.app.Application
import android.os.Build
import androidx.media3.common.MediaMetadata
import com.valiantyan.music801.domain.model.Song
import com.valiantyan.music801.player.Media3PlayerManager
import com.valiantyan.music801.player.MediaQueueManager
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 测试 MediaSession 元数据同步所需的媒体项信息
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class PlayerRepositoryMetadataTest {
    @Test
    fun `播放时应设置媒体元数据`() {
        // Arrange
        val context: Application = RuntimeEnvironment.getApplication()
        val inputFile: File = createTempAudioFile(context = context)
        val inputSong: Song = Song(
            id = inputFile.absolutePath,
            title = "Test Title",
            artist = "Test Artist",
            album = "Test Album",
            duration = 180000L,
            filePath = inputFile.absolutePath,
            fileSize = inputFile.length(),
            dateAdded = System.currentTimeMillis(),
            albumArtPath = null,
        )
        val manager: Media3PlayerManager = Media3PlayerManager(context = context)
        val repository: PlayerRepositoryImpl = PlayerRepositoryImpl(
            mediaQueueManager = MediaQueueManager(),
            mediaPlayerManager = manager,
        )
        // Act
        repository.setQueue(songs = listOf(inputSong), startIndex = 0)
        repository.play()
        val actualMetadata: MediaMetadata? = manager.exoPlayer.currentMediaItem?.mediaMetadata
        // Assert
        assertNotNull(actualMetadata)
        assertEquals("Test Title", actualMetadata?.title)
        assertEquals("Test Artist", actualMetadata?.artist)
        assertEquals("Test Album", actualMetadata?.albumTitle)
        manager.release()
    }

    private fun createTempAudioFile(context: Application): File {
        val file: File = File(context.cacheDir, "metadata-audio.mp3")
        if (!file.exists()) {
            file.writeBytes(byteArrayOf(0x00))
        }
        return file
    }
}
