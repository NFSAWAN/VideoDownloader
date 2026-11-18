package com.app.videodownloader.data.download

import android.content.Context
import com.app.videodownloader.data.local.MediaStoreVideoSaver
import com.app.videodownloader.data.remote.YoutubeDlInitializer
import com.app.videodownloader.data.remote.formatSelector
import com.app.videodownloader.data.remote.mapYtDlpError
import com.app.videodownloader.data.remote.sanitizeFileName
import com.app.videodownloader.domain.model.DownloadError
import com.app.videodownloader.domain.model.DownloadState
import com.app.videodownloader.domain.model.VideoDownloadException
import com.app.videodownloader.domain.model.VideoLink
import com.app.videodownloader.domain.model.VideoQuality
import com.app.videodownloader.domain.repository.VideoDownloader
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class YtDlpVideoDownloader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val initializer: YoutubeDlInitializer,
    private val saver: MediaStoreVideoSaver,
) : VideoDownloader {

    override fun download(link: VideoLink, title: String, quality: VideoQuality): Flow<DownloadState> =
        callbackFlow {
            val processId = UUID.randomUUID().toString()
            val workDir = File(context.cacheDir, "downloads/$processId").apply { mkdirs() }

            val job = launch(Dispatchers.IO) {
                try {
                    trySend(DownloadState.InProgress(0f, null))
                    initializer.ensureInitialized()
                    val request = YoutubeDLRequest(link.url)
                        .addOption("--no-playlist")
                        .addOption("--no-mtime")
                        .addOption("-f", formatSelector(quality))
                        .addOption("--merge-output-format", "mp4")
                        .addOption("-o", "${workDir.absolutePath}/%(id)s.%(ext)s")
                    YoutubeDL.getInstance().execute(request, processId) { progress, eta, _ ->
                        trySend(
                            DownloadState.InProgress(
                                percent = progress.coerceIn(0f, 100f),
                                etaSeconds = eta.takeIf { it >= 0 },
                            ),
                        )
                    }
                    val file = workDir.listFiles()
                        ?.filter { it.isFile && it.extension in VIDEO_EXTENSIONS }
                        ?.maxByOrNull { it.length() }
                        ?: throw VideoDownloadException(DownloadError.Unknown)
                    val name = sanitizeFileName(title, fallback = "video_${System.currentTimeMillis()}")
                    trySend(DownloadState.Completed(saver.save(file, name)))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val error = (e as? VideoDownloadException)?.error ?: mapYtDlpError(e.message)
                    trySend(DownloadState.Failed(error))
                } finally {
                    close()
                }
            }

            awaitClose {
                YoutubeDL.getInstance().destroyProcessById(processId)
                job.cancel()
                workDir.deleteRecursively()
            }
        }

    private companion object {
        val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm", "mov")
    }
}
