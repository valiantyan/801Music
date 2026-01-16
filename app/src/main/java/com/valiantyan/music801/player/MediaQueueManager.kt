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
    fun setQueue(songs: List<Song>, startIndex: Int): Unit {
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
        val nextIndex: Int = currentIndex + 1
        if (nextIndex < 0 || nextIndex >= queue.size) {
            return null
        }
        return queue[nextIndex]
    }

    /**
     * 获取上一首歌曲
     *
     * @return 上一首歌曲，不存在时返回 null
     */
    fun getPreviousSong(): Song? {
        val previousIndex: Int = currentIndex - 1
        if (previousIndex < 0 || previousIndex >= queue.size) {
            return null
        }
        return queue[previousIndex]
    }

    /**
     * 切换到下一首
     *
     * @return 是否切换成功
     */
    fun moveToNext(): Boolean {
        val nextIndex: Int = currentIndex + 1
        if (nextIndex < 0 || nextIndex >= queue.size) {
            return false
        }
        currentIndex = nextIndex
        return true
    }

    /**
     * 切换到上一首
     *
     * @return 是否切换成功
     */
    fun moveToPrevious(): Boolean {
        val previousIndex: Int = currentIndex - 1
        if (previousIndex < 0 || previousIndex >= queue.size) {
            return false
        }
        currentIndex = previousIndex
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
