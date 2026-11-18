package com.app.videodownloader.presentation.download

import com.app.videodownloader.domain.model.DownloadError
import com.app.videodownloader.domain.model.DownloadedVideo
import com.app.videodownloader.domain.model.VideoInfo
import com.app.videodownloader.domain.model.VideoLink
import com.app.videodownloader.domain.model.VideoQuality

sealed interface DownloadUiState {
    data object Idle : DownloadUiState
    data object InvalidLink : DownloadUiState
    data class LoadingInfo(val link: VideoLink) : DownloadUiState
    data class Ready(
        val link: VideoLink,
        val info: VideoInfo,
        val selected: VideoQuality,
    ) : DownloadUiState
    data class Downloading(
        val title: String,
        val percent: Float,
        val etaSeconds: Long?,
    ) : DownloadUiState
    data class Completed(val video: DownloadedVideo) : DownloadUiState
    data class Error(val error: DownloadError) : DownloadUiState
}
