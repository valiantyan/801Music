package com.valiantyan.music801.data.repository

import android.os.Environment
import com.valiantyan.music801.data.datasource.AudioFileScanner
import com.valiantyan.music801.data.local.dao.LibrarySyncStateDao
import com.valiantyan.music801.data.local.dao.SongDao
import com.valiantyan.music801.data.local.entity.LibrarySyncStateEntity
import com.valiantyan.music801.data.local.entity.ScanStatus
import com.valiantyan.music801.data.local.entity.SongEntity
import com.valiantyan.music801.data.local.mapper.toDomain
import com.valiantyan.music801.data.local.mapper.toEntity
import com.valiantyan.music801.domain.model.InitialScanDecision
import com.valiantyan.music801.domain.model.ScanMode
import com.valiantyan.music801.domain.model.ScanProgress
import com.valiantyan.music801.domain.model.Song
import java.util.logging.Level
import java.util.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 音频数据仓库
 *
 * 封装音频文件扫描逻辑，提供统一的数据访问接口。
 * 管理歌曲数据真源（Room），暴露 Flow<List<Song>> 供其他模块订阅。
 *
 * @param audioFileScanner 音频文件扫描器
 * @param songDao 歌曲数据访问对象
 * @param librarySyncStateDao 扫描同步状态数据访问对象
 * @param ioDispatcher IO 调度器
 * @param rootPathProvider 扫描根目录提供者
 * @param nowProvider 当前时间提供者
 */
