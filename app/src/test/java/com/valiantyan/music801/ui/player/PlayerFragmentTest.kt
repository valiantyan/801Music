package com.valiantyan.music801.ui.player

import android.os.Build
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.add
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.valiantyan.music801.R
import com.valiantyan.music801.data.repository.PlayerRepository
import com.valiantyan.music801.domain.model.PlaybackState
import com.valiantyan.music801.domain.model.Song
import com.valiantyan.music801.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class PlayerFragmentTest {
    private lateinit var repository: PlayerRepository
    private lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var playbackStateFlow: MutableStateFlow<PlaybackState>

    @Before
    fun setup(): Unit {
        repository = mock()
        playbackStateFlow = MutableStateFlow(PlaybackState())
        whenever(repository.playbackState).thenReturn(playbackStateFlow)
        viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlayerViewModel(playerRepository = repository) as T
            }
        }
    }

    @Test
    fun `点击播放按钮应调用播放`() {
        val fragment: PlayerFragment = launchFragment()
        idleMainLooper()
        fragment.requireView().findViewById<View>(R.id.playerPlayPause).performClick()
        verify(repository).play()
    }

    @Test
    fun `播放中点击暂停应调用暂停`() {
        val fragment: PlayerFragment = launchFragment()
        playbackStateFlow.value = PlaybackState(isPlaying = true)
        idleMainLooper()
        fragment.requireView().findViewById<View>(R.id.playerPlayPause).performClick()
        verify(repository).pause()
    }

    @Test
    fun `点击上一首下一首应触发切换`() {
        val fragment: PlayerFragment = launchFragment()
        idleMainLooper()
        fragment.requireView().findViewById<View>(R.id.playerPrevious).performClick()
        fragment.requireView().findViewById<View>(R.id.playerNext).performClick()
        verify(repository).skipToPrevious()
        verify(repository).skipToNext()
    }

    @Test
    fun `拖拽进度应调用跳转`() {
        val fragment: PlayerFragment = launchFragment()
        invokeHandleSeek(fragment = fragment, position = 1000L)
        verify(repository).seekTo(position = 1000L)
    }

    @Test
    fun `更新 UI 时应展示歌曲信息`() {
        val fragment: PlayerFragment = launchFragment()
        val song: Song = createSong(id = "/storage/music/song1.mp3", title = "Song 1")
        playbackStateFlow.value = PlaybackState(
            currentSong = song,
            isPlaying = false,
            position = 0L,
            duration = 60000L,
            queue = listOf(song),
            currentIndex = 0,
        )
        idleMainLooper()
        val titleText: String = fragment.requireView()
            .findViewById<android.widget.TextView>(R.id.playerSongTitle)
            .text
            .toString()
        val artistText: String = fragment.requireView()
            .findViewById<android.widget.TextView>(R.id.playerSongArtist)
            .text
            .toString()
        assertEquals("Song 1", titleText)
        assertEquals("Artist 1", artistText)
    }

    private fun launchFragment(): PlayerFragment {
        val activityController = Robolectric.buildActivity(FragmentActivity::class.java)
        activityController.setup()
        val activity: FragmentActivity = activityController.get()
        activity.setTheme(R.style.Theme_music801)
        val containerId: Int = View.generateViewId()
        val container: FrameLayout = FrameLayout(activity)
        container.id = containerId
        activity.setContentView(container)
        val fragmentFactory: FragmentFactory = TestFragmentFactory(viewModelFactory = viewModelFactory)
        activity.supportFragmentManager.fragmentFactory = fragmentFactory
        activity.supportFragmentManager.commitNow {
            add<PlayerFragment>(
                containerViewId = containerId,
                tag = "player",
            )
        }
        return activity.supportFragmentManager.findFragmentByTag("player") as PlayerFragment
    }

    private fun idleMainLooper(): Unit {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private fun invokeHandleSeek(fragment: PlayerFragment, position: Long): Unit {
        val method: java.lang.reflect.Method = PlayerFragment::class.java.getDeclaredMethod(
            "handleSeek",
            Long::class.java,
        )
        method.isAccessible = true
        method.invoke(fragment, position)
    }

    private fun createSong(
        id: String,
        title: String,
    ): Song {
        return Song(
            id = id,
            title = title,
            artist = "Artist 1",
            album = null,
            duration = 60000L,
            filePath = id,
            fileSize = 1024L,
            dateAdded = 1700000000000L,
            albumArtPath = null,
        )
    }
}

private class TestFragmentFactory(
    private val viewModelFactory: ViewModelProvider.Factory,
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        val fragment: Fragment = super.instantiate(classLoader, className)
        if (fragment is PlayerFragment) {
            fragment.viewModelFactoryForTest = viewModelFactory
        }
        return fragment
    }
}
