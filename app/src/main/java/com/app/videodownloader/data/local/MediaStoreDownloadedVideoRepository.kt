package com.app.videodownloader.data.local

import com.app.videodownloader.domain.model.DownloadedVideo
import com.app.videodownloader.domain.repository.DownloadedVideoRepository
import javax.inject.Inject

class MediaStoreDownloadedVideoRepository @Inject constructor(
    private val saver: MediaStoreVideoSaver,
) : DownloadedVideoRepository {

    override suspend fun getAll(): List<DownloadedVideo> = saver.queryAll()

    override suspend fun delete(video: DownloadedVideo): Boolean = saver.delete(video)
}
