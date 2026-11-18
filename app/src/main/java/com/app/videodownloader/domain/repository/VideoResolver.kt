package com.app.videodownloader.domain.repository

import com.app.videodownloader.domain.model.VideoInfo
import com.app.videodownloader.domain.model.VideoLink

interface VideoResolver {
    /** @throws com.app.videodownloader.domain.model.VideoDownloadException */
    suspend fun resolve(link: VideoLink): VideoInfo
}
