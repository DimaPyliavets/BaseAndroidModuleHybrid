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
import org.json.JSONObject
import java.io.File
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
                    val errorMsg = "HTTP ${response.code}: Failed to download version.json"
                    Log.e(TAG, errorMsg)
                    error(errorMsg)
                }
                val body = response.body?.string()
                Log.d(TAG, "Received JSON: $body")
                if (body.isNullOrBlank()) {
                    error("Empty response from server")
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
        val localVersion = prefs.getString(AppConfig.PREFS_KEY_VERSION, null)
        Log.d(TAG, "Version check: Local=$localVersion, Remote=$remoteVersion")
        return localVersion != remoteVersion
    }

    suspend fun downloadBundle(
        versionInfo: VersionInfo,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${AppConfig.GITHUB_BUNDLE_BASE_URL}${versionInfo.version}/${versionInfo.bundleFileName}"
            Log.d(TAG, "Downloading bundle from: $url")
            
            val request = Request.Builder().url(url).build()
            val tempFile = File(context.cacheDir, "bundle_download.zip")

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}: Download failed")

                val body = response.body ?: error("Empty response body")
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
        }.onFailure {
            Log.e(TAG, "Bundle download failed", it)
        }
    }

    suspend fun verifyAndExtract(
        zipFile: File,
        expectedHash: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "Verifying and extracting bundle...")
            if (!HashUtils.verifyFile(zipFile, expectedHash)) {
                zipFile.delete()
                error("SHA-256 verification failed")
            }

            val bundleDir = File(context.filesDir, AppConfig.BUNDLE_DIR_NAME)
            if (bundleDir.exists()) bundleDir.deleteRecursively()
            bundleDir.mkdirs()

            ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outFile = File(bundleDir, entry.name)
                    if (!outFile.canonicalPath.startsWith(bundleDir.canonicalPath)) {
                        error("Zip Slip detected: ${entry.name}")
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
            Log.d(TAG, "Bundle extracted successfully")
            Unit
        }.onFailure {
            Log.e(TAG, "Verification or extraction failed", it)
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
