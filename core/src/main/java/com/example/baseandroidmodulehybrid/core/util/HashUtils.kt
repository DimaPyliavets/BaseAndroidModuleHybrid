package com.example.baseandroidmodulehybrid.core.util

import java.io.File
import java.security.MessageDigest

/**
 * HashUtils — утиліти для перевірки цілісності файлів.
 *
 * Використовується Updater модулем для валідації завантажених zip-бандлів.
 * ⚠️ НЕ змінюй алгоритм (SHA-256) без синхронного оновлення server-side version.json
 */
object HashUtils {

    /**
     * Обчислює SHA-256 хеш файлу та повертає hex-рядок.
     * Для великих файлів використовує буферизоване читання.
     */
    fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(DEFAULT_BUFFER_SIZE).use { stream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Перевіряє чи збігається хеш файлу з очікуваним.
     *
     * @param file     файл для перевірки
     * @param expected очікуваний SHA-256 hex (з version.json)
     * @return true якщо хеші збігаються
     */
    fun verifyFile(file: File, expected: String): Boolean {
        val actual = sha256Hex(file)
        // ⚠️ Порівняння без урахування регістру — GitHub може повертати upper/lowercase
        return actual.equals(expected.trim(), ignoreCase = true)
    }

    private const val DEFAULT_BUFFER_SIZE = 8192
}
