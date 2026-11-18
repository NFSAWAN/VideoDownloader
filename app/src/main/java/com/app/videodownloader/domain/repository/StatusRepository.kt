package com.app.videodownloader.domain.repository

import com.app.videodownloader.domain.model.StatusItem
import com.app.videodownloader.domain.model.StatusSource

interface StatusRepository {
    fun hasAccess(source: StatusSource): Boolean

    /** Persists access to the picked folder. Returns false when [treeUri] isn't the `.Statuses` folder. */
    fun grantAccess(source: StatusSource, treeUri: String): Boolean

    /** Statuses currently visible in WhatsApp, newest first. */
    suspend fun getStatuses(source: StatusSource): List<StatusItem>

    /** Statuses previously saved by this app, newest first. */
    suspend fun getSaved(): List<StatusItem>

    suspend fun save(item: StatusItem)

    suspend fun deleteSaved(item: StatusItem): Boolean
}
