package com.valiantyan.music801.data.repository

import com.valiantyan.music801.data.datasource.AudioFileScanner
import com.valiantyan.music801.domain.model.ScanProgress
import com.valiantyan.music801.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 音频数据仓库
 * 
 * 封装音频文件扫描逻辑，提供统一的数据访问接口。
 * 管理扫描结果缓存（内存中），暴露 Flow<List<Song>> 供其他模块订阅。
 * 
 * @param audioFileScanner 音频文件扫描器
 */
class AudioRepository(
    private val audioFileScanner: AudioFileScanner
) {
    /**
     * 扫描结果缓存（内存中）
     */
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    /**
     * 扫描结果 Flow，供其他模块订阅
     */
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    /**
     * 扫描指定目录中的音频文件
     * 
     * @param rootPath 要扫描的根目录路径
     * @param onSongFound 当找到音频文件时的回调函数（可选）
     * @return Flow<ScanProgress> 扫描进度更新流
     */
    fun scanAudioFiles(
        rootPath: String,
        onSongFound: (Song) -> Unit = {}
    ): Flow<ScanProgress> {
        _songs.value = emptyList()
        val foundSongs = mutableListOf<Song>()
        return audioFileScanner.scanDirectory(rootPath) { song ->
            foundSongs.add(song)
            _songs.value = foundSongs.toList()
            onSongFound(song)
        }
    }

    /**
     * 获取所有扫描到的歌曲
     * 
     * @return Flow<List<Song>> 歌曲列表 Flow
     */
    fun getAllSongs(): Flow<List<Song>> = songs

    /**
     * 清空缓存
     */
    fun clearCache() {
        _songs.value = emptyList()
    }
}
