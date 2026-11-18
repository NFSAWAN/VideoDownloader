package com.app.videodownloader.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.videodownloader.domain.model.DownloadedVideo
import com.app.videodownloader.domain.usecase.DeleteDownloadUseCase
import com.app.videodownloader.domain.usecase.GetDownloadsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val loading: Boolean = true,
    val videos: List<DownloadedVideo> = emptyList(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getDownloads: GetDownloadsUseCase,
    private val deleteDownload: DeleteDownloadUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = HistoryUiState(loading = false, videos = getDownloads())
        }
    }

    fun delete(video: DownloadedVideo) {
        viewModelScope.launch {
            deleteDownload(video)
            refresh()
        }
    }
}
