package com.app.videodownloader.domain.model

data class VideoQuality(val height: Int?) {
    companion object {
        val Best = VideoQuality(null)
    }
}
