package com.odos3d.slider.ui.components

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.odos3d.slider.R

class StatusChipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.chipStyle,
) : Chip(context, attrs, defStyleAttr) {

    enum class State { DISCONNECTED, CONNECTING, CONNECTED, ALARM }

    init {
        isCheckable = false
        isClickable = false
        isFocusable = false
        setState(State.DISCONNECTED)
    }

    fun setState(state: State) {
        when (state) {
            State.DISCONNECTED -> {
                text = context.getString(R.string.estado_desconectado)
                setChipBackgroundColorResource(R.color.md_surface)
                setTextColor(ContextCompat.getColor(context, R.color.md_text))
                isEnabled = true
            }
            State.CONNECTING -> {
                text = context.getString(R.string.estado_conectando)
                setChipBackgroundColorResource(R.color.md_surface)
                setTextColor(ContextCompat.getColor(context, R.color.md_text))
                isEnabled = false
            }
            State.CONNECTED -> {
                text = context.getString(R.string.estado_conectado)
                setChipBackgroundColorResource(R.color.md_primary)
                setTextColor(ContextCompat.getColor(context, R.color.md_on_primary))
                isEnabled = true
            }
            State.ALARM -> {
                text = context.getString(R.string.estado_alarma)
                setChipBackgroundColorResource(R.color.md_alert)
                setTextColor(ContextCompat.getColor(context, R.color.md_on_alert))
                isEnabled = true
            }
        }
    }
}
