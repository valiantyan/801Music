package com.valiantyan.music801.player

import com.valiantyan.music801.domain.model.Song

/**
 * 管理播放队列的基础顺序逻辑
 */
class MediaQueueManager {
    private var queue: List<Song> = emptyList()
    private var currentIndex: Int = -1

    /**
     * 设置歌曲列表
     *
     * @param songs 歌曲列表
     * @param startIndex 开始位置
     */
    fun setQueue(songs: List<Song>, startIndex: Int) {
        queue = songs
        currentIndex = resolveStartIndex(songs = songs, startIndex = startIndex)
    }

    /**
     * 获取当前歌曲
     *
     * @return 当前歌曲，不存在时返回 null
     */
    fun getCurrentSong(): Song? {
        if (currentIndex < 0 || currentIndex >= queue.size) {
            return null
        }
        return queue[currentIndex]
    }

    /**
     * 获取下一首歌曲
     *
     * @return 下一首歌曲，不存在时返回 null
     */
    fun getNextSong(): Song? {
        if (queue.isEmpty() || currentIndex < 0) {
            return null
        }
        val nextIndex: Int = if (currentIndex == queue.size - 1) {
            0
        } else {
            currentIndex + 1
        }
        return queue[nextIndex]
    }

    /**
     * 获取上一首歌曲
     *
     * @return 上一首歌曲，不存在时返回 null
     */
    fun getPreviousSong(): Song? {
        if (queue.isEmpty() || currentIndex < 0) {
            return null
        }
        val previousIndex: Int = if (currentIndex == 0) {
            queue.size - 1
        } else {
            currentIndex - 1
        }
        return queue[previousIndex]
    }

    /**
     * 切换到下一首
     *
     * @return 是否切换成功
     */
    fun moveToNext(): Boolean {
        if (queue.isEmpty() || currentIndex < 0) {
            return false
        }
        currentIndex = if (currentIndex == queue.size - 1) {
            0
        } else {
            currentIndex + 1
        }
        return true
    }

    /**
     * 切换到上一首
     *
     * @return 是否切换成功
     */
    fun moveToPrevious(): Boolean {
        if (queue.isEmpty() || currentIndex < 0) {
            return false
        }
        currentIndex = if (currentIndex == 0) {
            queue.size - 1
        } else {
            currentIndex - 1
        }
        return true
    }

    /**
     * 判断是否为最后一首
     *
     * @return 是否位于队列末尾
     */
    fun isLastSong(): Boolean {
        return queue.isNotEmpty() && currentIndex == queue.size - 1
    }

    /**
     * 判断是否为第一首
     *
     * @return 是否位于队列起始
     */
    fun isFirstSong(): Boolean {
        return queue.isNotEmpty() && currentIndex == 0
    }

    private fun resolveStartIndex(songs: List<Song>, startIndex: Int): Int {
        if (songs.isEmpty()) {
            return -1
        }
        if (startIndex < 0 || startIndex >= songs.size) {
            return -1
        }
        return startIndex
    }
}
