package com.app.videodownloader.domain.usecase

import com.app.videodownloader.domain.model.DownloadState
import com.app.videodownloader.domain.model.VideoLink
import com.app.videodownloader.domain.model.VideoQuality
import com.app.videodownloader.domain.repository.VideoDownloader
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DownloadVideoUseCase @Inject constructor(
    private val downloader: VideoDownloader,
) {
    operator fun invoke(link: VideoLink, title: String, quality: VideoQuality): Flow<DownloadState> =
        downloader.download(link, title, quality)
}
