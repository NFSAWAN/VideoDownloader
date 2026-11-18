package com.app.videodownloader.data.remote

import com.app.videodownloader.domain.model.DownloadError
import com.app.videodownloader.domain.model.VideoQuality

internal fun mapYtDlpError(message: String?): DownloadError {
    val text = message?.lowercase().orEmpty()
    return when {
        PRIVATE_HINTS.any(text::contains) -> DownloadError.PrivateOrUnavailable
        NETWORK_HINTS.any(text::contains) -> DownloadError.Network
        else -> DownloadError.Unknown
    }
}

internal fun formatSelector(quality: VideoQuality): String {
    val height = quality.height ?: return "bv*+ba/b"
    return "bv*[height<=$height]+ba/b[height<=$height]/b"
}

/** "Best" first, then each distinct video height from tallest to shortest. */
internal fun availableQualities(heights: List<Int>): List<VideoQuality> =
    listOf(VideoQuality.Best) +
        heights.filter { it > 0 }.distinct().sortedDescending().map(::VideoQuality)

internal fun sanitizeFileName(title: String, fallback: String): String =
    title.replace(INVALID_FILE_CHARS, " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(MAX_NAME_LENGTH)
        .trim()
        .ifEmpty { fallback }

private val PRIVATE_HINTS = listOf(
    "private", "login", "cookies", "not available", "unavailable", "removed", "authenticat",
)
private val NETWORK_HINTS = listOf(
    "unable to download", "network", "timed out", "connection", "resolve", "urlopen", "unreachable",
)
private val INVALID_FILE_CHARS = Regex("""[\\/:*?"<>|\r\n]+""")
private const val MAX_NAME_LENGTH = 80
