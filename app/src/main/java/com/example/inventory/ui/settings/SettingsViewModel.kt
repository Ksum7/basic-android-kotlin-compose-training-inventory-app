package com.example.inventory.ui.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.inventory.data.SettingsRepository

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    private val _settings = mutableStateOf(settingsRepository.getSettings())
    val settings: State<SettingsRepository.Settings> = _settings

    fun updateHideSensitive(value: Boolean) {
        settingsRepository.updateHideSensitive(value)
        _settings.value = _settings.value.copy(hideSensitive = value)
    }

    fun updateAllowShare(value: Boolean) {
        settingsRepository.updateAllowShare(value)
        _settings.value = _settings.value.copy(allowShare = value)
    }

    fun updateUseDefaultQuantity(value: Boolean) {
        settingsRepository.updateUseDefaultQuantity(value)
        _settings.value = _settings.value.copy(useDefaultQuantity = value)
    }

    fun updateDefaultQuantity(value: Int) {
        settingsRepository.updateDefaultQuantity(value)
        _settings.value = _settings.value.copy(defaultQuantity = value)
    }
}