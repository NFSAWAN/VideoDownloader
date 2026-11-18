package com.app.videodownloader.data.status

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.edit
import androidx.core.net.toUri
import com.app.videodownloader.domain.model.DownloadError
import com.app.videodownloader.domain.model.StatusItem
import com.app.videodownloader.domain.model.StatusSource
import com.app.videodownloader.domain.model.VideoDownloadException
import com.app.videodownloader.domain.model.statusMimeType
import com.app.videodownloader.domain.repository.StatusRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Reads WhatsApp's `.Statuses` folders through a user-granted SAF tree and saves copies into MediaStore. */
class SafStatusRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : StatusRepository {

    private val resolver get() = context.contentResolver
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun hasAccess(source: StatusSource): Boolean {
        val tree = treeUri(source) ?: return false
        return resolver.persistedUriPermissions.any { it.uri == tree && it.isReadPermission }
    }

    override fun grantAccess(source: StatusSource, treeUri: String): Boolean {
        val tree = treeUri.toUri()
        val documentId = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull() ?: return false
        if (!documentId.endsWith(".Statuses")) return false
        resolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        prefs.edit { putString(source.name, treeUri) }
        return true
    }

    override suspend fun getStatuses(source: StatusSource): List<StatusItem> = withContext(Dispatchers.IO) {
        val tree = treeUri(source) ?: return@withContext emptyList()
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree,
            DocumentsContract.getTreeDocumentId(tree),
        )
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        val items = mutableListOf<StatusItem>()
        runCatching {
            resolver.query(children, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(1) ?: continue
                    val mime = statusMimeType(name) ?: continue
                    items += StatusItem(
                        uri = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0)).toString(),
                        name = name,
                        mimeType = mime,
                        sizeBytes = cursor.getLong(2),
                        lastModifiedMs = cursor.getLong(3),
                    )
                }
            }
        }
        items.sortedByDescending { it.lastModifiedMs }
    }

    override suspend fun getSaved(): List<StatusItem> = withContext(Dispatchers.IO) {
        val images = querySaved(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), IMAGE_PATH)
        val videos = querySaved(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), VIDEO_PATH)
        (images + videos).sortedByDescending { it.lastModifiedMs }
    }

    override suspend fun save(item: StatusItem): Unit = withContext(Dispatchers.IO) {
        val video = item.isVideo
        val collection = if (video) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, item.name)
            put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, if (video) VIDEO_PATH else IMAGE_PATH)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: throw VideoDownloadException(DownloadError.Storage)
        try {
            val input = resolver.openInputStream(item.uri.toUri())
                ?: throw VideoDownloadException(DownloadError.Storage)
            val output = resolver.openOutputStream(uri) ?: throw VideoDownloadException(DownloadError.Storage)
            input.use { i -> output.use { o -> i.copyTo(o) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw if (e is VideoDownloadException) e else VideoDownloadException(DownloadError.Storage, e)
        }
    }

    override suspend fun deleteSaved(item: StatusItem): Boolean = withContext(Dispatchers.IO) {
        runCatching { resolver.delete(item.uri.toUri(), null, null) > 0 }.getOrDefault(false)
    }

    private fun querySaved(collection: Uri, relativePath: String): List<StatusItem> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
        )
        val items = mutableListOf<StatusItem>()
        resolver.query(
            collection,
            projection,
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
            arrayOf("$relativePath/%"),
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                items += StatusItem(
                    uri = ContentUris.withAppendedId(collection, cursor.getLong(0)).toString(),
                    name = cursor.getString(1),
                    mimeType = cursor.getString(2),
                    sizeBytes = cursor.getLong(3),
                    lastModifiedMs = cursor.getLong(4) * 1000,
                )
            }
        }
        return items
    }

    private fun treeUri(source: StatusSource): Uri? = prefs.getString(source.name, null)?.toUri()

    private companion object {
        const val PREFS = "status_access"
        const val IMAGE_PATH = "Pictures/StatusSaver"
        const val VIDEO_PATH = "Movies/StatusSaver"
    }
}
