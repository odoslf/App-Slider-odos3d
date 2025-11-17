package com.odos3d.slider.grbl

import com.odos3d.slider.link.LinkTransport
import com.odos3d.slider.util.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean

interface GrblListener {
    fun onState(text: String) {}
    fun onStatus(status: GrblStatus) {}
    fun onOk() {}
    fun onError(msg: String) {}
    fun onAlarm(msg: String) {}
}

data class GrblStatus(
    val state: String = "Unknown",
    val mpos: String? = null,
    val wpos: String? = null,
    val limits: String? = null,
    val pins: String? = null
)

class GrblClient(
    private val transport: LinkTransport,
    private val listener: GrblListener
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connected = AtomicBoolean(false)
    private val pending = Channel<CompletableDeferred<Boolean>>(Channel.UNLIMITED)
    private var watchdogJob: Job? = null
    @Volatile private var lastAddress: String? = null

    fun isConnected(): Boolean = connected.get()

    fun connectWithRetries(address: String, attempts: Int = 3, backoffMs: Long = 1500) {
        disconnect()
        lastAddress = address
        scope.launch {
            var ok = false
            var lastErr: String? = null
            repeat(attempts.coerceAtLeast(1)) { i ->
                val latch = CompletableDeferred<Boolean>()
                transport.setReader { parseLine(it) }
                transport.connect(address) { success, err ->
                    ok = success
                    lastErr = err
                    latch.complete(true)
                    connected.set(success)
                    listener.onState(if (success) "Conectado" else "Desconectado")
                }
                latch.await()
                if (ok) {
                    Logger.i("GRBL", "Conectado a $address")
                    return@launch
                }
                if (i < attempts - 1) delay(backoffMs.coerceAtLeast(0))
            }
            Logger.e("GRBL", lastErr ?: "Error al conectar")
            if (!ok) listener.onError(lastErr ?: "Error al conectar")
        }
    }

    fun disconnect() {
        transport.close()
        connected.set(false)
        drainPending()
        listener.onState("Desconectado")
        scope.launch { Logger.i("GRBL", "Desconectado") }
    }

    suspend fun sendLineBlocking(cmd: String): Boolean {
        if (!isConnected()) return false
        val def = CompletableDeferred<Boolean>()
        pending.send(def)
        return try {
            sendInternal(cmd)
            def.await()
        } catch (_: Exception) {
            pending.tryReceive().getOrNull()
            false
        }
    }

    fun sendLine(cmd: String) {
        scope.launch {
            if (!isConnected()) return@launch
            sendInternal(cmd)
        }
    }

    fun queryStatus() = realtime('?')
    fun pause() = realtime('!')
    fun resume() = realtime('~')
    fun reset() = realtime(0x18.toChar())
    fun cancelJog() = realtime(0x85.toChar())

    private fun realtime(code: Char) {
        if (!isConnected()) return
        val ok = transport.write(byteArrayOf(code.code.toByte()))
        if (!ok) {
            scope.launch { Logger.e("GRBL", "Error enviando RT") }
            listener.onError("Error enviando RT")
            disconnect()
        }
    }

    private fun sendInternal(command: String) {
        val bytes = (command.trimEnd() + "\n").toByteArray(Charset.forName("UTF-8"))
        val ok = transport.write(bytes)
        if (!ok) {
            scope.launch { Logger.e("GRBL", "Error enviando comando: $command") }
            listener.onError("Error enviando comando")
            disconnect()
        }
    }

    private fun parseLine(line: String) {
        when {
            line == "ok" -> {
                listener.onOk()
                GlobalScope.launch { pending.tryReceive().getOrNull()?.complete(true) }
            }
            line.startsWith("error", true) -> {
                scope.launch { Logger.e("GRBL", line) }
                listener.onError(line)
                GlobalScope.launch { pending.tryReceive().getOrNull()?.complete(false) }
            }
            line.startsWith("ALARM", true) -> listener.onAlarm(line)
            line.startsWith("<") -> listener.onStatus(parseStatus(line))
            line.isNotBlank() -> listener.onState(line)
        }
    }

    private fun parseStatus(payload: String): GrblStatus {
        val inner = payload.trim('<', '>')
        val parts = inner.split('|')
        val state = parts.firstOrNull() ?: "Unknown"
        var mpos: String? = null
        var wpos: String? = null
        var pins: String? = null
        var lim: String? = null
        parts.drop(1).forEach { s ->
            when {
                s.startsWith("MPos:") -> mpos = s.removePrefix("MPos:")
                s.startsWith("WPos:") -> wpos = s.removePrefix("WPos:")
                s.startsWith("Pn:") -> pins = s.removePrefix("Pn:")
                s.startsWith("Lim:") -> lim = s.removePrefix("Lim:")
            }
        }
        return GrblStatus(state, mpos, wpos, lim, pins)
    }

    private fun drainPending() {
        while (true) {
            val d = pending.tryReceive().getOrNull() ?: break
            if (!d.isCompleted) d.complete(false)
        }
    }

    fun startWatchdog(
        scope: CoroutineScope,
        autoReconnectFlow: Flow<Boolean>,
        intervalSecsFlow: Flow<Int>,
    ) {
        watchdogJob?.cancel()
        watchdogJob = scope.launch(Dispatchers.IO) {
            combine(autoReconnectFlow, intervalSecsFlow) { enabled, secs -> enabled to secs }
                .collectLatest { (enabled, secs) ->
                    if (!enabled) return@collectLatest
                    val delayMs = secs.coerceIn(2, 30) * 1000L
                    while (isActive) {
                        if (!isConnected()) {
                            val mac = lastAddress
                            if (!mac.isNullOrBlank()) {
                                Logger.i("Watchdog", "Intentando reconectar a $mac")
                                connectWithRetries(mac, attempts = 1)
                            }
                        }
                        delay(delayMs)
                    }
                }
        }
    }

    fun ensureWatchdog(
        scope: CoroutineScope,
        autoReconnectFlow: Flow<Boolean>,
        intervalSecsFlow: Flow<Int>,
    ) {
        if (watchdogJob?.isActive == true) return
        startWatchdog(scope, autoReconnectFlow, intervalSecsFlow)
    }

    fun stopWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = null
    }

    suspend fun sendJogIncremental(axis: String, deltaMm: Float, feedMmMin: Int) {
        val ax = axis.uppercase().take(1)
        val delta = String.format("%.4f", deltaMm)
        sendLineBlocking("\$J=G91 G21 F${feedMmMin.coerceAtLeast(1)} ${ax}${delta}")
    }

    fun sendLineNonBlocking(cmd: String) {
        try {
            val bytes = (cmd.trimEnd() + "\n").toByteArray(Charset.forName("UTF-8"))
            transport.write(bytes)
        } catch (_: Exception) {
            // Ignorar errores no críticos en envío fire-and-forget
        }
    }
}
