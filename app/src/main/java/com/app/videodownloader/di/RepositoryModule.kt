package com.app.videodownloader.di

import com.app.videodownloader.data.download.YtDlpVideoDownloader
import com.app.videodownloader.data.local.MediaStoreDownloadedVideoRepository
import com.app.videodownloader.data.remote.YtDlpVideoResolver
import com.app.videodownloader.data.status.SafStatusRepository
import com.app.videodownloader.data.tracker.InMemoryDownloadTracker
import com.app.videodownloader.domain.repository.DownloadTracker
import com.app.videodownloader.domain.repository.DownloadedVideoRepository
import com.app.videodownloader.domain.repository.StatusRepository
import com.app.videodownloader.domain.repository.VideoDownloader
import com.app.videodownloader.domain.repository.VideoResolver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindVideoResolver(impl: YtDlpVideoResolver): VideoResolver

    @Binds
    abstract fun bindVideoDownloader(impl: YtDlpVideoDownloader): VideoDownloader

    @Binds
    abstract fun bindDownloadedVideoRepository(impl: MediaStoreDownloadedVideoRepository): DownloadedVideoRepository

    @Binds
    abstract fun bindDownloadTracker(impl: InMemoryDownloadTracker): DownloadTracker

    @Binds
    abstract fun bindStatusRepository(impl: SafStatusRepository): StatusRepository
}
