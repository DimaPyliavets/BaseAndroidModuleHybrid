package com.example.baseandroidmodulehybrid.bridge

import android.content.Context
import android.webkit.JavascriptInterface
import com.example.baseandroidmodulehybrid.core.model.AppConfig
import com.example.baseandroidmodulehybrid.core.model.WidgetDataDao
import com.example.baseandroidmodulehybrid.core.model.WidgetDataEntity
import com.example.baseandroidmodulehybrid.notifications.NotificationHelper
import com.example.baseandroidmodulehybrid.widget.HybridWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale
import javax.inject.Inject

/**
 * NativeBridge — безпечний JavascriptInterface між JS та Kotlin.
 */
@ActivityRetainedScoped
class NativeBridge @Inject constructor(
    private val notificationHelper: NotificationHelper,
    private val dataSyncHandler: DataSyncHandler
) {

    @JavascriptInterface
    fun showNotification(title: String, message: String) {
        val safeTitle = sanitize(title, AppConfig.MAX_NOTIFICATION_TITLE_LEN)
        val safeMessage = sanitize(message, AppConfig.MAX_NOTIFICATION_BODY_LEN)

        if (safeTitle.isBlank() || safeMessage.isBlank()) return

        notificationHelper.show(safeTitle, safeMessage)
    }

    @JavascriptInterface
    fun syncData(json: String) {
        if (json.toByteArray().size > AppConfig.MAX_JSON_PAYLOAD_BYTES) return

        val jsonObject = try {
            JSONObject(json)
        } catch (e: Exception) {
            return
        }

        val allowedKeys = setOf("title", "subtitle")
        val sanitized = JSONObject()
        for (key in allowedKeys) {
            if (jsonObject.has(key)) {
                sanitized.put(key, sanitize(jsonObject.optString(key), 200))
            }
        }

        dataSyncHandler.handle(sanitized)
    }

    @JavascriptInterface
    fun getDeviceLocale(): String {
        return Locale.getDefault().toLanguageTag()
    }

    @JavascriptInterface
    fun getAppVersion(): String {
        return "1.0.0"
    }

    private fun sanitize(input: String, maxLength: Int): String {
        return input
            .take(maxLength)
            .replace(Regex("[<>\"'&;]"), "")
            .trim()
    }
}

/**
 * DataSyncHandler — обробник даних що надходять з JS через syncData().
 */
class DataSyncHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val widgetDao: WidgetDataDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun handle(data: JSONObject) {
        scope.launch {
            val title = data.optString("title")
            val subtitle = data.optString("subtitle")
            
            // Зберігаємо в Room
            widgetDao.upsert(
                WidgetDataEntity(
                    title = title,
                    subtitle = subtitle,
                    lastUpdated = System.currentTimeMillis()
                )
            )
            
            // Оновлюємо віджет
            HybridWidget.updateAll(context)
        }
    }
}
