package com.beeregg2001.komorebi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeregg2001.komorebi.data.SettingsRepository
import com.beeregg2001.komorebi.data.repository.KonomiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val konomiRepository: KonomiRepository
) : ViewModel() {

    val mirakurunIp: StateFlow<String> = settingsRepository.mirakurunIp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "192.168.11.100")

    val mirakurunPort: StateFlow<String> = settingsRepository.mirakurunPort
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "40772")

    val konomiIp: StateFlow<String> = settingsRepository.konomiIp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "https://192-168-11-100.local.konomi.tv")

    val konomiPort: StateFlow<String> = settingsRepository.konomiPort
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "7000")

    val themePreset: StateFlow<String> = settingsRepository.themePreset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Dark")

    val themeCustomAccent: StateFlow<String> = settingsRepository.themeCustomAccent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#7C4DFF")

    val enableJikkyoOverlay: StateFlow<Boolean> = settingsRepository.enableJikkyoOverlay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val jikkyoDensity: StateFlow<Int> = settingsRepository.jikkyoDensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val jikkyoOpacity: StateFlow<Float> = settingsRepository.jikkyoOpacity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.65f)

    val jikkyoPosition: StateFlow<String> = settingsRepository.jikkyoPosition
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Top")

    val enableExternalLinkage: StateFlow<Boolean> = settingsRepository.enableExternalLinkage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val currentUser = konomiRepository.currentUser
    val isUserLoading = konomiRepository.isUserLoading

    val isJikkyoSupported: StateFlow<Boolean> = konomiRepository.currentUser
        .map { it?.capabilities?.jikkyo ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isLinkageSupported: StateFlow<Boolean> = konomiRepository.currentUser
        .map { it?.capabilities?.external_account_linkage ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isSettingsInitialized: StateFlow<Boolean> = settingsRepository.isInitialized
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        refreshUserState()
    }

    fun refreshUserState() {
        viewModelScope.launch { konomiRepository.refreshUser() }
    }

    fun saveString(key: androidx.datastore.preferences.core.Preferences.Key<String>, value: String) {
        viewModelScope.launch { settingsRepository.saveString(key, value) }
    }

    fun setThemePreset(value: String) {
        viewModelScope.launch { settingsRepository.saveString(SettingsRepository.THEME_PRESET, value) }
    }

    fun setThemeAccent(value: String) {
        viewModelScope.launch { settingsRepository.saveString(SettingsRepository.THEME_CUSTOM_ACCENT, value) }
    }

    fun setJikkyoEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.saveBoolean(SettingsRepository.ENABLE_JIKKYO_OVERLAY, value) }
    }

    fun setJikkyoDensity(value: Int) {
        viewModelScope.launch { settingsRepository.saveInt(SettingsRepository.JIKKYO_DENSITY, value.coerceIn(1, 3)) }
    }

    fun setJikkyoOpacity(value: Float) {
        viewModelScope.launch { settingsRepository.saveFloat(SettingsRepository.JIKKYO_OPACITY, value.coerceIn(0.2f, 1f)) }
    }

    fun setJikkyoPosition(value: String) {
        viewModelScope.launch { settingsRepository.saveString(SettingsRepository.JIKKYO_POSITION, value) }
    }

    fun setExternalLinkageEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.saveBoolean(SettingsRepository.ENABLE_EXTERNAL_LINKAGE, value) }
    }
}
