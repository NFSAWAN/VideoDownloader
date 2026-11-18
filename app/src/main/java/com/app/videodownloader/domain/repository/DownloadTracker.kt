package com.app.videodownloader.domain.repository

import com.app.videodownloader.domain.model.DownloadState
import kotlinx.coroutines.flow.StateFlow

/** Holds the state of the single active download so the UI and the service can share it. */
interface DownloadTracker {
    val state: StateFlow<DownloadState>
    fun update(state: DownloadState)
    fun reset()
}
