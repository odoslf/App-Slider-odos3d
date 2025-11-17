package com.odos3d.slider.scenes

import com.odos3d.slider.grbl.GrblClient
import com.odos3d.slider.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class ScenesRunner(
    private val scope: CoroutineScope,
    private val settings: SettingsStore,
    private val grblClientProvider: () -> GrblClient?,
) {
    enum class Failure { OFFLINE, NO_CLIENT, LIMIT, SEND_FAIL }
    data class MoveResult(val success: Boolean, val reason: Failure? = null, val limitMm: Float? = null)
    data class Progress(val index: Int, val total: Int)
    private var progressedMm = 0f

    fun resetProgress() { progressedMm = 0f }

    fun runPreset(
        template: SceneTemplate,
        onProgress: (Progress) -> Unit = {},
        onFailure: (Failure) -> Unit = {},
        startDelayMs: Long = 0L,
    ) {
        scope.launch {
            val grbl = grblClientProvider.invoke()
            if (grbl == null) {
                onFailure(Failure.NO_CLIENT)
                return@launch
            }
            if (settings.offlineMode.first()) {
                onFailure(Failure.OFFLINE)
                return@launch
            }

            val maxFeed = settings.maxFeed.first()
            val maxTravel = settings.maxTravelMm.first()
            val axis = (template.axis.ifBlank { settings.axisDefault.first() }).first()
            val shots = max(1, (template.durationMin * 60) / template.intervalSec)
            val desiredStep = template.stepMmPerShot.coerceAtLeast(0.001f)
            val desiredTravel = desiredStep * max(0, shots - 1)
            val cappedTravel = min(desiredTravel, maxTravel)
            val step = if (shots > 1) cappedTravel / (shots - 1) else 0f
            val feed = template.feedMmMin.coerceIn(1, maxFeed)
            val settle = template.settleMs.coerceAtLeast(0L)
            val preDelay = 0L
            val postDelay = 0L

            progressedMm = 0f
            if (startDelayMs > 0) delay(startDelayMs)
            if (preDelay > 0) delay(preDelay)
            onProgress(Progress(1, shots))
            for (i in 2..shots) {
                val move = moveOnce(axis, step, feed, maxTravel)
                if (move.success) {
                    if (settle > 0) delay(settle)
                    onProgress(Progress(i, shots))
                    delay(template.intervalSec * 1000L)
                } else {
                    move.reason?.let { onFailure(it) }
                    break
                }
            }
            if (postDelay > 0) delay(postDelay)
        }
    }

    suspend fun moveOnce(template: SceneTemplate): MoveResult {
        val grbl = grblClientProvider.invoke() ?: return
        if (settings.offlineMode.first()) return MoveResult(false, Failure.OFFLINE)
        val maxTravel = settings.maxTravelMm.first()
        val axis = (template.axis.ifBlank { settings.axisDefault.first() }).first()
        val feed = template.feedMmMin.coerceIn(1, settings.maxFeed.first())
        return moveOnce(axis, template.stepMmPerShot, feed, maxTravel)
    }

    private suspend fun moveOnce(axis: Char, stepMm: Float, feed: Int, maxTravel: Float): MoveResult {
        val step = stepMm.coerceAtLeast(0.001f)
        val candidate = progressedMm + step
        if (candidate > maxTravel) return MoveResult(false, Failure.LIMIT, maxTravel)
        val ok = try {
            grblClientProvider.invoke()?.sendJogIncremental(axis.toString(), step, feed)
            true
        } catch (_: Exception) { false }
        if (!ok) return MoveResult(false, Failure.SEND_FAIL)
        progressedMm = candidate
        return MoveResult(true)
    }

}
