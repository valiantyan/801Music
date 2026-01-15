package com.valiantyan.music801.util

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * 权限助手
 * 
 * 封装权限请求逻辑，支持 Android 不同版本的权限模型。
 * 
 * @param activity Activity 实例
 */
class PermissionHelper(
    private val activity: ComponentActivity
) {
    /**
     * 权限请求结果回调
     */
    var onPermissionResult: ((Boolean) -> Unit)? = null

    /**
     * 权限请求启动器
     * 
     * 注意：必须在 Activity 的 onCreate 中创建 PermissionHelper 实例，
     * 因为 registerForActivityResult 必须在 onCreate 中调用。
     */
    private val permissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            onPermissionResult?.invoke(isGranted)
        }

    /**
     * 获取当前需要的权限
     * 
     * @return 权限字符串，根据 Android 版本返回不同的权限
     */
    fun getRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            // Android 12 及以下 (API < 33)
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    /**
     * 检查权限是否已授予
     * 
     * @return true 如果权限已授予，false 否则
     */
    fun hasPermission(): Boolean {
        val permission = getRequiredPermission()
        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 请求权限
     * 
     * 如果权限已授予，直接调用回调并返回 true。
     * 如果权限未授予，请求权限并返回 false。
     * 
     * @return true 如果权限已授予，false 如果正在请求权限
     */
    fun requestPermission(): Boolean {
        return if (hasPermission()) {
            // 权限已授予，直接调用回调
            onPermissionResult?.invoke(true)
            true
        } else {
            // 请求权限
            permissionLauncher.launch(getRequiredPermission())
            false
        }
    }

    /**
     * 检查是否应该显示权限说明
     * 
     * 当用户之前拒绝过权限时，应该显示说明为什么需要这个权限。
     * 
     * @return true 如果应该显示说明，false 否则
     */
    fun shouldShowRationale(): Boolean {
        val permission = getRequiredPermission()
        return activity.shouldShowRequestPermissionRationale(permission)
    }
}
