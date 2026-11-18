package com.app.videodownloader.domain.repository

import com.app.videodownloader.domain.model.DownloadState
import com.app.videodownloader.domain.model.VideoLink
import com.app.videodownloader.domain.model.VideoQuality
import kotlinx.coroutines.flow.Flow

interface VideoDownloader {
    /** Cancelling the collector cancels the download. Terminal states are Completed and Failed. */
    fun download(link: VideoLink, title: String, quality: VideoQuality): Flow<DownloadState>
}
