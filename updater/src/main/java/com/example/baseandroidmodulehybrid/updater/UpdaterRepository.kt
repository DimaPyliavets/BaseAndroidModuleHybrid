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
            // Додаємо ігнорування кешу, щоб завжди отримувати найсвіжіший version.json
            val request = Request.Builder()
                .url(AppConfig.GITHUB_VERSION_URL)
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .cacheControl(okhttp3.CacheControl.FORCE_NETWORK)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = "HTTP ${response.code}: Failed to download version.json"
                    Log.e(TAG, errorMsg)
                    throw IOException(errorMsg)
                }
                val body = response.body?.string()
                Log.d(TAG, "Received JSON: $body")
                if (body.isNullOrBlank()) {
                    throw IOException("Empty response from server")
                }
                parseVersionInfo(body)
            }
        }.onFailure {
            Log.e(TAG, "Failed to fetch version info", it)
        }
    }

    private fun parseVersionInfo(json: String): VersionInfo {
        return try {
            val obj = JSONObject(json)
            VersionInfo(
                version        = obj.getString("version"),
                bundleFileName = obj.getString("bundleFileName"),
                sha256         = obj.getString("sha256"),
                releaseNotes   = obj.optString("releaseNotes", "")
            )
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing error", e)
            throw e
        }
    }

    fun isUpdateRequired(remoteVersion: String): Boolean {
        val localVersion = getCurrentVersion()
        Log.d(TAG, "Version check: Local=$localVersion, Remote=$remoteVersion")
        return localVersion != remoteVersion
    }

    fun getCurrentVersion(): String? {
        return prefs.getString(AppConfig.PREFS_KEY_VERSION, null)
    }

    suspend fun downloadBundle(
        versionInfo: VersionInfo,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${AppConfig.GITHUB_BUNDLE_BASE_URL}${versionInfo.bundleFileName}"
            Log.d(TAG, "Downloading bundle from: $url")
            
            val request = Request.Builder()
                .url(url)
                .header("Cache-Control", "no-cache")
                .build()
                
            val tempFile = File(context.cacheDir, "bundle_download.zip")

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: Download failed for $url")
                }
                saveResponseToFile(response, tempFile, onProgress)
            }
            tempFile
        }.onFailure {
            Log.e(TAG, "Bundle download failed", it)
        }
    }

    private fun saveResponseToFile(response: Response, file: File, onProgress: (Int) -> Unit) {
        response.use { resp ->
            val body = resp.body ?: throw IOException("Empty response body")
            val contentLength = body.contentLength()
            var bytesRead = 0L

            file.outputStream().buffered().use { out ->
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
                    zipFile.delete()
                    throw IOException("SHA-256 verification failed")
                }
            }

            val bundleDir = File(context.filesDir, AppConfig.BUNDLE_DIR_NAME)
            if (bundleDir.exists()) bundleDir.deleteRecursively()
            bundleDir.mkdirs()

            val totalEntries = countZipEntries(zipFile)
            var extractedEntries = 0

            ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outFile = File(bundleDir, entry.name)
                    if (!outFile.canonicalPath.startsWith(bundleDir.canonicalPath)) {
                        throw IOException("Zip Slip detected: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().buffered().use { out ->
                            zis.copyTo(out)
                        }
                    }
                    
                    extractedEntries++
                    if (totalEntries > 0) {
                        onProgress((extractedEntries * 100 / totalEntries))
                    }
                    
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            zipFile.delete()

            // Знаходимо точку входу
            val entryPoint = findEntryPoint(bundleDir) ?: throw IOException("index.html not found")
            val relativePath = entryPoint.relativeTo(bundleDir).path
            prefs.edit().putString("entry_point_path", relativePath).apply()

            Log.d(TAG, "Bundle extracted successfully to $relativePath")
            Unit
        }.onFailure {
            Log.e(TAG, "Verification or extraction failed", it)
        }
    }

    private fun findEntryPoint(root: File): File? {
        val priorityList = listOf("index.html", "main.html", "home.html")
        for (name in priorityList) {
            val found = root.walkTopDown().find { it.name.equals(name, ignoreCase = true) }
            if (found != null) return found
        }
        return root.walkTopDown().find { it.extension.lowercase() == "html" }
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
        } catch (e: Exception) {
            0
        }
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
