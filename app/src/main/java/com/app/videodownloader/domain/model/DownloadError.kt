package com.app.videodownloader.domain.model

enum class DownloadError {
    Network,
    PrivateOrUnavailable,
    Storage,
    Unknown,
}

class VideoDownloadException(
    val error: DownloadError,
    cause: Throwable? = null,
) : Exception(error.name, cause)
