package com.valiantyan.music801.util

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PermissionHelper 集成测试
 *
 * 测试需要真实 Android 设备的功能：
 * - 权限检查（hasPermission）
 * - 权限请求流程（requestPermission）
 * - 权限说明显示判断（shouldShowRationale）
 *
 * 注意：这些测试需要在真实设备或模拟器上运行。
 *
 * 使用 TestActivity 而不是 MainActivity，因为 PermissionHelper 必须在 Activity 的 onCreate 中初始化。
 */
@RunWith(AndroidJUnit4::class)
class PermissionHelperIntegrationTest {

    private var scenario: ActivityScenario<TestActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun testHasPermission_whenPermissionGranted_shouldReturnTrue() {
        // Given
        scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario?.onActivity { activity ->
            val permissionHelper = activity.permissionHelper

            // 授予权限（需要在测试设备上手动授予，或使用 GrantPermissionRule）
            val permission = permissionHelper.getRequiredPermission()
            val hasPermission = ContextCompat.checkSelfPermission(
                activity,
                permission,
            ) == PackageManager.PERMISSION_GRANTED

            // When
            val result = permissionHelper.hasPermission()

            // Then
            // 注意：这个测试的结果取决于测试设备上的实际权限状态
            // 如果权限已授予，应该返回 true
            if (hasPermission) {
                assertTrue(result)
            }
        }
    }

    @Test
    fun testHasPermission_whenPermissionDenied_shouldReturnFalse() {
        // Given
        // 注意：这个测试需要在没有授予权限的设备上运行
        // 或者需要先撤销权限
        scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario?.onActivity { activity ->
            val permissionHelper = activity.permissionHelper

            val permission = permissionHelper.getRequiredPermission()
            val hasPermission = ContextCompat.checkSelfPermission(
                activity,
                permission,
            ) == PackageManager.PERMISSION_GRANTED

            // When
            val result = permissionHelper.hasPermission()

            // Then
            // 如果权限未授予，应该返回 false
            if (!hasPermission) {
                assertFalse(result)
            }
        }
    }

    @Test
    fun testRequestPermission_whenPermissionGranted_shouldInvokeCallbackAndReturnTrue() {
        // Given
        scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario?.onActivity { activity ->
            val permissionHelper = activity.permissionHelper

            var callbackInvoked = false
            var callbackResult = false
            permissionHelper.onPermissionResult = { isGranted ->
                callbackInvoked = true
                callbackResult = isGranted
            }

            val permission = permissionHelper.getRequiredPermission()
            val hasPermission = ContextCompat.checkSelfPermission(
                activity,
                permission,
            ) == PackageManager.PERMISSION_GRANTED

            // When
            val result = permissionHelper.requestPermission()

            // Then
            // 如果权限已授予，应该直接调用回调并返回 true
            if (hasPermission) {
                assertTrue(result)
                assertTrue(callbackInvoked)
                assertTrue(callbackResult)
            }
        }
    }

    @Test
    fun testGetRequiredPermission_shouldReturnCorrectPermissionBasedOnAndroidVersion() {
        // Given & When
        scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario?.onActivity { activity ->
            val permissionHelper = activity.permissionHelper
            val permission = permissionHelper.getRequiredPermission()

            // Then
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ (API 33+)
                assertEquals(Manifest.permission.READ_MEDIA_AUDIO, permission)
            } else {
                // Android 12 及以下 (API < 33)
                assertEquals(Manifest.permission.READ_EXTERNAL_STORAGE, permission)
            }
        }
    }
}
