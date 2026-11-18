package com.app.videodownloader.presentation.share

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

object VideoActions {

    fun share(context: Context, uri: String, mimeType: String = "video/mp4") {
        val contentUri = uri.toUri()
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            clipData = ClipData.newRawUri("", contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share"))
    }

    fun play(context: Context, uri: String, mimeType: String = "video/mp4") {
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri.toUri(), mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(view)
        } catch (_: ActivityNotFoundException) {
            // No video player installed.
        }
    }
}
