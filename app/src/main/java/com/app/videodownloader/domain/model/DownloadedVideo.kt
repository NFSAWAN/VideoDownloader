package com.app.videodownloader.domain.model

data class DownloadedVideo(
    val id: Long,
    val uri: String,
    val title: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val dateAddedSeconds: Long,
)
