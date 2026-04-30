package com.example.baseandroidmodulehybrid.updater

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baseandroidmodulehybrid.core.model.VersionInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class UpdateState {
    object Idle        : UpdateState()
    object Checking    : UpdateState()
    object UpToDate    : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data class ReadyToInstall(val versionInfo: VersionInfo, val zipFile: File) : UpdateState()
    data class Extracting(val progress: Int)  : UpdateState()
    object Applying    : UpdateState()
    object Success     : UpdateState()
    data class Error(val message: String) : UpdateState()
}

@HiltViewModel
class UpdaterViewModel @Inject constructor(
    private val repository: UpdaterRepository
) : ViewModel() {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _installedBundlePath = MutableStateFlow<String?>(repository.getLocalBundlePath())
    val installedBundlePath: StateFlow<String?> = _installedBundlePath.asStateFlow()

    private val _currentVersion = MutableStateFlow<String?>(repository.getCurrentVersion())
    val currentVersion: StateFlow<String?> = _currentVersion.asStateFlow()

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }

    fun checkAndUpdate(force: Boolean = false) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking

            val versionResult = repository.fetchVersionInfo()
            if (versionResult.isFailure) {
                _updateState.value = UpdateState.Error("Помилка зв'язку")
                return@launch
            }

            val versionInfo = versionResult.getOrThrow()

            if (!force && !repository.isUpdateRequired(versionInfo.version)) {
                _updateState.value = UpdateState.UpToDate
                return@launch
            }

            _updateState.value = UpdateState.Downloading(0)
            val downloadResult = repository.downloadBundle(versionInfo) { progress ->
                _updateState.value = UpdateState.Downloading(progress)
            }
            
            if (downloadResult.isFailure) {
                _updateState.value = UpdateState.Error("Завантаження не вдалося")
                return@launch
            }

            _updateState.value = UpdateState.ReadyToInstall(versionInfo, downloadResult.getOrThrow())
        }
    }

    fun completeInstallation(versionInfo: VersionInfo, zipFile: File) {
        viewModelScope.launch {
            // Змінюємо стан на Extracting, щоб закрити діалог у MainActivity
            _updateState.value = UpdateState.Extracting(0)
            
            val extractResult = repository.verifyAndExtract(
                zipFile      = zipFile,
                expectedHash = versionInfo.sha256,
                onProgress = { progress ->
                    _updateState.value = UpdateState.Extracting(progress)
                }
            )

            if (extractResult.isFailure) {
                _updateState.value = UpdateState.Error("Розпакування не вдалося")
                return@launch
            }

            repository.saveCurrentVersion(versionInfo.version)
            
            // Тригеримо оновлення шляху та версії
            val newPath = repository.getLocalBundlePath()
            _currentVersion.value = versionInfo.version
            _installedBundlePath.value = newPath
            
            // Тільки після того як всі дані оновлені — Success
            _updateState.value = UpdateState.Success
        }
    }
}
