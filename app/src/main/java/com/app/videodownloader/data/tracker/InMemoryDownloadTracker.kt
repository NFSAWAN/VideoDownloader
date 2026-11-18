package com.app.videodownloader.data.tracker

import com.app.videodownloader.domain.model.DownloadState
import com.app.videodownloader.domain.repository.DownloadTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryDownloadTracker @Inject constructor() : DownloadTracker {

    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    override val state: StateFlow<DownloadState> = _state.asStateFlow()

    override fun update(state: DownloadState) {
        _state.value = state
    }

    override fun reset() {
        _state.value = DownloadState.Idle
    }
}
