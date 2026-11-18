package com.app.videodownloader.presentation.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.videodownloader.domain.model.StatusItem
import com.app.videodownloader.domain.model.StatusSource
import com.app.videodownloader.domain.repository.StatusRepository
import com.app.videodownloader.domain.usecase.DeleteSavedStatusUseCase
import com.app.videodownloader.domain.usecase.GetSavedStatusesUseCase
import com.app.videodownloader.domain.usecase.GetStatusesUseCase
import com.app.videodownloader.domain.usecase.SaveStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StatusTab { Live, Saved }

data class StatusUiState(
    val tab: StatusTab = StatusTab.Live,
    val source: StatusSource = StatusSource.WhatsApp,
    val hasAccess: Boolean = false,
    val loading: Boolean = true,
    val items: List<StatusItem> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val repository: StatusRepository,
    private val getStatuses: GetStatusesUseCase,
    private val getSaved: GetSavedStatusesUseCase,
    private val saveStatus: SaveStatusUseCase,
    private val deleteSaved: DeleteSavedStatusUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatusUiState())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    fun refresh() {
        val state = _uiState.value
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val access = state.tab == StatusTab.Saved || repository.hasAccess(state.source)
            val items = when {
                !access -> emptyList()
                state.tab == StatusTab.Saved -> getSaved()
                else -> getStatuses(state.source)
            }
            _uiState.update { it.copy(hasAccess = access, loading = false, items = items) }
        }
    }

    fun selectTab(tab: StatusTab) {
        _uiState.update { it.copy(tab = tab, items = emptyList()) }
        refresh()
    }

    fun selectSource(source: StatusSource) {
        _uiState.update { it.copy(source = source, items = emptyList()) }
        refresh()
    }

    fun onFolderPicked(uri: String?) {
        if (uri == null) return
        if (repository.grantAccess(_uiState.value.source, uri)) {
            refresh()
        } else {
            _uiState.update { it.copy(message = "Please select the .Statuses folder itself") }
        }
    }

    fun save(item: StatusItem) {
        viewModelScope.launch {
            val result = runCatching { saveStatus(item) }
            _uiState.update { it.copy(message = if (result.isSuccess) "Saved" else "Couldn't save status") }
        }
    }

    fun delete(item: StatusItem) {
        viewModelScope.launch {
            deleteSaved(item)
            refresh()
        }
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }
}
