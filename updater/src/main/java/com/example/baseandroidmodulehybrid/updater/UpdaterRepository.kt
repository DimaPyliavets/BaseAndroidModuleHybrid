package com.example.baseandroidmodulehybrid.updater

import android.content.Context
import android.content.SharedPreferences
import com.example.baseandroidmodulehybrid.core.model.AppConfig
import com.example.baseandroidmodulehybrid.core.model.VersionInfo
import com.example.baseandroidmodulehybrid.core.util.HashUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UpdaterRepository — логіка перевірки та застосування оновлень.
 */
@Singleton
class UpdaterRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)
    }

    suspend fun fetchVersionInfo(): Result<VersionInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(AppConfig.GITHUB_VERSION_URL)
                .cacheControl(okhttp3.CacheControl.FORCE_NETWORK)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("HTTP ${response.code}: не вдалося завантажити version.json")
                }
                val body = response.body?.string()
                    ?: error("Порожня відповідь від сервера")
                parseVersionInfo(body)
            }
        }
    }

    private fun parseVersionInfo(json: String): VersionInfo {
        val obj = JSONObject(json)
        return VersionInfo(
            version        = obj.getString("version"),
            bundleFileName = obj.getString("bundleFileName"),
            sha256         = obj.getString("sha256"),
            releaseNotes   = obj.optString("releaseNotes", "")
        )
    }

    fun isUpdateRequired(remoteVersion: String): Boolean {
        val localVersion = prefs.getString(AppConfig.PREFS_KEY_VERSION, null)
        return localVersion != remoteVersion
    }

    suspend fun downloadBundle(
        versionInfo: VersionInfo,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${AppConfig.GITHUB_BUNDLE_BASE_URL}${versionInfo.version}/${versionInfo.bundleFileName}"
            val request = Request.Builder().url(url).build()
            val tempFile = File(context.cacheDir, "bundle_download.zip")

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}: помилка завантаження")

                val body = response.body ?: error("Порожнє тіло відповіді")
                val contentLength = body.contentLength()
                var bytesRead = 0L

                tempFile.outputStream().buffered().use { out ->
                    body.byteStream().buffered().use { input ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            out.write(buffer, 0, read)
                            bytesRead += read
                            if (contentLength > 0) {
                                onProgress((bytesRead * 100 / contentLength).toInt())
                            }
                        }
                    }
                }
            }
            tempFile
        }
    }

    suspend fun verifyAndExtract(
        zipFile: File,
        expectedHash: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!HashUtils.verifyFile(zipFile, expectedHash)) {
                zipFile.delete()
                error("SHA-256 перевірка не пройдена")
            }

            val bundleDir = File(context.filesDir, AppConfig.BUNDLE_DIR_NAME)
            if (bundleDir.exists()) bundleDir.deleteRecursively()
            bundleDir.mkdirs()

            ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outFile = File(bundleDir, entry.name)
                    if (!outFile.canonicalPath.startsWith(bundleDir.canonicalPath)) {
                        error("Zip Slip виявлено: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().buffered().use { out ->
                            zis.copyTo(out)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            zipFile.delete()
            Unit
        }
    }

    fun saveCurrentVersion(version: String) {
        prefs.edit().putString(AppConfig.PREFS_KEY_VERSION, version).apply()
    }

    fun getLocalBundlePath(): String? {
        val entry = File(context.filesDir, "${AppConfig.BUNDLE_DIR_NAME}/${AppConfig.WEB_ENTRY_POINT}")
        return if (entry.exists()) entry.absolutePath else null
    }
}
