package com.valiantyan.music801.data.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 测试音频文件格式识别逻辑
 */
class AudioFormatRecognizerTest {

    @Test
    fun `识别 MP3 格式文件`() {
        // Given
        val mp3Files = listOf(
            "song.mp3",
            "MUSIC.MP3",
            "test_file.Mp3",
            "/storage/emulated/0/Music/song.mp3",
            "path/to/file.mp3",
        )

        // Then
        mp3Files.forEach { fileName ->
            assertTrue(
                "文件 $fileName 应该被识别为音频文件",
                AudioFormatRecognizer.isAudioFile(fileName),
            )
        }
    }

    @Test
    fun `识别 AAC 格式文件`() {
        // Given
        val aacFiles = listOf(
            "song.aac",
            "MUSIC.AAC",
            "test_file.Aac",
            "/storage/emulated/0/Music/song.aac",
        )

        // Then
        aacFiles.forEach { fileName ->
            assertTrue(
                "文件 $fileName 应该被识别为音频文件",
                AudioFormatRecognizer.isAudioFile(fileName),
            )
        }
    }

    @Test
    fun `识别 FLAC 格式文件`() {
        // Given
        val flacFiles = listOf(
            "song.flac",
            "MUSIC.FLAC",
            "test_file.Flac",
            "/storage/emulated/0/Music/song.flac",
        )

        // Then
        flacFiles.forEach { fileName ->
            assertTrue(
                "文件 $fileName 应该被识别为音频文件",
                AudioFormatRecognizer.isAudioFile(fileName),
            )
        }
    }

    @Test
    fun `识别 WAV 格式文件`() {
        // Given
        val wavFiles = listOf(
            "song.wav",
            "MUSIC.WAV",
            "test_file.Wav",
            "/storage/emulated/0/Music/song.wav",
        )

        // Then
        wavFiles.forEach { fileName ->
            assertTrue(
                "文件 $fileName 应该被识别为音频文件",
                AudioFormatRecognizer.isAudioFile(fileName),
            )
        }
    }

    @Test
    fun `识别 OGG 格式文件`() {
        // Given
        val oggFiles = listOf(
            "song.ogg",
            "MUSIC.OGG",
            "test_file.Ogg",
            "/storage/emulated/0/Music/song.ogg",
        )

        // Then
        oggFiles.forEach { fileName ->
            assertTrue(
                "文件 $fileName 应该被识别为音频文件",
                AudioFormatRecognizer.isAudioFile(fileName),
            )
        }
    }

    @Test
    fun `识别 M4A 格式文件`() {
        // Given
        val m4aFiles = listOf(
            "song.m4a",
            "MUSIC.M4A",
            "test_file.M4a",
            "/storage/emulated/0/Music/song.m4a",
        )

        // Then
        m4aFiles.forEach { fileName ->
            assertTrue(
                "文件 $fileName 应该被识别为音频文件",
                AudioFormatRecognizer.isAudioFile(fileName),
            )
        }
    }

    @Test
    fun `过滤非音频文件`() {
        // Given
        val nonAudioFiles = listOf(
            "document.pdf",
            "image.jpg",
            "video.mp4",
            "text.txt",
            "data.json",
            "script.js",
            "style.css",
            "archive.zip",
            "executable.exe",
            "song.mp3.backup", // 备份文件
            "song", // 无扩展名
            ".hidden", // 隐藏文件
            "song.mp3.", // 异常扩展名
            "", // 空字符串
        )

        // Then
        nonAudioFiles.forEach { fileName ->
            assertFalse(
                "文件 $fileName 不应该被识别为音频文件",
                AudioFormatRecognizer.isAudioFile(fileName),
            )
        }
    }

    @Test
    fun `处理大小写不敏感的文件扩展名`() {
        // Given
        val testCases = listOf(
            "song.MP3" to true,
            "song.mp3" to true,
            "song.Mp3" to true,
            "song.mP3" to true,
            "SONG.MP3" to true,
            "song.AAC" to true,
            "song.aac" to true,
            "song.FLAC" to true,
            "song.flac" to true,
        )

        // Then
        testCases.forEach { (fileName, expected) ->
            assertEquals(
                "文件 $fileName 的识别结果应该为 $expected",
                expected,
                AudioFormatRecognizer.isAudioFile(fileName),
            )
        }
    }

    @Test
    fun `处理带路径的文件名`() {
        // Given
        val testCases = listOf(
            "/storage/emulated/0/Music/song.mp3" to true,
            "/sdcard/Music/album/track.aac" to true,
            "/data/user/0/com.app/files/audio.flac" to true,
            "/storage/emulated/0/Documents/file.pdf" to false,
            "/sdcard/Images/photo.jpg" to false,
        )

        // Then
        testCases.forEach { (filePath, expected) ->
            assertEquals(
                "路径 $filePath 的识别结果应该为 $expected",
                expected,
                AudioFormatRecognizer.isAudioFile(filePath),
            )
        }
    }
}
