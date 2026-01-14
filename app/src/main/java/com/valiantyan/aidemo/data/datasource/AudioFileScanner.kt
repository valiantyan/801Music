package com.valiantyan.aidemo.data.datasource

import com.valiantyan.aidemo.data.util.AudioFormatRecognizer
import com.valiantyan.aidemo.domain.model.ScanProgress
import com.valiantyan.aidemo.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * 音频文件扫描器
 * 
 * 负责扫描设备存储中的音频文件，支持递归扫描子目录。
 * 使用 Kotlin Coroutines 在后台线程执行，通过 Flow 发送扫描进度更新。
 * 
 * @param metadataExtractor 元数据提取器，用于从音频文件中提取元数据
 */
class AudioFileScanner(
    private val metadataExtractor: MediaMetadataExtractor
) {

    /**
     * 扫描指定目录中的音频文件
     * 
     * @param rootPath 要扫描的根目录路径
     * @param onSongFound 当找到音频文件时的回调函数
     * @return Flow<ScanProgress> 扫描进度更新流
     */
    fun scanDirectory(
        rootPath: String,
        onSongFound: (Song) -> Unit = {}
    ): Flow<ScanProgress> = flow {
        val rootDir = File(rootPath)
        
        // 验证目录是否存在
        if (!rootDir.exists() || !rootDir.isDirectory) {
            emit(ScanProgress(
                scannedCount = 0,
                totalCount = null,
                currentPath = null,
                isScanning = false
            ))
            return@flow
        }

        // 发送开始扫描的进度
        emit(ScanProgress(
            scannedCount = 0,
            totalCount = null,
            currentPath = rootPath,
            isScanning = true
        ))

        var scannedCount = 0

        // 递归扫描目录
        scanDirectoryRecursive(rootDir, onSongFound) { currentPath ->
            scannedCount++
            emit(ScanProgress(
                scannedCount = scannedCount,
                totalCount = null,
                currentPath = currentPath,
                isScanning = true
            ))
        }

        // 发送完成扫描的进度
        emit(ScanProgress(
            scannedCount = scannedCount,
            totalCount = scannedCount,
            currentPath = null,
            isScanning = false
        ))
    }.flowOn(Dispatchers.IO)

    /**
     * 递归扫描目录
     * 
     * @param directory 要扫描的目录
     * @param onSongFound 当找到音频文件时的回调
     * @param onFileScanned 当扫描一个文件时的回调（用于进度更新），这是一个挂起函数
     */
    private suspend fun scanDirectoryRecursive(
        directory: File,
        onSongFound: (Song) -> Unit,
        onFileScanned: suspend (String) -> Unit
    ) {
        try {
            val files = directory.listFiles() ?: return

            for (file in files) {
                // 跳过隐藏文件和系统文件
                if (file.name.startsWith(".")) {
                    continue
                }

                if (file.isDirectory) {
                    // 递归扫描子目录
                    scanDirectoryRecursive(file, onSongFound, onFileScanned)
                } else if (file.isFile) {
                    // 检查是否为音频文件
                    if (AudioFormatRecognizer.isAudioFile(file.absolutePath)) {
                        // 提取元数据
                        val song = metadataExtractor.extractMetadata(
                            filePath = file.absolutePath,
                            fileSize = file.length(),
                            dateAdded = file.lastModified()
                        )

                        // 如果元数据提取成功，调用回调
                        song?.let {
                            onSongFound(it)
                            onFileScanned(file.absolutePath)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 如果扫描过程中出现错误（如权限问题），记录日志并继续
            // 在实际使用中，可以记录日志
            // Log.e("AudioFileScanner", "Error scanning directory: ${directory.absolutePath}", e)
        }
    }
}
