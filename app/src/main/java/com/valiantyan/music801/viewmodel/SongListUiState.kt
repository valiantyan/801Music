package com.valiantyan.music801.viewmodel

import com.valiantyan.music801.domain.model.Song

/**
 * 歌曲列表界面状态
 *
 * @param songs 歌曲列表数据
 * @param isLoading 是否正在加载
 * @param isEmpty 是否为空状态
 * @param error 错误信息
 */
data class SongListUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val isEmpty: Boolean = false,
    val error: String? = null,
)
