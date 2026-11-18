package com.app.videodownloader.data.local

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import com.app.videodownloader.domain.model.DownloadError
import com.app.videodownloader.domain.model.DownloadedVideo
import com.app.videodownloader.domain.model.VideoDownloadException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class MediaStoreVideoSaver @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val resolver get() = context.contentResolver

    /** Copies [file] into Movies/VideoDownloader and returns the saved entry. */
    suspend fun save(file: File, displayName: String): DownloadedVideo = withContext(Dispatchers.IO) {
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "$displayName.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, RELATIVE_PATH)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values)
            ?: throw VideoDownloadException(DownloadError.Storage)
        try {
            val out = resolver.openOutputStream(uri) ?: throw VideoDownloadException(DownloadError.Storage)
            out.use { output -> file.inputStream().use { it.copyTo(output) } }
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) },
                null,
                null,
            )
            queryByUri(uri) ?: throw VideoDownloadException(DownloadError.Storage)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw if (e is VideoDownloadException) e else VideoDownloadException(DownloadError.Storage, e)
        }
    }

    suspend fun queryAll(): List<DownloadedVideo> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        query(
            collection,
            "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("$RELATIVE_PATH/%"),
            "${MediaStore.Video.Media.DATE_ADDED} DESC",
        )
    }

    suspend fun delete(video: DownloadedVideo): Boolean = withContext(Dispatchers.IO) {
        runCatching { resolver.delete(android.net.Uri.parse(video.uri), null, null) > 0 }
            .getOrDefault(false)
    }

    private fun queryByUri(uri: android.net.Uri): DownloadedVideo? =
        query(uri, null, null, null).firstOrNull()

    private fun query(
        uri: android.net.Uri,
        selection: String?,
        args: Array<String>?,
        sortOrder: String?,
    ): List<DownloadedVideo> {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED,
        )
        val videos = mutableListOf<DownloadedVideo>()
        resolver.query(uri, projection, selection, args, sortOrder)?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val name = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val size = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val duration = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val added = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val rowId = cursor.getLong(id)
                videos += DownloadedVideo(
                    id = rowId,
                    uri = android.content.ContentUris.withAppendedId(
                        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                        rowId,
                    ).toString(),
                    title = cursor.getString(name).substringBeforeLast('.'),
                    sizeBytes = cursor.getLong(size),
                    durationMs = cursor.getLong(duration),
                    dateAddedSeconds = cursor.getLong(added),
                )
            }
        }
        return videos
    }

    private companion object {
        const val RELATIVE_PATH = "Movies/VideoDownloader"
    }
}
