package com.example.baseandroidmodulehybrid.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.baseandroidmodulehybrid.core.util.NotificationConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NotificationHelper — утиліта для показу локальних сповіщень.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val notificationCounter = AtomicInteger(1000)

    fun show(title: String, message: String) {
        val intent = Intent(context, getMainActivityClass())
            .apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationCounter.get(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_GENERAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(notificationCounter.getAndIncrement(), notification)
    }

    fun showUpdateReady(version: String) {
        val notification = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_UPDATES)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Оновлення встановлено")
            .setContentText("Версія $version завантажена. Перезапусти додаток.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID_UPDATE, notification)
    }

    private fun getMainActivityClass(): Class<*> = try {
        Class.forName("com.example.baseandroidmodulehybrid.ui.MainActivity")
    } catch (e: ClassNotFoundException) {
        throw IllegalStateException("⚠️ Не знайдено MainActivity", e)
    }

    companion object {
        private const val NOTIFICATION_ID_UPDATE = 999
    }
}
