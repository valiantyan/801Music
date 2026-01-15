package com.valiantyan.aidemo.ui.scan

import android.os.Build
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.commitNow
import androidx.fragment.app.add
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.valiantyan.aidemo.R
import com.valiantyan.aidemo.data.repository.AudioRepository
import com.valiantyan.aidemo.viewmodel.ScanViewModel
import com.valiantyan.aidemo.viewmodel.ScanUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
@OptIn(ExperimentalCoroutinesApi::class)
class ScanProgressFragmentTest {

    private lateinit var repository: AudioRepository
    private lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var activityController: ActivityController<FragmentActivity>
    private val mainDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup(): Unit {
        Dispatchers.setMain(mainDispatcher)
        repository = mock()
        whenever(repository.scanAudioFiles(any(), any())).thenReturn(emptyFlow())
        viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ScanViewModel(repository) as T
            }
        }
    }
    
    @After
    fun tearDown(): Unit {
        Dispatchers.resetMain()
    }

    @Test
    fun `扫描中状态应显示进度与路径`() : Unit {
        val fragment: ScanProgressFragment = launchFragment()
        idleMainLooper()
        runOnUiThread(fragment = fragment) {
            invokeUpdateUi(
                fragment = fragment,
                state = ScanUiState(
                    isScanning = true,
                    scannedCount = 5,
                    totalCount = 10,
                    currentPath = "/music",
                    error = null,
                ),
            )
        }
        val progressBar: ProgressBar = fragment.requireView().findViewById(R.id.progressBar)
        val scannedCountText: TextView = fragment.requireView().findViewById(R.id.scannedCountText)
        val currentPathText: TextView = fragment.requireView().findViewById(R.id.currentPathText)
        val expectedCount: String = fragment.getString(R.string.scanned_files_count, 5)
        val expectedPath: String = fragment.getString(R.string.current_scanning_path, "/music")
        assertFalse(progressBar.isIndeterminate)
        assertEquals(expectedCount, scannedCountText.text.toString())
        assertEquals(expectedPath, currentPathText.text.toString())
        assertEquals(View.VISIBLE, currentPathText.visibility)
    }

    @Test
    fun `取消扫描后应显示错误并禁用按钮`() : Unit {
        val fragment: ScanProgressFragment = launchFragment()
        idleMainLooper()
        runOnUiThread(fragment = fragment) {
            invokeUpdateUi(
                fragment = fragment,
                state = ScanUiState(
                    isScanning = true,
                    scannedCount = 1,
                    totalCount = null,
                    currentPath = "/music/song.mp3",
                    error = null,
                ),
            )
        }
        setViewModelState(
            fragment = fragment,
            state = ScanUiState(
                isScanning = true,
                scannedCount = 1,
                totalCount = null,
                currentPath = "/music/song.mp3",
                error = null,
            ),
        )
        val cancelButton: View = fragment.requireView().findViewById(R.id.cancelButton)
        runOnUiThread(fragment = fragment) {
            cancelButton.performClick()
        }
        val viewModel: ScanViewModel = getViewModel(fragment)
        val updatedState: ScanUiState = viewModel.uiState.value
        runOnUiThread(fragment = fragment) {
            invokeUpdateUi(
                fragment = fragment,
                state = updatedState,
            )
        }
        val errorText: TextView = fragment.requireView().findViewById(R.id.errorText)
        val expectedError: String = fragment.getString(R.string.scan_error, "扫描已取消")
        assertEquals("扫描已取消", updatedState.error)
        assertFalse(cancelButton.isEnabled)
        assertEquals(View.VISIBLE, errorText.visibility)
        assertEquals(expectedError, errorText.text.toString())
    }

    private fun launchFragment(): ScanProgressFragment {
        activityController = Robolectric.buildActivity(FragmentActivity::class.java)
        activityController.setup()
        val activity: FragmentActivity = activityController.get()
        activity.setTheme(R.style.Theme_AIDemo)
        val containerId: Int = View.generateViewId()
        val container: FrameLayout = FrameLayout(activity)
        container.id = containerId
        activity.setContentView(container)
        val fragmentFactory: FragmentFactory = TestFragmentFactory(viewModelFactory)
        activity.supportFragmentManager.fragmentFactory = fragmentFactory
        activity.supportFragmentManager.commitNow {
            add<ScanProgressFragment>(
                containerViewId = containerId,
                tag = "scan_progress",
            )
        }
        return activity.supportFragmentManager.findFragmentByTag("scan_progress") as ScanProgressFragment
    }

    private fun idleMainLooper(): Unit {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private fun invokeUpdateUi(fragment: ScanProgressFragment, state: ScanUiState): Unit {
        val method: java.lang.reflect.Method = ScanProgressFragment::class.java.getDeclaredMethod(
            "updateUI",
            ScanUiState::class.java,
        )
        method.isAccessible = true
        method.invoke(fragment, state)
    }

    private fun setViewModelState(fragment: ScanProgressFragment, state: ScanUiState): Unit {
        val viewModel: ScanViewModel = getViewModel(fragment)
        val field: java.lang.reflect.Field = ScanViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow: MutableStateFlow<ScanUiState> = field.get(viewModel) as MutableStateFlow<ScanUiState>
        stateFlow.value = state
    }

    private fun getViewModel(fragment: ScanProgressFragment): ScanViewModel {
        val field: java.lang.reflect.Field = ScanProgressFragment::class.java.getDeclaredField("viewModel")
        field.isAccessible = true
        return field.get(fragment) as ScanViewModel
    }

    private fun runOnUiThread(fragment: ScanProgressFragment, action: () -> Unit): Unit {
        val activity: FragmentActivity = fragment.requireActivity()
        activity.runOnUiThread(action)
        idleMainLooper()
    }
}

private class TestFragmentFactory(
    private val viewModelFactory: ViewModelProvider.Factory,
) : FragmentFactory() {

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        val fragment: Fragment = super.instantiate(classLoader, className)
        if (fragment is ScanProgressFragment) {
            fragment.viewModelFactoryForTest = viewModelFactory
        }
        return fragment
    }
}
