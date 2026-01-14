package com.valiantyan.aidemo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valiantyan.aidemo.data.repository.AudioRepository
import com.valiantyan.aidemo.domain.model.ScanProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 扫描 ViewModel
 * 
 * 管理音频文件扫描的状态和逻辑，协调 UI 和 Repository 之间的交互。
 * 使用 StateFlow 管理 UI 状态，支持配置变更后状态恢复。
 * 
 * @param audioRepository 音频数据仓库
 */
class ScanViewModel(
    private val audioRepository: AudioRepository
) : ViewModel() {

    /**
     * UI 状态（可变）
     */
    private val _uiState = MutableStateFlow(ScanUiState())

    /**
     * UI 状态（只读，供 UI 订阅）
     */
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    /**
     * 当前扫描任务（用于取消）
     */
    private var scanJob: Job? = null

    /**
     * 开始扫描音频文件
     * 
     * @param rootPath 要扫描的根目录路径
     */
    fun startScan(rootPath: String) {
        // 如果已有扫描任务在运行，先取消
        cancelScan()

        // 重置状态
        _uiState.value = ScanUiState()

        // 启动新的扫描任务
        scanJob = viewModelScope.launch {
            audioRepository.scanAudioFiles(rootPath) { song ->
                // 当找到歌曲时的回调（可以用于其他用途，如通知其他模块）
                // 当前实现中，歌曲已经通过 Repository 的 Flow 自动更新
            }
                .catch { exception ->
                    // 处理扫描过程中的错误
                    _uiState.update { currentState ->
                        currentState.copy(
                            isScanning = false,
                            error = exception.message ?: "扫描过程中发生未知错误"
                        )
                    }
                }
                .collect { progress ->
                    // 更新 UI 状态
                    _uiState.update { currentState ->
                        currentState.copy(
                            isScanning = progress.isScanning,
                            scannedCount = progress.scannedCount,
                            totalCount = progress.totalCount,
                            currentPath = progress.currentPath,
                            error = null // 清除之前的错误（如果有）
                        )
                    }
                }
        }
    }

    /**
     * 取消扫描
     */
    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        
        // 更新状态为已取消（但保留当前进度）
        _uiState.update { currentState ->
            if (currentState.isScanning) {
                currentState.copy(
                    isScanning = false,
                    error = "扫描已取消"
                )
            } else {
                currentState
            }
        }
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.update { currentState ->
            currentState.copy(error = null)
        }
    }
}
