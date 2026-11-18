package com.app.videodownloader.domain.model

enum class StatusSource(val label: String, val statusesPath: String) {
    WhatsApp("WhatsApp", "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
    Business("Business", "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"),
}

data class StatusItem(
    val uri: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val lastModifiedMs: Long,
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
}

/** Returns the mime type of a status file, or null for anything that isn't a photo or video (e.g. `.nomedia`). */
fun statusMimeType(fileName: String): String? = when (fileName.substringAfterLast('.', "").lowercase()) {
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "mp4" -> "video/mp4"
    else -> null
}
