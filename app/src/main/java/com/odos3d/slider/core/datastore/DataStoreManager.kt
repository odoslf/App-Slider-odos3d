package com.odos3d.slider.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

class DataStoreManager(private val ctx: Context) {
  companion object {
    val KEY_BT_ADDRESS = stringPreferencesKey("bt_mac")
    val KEY_INTERVAL_MS = intPreferencesKey("interval_ms")
    val KEY_SLIDER_SPEED = intPreferencesKey("slider_speed")
  }

  suspend fun setBt(mac: String) = ctx.dataStore.edit { it[KEY_BT_ADDRESS] = mac }
  val btAddress = ctx.dataStore.data.map { it[KEY_BT_ADDRESS] ?: "" }

  suspend fun setInterval(ms: Int) = ctx.dataStore.edit { it[KEY_INTERVAL_MS] = ms }
  val intervalMs = ctx.dataStore.data.map { it[KEY_INTERVAL_MS] ?: 2000 }

  suspend fun setSpeed(v: Int) = ctx.dataStore.edit { it[KEY_SLIDER_SPEED] = v }
  val speed = ctx.dataStore.data.map { it[KEY_SLIDER_SPEED] ?: 600 }
}
