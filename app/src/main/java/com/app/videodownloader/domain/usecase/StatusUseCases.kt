package com.app.videodownloader.domain.usecase

import com.app.videodownloader.domain.model.StatusItem
import com.app.videodownloader.domain.model.StatusSource
import com.app.videodownloader.domain.repository.StatusRepository
import javax.inject.Inject

class GetStatusesUseCase @Inject constructor(private val repository: StatusRepository) {
    suspend operator fun invoke(source: StatusSource): List<StatusItem> = repository.getStatuses(source)
}

class GetSavedStatusesUseCase @Inject constructor(private val repository: StatusRepository) {
    suspend operator fun invoke(): List<StatusItem> = repository.getSaved()
}

class SaveStatusUseCase @Inject constructor(private val repository: StatusRepository) {
    suspend operator fun invoke(item: StatusItem) = repository.save(item)
}

class DeleteSavedStatusUseCase @Inject constructor(private val repository: StatusRepository) {
    suspend operator fun invoke(item: StatusItem): Boolean = repository.deleteSaved(item)
}
