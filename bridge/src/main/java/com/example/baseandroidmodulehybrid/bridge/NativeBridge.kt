package com.example.baseandroidmodulehybrid.bridge

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
    @ApplicationContext private val context: Context,
    private val notificationHelper: NotificationHelper,
    private val dataSyncHandler: DataSyncHandler
) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    @JavascriptInterface
    fun showNotification(title: String, message: String) {
        val safeTitle = sanitize(title, AppConfig.MAX_NOTIFICATION_TITLE_LEN)
        val safeMessage = sanitize(message, AppConfig.MAX_NOTIFICATION_BODY_LEN)
        if (safeTitle.isBlank() || safeMessage.isBlank()) return
        notificationHelper.show(safeTitle, safeMessage)
    }

    @JavascriptInterface
    fun vibrate(milliseconds: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(milliseconds)
        }
    }

    @JavascriptInterface
    fun setFlashlight(enabled: Boolean) {
        try {
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, enabled)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun syncData(json: String) {
        if (json.toByteArray().size > AppConfig.MAX_JSON_PAYLOAD_BYTES) return
        val jsonObject = try { JSONObject(json) } catch (e: Exception) { return }
        val sanitized = JSONObject()
        sanitized.put("title", sanitize(jsonObject.optString("title"), 200))
        sanitized.put("subtitle", sanitize(jsonObject.optString("subtitle"), 200))
        dataSyncHandler.handle(sanitized)
    }

    @JavascriptInterface
    fun getDeviceInfo(): String {
        val info = JSONObject().apply {
            put("model", Build.MODEL)
            put("manufacturer", Build.MANUFACTURER)
            put("androidVersion", Build.VERSION.RELEASE)
            put("sdkInt", Build.VERSION.SDK_INT)
            put("locale", Locale.getDefault().toLanguageTag())
        }
        return info.toString()
    }

    private fun sanitize(input: String, maxLength: Int): String {
        return input.take(maxLength).replace(Regex("[<>\"'&;]"), "").trim()
    }
}

class DataSyncHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val widgetDao: WidgetDataDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    fun handle(data: JSONObject) {
        scope.launch {
            widgetDao.upsert(WidgetDataEntity(
                title = data.optString("title"),
                subtitle = data.optString("subtitle"),
                lastUpdated = System.currentTimeMillis()
            ))
            HybridWidget.updateAll(context)
        }
    }
}
