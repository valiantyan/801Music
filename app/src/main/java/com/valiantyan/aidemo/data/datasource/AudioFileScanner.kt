package com.valiantyan.aidemo.data.datasource

import com.valiantyan.aidemo.data.util.AudioFormatRecognizer
import com.valiantyan.aidemo.domain.model.ScanProgress
import com.valiantyan.aidemo.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.ArrayDeque
import java.util.logging.Level
import java.util.logging.Logger

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
     * 扫描日志记录器
     */
    private val logger: Logger = Logger.getLogger(AudioFileScanner::class.java.name)

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
        val rootDir: File = File(rootPath)
        if (!isValidDirectory(rootDir = rootDir)) {
            emit(createInvalidRootProgress())
            return@flow
        }
        emit(createStartProgress(rootPath = rootPath))
        var scannedCount: Int = 0
        scanDirectoryIterative(
            directory = rootDir,
            onSongFound = onSongFound,
            onFileScanned = { currentPath: String ->
                scannedCount += 1
                emit(createProgress(
                    scannedCount = scannedCount,
                    totalCount = null,
                    currentPath = currentPath,
                    isScanning = true,
                ))
            },
        )
        emit(createProgress(
            scannedCount = scannedCount,
            totalCount = scannedCount,
            currentPath = null,
            isScanning = false,
        ))
    }.flowOn(Dispatchers.IO)

    /**
     * 迭代扫描目录
     * 
     * @param directory 要扫描的目录
     * @param onSongFound 当找到音频文件时的回调
     * @param onFileScanned 当扫描一个文件时的回调（用于进度更新），这是一个挂起函数
     */
    private suspend fun scanDirectoryIterative(
        directory: File,
        onSongFound: (Song) -> Unit,
        onFileScanned: suspend (String) -> Unit,
    ) {
        try {
            logger.info("开始扫描目录: path=${directory.absolutePath}")
            val pendingDirectories: ArrayDeque<File> = ArrayDeque()
            pendingDirectories.add(directory)
            processDirectoryQueue(
                pendingDirectories = pendingDirectories,
                onSongFound = onSongFound,
                onFileScanned = onFileScanned,
            )
            logger.info("完成扫描目录: path=${directory.absolutePath}")
        } catch (e: SecurityException) {
            logger.log(Level.SEVERE, "扫描目录权限不足: path=${directory.absolutePath}", e)
            throw IllegalStateException("扫描目录权限不足: ${directory.absolutePath}", e)
        }
    }

    private suspend fun processDirectoryQueue(
        pendingDirectories: ArrayDeque<File>,
        onSongFound: (Song) -> Unit,
        onFileScanned: suspend (String) -> Unit,
    ) {
        while (pendingDirectories.isNotEmpty()) {
            val currentDirectory: File = pendingDirectories.removeLast()
            handleDirectory(
                currentDirectory = currentDirectory,
                pendingDirectories = pendingDirectories,
                onSongFound = onSongFound,
                onFileScanned = onFileScanned,
            )
        }
    }

    private suspend fun handleDirectory(
        currentDirectory: File,
        pendingDirectories: ArrayDeque<File>,
        onSongFound: (Song) -> Unit,
        onFileScanned: suspend (String) -> Unit,
    ) {
        val files: Array<File> = currentDirectory.listFiles() ?: return
        for (file: File in files) {
            if (shouldSkip(file = file)) {
                continue
            }
            if (file.isDirectory) {
                pendingDirectories.add(file)
            } else if (file.isFile) {
                processFile(
                    file = file,
                    onSongFound = onSongFound,
                    onFileScanned = onFileScanned,
                )
            }
        }
    }

    private fun isValidDirectory(rootDir: File): Boolean {
        return rootDir.exists() && rootDir.isDirectory
    }

    private fun shouldSkip(file: File): Boolean {
        return file.name.startsWith(".")
    }

    private suspend fun processFile(
        file: File,
        onSongFound: (Song) -> Unit,
        onFileScanned: suspend (String) -> Unit,
    ) {
        if (!AudioFormatRecognizer.isAudioFile(file.absolutePath)) {
            return
        }
        val song: Song? = extractSong(file = file)
        if (song == null) {
            return
        }
        onSongFound(song)
        onFileScanned(file.absolutePath)
    }

    private fun extractSong(file: File): Song? {
        return metadataExtractor.extractMetadata(
            filePath = file.absolutePath,
            fileSize = file.length(),
            dateAdded = file.lastModified(),
        )
    }

    private fun createStartProgress(rootPath: String): ScanProgress {
        return createProgress(
            scannedCount = 0,
            totalCount = null,
            currentPath = rootPath,
            isScanning = true,
        )
    }

    private fun createInvalidRootProgress(): ScanProgress {
        return createProgress(
            scannedCount = 0,
            totalCount = null,
            currentPath = null,
            isScanning = false,
        )
    }

    private fun createProgress(
        scannedCount: Int,
        totalCount: Int?,
        currentPath: String?,
        isScanning: Boolean,
    ): ScanProgress {
        return ScanProgress(
            scannedCount = scannedCount,
            totalCount = totalCount,
            currentPath = currentPath,
            isScanning = isScanning,
        )
    }
}
