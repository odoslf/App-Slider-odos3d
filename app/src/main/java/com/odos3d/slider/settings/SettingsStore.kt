package com.odos3d.slider.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
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

        val KEY_AXIS_DEFAULT = stringPreferencesKey("axis_default")
        val KEY_MAX_TRAVEL = floatPreferencesKey("max_travel_mm")
        val KEY_MAX_FEED = intPreferencesKey("max_feed")
        val KEY_OFFLINE = booleanPreferencesKey("offline_mode")
        val KEY_AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val KEY_RECONNECT_SECS = intPreferencesKey("reconnect_secs")
    }

    val deviceName: Flow<String> = context.dataStore.data.map { it[KEY_DEVICE_NAME] ?: "" }
    val deviceMac: Flow<String> = context.dataStore.data.map { it[KEY_DEVICE_MAC] ?: "" }
    val pollHz: Flow<Int> = context.dataStore.data.map { (it[KEY_POLL_HZ] ?: 4).coerceAtLeast(1) }
    val defaultStep: Flow<Float> = context.dataStore.data.map { (it[KEY_DEFAULT_STEP] ?: 1f).coerceAtLeast(0.001f) }
    val defaultFeed: Flow<Int> = context.dataStore.data.map {
        val maxFeed = it[KEY_MAX_FEED] ?: 1500
        (it[KEY_DEFAULT_FEED] ?: 300).coerceIn(1, maxFeed)
    }
    val axisDefault: Flow<String> = context.dataStore.data.map { (it[KEY_AXIS_DEFAULT] ?: "X").uppercase() }
    val maxTravelMm: Flow<Float> = context.dataStore.data.map { (it[KEY_MAX_TRAVEL] ?: 400f).coerceAtLeast(0.1f) }
    val maxFeed: Flow<Int> = context.dataStore.data.map { (it[KEY_MAX_FEED] ?: 1500).coerceAtLeast(1) }
    val offlineMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_OFFLINE] ?: false }
    val autoReconnect: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_RECONNECT] ?: true }
    val reconnectSecs: Flow<Int> = context.dataStore.data.map { (it[KEY_RECONNECT_SECS] ?: 5).coerceIn(2, 30) }

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
            val maxFeed = prefs[KEY_MAX_FEED] ?: 1500
            prefs[KEY_DEFAULT_STEP] = step.coerceAtLeast(0.001f)
            prefs[KEY_DEFAULT_FEED] = feed.coerceIn(1, maxFeed)
        }
    }

    suspend fun saveAxis(axis: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AXIS_DEFAULT] = axis.uppercase().take(1)
        }
    }

    suspend fun saveMaxTravelMm(value: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MAX_TRAVEL] = value.coerceAtLeast(0.1f)
        }
    }

    suspend fun saveMaxFeed(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MAX_FEED] = value.coerceAtLeast(1)
            val defFeed = prefs[KEY_DEFAULT_FEED] ?: 300
            prefs[KEY_DEFAULT_FEED] = defFeed.coerceIn(1, prefs[KEY_MAX_FEED] ?: 1)
        }
    }

    suspend fun setOfflineMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OFFLINE] = enabled
        }
    }

    suspend fun saveReconnect(enabled: Boolean, seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_RECONNECT] = enabled
            prefs[KEY_RECONNECT_SECS] = seconds.coerceIn(2, 30)
        }
    }
}
