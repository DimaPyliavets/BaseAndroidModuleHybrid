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
 * ⚠️ ЗМІНИТИ: додай стани якщо потрібні більш деталізовані кроки
 */
sealed class UpdateState {
    object Idle        : UpdateState()        // Початковий стан
    object Checking    : UpdateState()        // Перевірка version.json
    object UpToDate    : UpdateState()        // Оновлень немає
    object Downloading : UpdateState()        // Завантаження бандлу
    object Extracting  : UpdateState()        // Розпакування
    object Applying    : UpdateState()        // Застосування (перезавантаження WebView)
    object Success     : UpdateState()        // Успішно
    data class Error(val message: String) : UpdateState() // Помилка
}

/**
 * UpdaterViewModel — управляє станом оновлення для UI.
 *
 * ⚠️ ЗМІНИТИ:
 *  - Додай логіку примусового оновлення (force update) якщо потрібно
 *  - Підключи WorkManager для планових фонових перевірок
 */
@HiltViewModel
class UpdaterViewModel @Inject constructor(
    private val repository: UpdaterRepository
) : ViewModel() {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    /**
     * Запускає повний цикл перевірки та застосування оновлення.
     * Викликається з MainActivity.onCreate().
     *
     * ⚠️ ЗМІНИТИ: додай параметр force: Boolean = false для примусового оновлення
     */
    fun checkAndUpdate() {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking

            // ─── Крок 1: Завантажити version.json ──────────────────────
            val versionResult = repository.fetchVersionInfo()
            if (versionResult.isFailure) {
                // ⚠️ Тут можна вирішити: fallback на локальний бандл або показ помилки
                _updateState.value = UpdateState.Error(
                    "Не вдалося перевірити оновлення: ${versionResult.exceptionOrNull()?.message}"
                )
                return@launch
            }

            val versionInfo = versionResult.getOrThrow()

            // ─── Крок 2: Порівняти версії ──────────────────────────────
            if (!repository.isUpdateRequired(versionInfo.version)) {
                _updateState.value = UpdateState.UpToDate
                return@launch
            }

            // ─── Крок 3: Завантажити бандл ─────────────────────────────
            _updateState.value = UpdateState.Downloading
            val downloadResult = repository.downloadBundle(versionInfo)
            if (downloadResult.isFailure) {
                _updateState.value = UpdateState.Error(
                    "Помилка завантаження: ${downloadResult.exceptionOrNull()?.message}"
                )
                return@launch
            }

            // ─── Крок 4: Верифікація + розпакування ────────────────────
            _updateState.value = UpdateState.Extracting
            val extractResult = repository.verifyAndExtract(
                zipFile      = downloadResult.getOrThrow(),
                expectedHash = versionInfo.sha256
            )
            if (extractResult.isFailure) {
                _updateState.value = UpdateState.Error(
                    "Помилка перевірки/розпакування: ${extractResult.exceptionOrNull()?.message}"
                )
                return@launch
            }

            // ─── Крок 5: Зберегти нову версію ──────────────────────────
            repository.saveCurrentVersion(versionInfo.version)

            _updateState.value = UpdateState.Success
            // ⚠️ ЗМІНИТИ: після Success — тригернути перезавантаження WebView
            // Можна через SharedFlow або EventBus (залежить від архітектури)
        }
    }
}
