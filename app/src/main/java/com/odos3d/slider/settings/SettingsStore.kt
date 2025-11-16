package com.odos3d.slider.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "slider_settings")

class SettingsStore private constructor(private val context: Context) {
    companion object {
        private var instance: SettingsStore? = null
        fun get(ctx: Context): SettingsStore = instance ?: SettingsStore(ctx.applicationContext).also { instance = it }

        val KEY_DEVICE_NAME = stringPreferencesKey("device_name")
        val KEY_DEVICE_MAC = stringPreferencesKey("device_mac")
        val KEY_POLL_HZ = intPreferencesKey("poll_hz")
        val KEY_DEFAULT_STEP = floatPreferencesKey("default_step")
        val KEY_DEFAULT_FEED = intPreferencesKey("default_feed")
    }

    val deviceName: Flow<String> = context.dataStore.data.map { it[KEY_DEVICE_NAME] ?: "" }
    val deviceMac: Flow<String> = context.dataStore.data.map { it[KEY_DEVICE_MAC] ?: "" }
    val pollHz: Flow<Int> = context.dataStore.data.map { (it[KEY_POLL_HZ] ?: 4).coerceAtLeast(1) }
    val defaultStep: Flow<Float> = context.dataStore.data.map { (it[KEY_DEFAULT_STEP] ?: 1f).coerceAtLeast(0.001f) }
    val defaultFeed: Flow<Int> = context.dataStore.data.map { (it[KEY_DEFAULT_FEED] ?: 300).coerceIn(1, 1500) }

    suspend fun saveDevice(name: String, mac: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEVICE_NAME] = name
            prefs[KEY_DEVICE_MAC] = mac
        }
    }

    suspend fun savePollHz(hz: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_POLL_HZ] = hz.coerceAtLeast(1)
        }
    }

    suspend fun saveDefaults(step: Float, feed: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_STEP] = step.coerceAtLeast(0.001f)
            prefs[KEY_DEFAULT_FEED] = feed.coerceIn(1, 1500)
        }
    }
}
