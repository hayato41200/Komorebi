package com.beeregg2001.komorebi.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        val KONOMI_IP = stringPreferencesKey("konomi_ip")
        val KONOMI_PORT = stringPreferencesKey("konomi_port")
        val MIRAKURUN_IP = stringPreferencesKey("mirakurun_ip")
        val MIRAKURUN_PORT = stringPreferencesKey("mirakurun_port")

        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val THEME_CUSTOM_ACCENT = stringPreferencesKey("theme_custom_accent")

        val ENABLE_JIKKYO_OVERLAY = booleanPreferencesKey("enable_jikkyo_overlay")
        val JIKKYO_DENSITY = intPreferencesKey("jikkyo_density")
        val JIKKYO_OPACITY = floatPreferencesKey("jikkyo_opacity")
        val JIKKYO_POSITION = stringPreferencesKey("jikkyo_position")

        val ENABLE_EXTERNAL_LINKAGE = booleanPreferencesKey("enable_external_linkage")
    }

    val konomiIp: Flow<String> = context.dataStore.data.map { it[KONOMI_IP] ?: "https://192-168-xxx-xxx.local.konomi.tv" }
    val konomiPort: Flow<String> = context.dataStore.data.map { it[KONOMI_PORT] ?: "7000" }
    val mirakurunIp: Flow<String> = context.dataStore.data.map { it[MIRAKURUN_IP] ?: "" }
    val mirakurunPort: Flow<String> = context.dataStore.data.map { it[MIRAKURUN_PORT] ?: "" }
    val liveChannelKeyMode: Flow<String> = context.dataStore.data.map { it[LIVE_CHANNEL_KEY_MODE] ?: "channel" }

    val themePreset: Flow<String> = context.dataStore.data.map { it[THEME_PRESET] ?: "Dark" }
    val themeCustomAccent: Flow<String> = context.dataStore.data.map { it[THEME_CUSTOM_ACCENT] ?: "#7C4DFF" }

    val enableJikkyoOverlay: Flow<Boolean> = context.dataStore.data.map { it[ENABLE_JIKKYO_OVERLAY] ?: true }
    val jikkyoDensity: Flow<Int> = context.dataStore.data.map { (it[JIKKYO_DENSITY] ?: 2).coerceIn(1, 3) }
    val jikkyoOpacity: Flow<Float> = context.dataStore.data.map { (it[JIKKYO_OPACITY] ?: 0.65f).coerceIn(0.2f, 1f) }
    val jikkyoPosition: Flow<String> = context.dataStore.data.map { it[JIKKYO_POSITION] ?: "Top" }

    val enableExternalLinkage: Flow<Boolean> = context.dataStore.data.map { it[ENABLE_EXTERNAL_LINKAGE] ?: true }

    val isInitialized: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs.contains(KONOMI_IP) || prefs.contains(MIRAKURUN_IP)
    }

    suspend fun saveString(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { settings -> settings[key] = value }
    }

    suspend fun saveBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { settings -> settings[key] = value }
    }

    suspend fun saveInt(key: Preferences.Key<Int>, value: Int) {
        context.dataStore.edit { settings -> settings[key] = value }
    }

    suspend fun saveFloat(key: Preferences.Key<Float>, value: Float) {
        context.dataStore.edit { settings -> settings[key] = value }
    }

    suspend fun getBaseUrl(): String {
        val prefs = context.dataStore.data.first()
        var ip = prefs[KONOMI_IP] ?: "https://192-168-11-100.local.konomi.tv"
        val port = prefs[KONOMI_PORT] ?: "7000"

        if (!ip.startsWith("http://") && !ip.startsWith("https://")) {
            ip = "https://$ip"
        }

        val base = ip.removeSuffix("/")
        return "$base:$port/"
    }
}
