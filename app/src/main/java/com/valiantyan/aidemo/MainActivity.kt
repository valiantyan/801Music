package com.valiantyan.aidemo

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.valiantyan.aidemo.util.PermissionHelper

/**
 * 主 Activity
 * 
 * 负责应用入口、权限请求和导航管理。
 */
class MainActivity : AppCompatActivity() {

    /**
     * 权限助手
     */
    private lateinit var permissionHelper: PermissionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始化权限助手
        permissionHelper = PermissionHelper(this)
        permissionHelper.onPermissionResult = { isGranted ->
            handlePermissionResult(isGranted)
        }

        // 检查并请求权限
        if (!permissionHelper.hasPermission()) {
            requestStoragePermission()
        }
    }

    /**
     * 请求存储权限
     */
    private fun requestStoragePermission() {
        if (permissionHelper.shouldShowRationale()) {
            // 显示权限说明对话框
            showPermissionRationaleDialog {
                permissionHelper.requestPermission()
            }
        } else {
            // 直接请求权限
            permissionHelper.requestPermission()
        }
    }

    /**
     * 显示权限说明对话框
     * 
     * @param onConfirm 用户确认后的回调
     */
    private fun showPermissionRationaleDialog(onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("需要存储权限")
            .setMessage("应用需要访问设备存储以扫描和播放音频文件。")
            .setPositiveButton("授予权限") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 处理权限请求结果
     * 
     * @param isGranted 权限是否被授予
     */
    private fun handlePermissionResult(isGranted: Boolean) {
        if (isGranted) {
            // 权限已授予，可以开始扫描
            // TODO: 触发扫描流程（将在后续 Task 中实现）
        } else {
            // 权限被拒绝，显示提示
            showPermissionDeniedDialog()
        }
    }

    /**
     * 显示权限被拒绝对话框
     */
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("权限被拒绝")
            .setMessage("应用需要存储权限才能扫描音频文件。请在设置中授予权限。")
            .setPositiveButton("去设置") { _, _ ->
                // TODO: 打开应用设置页面（可选功能）
            }
            .setNegativeButton("取消", null)
            .show()
    }
}