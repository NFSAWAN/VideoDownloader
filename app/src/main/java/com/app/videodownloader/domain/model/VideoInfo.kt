package com.app.videodownloader.domain.model

data class VideoInfo(
    val title: String,
    val durationSeconds: Int,
    val qualities: List<VideoQuality>,
)
