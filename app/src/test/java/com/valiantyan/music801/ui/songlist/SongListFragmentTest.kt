package com.valiantyan.music801.ui.songlist

import android.os.Build
import android.os.Looper
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
import com.valiantyan.music801.R
import com.valiantyan.music801.data.repository.AudioRepository
import com.valiantyan.music801.viewmodel.SongListUiState
import com.valiantyan.music801.viewmodel.SongListViewModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
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

    @Before
    fun setup() {
        repository = mock()
        whenever(repository.getAllSongs()).thenReturn(flowOf(emptyList()))
        viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SongListViewModel(audioRepository = repository) as T
            }
        }
    }

    @Test
    fun `更新空状态时显示空提示`() : Unit {
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
    fun `更新加载状态时显示加载指示`() : Unit {
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

    private fun launchFragment(): SongListFragment {
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
            add<SongListFragment>(
                containerViewId = containerId,
                tag = "song_list",
            )
        }
        return activity.supportFragmentManager.findFragmentByTag("song_list") as SongListFragment
    }

    private fun idleMainLooper(): Unit {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private fun invokeUpdateUi(fragment: SongListFragment, state: SongListUiState): Unit {
        val method: java.lang.reflect.Method = SongListFragment::class.java.getDeclaredMethod(
            "updateUI",
            SongListUiState::class.java,
        )
        method.isAccessible = true
        method.invoke(fragment, state)
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
