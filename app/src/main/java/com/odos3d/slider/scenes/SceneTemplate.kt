package com.odos3d.slider.scenes

data class SceneTemplate(
    val id: String,
    val title: String,
    val intervalSec: Int,
    val durationMin: Int,
    val description: String,
    val stepMmPerShot: Float = 5f,
    val axis: String = "X",
    val moveBeforeShot: Boolean = true,
    val settleMs: Long = 250L,
    val feedMmMin: Int = 800
)

object SceneTemplates {
    val all: List<SceneTemplate> = listOf(
        SceneTemplate("quick_test", "Prueba rápida", 1, 1, "Clip de prueba 1 min · 1s/Foto"),
        SceneTemplate("city_flow", "Flujo urbano", 1, 10, "Tráfico/peatones 10 min · 1s/Foto"),
        SceneTemplate("clouds_fast", "Nubes rápidas", 2, 20, "Cielo con dinamismo 20 min · 2s/Foto"),
        SceneTemplate("sunset", "Puesta de sol", 2, 20, "Transición de luz 20 min · 2s/Foto"),
        SceneTemplate("clouds_slow", "Nubes lentas", 5, 30, "Evolución suave 30 min · 5s/Foto"),
        SceneTemplate("construction", "Obra/Time-lapse", 10, 60, "Progreso 60 min · 10s/Foto"),
        SceneTemplate("plants", "Crecimiento plantas", 15, 120, "Cambios visibles 120 min · 15s/Foto"),
        SceneTemplate("astro_basic", "Astro básico", 15, 60, "Trazos iniciales 60 min · 15s/Foto"),
        SceneTemplate("print3d", "Impresión 3D", 2, 30, "Pieza en progreso 30 min · 2s/Foto"),
        SceneTemplate("people_flow", "Personas (interior)", 1, 15, "Movimiento suave 15 min · 1s/Foto")
    )
}
