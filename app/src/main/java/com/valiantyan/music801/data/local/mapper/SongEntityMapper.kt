package com.valiantyan.music801.data.local.mapper

import com.valiantyan.music801.data.local.entity.SongEntity
import com.valiantyan.music801.domain.model.Song

/**
 * 将 [SongEntity] 转为 [Song]
 */
fun SongEntity.toDomain(): Song {
    return Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        filePath = filePath,
        fileSize = fileSize,
        dateAdded = dateAdded,
        albumArtPath = albumArtPath,
    )
}

/**
 * 将 [Song] 转为 [SongEntity]
 */
fun Song.toEntity(modifiedAt: Long, scannedAt: Long): SongEntity {
    return SongEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        filePath = filePath,
        fileSize = fileSize,
        dateAdded = dateAdded,
        albumArtPath = albumArtPath,
        modifiedAt = modifiedAt,
        scannedAt = scannedAt,
    )
}
