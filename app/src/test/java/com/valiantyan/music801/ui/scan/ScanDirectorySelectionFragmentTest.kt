package com.valiantyan.music801.ui.scan

import android.os.Build
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.add
import androidx.fragment.app.commitNow
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import com.valiantyan.music801.R
import com.valiantyan.music801.domain.model.ScanMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class ScanDirectorySelectionFragmentTest {
    @Test
    fun `未选择目录时确认按钮不可点击`() {
        val fragment: ScanDirectorySelectionFragment = launchFragment()
        idleMainLooper()
        val confirmButton: Button = fragment.requireView().findViewById(R.id.confirmScanButton)
        val selectedCountText: TextView = fragment.requireView().findViewById(R.id.selectedCountText)
        assertFalse(confirmButton.isEnabled)
        assertEquals("已选目录：0", selectedCountText.text.toString())
    }

    @Test
    fun `点击全选后应更新已选数量并启用确认按钮`() {
        val fragment: ScanDirectorySelectionFragment = launchFragment()
        idleMainLooper()
        val selectAllButton: Button = fragment.requireView().findViewById(R.id.selectAllButton)
        val confirmButton: Button = fragment.requireView().findViewById(R.id.confirmScanButton)
        val selectedCountText: TextView = fragment.requireView().findViewById(R.id.selectedCountText)
        selectAllButton.performClick()
        idleMainLooper()
        assertTrue(confirmButton.isEnabled)
        assertEquals("已选目录：2", selectedCountText.text.toString())
    }

    @Test
    fun `确认后应携带手动扫描参数跳转到扫描页`() {
        val fragment: ScanDirectorySelectionFragment = launchFragment()
        val navController: TestNavHostController = createNavController(fragment = fragment)
        Navigation.setViewNavController(fragment.requireView(), navController)
        idleMainLooper()
        val selectAllButton: Button = fragment.requireView().findViewById(R.id.selectAllButton)
        val confirmButton: Button = fragment.requireView().findViewById(R.id.confirmScanButton)
        selectAllButton.performClick()
        confirmButton.performClick()
        val args = navController.currentBackStackEntry?.arguments
        val actualMode: ScanMode = ScanNavigationArgs.parseScanMode(args)
        val actualDirectories: List<String> = ScanNavigationArgs.parseSelectedDirectories(args)
        assertEquals(R.id.scanProgressFragment, navController.currentDestination?.id)
        assertEquals(ScanMode.MANUAL_FULL, actualMode)
        assertEquals(
            listOf(
                "/storage/emulated/0/Music",
                "/storage/emulated/0/Podcasts",
            ),
            actualDirectories,
        )
    }

    private fun launchFragment(): ScanDirectorySelectionFragment {
        val activityController = Robolectric.buildActivity(FragmentActivity::class.java)
        activityController.setup()
        val activity: FragmentActivity = activityController.get()
        activity.setTheme(R.style.Theme_music801)
        val containerId: Int = View.generateViewId()
        val container: FrameLayout = FrameLayout(activity)
        container.id = containerId
        activity.setContentView(container)
        activity.supportFragmentManager.fragmentFactory = DirectorySelectionTestFragmentFactory()
        activity.supportFragmentManager.commitNow {
            add<ScanDirectorySelectionFragment>(
                containerViewId = containerId,
                tag = "scan_directory_selection",
            )
        }
        return activity.supportFragmentManager.findFragmentByTag(
            "scan_directory_selection",
        ) as ScanDirectorySelectionFragment
    }

    private fun createNavController(fragment: ScanDirectorySelectionFragment): TestNavHostController {
        val navController = TestNavHostController(fragment.requireContext())
        navController.setGraph(R.navigation.nav_graph)
        navController.setCurrentDestination(R.id.scanDirectorySelectionFragment)
        return navController
    }

    private fun idleMainLooper() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }
}

private class DirectorySelectionTestFragmentFactory : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        val fragment: Fragment = super.instantiate(classLoader, className)
        if (fragment is ScanDirectorySelectionFragment) {
            fragment.directoryProviderForTest = {
                listOf(
                    "/storage/emulated/0/Music",
                    "/storage/emulated/0/Podcasts",
                )
            }
        }
        return fragment
    }
}
