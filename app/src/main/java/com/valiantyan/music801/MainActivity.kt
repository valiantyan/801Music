package com.valiantyan.music801

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.valiantyan.music801.data.datasource.AudioFileScanner
import com.valiantyan.music801.data.datasource.MediaMetadataExtractor
import com.valiantyan.music801.data.repository.AudioRepository
import com.valiantyan.music801.data.repository.PlayerRepository
import com.valiantyan.music801.di.AudioRepositoryProvider
import com.valiantyan.music801.di.PlayerControllerHolder
import com.valiantyan.music801.di.PlayerControllerProvider
import com.valiantyan.music801.di.PlayerRepositoryHolder
import com.valiantyan.music801.di.PlayerRepositoryProvider
import com.valiantyan.music801.player.PlayerController
import com.valiantyan.music801.util.PermissionHelper

/**
 * 主 Activity
 *
 * 负责应用入口、权限请求和导航管理。
 */
class MainActivity : AppCompatActivity(), AudioRepositoryProvider, PlayerRepositoryProvider, PlayerControllerProvider {

    /**
     * 权限助手
     */
    private lateinit var permissionHelper: PermissionHelper

    /**
     * 音频仓库
     */
    private lateinit var audioRepository: AudioRepository

    /**
     * 导航控制器
     */
    private lateinit var navController: NavController

    /**
     * 播放控制器
     */
    private lateinit var playerController: PlayerController

    /**
     * 初始化入口页面与权限检查
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        permissionHelper = PermissionHelper(this)
        permissionHelper.onPermissionResult = { isGranted ->
            handlePermissionResult(isGranted = isGranted)
        }
        permissionHelper.onNotificationPermissionResult = { isGranted ->
            handleNotificationPermissionResult(isGranted = isGranted)
        }
        audioRepository = createAudioRepository()
        playerController = createPlayerController()
        navController = getNavController()
        if (!permissionHelper.hasPermission()) {
            requestStoragePermission()
            return
        }
        setNavGraphIfNeeded()
        requestNotificationPermissionIfNeeded()
    }

    /**
     * 请求存储权限
     */
    private fun requestStoragePermission() {
        if (permissionHelper.shouldShowRationale()) {
            showPermissionRationaleDialog {
                permissionHelper.requestPermission()
            }
        } else {
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
            setNavGraphIfNeeded()
            requestNotificationPermissionIfNeeded()
        } else {
            showPermissionDeniedDialog()
        }
    }
    /**
     * 请求通知权限（Android 13+），避免播放通知被系统屏蔽
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (permissionHelper.hasNotificationPermission()) {
            return
        }
        if (permissionHelper.shouldShowNotificationRationale()) {
            showNotificationPermissionRationaleDialog {
                permissionHelper.requestNotificationPermission()
            }
            return
        }
        permissionHelper.requestNotificationPermission()
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
    /**
     * 处理通知权限请求结果，避免反复请求影响体验
     *
     * @param isGranted 权限是否被授予
     */
    private fun handleNotificationPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            return
        }
        showNotificationPermissionDeniedDialog()
    }
    /**
     * 显示通知权限说明对话框，解释通知用途
     *
     * @param onConfirm 用户确认后的回调
     */
    private fun showNotificationPermissionRationaleDialog(onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("需要通知权限")
            .setMessage("应用需要通知权限以展示播放控制通知。")
            .setPositiveButton("授予权限") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    /**
     * 显示通知权限被拒绝对话框，提醒用户可在设置中开启
     */
    private fun showNotificationPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("通知权限被拒绝")
            .setMessage("未授予通知权限将无法显示播放控制通知，可在设置中开启。")
            .setPositiveButton("我知道了", null)
            .show()
    }

    /**
     * 暴露统一的 [AudioRepository] 供页面共享
     */
    override fun provideAudioRepository(): AudioRepository {
        return audioRepository
    }

    /**
     * 暴露统一的 [PlayerRepository] 供页面共享
     */
    override fun providePlayerRepository(): PlayerRepository {
        return PlayerRepositoryHolder.getOrCreate(context = this)
    }

    /**
     * 暴露统一的 [PlayerController] 供页面共享
     */
    override fun providePlayerController(): PlayerController {
        return playerController
    }

    /**
     * 创建音频仓库实例
     */
    private fun createAudioRepository(): AudioRepository {
        val metadataExtractor: MediaMetadataExtractor = MediaMetadataExtractor()
        val audioFileScanner: AudioFileScanner = AudioFileScanner(metadataExtractor = metadataExtractor)
        return AudioRepository(audioFileScanner = audioFileScanner)
    }

    /**
     * 创建播放器仓库实例
     */
    private fun createPlayerController(): PlayerController {
        return PlayerControllerHolder.getOrCreate(context = this)
    }

    /**
     * 获取导航控制器实例
     */
    private fun getNavController(): NavController {
        val navHostFragment: NavHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        return navHostFragment.navController
    }

    /**
     * 初始化导航图，避免重复设置
     */
    private fun setNavGraphIfNeeded() {
        if (navController.currentDestination != null) {
            return
        }
        navController.setGraph(R.navigation.nav_graph)
    }

    /**
     * Activity 销毁时释放播放器资源
     */
    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            PlayerControllerHolder.clear()
        }
    }
}
