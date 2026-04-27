package com.example.baseandroidmodulehybrid

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.baseandroidmodulehybrid.core.util.NotificationConstants
import dagger.hilt.android.HiltAndroidApp

/**
 * HybridApp — точка входу додатка.
 */
@HiltAndroidApp
class HybridApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    /**
     * Створення Notification Channels (обов'язково для API 26+).
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // ─── Канал для загальних сповіщень з WebView ─────────────────
            val generalChannel = NotificationChannel(
                NotificationConstants.CHANNEL_GENERAL,
                "Загальні сповіщення",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Сповіщення від веб-додатка"
            }

            // ─── Канал для фонових оновлень ───────────────────────────────
            val updatesChannel = NotificationChannel(
                NotificationConstants.CHANNEL_UPDATES,
                "Оновлення",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Прогрес завантаження оновлень"
            }

            manager.createNotificationChannels(listOf(generalChannel, updatesChannel))
        }
    }
}
