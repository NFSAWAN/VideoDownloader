package com.app.videodownloader.data.remote

import com.app.videodownloader.domain.model.DownloadError
import com.app.videodownloader.domain.model.VideoDownloadException
import com.app.videodownloader.domain.model.VideoInfo
import com.app.videodownloader.domain.model.VideoLink
import com.app.videodownloader.domain.repository.VideoResolver
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class YtDlpVideoResolver @Inject constructor(
    private val initializer: YoutubeDlInitializer,
) : VideoResolver {

    override suspend fun resolve(link: VideoLink): VideoInfo {
        try {
            initializer.ensureInitialized()
            return try {
                fetch(link)
            } catch (e: Exception) {
                if (e is CancellationException || mapYtDlpError(e.message) != DownloadError.Unknown) throw e
                initializer.updateYtDlp()
                fetch(link)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw VideoDownloadException(mapYtDlpError(e.message), e)
        }
    }

    private suspend fun fetch(link: VideoLink): VideoInfo = withContext(Dispatchers.IO) {
        val request = YoutubeDLRequest(link.url).addOption("--no-playlist")
        val info = YoutubeDL.getInstance().getInfo(request)
        val heights = info.formats.orEmpty()
            .filter { it.vcodec != null && it.vcodec != "none" }
            .map { it.height }
        VideoInfo(
            title = info.title ?: info.fulltitle.orEmpty(),
            durationSeconds = info.duration,
            qualities = availableQualities(heights),
        )
    }
}
