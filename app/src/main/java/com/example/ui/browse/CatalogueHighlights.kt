package com.example.ui.browse

/**
 * Hand-picked widget IDs shared by Explore and Home. Keep the exclusive list in sync with the
 * exclusive widgets registered in [com.example.widgets.base.WidgetRegistry].
 */
object CatalogueHighlights {

    val RECOMMENDED = listOf(
        "clock.dotdigital.s",
        "sensor.glyph.lights",
        "weather.current.temp",
        "calendar.agenda.3",
        "media.nowplaying.compact",
        "clock.flip.style"
    )

    /** Designs no competitor ships: badged in Explore and featured on Home. */
    val EXCLUSIVE = listOf(
        "clock.glyph.ring",
        "calendar.life.dots",
        "battery.fuel.gauge",
        "prod.week.dots",
        "prod.focus.companion",
        "fun.orbit.ambient",
        "clock.flip.style",
        "clock.binary",
        "clock.text.words",
        "sensor.ram.meter",
        "sensor.year.progress"
    )
}
