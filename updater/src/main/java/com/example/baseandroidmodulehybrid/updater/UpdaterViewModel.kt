package com.example.baseandroidmodulehybrid.updater

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UpdateState — стани процесу оновлення.
 */
sealed class UpdateState {
    object Idle        : UpdateState()
    object Checking    : UpdateState()
    object UpToDate    : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data class Extracting(val progress: Int)  : UpdateState()
    object Applying    : UpdateState()
    object Success     : UpdateState()
    data class Error(val message: String) : UpdateState()
}

/**
 * UpdaterViewModel — управляє станом оновлення для UI.
 */
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

    /**
     * Запускає повний цикл перевірки та застосування оновлення.
     */
    fun checkAndUpdate() {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking

            val versionResult = repository.fetchVersionInfo()
            if (versionResult.isFailure) {
                _updateState.value = UpdateState.Error(
                    "Не вдалося перевірити оновлення: ${versionResult.exceptionOrNull()?.message}"
                )
                return@launch
            }

            val versionInfo = versionResult.getOrThrow()

            if (!repository.isUpdateRequired(versionInfo.version)) {
                _updateState.value = UpdateState.UpToDate
                return@launch
            }

            _updateState.value = UpdateState.Downloading(0)
            val downloadResult = repository.downloadBundle(versionInfo) { progress ->
                _updateState.value = UpdateState.Downloading(progress)
            }
            
            if (downloadResult.isFailure) {
                _updateState.value = UpdateState.Error(
                    "Помилка завантаження: ${downloadResult.exceptionOrNull()?.message}"
                )
                return@launch
            }

            _updateState.value = UpdateState.Extracting(0)
            val extractResult = repository.verifyAndExtract(
                zipFile      = downloadResult.getOrThrow(),
                expectedHash = versionInfo.sha256,
                onProgress = { progress ->
                    _updateState.value = UpdateState.Extracting(progress)
                }
            )

            if (extractResult.isFailure) {
                _updateState.value = UpdateState.Error(
                    "Помилка перевірки/розпакування: ${extractResult.exceptionOrNull()?.message}"
                )
                return@launch
            }

            repository.saveCurrentVersion(versionInfo.version)
            
            // Оновлюємо версію та шлях, що змусить UI (MainActivity) перемалюватися
            _currentVersion.value = versionInfo.version
            _installedBundlePath.value = repository.getLocalBundlePath()
            _updateState.value = UpdateState.Success
        }
    }
}
