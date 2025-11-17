package com.odos3d.slider.grbl

import android.content.Context
import com.odos3d.slider.link.BtTransport
import com.odos3d.slider.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object GrblProvider {
    @Volatile
    var client: GrblClient? = null

    /**
     * Garantiza una instancia conectada si no estamos en modo offline y hay MAC configurada.
     * Devuelve null si no es posible conectar (sin MAC u offline).
     */
    suspend fun ensureConnected(
        context: Context,
        settings: SettingsStore,
        scope: CoroutineScope,
        listener: GrblListener = object : GrblListener {},
        retry: Boolean = true,
    ): GrblClient? {
        if (settings.offlineMode.first()) return null
        val existing = client
        if (existing?.isConnected() == true) {
            existing.ensureWatchdog(scope, settings.autoReconnect, settings.reconnectSecs)
            return existing
        }

        val mac = settings.deviceMac.first().orEmpty()
        if (mac.isBlank()) return null

        val transport = BtTransport(context.applicationContext)
        val grbl = GrblClient(transport, listener)
        client = grbl
        grbl.connectWithRetries(mac, attempts = if (retry) 3 else 1)
        scope.launch {
            grbl.ensureWatchdog(
                scope = this,
                autoReconnectFlow = settings.autoReconnect,
                intervalSecsFlow = settings.reconnectSecs
            )
        }
        return grbl
    }
}
