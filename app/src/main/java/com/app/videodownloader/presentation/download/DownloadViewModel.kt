package com.app.videodownloader.presentation.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.videodownloader.domain.model.DownloadError
import com.app.videodownloader.domain.model.DownloadState
import com.app.videodownloader.domain.model.VideoDownloadException
import com.app.videodownloader.domain.model.VideoLink
import com.app.videodownloader.domain.model.VideoQuality
import com.app.videodownloader.domain.repository.DownloadTracker
import com.app.videodownloader.domain.usecase.ExtractLinkUseCase
import com.app.videodownloader.domain.usecase.ResolveVideoUseCase
import com.app.videodownloader.presentation.service.DownloadServiceLauncher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val extractLink: ExtractLinkUseCase,
    private val resolveVideo: ResolveVideoUseCase,
    private val tracker: DownloadTracker,
    private val launcher: DownloadServiceLauncher,
) : ViewModel() {

    private val stage = MutableStateFlow<DownloadUiState>(DownloadUiState.Idle)
    private var lastLink: VideoLink? = null

    val uiState: StateFlow<DownloadUiState> = combine(stage, tracker.state) { stage, download ->
        when (download) {
            is DownloadState.InProgress -> DownloadUiState.Downloading(
                title = (stage as? DownloadUiState.Ready)?.info?.title.orEmpty(),
                percent = download.percent,
                etaSeconds = download.etaSeconds,
            )
            is DownloadState.Completed -> DownloadUiState.Completed(download.video)
            is DownloadState.Failed -> DownloadUiState.Error(download.error)
            DownloadState.Idle -> stage
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadUiState.Idle)

    fun onSharedText(text: String?) {
        if (tracker.state.value is DownloadState.InProgress) return
        tracker.reset()
        val link = extractLink(text)
        if (link == null) {
            stage.value = DownloadUiState.InvalidLink
            return
        }
        resolve(link)
    }

    fun onQualitySelected(quality: VideoQuality) {
        stage.update { if (it is DownloadUiState.Ready) it.copy(selected = quality) else it }
    }

    fun onDownload() {
        val ready = stage.value as? DownloadUiState.Ready ?: return
        launcher.start(ready.link, ready.info.title, ready.selected)
    }

    fun onCancel() = launcher.cancel()

    fun onRetry() {
        tracker.reset()
        val link = lastLink
        if (stage.value is DownloadUiState.Error && link != null) resolve(link)
    }

    fun onReset() {
        tracker.reset()
        stage.value = DownloadUiState.Idle
    }

    private fun resolve(link: VideoLink) {
        lastLink = link
        stage.value = DownloadUiState.LoadingInfo(link)
        viewModelScope.launch {
            stage.value = resolveVideo(link).fold(
                onSuccess = { DownloadUiState.Ready(link, it, VideoQuality.Best) },
                onFailure = {
                    DownloadUiState.Error((it as? VideoDownloadException)?.error ?: DownloadError.Unknown)
                },
            )
        }
    }
}
