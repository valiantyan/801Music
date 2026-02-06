package com.valiantyan.music801.ui.scan

import android.os.Bundle
import com.valiantyan.music801.domain.model.ScanMode

/**
 * 扫描流程导航参数
 *
 * 统一管理目录选择页与扫描页之间的参数传递，避免键名不一致。
 */
object ScanNavigationArgs {
    /**
     * 扫描模式参数键
     */
    private const val KEY_SCAN_MODE: String = "scan_mode"

    /**
     * 扫描目录参数键
     */
    private const val KEY_SELECTED_DIRECTORIES: String = "selected_directories"

    /**
     * 构建扫描参数
     *
     * @param scanMode 扫描模式
     * @param selectedDirectories 用户选择的目录列表
     * @return 导航参数
     */
    fun createBundle(
        scanMode: ScanMode,
        selectedDirectories: List<String>,
    ): Bundle {
        val args: Bundle = Bundle()
        args.putString(KEY_SCAN_MODE, scanMode.name)
        args.putStringArrayList(
            KEY_SELECTED_DIRECTORIES,
            ArrayList(selectedDirectories),
        )
        return args
    }

    /**
     * 解析扫描模式
     *
     * @param args 导航参数
     * @return 扫描模式，异常时兜底 [ScanMode.MANUAL_FULL]
     */
    fun parseScanMode(args: Bundle?): ScanMode {
        val rawValue: String = args?.getString(KEY_SCAN_MODE) ?: return ScanMode.MANUAL_FULL
        return ScanMode.entries.firstOrNull { mode: ScanMode ->
            mode.name == rawValue
        } ?: ScanMode.MANUAL_FULL
    }

    /**
     * 解析选中目录
     *
     * @param args 导航参数
     * @return 目录列表
     */
    fun parseSelectedDirectories(args: Bundle?): List<String> {
        val values: ArrayList<String> = args?.getStringArrayList(KEY_SELECTED_DIRECTORIES) ?: arrayListOf()
        return values.filter { path: String ->
            path.isNotBlank()
        }
    }
}
