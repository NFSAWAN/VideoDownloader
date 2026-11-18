package com.app.videodownloader.domain.repository

import com.app.videodownloader.domain.model.DownloadedVideo

interface DownloadedVideoRepository {
    suspend fun getAll(): List<DownloadedVideo>
    suspend fun delete(video: DownloadedVideo): Boolean
}
