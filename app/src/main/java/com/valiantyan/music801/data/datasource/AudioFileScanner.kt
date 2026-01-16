package com.valiantyan.music801.data.datasource

import com.valiantyan.music801.data.util.AudioFormatRecognizer
import com.valiantyan.music801.domain.model.ScanProgress
import com.valiantyan.music801.domain.model.Song
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
    private val metadataExtractor: MediaMetadataExtractor,
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
        onSongFound: (Song) -> Unit = {},
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
                emit(
                    createProgress(
                        scannedCount = scannedCount,
                        totalCount = null,
                        currentPath = currentPath,
                        isScanning = true,
                    ),
                )
            },
        )
        emit(
            createProgress(
                scannedCount = scannedCount,
                totalCount = scannedCount,
                currentPath = null,
                isScanning = false,
            ),
        )
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

    /**
     * 处理待扫描目录队列
     *
     * @param pendingDirectories 待扫描目录队列
     * @param onSongFound 找到歌曲后的回调
     * @param onFileScanned 单文件扫描回调
     */
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

    /**
     * 处理单个目录中的文件与子目录
     *
     * @param currentDirectory 当前目录
     * @param pendingDirectories 待扫描目录队列
     * @param onSongFound 找到歌曲后的回调
     * @param onFileScanned 单文件扫描回调
     */
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

    /**
     * 判断目录是否可扫描
     *
     * @param rootDir 根目录
     * @return 是否为有效目录
     */
    private fun isValidDirectory(rootDir: File): Boolean {
        return rootDir.exists() && rootDir.isDirectory
    }

    /**
     * 判断是否跳过隐藏文件
     *
     * @param file 目标文件
     * @return 是否需要跳过
     */
    private fun shouldSkip(file: File): Boolean {
        return file.name.startsWith(".")
    }

    /**
     * 处理单个文件的扫描逻辑
     *
     * @param file 目标文件
     * @param onSongFound 找到歌曲后的回调
     * @param onFileScanned 单文件扫描回调
     */
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

    /**
     * 从文件中提取 [Song]
     *
     * @param file 音频文件
     * @return 提取到的 [Song]，失败时返回 null
     */
    private fun extractSong(file: File): Song? {
        return metadataExtractor.extractMetadata(
            filePath = file.absolutePath,
            fileSize = file.length(),
            dateAdded = file.lastModified(),
        )
    }

    /**
     * 创建扫描开始进度
     *
     * @param rootPath 根目录路径
     * @return 扫描进度
     */
    private fun createStartProgress(rootPath: String): ScanProgress {
        return createProgress(
            scannedCount = 0,
            totalCount = null,
            currentPath = rootPath,
            isScanning = true,
        )
    }

    /**
     * 创建无效目录的进度
     *
     * @return 扫描进度
     */
    private fun createInvalidRootProgress(): ScanProgress {
        return createProgress(
            scannedCount = 0,
            totalCount = null,
            currentPath = null,
            isScanning = false,
        )
    }

    /**
     * 创建通用扫描进度
     *
     * @param scannedCount 已扫描数量
     * @param totalCount 总数量
     * @param currentPath 当前路径
     * @param isScanning 是否扫描中
     * @return 扫描进度
     */
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
