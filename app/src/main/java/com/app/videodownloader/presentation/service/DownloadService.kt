package com.app.videodownloader.presentation.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.app.videodownloader.domain.model.DownloadState
import com.app.videodownloader.domain.model.VideoLink
import com.app.videodownloader.domain.model.VideoQuality
import com.app.videodownloader.domain.repository.DownloadTracker
import com.app.videodownloader.domain.usecase.DownloadVideoUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject lateinit var downloadVideo: DownloadVideoUseCase
    @Inject lateinit var tracker: DownloadTracker

    private lateinit var notifier: DownloadNotifier
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notifier = DownloadNotifier(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start(intent)
            ACTION_CANCEL -> cancel()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun start(intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        ServiceCompat.startForeground(
            this,
            DownloadNotifier.ID_FOREGROUND,
            notifier.progress(title, null),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        val url = intent.getStringExtra(EXTRA_URL)
        if (url == null) {
            finish()
            return
        }
        if (job?.isActive == true) return

        val height = intent.getIntExtra(EXTRA_HEIGHT, 0).takeIf { it > 0 }
        job = scope.launch {
            downloadVideo(VideoLink(url), title, VideoQuality(height)).collect { state ->
                tracker.update(state)
                when (state) {
                    is DownloadState.InProgress -> notifier.notifyProgress(title, state.percent)
                    is DownloadState.Completed -> notifier.notifyDone(title)
                    is DownloadState.Failed -> notifier.notifyFailed(title)
                    DownloadState.Idle -> Unit
                }
            }
            finish()
        }
    }

    private fun cancel() {
        job?.cancel()
        tracker.reset()
        finish()
    }

    private fun finish() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.app.videodownloader.action.START"
        const val ACTION_CANCEL = "com.app.videodownloader.action.CANCEL"
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
        const val EXTRA_HEIGHT = "height"
    }
}
