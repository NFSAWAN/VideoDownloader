package com.app.videodownloader.domain.usecase

import com.app.videodownloader.domain.model.DownloadedVideo
import com.app.videodownloader.domain.repository.DownloadedVideoRepository
import javax.inject.Inject

class GetDownloadsUseCase @Inject constructor(
    private val repository: DownloadedVideoRepository,
) {
    suspend operator fun invoke(): List<DownloadedVideo> = repository.getAll()
}
