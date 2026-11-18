package com.app.videodownloader.domain.usecase

import com.app.videodownloader.domain.model.DownloadedVideo
import com.app.videodownloader.domain.repository.DownloadedVideoRepository
import javax.inject.Inject

class DeleteDownloadUseCase @Inject constructor(
    private val repository: DownloadedVideoRepository,
) {
    suspend operator fun invoke(video: DownloadedVideo): Boolean = repository.delete(video)
}
