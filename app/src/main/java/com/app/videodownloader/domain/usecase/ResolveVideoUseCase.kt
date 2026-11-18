package com.app.videodownloader.domain.usecase

import com.app.videodownloader.domain.model.VideoInfo
import com.app.videodownloader.domain.model.VideoLink
import com.app.videodownloader.domain.repository.VideoResolver
import javax.inject.Inject

class ResolveVideoUseCase @Inject constructor(
    private val resolver: VideoResolver,
) {
    suspend operator fun invoke(link: VideoLink): Result<VideoInfo> =
        runCatching { resolver.resolve(link) }
}
