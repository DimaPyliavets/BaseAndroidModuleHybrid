package com.example.baseandroidmodulehybrid.updater

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.baseandroidmodulehybrid.core.model.AppConfig
import com.example.baseandroidmodulehybrid.core.model.VersionInfo
import com.example.baseandroidmodulehybrid.core.util.HashUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdaterRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient
) {
    private val TAG = "UpdaterRepository"
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)
    }

    suspend fun fetchVersionInfo(): Result<VersionInfo> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "Fetching version info from: ${AppConfig.GITHUB_VERSION_URL}")
            val request = Request.Builder()
                .url(AppConfig.GITHUB_VERSION_URL)
                .cacheControl(okhttp3.CacheControl.FORCE_NETWORK)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: Failed to download version.json")
                }
                val body = response.body?.string()
                Log.d(TAG, "Received JSON: $body")
                if (body.isNullOrBlank()) throw IOException("Empty response")
                parseVersionInfo(body)
            }
        }.onFailure { Log.e(TAG, "Failed to fetch version info", it) }
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
        val localVersion = getCurrentVersion()
        Log.d(TAG, "Version check: Local=$localVersion, Remote=$remoteVersion")
        return localVersion != remoteVersion
    }

    fun getCurrentVersion(): String? = prefs.getString(AppConfig.PREFS_KEY_VERSION, null)

    suspend fun downloadBundle(
        versionInfo: VersionInfo,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${AppConfig.GITHUB_BUNDLE_BASE_URL}${versionInfo.bundleFileName}"
            Log.d(TAG, "Downloading bundle from: $url")
            
            val request = Request.Builder().url(url).build()
            val tempFile = File(context.cacheDir, "bundle_download.zip")

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                saveResponseToFile(response, tempFile, onProgress)
            }
            tempFile
        }.onFailure { Log.e(TAG, "Bundle download failed", it) }
    }

    private fun saveResponseToFile(response: Response, file: File, onProgress: (Int) -> Unit) {
        val body = response.body ?: throw IOException("Empty body")
        val contentLength = body.contentLength()
        var bytesRead = 0L

        file.outputStream().buffered().use { out ->
            body.byteStream().buffered().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                    bytesRead += read
                    if (contentLength > 0) onProgress((bytesRead * 100 / contentLength).toInt())
                }
            }
        }
    }

    suspend fun verifyAndExtract(
        zipFile: File,
        expectedHash: String,
        onProgress: (Int) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "Verifying and extracting bundle...")
            
            if (expectedHash != "0000000000000000000000000000000000000000000000000000000000000000") {
                if (!HashUtils.verifyFile(zipFile, expectedHash)) {
                    throw IOException("SHA-256 verification failed")
                }
            }

            val bundleDir = File(context.filesDir, AppConfig.BUNDLE_DIR_NAME)
            bundleDir.deleteRecursively()
            bundleDir.mkdirs()

            val totalEntries = countZipEntries(zipFile)
            var extracted = 0

            ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outFile = File(bundleDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().buffered().use { out ->
                            zis.copyTo(out)
                        }
                    }
                    extracted++
                    if (totalEntries > 0) onProgress((extracted * 100 / totalEntries))
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            zipFile.delete()
            
            // Логуємо структуру для відладки
            Log.d(TAG, "Files extracted to: ${bundleDir.absolutePath}")
            bundleDir.walkTopDown().forEach { Log.v(TAG, "File: ${it.relativeTo(bundleDir).path}") }

            val entryPoint = findEntryPoint(bundleDir)
            if (entryPoint == null) throw IOException("Не знайдено точку входу (html або js)")
            
            val relativePath = entryPoint.relativeTo(bundleDir).path
            prefs.edit().putString("entry_point_path", relativePath).apply()
            
            Log.d(TAG, "Встановлено! Точка входу: $relativePath")
            Unit
        }.onFailure { Log.e(TAG, "Verify/Extract failed", it) }
    }

    private fun findEntryPoint(root: File): File? {
        // 1. Шукаємо index.html у будь-якій папці (public, dist, корінь)
        val htmlFiles = root.walkTopDown().filter { it.extension.lowercase() == "html" }.toList()
        
        // Пріоритет для index.html
        val indexHtml = htmlFiles.find { it.name.lowercase() == "index.html" }
        if (indexHtml != null) return indexHtml
        
        // Будь-який інший HTML файл
        if (htmlFiles.isNotEmpty()) return htmlFiles.first()

        // 2. Якщо HTML немає, шукаємо index.js або main.js і створюємо обгортку
        val jsFiles = root.walkTopDown().filter { it.extension.lowercase() == "js" }.toList()
        val mainJs = jsFiles.find { it.name.lowercase() == "index.js" || it.name.lowercase() == "main.js" }
        
        if (mainJs != null) {
            return createHtmlWrapper(mainJs, root)
        }

        return null
    }

    private fun createHtmlWrapper(jsFile: File, bundleRoot: File): File {
        val wrapper = File(jsFile.parentFile, "index_generated.html")
        // Шукаємо стилі поруч
        val cssFile = jsFile.parentFile?.listFiles()?.find { it.extension.lowercase() == "css" }
        val cssTag = if (cssFile != null) "<link rel=\"stylesheet\" href=\"${cssFile.name}\">" else ""

        wrapper.writeText("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                $cssTag
            </head>
            <body>
                <div id="root"></div>
                <div id="app"></div>
                <script src="${jsFile.name}"></script>
            </body>
            </html>
        """.trimIndent())
        
        Log.d(TAG, "Generated HTML wrapper for ${jsFile.name}")
        return wrapper
    }

    private fun countZipEntries(zipFile: File): Int {
        return try {
            var count = 0
            ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
                while (zis.nextEntry != null) {
                    count++
                    zis.closeEntry()
                }
            }
            count
        } catch (e: Exception) { 0 }
    }

    fun saveCurrentVersion(version: String) {
        prefs.edit().putString(AppConfig.PREFS_KEY_VERSION, version).apply()
    }

    fun getLocalBundlePath(): String? {
        val bundleDir = File(context.filesDir, AppConfig.BUNDLE_DIR_NAME)
        val relativePath = prefs.getString("entry_point_path", AppConfig.WEB_ENTRY_POINT)
        val entry = File(bundleDir, relativePath ?: AppConfig.WEB_ENTRY_POINT)
        return if (entry.exists()) entry.absolutePath else null
    }
}
