package com.example.baseandroidmodulehybrid.bridge

import android.webkit.JavascriptInterface
import com.example.baseandroidmodulehybrid.core.model.AppConfig
import com.example.baseandroidmodulehybrid.notifications.NotificationHelper
import dagger.hilt.android.scopes.ActivityRetainedScoped
import org.json.JSONObject
import java.util.Locale
import javax.inject.Inject

/**
 * NativeBridge — безпечний JavascriptInterface між JS та Kotlin.
 *
 * Виклик з JavaScript:
 *   window.Android.showNotification("Заголовок", "Текст")
 *   window.Android.syncData('{"key":"value"}')
 *   const locale = window.Android.getDeviceLocale()
 *
 * ⚠️ ЗМІНИТИ:
 *  - Додай нові методи за потребою (@JavascriptInterface)
 *  - Кожен метод ПОВИНЕН валідувати вхідні дані перед обробкою!
 *  - Усі методи викликаються в JavaBridge thread — для UI потрібен Handler/Coroutine
 *
 * ⚠️ БЕЗПЕКА:
 *  - Ніколи не передавай Context, Activity або системні об'єкти напряму в JS
 *  - Не виконуй IO операції в @JavascriptInterface методах синхронно
 *  - Завжди обрізай та валідуй рядки від JS
 */
@ActivityRetainedScoped
class NativeBridge @Inject constructor(
    private val notificationHelper: NotificationHelper,
    private val dataSyncHandler: DataSyncHandler
) {

    /**
     * Показати локальне сповіщення.
     *
     * JS виклик: window.Android.showNotification("Привіт", "Новий запис!")
     *
     * ⚠️ ЗМІНИТИ: додай channelId параметр якщо різні типи сповіщень
     */
    @JavascriptInterface
    fun showNotification(title: String, message: String) {
        // ─── Валідація вхідних даних ──────────────────────────────────
        val safeTitle = sanitize(title, AppConfig.MAX_NOTIFICATION_TITLE_LEN)
        val safeMessage = sanitize(message, AppConfig.MAX_NOTIFICATION_BODY_LEN)

        if (safeTitle.isBlank() || safeMessage.isBlank()) return // Ігноруємо порожні

        notificationHelper.show(safeTitle, safeMessage)
    }

    /**
     * Синхронізація даних від веб-частини.
     * Веб-частина передає JSON з даними для збереження/відображення у Widget.
     *
     * JS виклик: window.Android.syncData('{"title":"Q3","value":"84%"}')
     *
     * ⚠️ ЗМІНИТИ: структуру JSON та логіку в dataSyncHandler відповідно до
     * твоїх даних для Widget або іншого бізнес-логіки.
     */
    @JavascriptInterface
    fun syncData(json: String) {
        // ─── Перевірка розміру payload ────────────────────────────────
        if (json.toByteArray().size > AppConfig.MAX_JSON_PAYLOAD_BYTES) {
            // ⚠️ Логуй або ігноруй — не кидай exception в JS thread
            return
        }

        // ─── Валідація JSON ───────────────────────────────────────────
        val jsonObject = try {
            JSONObject(json)
        } catch (e: Exception) {
            return // Невалідний JSON — ігноруємо
        }

        // ⚠️ ЗМІНИТИ: список дозволених ключів у JSON
        val allowedKeys = setOf("title", "subtitle", "value", "timestamp")
        val sanitized = JSONObject()
        for (key in allowedKeys) {
            if (jsonObject.has(key)) {
                sanitized.put(key, sanitize(jsonObject.optString(key), 200))
            }
        }

        dataSyncHandler.handle(sanitized)
    }

    /**
     * Повертає локаль пристрою у форматі BCP 47 (наприклад: "uk-UA", "en-US").
     *
     * JS виклик: const locale = window.Android.getDeviceLocale()
     *
     * ⚠️ НЕ повертай чутливі системні дані через цей метод!
     */
    @JavascriptInterface
    fun getDeviceLocale(): String {
        return Locale.getDefault().toLanguageTag() // Наприклад: "uk-UA"
    }

    /**
     * ⚠️ ОПЦІОНАЛЬНО: метод для отримання версії нативного додатка з JS.
     * JS: const version = window.Android.getAppVersion()
     */
    @JavascriptInterface
    fun getAppVersion(): String {
        // ⚠️ ЗМІНИТИ: повертай реальну версію
        return "1.0.0"
    }

    // ─── Приватні утиліти безпеки ────────────────────────────────────

    /**
     * Обрізає рядок до максимальної довжини та видаляє небезпечні символи.
     *
     * ⚠️ ЗМІНИТИ: розшир regex якщо потрібна більш сувора фільтрація
     */
    private fun sanitize(input: String, maxLength: Int): String {
        return input
            .take(maxLength)
            .replace(Regex("[<>\"'&;]"), "") // Базова XSS-превенція
            .trim()
    }
}

/**
 * DataSyncHandler — обробник даних що надходять з JS через syncData().
 *
 * ⚠️ ЗМІНИТИ: реалізуй логіку збереження у Room та оновлення Widget
 */
class DataSyncHandler @Inject constructor(
    // ⚠️ ЗМІНИТИ: inject WidgetRepository або Room DAO тут
) {
    fun handle(data: JSONObject) {
        // TODO: зберегти дані у Room
        // TODO: викликати HybridWidget.update(context)
        // Приклад:
        // widgetRepository.update(
        //     title    = data.optString("title"),
        //     subtitle = data.optString("subtitle")
        // )
    }
}