class AudioRepository(
    private val audioFileScanner: AudioFileScanner,
    private val songDao: SongDao,
    private val librarySyncStateDao: LibrarySyncStateDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val rootPathProvider: () -> String = { Environment.getExternalStorageDirectory().absolutePath },
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
) {
    /**
     * 扫描日志记录器
     */
    private val logger: Logger = Logger.getLogger(AudioRepository::class.java.name)

    /**
     * 订阅歌曲列表
     */
    fun observeSongs(): Flow<List<Song>> {
        return songDao.observeSongs()
            .map { entities: List<SongEntity> ->
                entities.map { entity: SongEntity ->
                    entity.toDomain()
                }
            }
    }

    /**
     * 判断是否需要执行首扫
     */
    suspend fun ensureInitialScanIfNeeded(): InitialScanDecision {
        return withContext(ioDispatcher) {
            val count: Int = songDao.countSongs()
            if (count == 0) {
                InitialScanDecision.RUN_INITIAL_SCAN
            } else {
                InitialScanDecision.SKIP_ALREADY_HAS_DATA
            }
        }
    }

    /**
     * 扫描指定目录中的音频文件
     *
     * @param rootPath 要扫描的根目录路径
     * @param onSongFound 当找到音频文件时的回调函数（可选）
     * @return Flow<ScanProgress> 扫描进度更新流
     */
    fun scanAudioFiles(
        rootPath: String,
        onSongFound: (Song) -> Unit = {},
    ): Flow<ScanProgress> {
        return scanAndSyncInternal(
            rootPaths = listOf(rootPath),
            scanMode = ScanMode.FULL_INITIAL,
            onSongFound = onSongFound,
        )
    }

    /**
     * 执行扫描并同步到数据库
     */
    fun scanAndSync(scanMode: ScanMode): Flow<ScanProgress> {
        return scanAndSync(
            scanMode = scanMode,
            selectedDirectories = listOf(resolveRootPath()),
        )
    }

    /**
     * 按指定目录执行扫描并同步到数据库
     *
     * @param scanMode 扫描模式
     * @param selectedDirectories 待扫描目录
     * @return 扫描进度流
     */
    fun scanAndSync(
        scanMode: ScanMode,
        selectedDirectories: List<String>,
    ): Flow<ScanProgress> {
        return scanAndSyncInternal(
            rootPaths = selectedDirectories,
            scanMode = scanMode,
            onSongFound = {},
        )
    }

    /**
     * 执行扫描并写入数据库
     */
    private fun scanAndSyncInternal(
        rootPaths: List<String>,
        scanMode: ScanMode,
        onSongFound: (Song) -> Unit,
    ): Flow<ScanProgress> {
        return flow {
            val scannedAt: Long = nowProvider()
            val pendingSongs: MutableList<SongEntity> = mutableListOf()
            val sanitizedRootPaths: List<String> = rootPaths.filter { path: String ->
                path.isNotBlank()
            }
            updateSyncState(
                status = ScanStatus.RUNNING,
                scanMode = scanMode,
                scannedAt = scannedAt,
                lastError = null,
            )
            try {
                logger.info("开始扫描入库: mode=$scanMode rootPaths=$sanitizedRootPaths")
                scanSelectedDirectories(
                    rootPaths = sanitizedRootPaths,
                    scannedAt = scannedAt,
                    pendingSongs = pendingSongs,
                    canFlushDuringScan = canFlushDuringScan(scanMode = scanMode),
                    onSongFound = onSongFound,
                    onProgress = { progress: ScanProgress ->
                        emit(progress)
                    },
                )
                flushRemaining(pendingSongs = pendingSongs)
                updateSyncState(
                    status = ScanStatus.SUCCESS,
                    scanMode = scanMode,
                    scannedAt = scannedAt,
                    lastError = null,
                )
                logger.info("扫描入库完成: mode=$scanMode rootPaths=$sanitizedRootPaths")
            } catch (e: CancellationException) {
                updateSyncState(
                    status = ScanStatus.CANCELED,
                    scanMode = scanMode,
                    scannedAt = scannedAt,
                    lastError = "扫描已取消",
                )
                logger.log(Level.WARNING, "扫描被取消: rootPaths=$sanitizedRootPaths", e)
                throw e
            } catch (e: IllegalStateException) {
                updateSyncState(
                    status = ScanStatus.FAILED,
                    scanMode = scanMode,
                    scannedAt = scannedAt,
                    lastError = e.message,
                )
                logger.log(Level.SEVERE, "扫描失败: rootPaths=$sanitizedRootPaths", e)
                throw IllegalStateException("扫描失败: rootPaths=$sanitizedRootPaths", e)
            } catch (e: Exception) {
                updateSyncState(
                    status = ScanStatus.FAILED,
                    scanMode = scanMode,
                    scannedAt = scannedAt,
                    lastError = e.message,
                )
                logger.log(Level.SEVERE, "扫描失败: rootPaths=$sanitizedRootPaths", e)
                throw IllegalStateException("扫描失败: rootPaths=$sanitizedRootPaths", e)
            }
        }.flowOn(ioDispatcher)
    }

    /**
     * 顺序扫描目录并汇总进度
     *
     * @param rootPaths 待扫描目录
     * @param scannedAt 本轮扫描时间戳
     * @param pendingSongs 待写入歌曲缓存
     * @param canFlushDuringScan 是否允许扫描中批量落库
     * @param onSongFound 歌曲回调
     * @param onProgress 进度回调
     */
    private suspend fun scanSelectedDirectories(
        rootPaths: List<String>,
        scannedAt: Long,
        pendingSongs: MutableList<SongEntity>,
        canFlushDuringScan: Boolean,
        onSongFound: (Song) -> Unit,
        onProgress: suspend (ScanProgress) -> Unit,
    ) {
        if (rootPaths.isEmpty()) {
            onProgress(
                ScanProgress(
                    scannedCount = 0,
                    totalCount = 0,
                    currentPath = null,
                    isScanning = false,
                ),
            )
            return
        }
        var scannedCountTotal: Int = 0
        rootPaths.forEachIndexed { index: Int, rootPath: String ->
            val isLastDirectory: Boolean = index == rootPaths.lastIndex
            val baseScannedCount: Int = scannedCountTotal
            var currentDirectoryScannedCount: Int = 0
            audioFileScanner.scanDirectory(
                rootPath = rootPath,
                onSongFound = { song: Song ->
                    onSongFound(song)
                    pendingSongs.add(
                        song.toEntity(
                            modifiedAt = song.dateAdded,
                            scannedAt = scannedAt,
                        ),
                    )
                },
            ).collect { progress: ScanProgress ->
                currentDirectoryScannedCount = progress.scannedCount
                if (canFlushDuringScan) {
                    flushIfNeeded(pendingSongs = pendingSongs)
                }
                onProgress(
                    mergeProgress(
                        baseScannedCount = baseScannedCount,
                        directoryProgress = progress,
                        isLastDirectory = isLastDirectory,
                    ),
                )
            }
            scannedCountTotal = baseScannedCount + currentDirectoryScannedCount
        }
    }

    /**
     * 将单目录进度映射为多目录汇总进度
     *
     * @param baseScannedCount 该目录开始前已扫描数量
     * @param directoryProgress 当前目录扫描进度
     * @param isLastDirectory 是否为最后一个目录
     * @return 汇总进度
     */
    private fun mergeProgress(
        baseScannedCount: Int,
        directoryProgress: ScanProgress,
        isLastDirectory: Boolean,
    ): ScanProgress {
        val aggregatedCount: Int = baseScannedCount + directoryProgress.scannedCount
        if (directoryProgress.isScanning) {
            return directoryProgress.copy(
                scannedCount = aggregatedCount,
                totalCount = null,
                isScanning = true,
            )
        }
        if (directoryProgress.totalCount == null) {
            return ScanProgress(
                scannedCount = aggregatedCount,
                totalCount = null,
                currentPath = null,
                isScanning = !isLastDirectory,
            )
        }
        if (!isLastDirectory) {
            return ScanProgress(
                scannedCount = aggregatedCount,
                totalCount = null,
                currentPath = null,
                isScanning = true,
            )
        }
        return ScanProgress(
            scannedCount = aggregatedCount,
            totalCount = aggregatedCount,
            currentPath = null,
            isScanning = false,
        )
    }

    /**
     * 根据扫描模式更新同步状态
     */
    private suspend fun updateSyncState(
        status: ScanStatus,
        scanMode: ScanMode,
        scannedAt: Long,
        lastError: String?,
    ): Unit {
        val existing: LibrarySyncStateEntity? = librarySyncStateDao.findById(id = SINGLE_ROW_ID)
        val baseState: LibrarySyncStateEntity = existing ?: createEmptySyncState()
        val lastFullScanAt: Long? = resolveLastFullScanAt(
            scanMode = scanMode,
            status = status,
            scannedAt = scannedAt,
            previous = baseState.lastFullScanAt,
        )
        val updated: LibrarySyncStateEntity = baseState.copy(
            lastScanAt = scannedAt,
            lastFullScanAt = lastFullScanAt,
            lastScanStatus = status,
            lastError = lastError,
        )
        librarySyncStateDao.upsert(state = updated)
    }

    /**
     * 创建空的同步状态
     */
    private fun createEmptySyncState(): LibrarySyncStateEntity {
        return LibrarySyncStateEntity(
            id = SINGLE_ROW_ID,
            lastScanAt = null,
            lastFullScanAt = null,
            lastSyncToken = null,
            lastScanStatus = null,
            lastError = null,
        )
    }

    /**
     * 计算全量扫描时间
     */
    private fun resolveLastFullScanAt(
        scanMode: ScanMode,
        status: ScanStatus,
        scannedAt: Long,
        previous: Long?,
    ): Long? {
        if (!isFullScanMode(scanMode = scanMode)) {
            return previous
        }
        if (status == ScanStatus.SUCCESS) {
            return scannedAt
        }
        return previous
    }

    /**
     * 判断是否为全量扫描模式
     */
    private fun isFullScanMode(scanMode: ScanMode): Boolean {
        return scanMode == ScanMode.FULL_INITIAL || scanMode == ScanMode.MANUAL_FULL
    }

    /**
     * 判断扫描中是否允许提前落库
     */
    private fun canFlushDuringScan(scanMode: ScanMode): Boolean {
        return scanMode == ScanMode.FULL_INITIAL
    }

    /**
     * 按批量写入歌曲
     */
    private suspend fun flushIfNeeded(pendingSongs: MutableList<SongEntity>): Unit {
        if (pendingSongs.size < UPSERT_BATCH_SIZE) {
            return
        }
        val batch: List<SongEntity> = pendingSongs.toList()
        pendingSongs.clear()
        songDao.upsertAll(songs = batch)
    }

    /**
     * 写入剩余歌曲
     */
    private suspend fun flushRemaining(pendingSongs: MutableList<SongEntity>): Unit {
        if (pendingSongs.isEmpty()) {
            return
        }
        val batch: List<SongEntity> = pendingSongs.toList()
        pendingSongs.clear()
        songDao.upsertAll(songs = batch)
    }

    /**
     * 获取默认扫描根目录
     */
    private fun resolveRootPath(): String {
        val provided: String = rootPathProvider()
        if (provided.isNotBlank()) {
            return provided
        }
        return FALLBACK_ROOT_PATH
    }

    private companion object {
        /**
         * 同步状态单行主键
         */
        private const val SINGLE_ROW_ID: Int = 1

        /**
         * 批量写入大小
         */
        private const val UPSERT_BATCH_SIZE: Int = 200

        /**
         * 根目录默认兜底路径
         */
        private const val FALLBACK_ROOT_PATH: String = "/storage/emulated/0"
    }
}
