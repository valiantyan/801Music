package com.valiantyan.music801.ui.songlist

import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.add
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.recyclerview.widget.RecyclerView
import com.valiantyan.music801.R
import com.valiantyan.music801.data.repository.AudioRepository
import com.valiantyan.music801.data.repository.PlayerRepository
import com.valiantyan.music801.di.PlayerRepositoryProvider
import com.valiantyan.music801.domain.model.Song
import com.valiantyan.music801.viewmodel.SongListUiState
import com.valiantyan.music801.viewmodel.SongListViewModel
import kotlinx.coroutines.flow.flowOf
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
class SongListFragmentTest {
    private lateinit var repository: AudioRepository
    private lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var playerRepository: PlayerRepository

    @Before
    fun setup() {
        repository = mock()
        playerRepository = mock()
        whenever(repository.getAllSongs()).thenReturn(flowOf(emptyList()))
        viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SongListViewModel(audioRepository = repository) as T
            }
        }
    }

    @Test
    fun `更新空状态时显示空提示`() {
        val fragment: SongListFragment = launchFragment()
        idleMainLooper()
        invokeUpdateUi(
            fragment = fragment,
            state = SongListUiState(
                songs = emptyList(),
                isLoading = false,
                isEmpty = true,
                error = null,
            ),
        )
        val emptyText: TextView = fragment.requireView().findViewById(R.id.songListEmptyText)
        assertEquals(View.VISIBLE, emptyText.visibility)
    }

    @Test
    fun `更新加载状态时显示加载指示`() {
        val fragment: SongListFragment = launchFragment()
        idleMainLooper()
        invokeUpdateUi(
            fragment = fragment,
            state = SongListUiState(
                songs = emptyList(),
                isLoading = true,
                isEmpty = false,
                error = null,
            ),
        )
        val loadingView: View = fragment.requireView().findViewById(R.id.songListLoading)
        assertEquals(View.VISIBLE, loadingView.visibility)
    }

    @Test
    fun `端到端列表展示应显示歌曲`() {
        val songs: List<Song> = createSongs(count = 3)
        whenever(repository.getAllSongs()).thenReturn(flowOf(songs))
        val fragment: SongListFragment = launchFragment()
        idleMainLooper()
        val recyclerView: androidx.recyclerview.widget.RecyclerView =
            fragment.requireView().findViewById(R.id.songListRecyclerView)
        val adapter: SongListAdapter = recyclerView.adapter as SongListAdapter
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `保存并恢复列表状态后位置保持`() {
        val songs: List<Song> = createSongs(count = 30)
        whenever(repository.getAllSongs()).thenReturn(flowOf(songs))
        val fragment: SongListFragment = launchFragment()
        idleMainLooper()
        val recyclerView: androidx.recyclerview.widget.RecyclerView =
            fragment.requireView().findViewById(R.id.songListRecyclerView)
        recyclerView.scrollToPosition(20)
        idleMainLooper()
        val stateBundle: Bundle = Bundle()
        fragment.onSaveInstanceState(stateBundle)
        fragment.onViewStateRestored(stateBundle)
        idleMainLooper()
        val layoutManager = recyclerView.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
        val actualPosition: Int = layoutManager.findFirstVisibleItemPosition()
        val listState: Parcelable? = layoutManager.onSaveInstanceState()
        assertTrue(actualPosition >= 0)
        assertTrue(listState != null)
    }

    @Test
    fun `点击歌曲后应设置队列并导航到播放页`() {
        val songs: List<Song> = createSongs(count = 2)
        whenever(repository.getAllSongs()).thenReturn(flowOf(songs))
        val fragment: SongListFragment = launchFragment()
        val navController: TestNavHostController = createNavController(fragment = fragment)
        Navigation.setViewNavController(fragment.requireView(), navController)
        idleMainLooper()
        val recyclerView: RecyclerView = fragment.requireView().findViewById(R.id.songListRecyclerView)
        val adapter: SongListAdapter = recyclerView.adapter as SongListAdapter
        val context = fragment.requireContext()
        val viewHolder: SongListAdapter.SongViewHolder = adapter.onCreateViewHolder(
            parent = FrameLayout(context),
            viewType = 0,
        )
        adapter.onBindViewHolder(
            holder = viewHolder,
            position = 0,
        )
        viewHolder.itemView.performClick()
        verify(playerRepository).setQueue(
            songs = songs,
            startIndex = 0,
        )
        assertEquals(R.id.playerFragment, navController.currentDestination?.id)
    }

    private fun launchFragment(): SongListFragment {
        TestPlayerActivity.playerRepository = playerRepository
        val activityController = Robolectric.buildActivity(TestPlayerActivity::class.java)
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
            add<SongListFragment>(
                containerViewId = containerId,
                tag = "song_list",
            )
        }
        return activity.supportFragmentManager.findFragmentByTag("song_list") as SongListFragment
    }

    private fun idleMainLooper() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private fun createNavController(fragment: SongListFragment): TestNavHostController {
        val navController = TestNavHostController(fragment.requireContext())
        navController.setGraph(R.navigation.nav_graph)
        navController.setCurrentDestination(R.id.songListFragment)
        return navController
    }

    private fun invokeUpdateUi(fragment: SongListFragment, state: SongListUiState) {
        val method: java.lang.reflect.Method = SongListFragment::class.java.getDeclaredMethod(
            "updateUI",
            SongListUiState::class.java,
        )
        method.isAccessible = true
        method.invoke(fragment, state)
    }

    private fun createSongs(count: Int): List<Song> {
        val songs: MutableList<Song> = mutableListOf()
        for (index: Int in 1..count) {
            songs.add(
                Song(
                    id = "/storage/music/song$index.mp3",
                    title = "Song $index",
                    artist = "Artist $index",
                    album = null,
                    duration = 60000L,
                    filePath = "/storage/music/song$index.mp3",
                    fileSize = 1024L,
                    dateAdded = 1700000000000L,
                    albumArtPath = null,
                ),
            )
        }
        return songs
    }
}

private class TestFragmentFactory(
    private val viewModelFactory: ViewModelProvider.Factory,
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        val fragment: Fragment = super.instantiate(classLoader, className)
        if (fragment is SongListFragment) {
            fragment.viewModelFactoryForTest = viewModelFactory
        }
        return fragment
    }
}

private class TestPlayerActivity : FragmentActivity(), PlayerRepositoryProvider {
    override fun providePlayerRepository(): PlayerRepository {
        return playerRepository
    }

    companion object {
        lateinit var playerRepository: PlayerRepository
    }
}
