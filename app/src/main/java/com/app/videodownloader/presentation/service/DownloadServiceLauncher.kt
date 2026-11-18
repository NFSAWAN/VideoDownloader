package com.app.videodownloader.presentation.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.app.videodownloader.domain.model.VideoLink
import com.app.videodownloader.domain.model.VideoQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DownloadServiceLauncher @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun start(link: VideoLink, title: String, quality: VideoQuality) {
        val intent = Intent(context, DownloadService::class.java)
            .setAction(DownloadService.ACTION_START)
            .putExtra(DownloadService.EXTRA_URL, link.url)
            .putExtra(DownloadService.EXTRA_TITLE, title)
            .putExtra(DownloadService.EXTRA_HEIGHT, quality.height ?: 0)
        ContextCompat.startForegroundService(context, intent)
    }

    fun cancel() {
        context.startService(
            Intent(context, DownloadService::class.java).setAction(DownloadService.ACTION_CANCEL),
        )
    }
}
