package com.app.videodownloader

import android.app.Application
import android.util.Log
import com.app.videodownloader.data.remote.YoutubeDlInitializer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class VideoDownloaderApp : Application() {

    @Inject lateinit var youtubeDlInitializer: YoutubeDlInitializer

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                youtubeDlInitializer.ensureInitialized()
            }.onFailure { Log.e(TAG, "Failed to initialize youtubedl-android", it) }
        }
    }

    private companion object {
        const val TAG = "VideoDownloaderApp"
    }
}
