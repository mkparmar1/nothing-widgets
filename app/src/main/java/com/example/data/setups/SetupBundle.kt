package com.example.data.setups

import com.example.core.theme.AccentOption

data class SetupBundle(
    val id: String,
    val title: String,
    val tagline: String,
    val accent: AccentOption,
    val wallpaperIndex: Int,
    val widgetIds: List<String>,
    val description: String
)

object SetupRepository {

    val SETUPS = listOf(
        SetupBundle(
            id = "setup_minimal_red",
            title = "Minimal Red",
            tagline = "Signature red accent with clean digital time and battery run",
            accent = AccentOption.RED,
            wallpaperIndex = 0,
            widgetIds = listOf("clock.dotdigital.m", "battery.segmented", "weather.current.temp"),
            description = "The definitive monochrome aesthetic with a singular high-contrast crimson accent."
        ),
        SetupBundle(
            id = "setup_monochrome_grid",
            title = "Monochrome Grid",
            tagline = "Pure black and white with 4-in-1 quick action toggles",
            accent = AccentOption.PURE,
            wallpaperIndex = 1,
            widgetIds = listOf("clock.stacked.hhmm", "toggle.grid.2x2", "calendar.week.progress"),
            description = "Zero colors. Extreme focus. Every element drawn in pure white on ink black."
        ),
        SetupBundle(
            id = "setup_data_dense",
            title = "Data Dense",
            tagline = "Information matrix with battery, 5-day weather, and today agenda",
            accent = AccentOption.CYAN,
            wallpaperIndex = 2,
            widgetIds = listOf("clock.seconds.bar", "weather.5day.strip", "calendar.agenda.3", "battery.combo.devices"),
            description = "High efficiency layout displaying schedules, live battery and atmospheric metrics."
        ),
        SetupBundle(
            id = "setup_clean_slate",
            title = "Clean Slate",
            tagline = "Spacious analog dial with ambient step counter",
            accent = AccentOption.LIME,
            wallpaperIndex = 3,
            widgetIds = listOf("clock.analog.ring", "prod.step.counter", "weather.sunrise.arc"),
            description = "Breathe easy with rounded dot analog dials and daily wellness metrics."
        ),
        SetupBundle(
            id = "setup_night_shift",
            title = "Night Shift",
            tagline = "Warm amber tones for reduced glare and late night focus",
            accent = AccentOption.AMBER,
            wallpaperIndex = 4,
            widgetIds = listOf("clock.dotdigital.l", "battery.ring", "prod.notes.pin"),
            description = "Warm amber phosphor styling inspired by vintage laboratory terminals."
        ),
        SetupBundle(
            id = "setup_terminal",
            title = "Terminal Matrix",
            tagline = "Binary BCD clock with system toggles and daily thought",
            accent = AccentOption.LIME,
            wallpaperIndex = 5,
            widgetIds = listOf("clock.binary", "toggle.wifi", "toggle.torch", "fun.quote.daily"),
            description = "Brutalist tech inspiration with raw binary time columns."
        ),
        SetupBundle(
            id = "setup_paper_white",
            title = "Paper Matrix",
            tagline = "High-key light aesthetic for daytime clarity",
            accent = AccentOption.RED,
            wallpaperIndex = 6,
            widgetIds = listOf("clock.dotdigital.m", "calendar.day.date", "prod.todo.3"),
            description = "Off-white paper textured design designed for bright sunlight visibility."
        ),
        SetupBundle(
            id = "setup_cyber_dot",
            title = "Cyber Dot",
            tagline = "Retro snake mini arcade and cyan digital seconds",
            accent = AccentOption.CYAN,
            wallpaperIndex = 7,
            widgetIds = listOf("clock.seconds.bar", "fun.snake.dot", "battery.ring"),
            description = "Playful nostalgia featuring a live retro snake dot frame."
        ),
        SetupBundle(
            id = "setup_cyberpunk_engineer",
            title = "Hardware Vitals Deck",
            tagline = "Live RAM, internal storage, audio visualizer and compass telemetry",
            accent = AccentOption.CYAN,
            wallpaperIndex = 2,
            widgetIds = listOf("sensor.ram.meter", "sensor.storage.bar", "sensor.visualizer.eq", "sensor.compass.heading"),
            description = "Complete system monitor displaying live memory allocations, storage partitions, and orientation."
        ),
        SetupBundle(
            id = "setup_cosmic_voyager",
            title = "Cosmic Orbit",
            tagline = "Lunar phase matrix, solar tracker arc, and precision digital time",
            accent = AccentOption.AMBER,
            wallpaperIndex = 4,
            widgetIds = listOf("sensor.lunar.moon", "weather.sunrise.arc", "clock.seconds.bar", "sensor.compass.heading"),
            description = "Astronomical rhythm tracker tracking current lunar illumination and daylight arc."
        ),
        SetupBundle(
            id = "setup_nothing_glyph",
            title = "Light Strip Studio",
            tagline = "Light-strip battery ring, 4-up device dock and activity rings",
            accent = AccentOption.RED,
            wallpaperIndex = 0,
            widgetIds = listOf("sensor.glyph.lights", "sensor.battery.quad", "sensor.fitness.rings"),
            description = "Minimal dot-matrix look with light-strip lighting and a device battery hub."
        ),
        SetupBundle(
            id = "setup_high_output",
            title = "High-Output Founder",
            tagline = "Annual progress countdown, task checklist, and chronometer timer",
            accent = AccentOption.LIME,
            wallpaperIndex = 3,
            widgetIds = listOf("sensor.year.progress", "prod.todo.3", "sensor.stopwatch.chrono", "prod.water.tracker"),
            description = "Hyper-focused productivity suite designed for relentless execution and wellness tracking."
        ),
        SetupBundle(
            id = "setup_global_nomad",
            title = "Global Nomad",
            tagline = "Dual capsule London/NYC time, world weather, and crypto ticker",
            accent = AccentOption.PURE,
            wallpaperIndex = 1,
            widgetIds = listOf("sensor.world.dual", "weather.current.temp", "sensor.crypto.ticker", "battery.segmented"),
            description = "International traveler dashboard with simultaneous multi-timezone tracking and live crypto pulse."
        )
    )
}
