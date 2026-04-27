package com.example.baseandroidmodulehybrid.core

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * HybridApp — Base Application class in core module.
 * ⚠️ Note: @HiltAndroidApp should only be in the :app module's Application class.
 */
open class HybridApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "Загальні сповіщення",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Сповіщення від веб-додатка"
            }

            val updatesChannel = NotificationChannel(
                CHANNEL_UPDATES,
                "Оновлення",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Прогрес завантаження оновлень"
            }

            manager.createNotificationChannels(listOf(generalChannel, updatesChannel))
        }
    }

    companion object {
        const val CHANNEL_GENERAL = "channel_general"
        const val CHANNEL_UPDATES = "channel_updates"
    }
}
