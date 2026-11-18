package com.app.videodownloader.domain.model

sealed interface DownloadState {
    data object Idle : DownloadState
    data class InProgress(val percent: Float, val etaSeconds: Long?) : DownloadState
    data class Completed(val video: DownloadedVideo) : DownloadState
    data class Failed(val error: DownloadError) : DownloadState
}
