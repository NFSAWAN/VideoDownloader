package com.app.videodownloader.presentation.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.app.videodownloader.MainActivity
import com.app.videodownloader.R

class DownloadNotifier(private val context: Context) {

    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW),
        )
    }

    fun progress(title: String, percent: Float?): Notification {
        val cancel = PendingIntent.getService(
            context,
            0,
            Intent(context, DownloadService::class.java).setAction(DownloadService.ACTION_CANCEL),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return base(title)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent?.toInt() ?: 0, percent == null || percent <= 0f)
            .addAction(0, "Cancel", cancel)
            .build()
    }

    fun notifyProgress(title: String, percent: Float?) {
        manager.notify(ID_FOREGROUND, progress(title, percent))
    }

    fun notifyDone(title: String) {
        manager.notify(
            ID_RESULT,
            base(title).setSmallIcon(R.drawable.ic_stat_download)
                .setContentText("Download complete")
                .setAutoCancel(true)
                .build(),
        )
    }

    fun notifyFailed(title: String) {
        manager.notify(
            ID_RESULT,
            base(title).setSmallIcon(R.drawable.ic_stat_download)
                .setContentText("Download failed")
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun base(title: String): NotificationCompat.Builder {
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title.ifBlank { "Video" })
            .setContentIntent(open)
    }

    companion object {
        const val ID_FOREGROUND = 1
        const val ID_RESULT = 2
        private const val CHANNEL_ID = "downloads"
    }
}
