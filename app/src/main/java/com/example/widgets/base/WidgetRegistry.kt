package com.example.widgets.base

import android.content.Context
import android.graphics.Canvas
import com.example.core.bitmap.SizeBucket
import com.example.widgets.catalogue.BatteryRenderers
import com.example.widgets.catalogue.CalendarRenderers
import com.example.widgets.catalogue.ClockRenderers
import com.example.widgets.catalogue.MediaAndFunRenderers
import com.example.widgets.catalogue.ProductivityRenderers
import com.example.widgets.catalogue.ToggleRenderers
import com.example.widgets.catalogue.UniqueSensorRenderers
import com.example.widgets.catalogue.WeatherRenderers

object WidgetRegistry {

    private val widgetsList = mutableListOf<WidgetDefinition>()

    init {
        // --- 1. CLOCK (14 widgets) ---
        register(createDef("clock.dotdigital.s", Category.CLOCK, "Digital Dot Clock S", "Minimalist dot-matrix digital clock", setOf(SizeBucket.S), TapAction.OPEN_CLOCK, true) { c, s, cv -> ClockRenderers.renderDigitalClock(s, cv) })
        register(createDef("clock.dotdigital.m", Category.CLOCK, "Digital Dot Clock M", "Wide dot-matrix digital clock with date", setOf(SizeBucket.M), TapAction.OPEN_CLOCK, true) { c, s, cv -> ClockRenderers.renderDigitalClock(s, cv) })
        register(createDef("clock.dotdigital.l", Category.CLOCK, "Digital Dot Clock L", "Hero size dot-matrix digital clock", setOf(SizeBucket.L), TapAction.OPEN_CLOCK, true) { c, s, cv -> ClockRenderers.renderDigitalClock(s, cv) })
        register(createDef("clock.analog.ring", Category.CLOCK, "Analog Dot Ring", "Circular dot-matrix analog face with live hands", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_CLOCK, true) { c, s, cv -> ClockRenderers.renderAnalogRing(s, cv) })
        register(createDef("clock.stacked.hhmm", Category.CLOCK, "Stacked HH/MM", "Vertical stacked hours and minutes in dot-matrix", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_CLOCK, true) { c, s, cv -> ClockRenderers.renderStackedTime(s, cv) })
        register(createDef("clock.seconds.bar", Category.CLOCK, "Seconds Progress Clock", "Digital clock with 60-dot second tracker", setOf(SizeBucket.M), TapAction.OPEN_CLOCK, true) { c, s, cv -> ClockRenderers.renderSecondsBarClock(s, cv) })
        register(createDef("clock.binary", Category.CLOCK, "Binary BCD Clock", "Pure binary dot matrix time readout", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_CLOCK, true) { c, s, cv -> ClockRenderers.renderBinaryClock(s, cv) })
        register(createDef("clock.world.london", Category.CLOCK, "World Clock: London", "Timezone override for London (GMT)", setOf(SizeBucket.M), TapAction.OPEN_CLOCK, true) { c, s, cv -> ClockRenderers.renderDigitalClock(s, cv, timeZoneId = "Europe/London") })
        register(createDef("clock.world.tokyo", Category.CLOCK, "World Clock: Tokyo", "Timezone override for Tokyo (JST)", setOf(SizeBucket.M), TapAction.OPEN_CLOCK, false) { c, s, cv -> ClockRenderers.renderDigitalClock(s, cv, timeZoneId = "Asia/Tokyo") })
        register(createDef("clock.world.nyc", Category.CLOCK, "World Clock: New York", "Timezone override for New York (EST)", setOf(SizeBucket.M), TapAction.OPEN_CLOCK, false) { c, s, cv -> ClockRenderers.renderDigitalClock(s, cv, timeZoneId = "America/New_York") })
        register(createDef("clock.flip.style", Category.CLOCK, "Flip-Style Dot Clock", "Split-flap dot digital aesthetic", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_CLOCK, false) { c, s, cv -> ClockRenderers.renderDigitalClock(s, cv) })
        register(createDef("clock.text.words", Category.CLOCK, "Text Matrix Clock", "Spelled out time representation", setOf(SizeBucket.M), TapAction.OPEN_CLOCK, false) { c, s, cv -> ClockRenderers.renderDigitalClock(s, cv) })
        register(createDef("clock.seconds.live", Category.CLOCK, "Precision Seconds", "Live hh:mm:ss second-accurate clock", setOf(SizeBucket.M), TapAction.OPEN_CLOCK, false) { c, s, cv -> ClockRenderers.renderDigitalClock(s, cv, showSeconds = true) })
        register(createDef("clock.minimal.pill", Category.CLOCK, "Capsule Time Pill", "Ultra-condensed dot time badge", setOf(SizeBucket.S), TapAction.OPEN_CLOCK, false) { c, s, cv -> ClockRenderers.renderDigitalClock(s, cv) })

        // --- 2. DATE & CALENDAR (10 widgets) ---
        register(createDef("calendar.day.date", Category.CALENDAR, "Day & Date Card", "Bold day number with weekday tag", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_CALENDAR, true) { c, s, cv -> CalendarRenderers.renderDayDateCard(s, cv) })
        register(createDef("calendar.month.grid", Category.CALENDAR, "Month Dot Matrix", "Full month calendar matrix with accented day dot", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_CALENDAR, true) { c, s, cv -> CalendarRenderers.renderMonthGrid(s, cv) })
        register(createDef("calendar.agenda.3", Category.CALENDAR, "Today Agenda (3)", "Upcoming schedule events in high contrast", setOf(SizeBucket.M), TapAction.OPEN_CALENDAR, true) { c, s, cv -> CalendarRenderers.renderAgendaEvents(s, cv, 3) })
        register(createDef("calendar.agenda.5", Category.CALENDAR, "Today Agenda (5)", "Full day agenda timeline", setOf(SizeBucket.L), TapAction.OPEN_CALENDAR, false) { c, s, cv -> CalendarRenderers.renderAgendaEvents(s, cv, 5) })
        register(createDef("calendar.week.progress", Category.CALENDAR, "Week Progress Bar", "7-dot day-by-day week run", setOf(SizeBucket.M), TapAction.OPEN_CALENDAR, true) { c, s, cv -> CalendarRenderers.renderWeekProgress(s, cv) })
        register(createDef("calendar.countdown.date", Category.CALENDAR, "Event Countdown", "Days remaining countdown pill", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_CALENDAR, false) { c, s, cv -> CalendarRenderers.renderDayDateCard(s, cv) })
        register(createDef("calendar.year.progress", Category.CALENDAR, "Year Progress Tracker", "Annual dot timeline run", setOf(SizeBucket.M), TapAction.OPEN_CALENDAR, false) { c, s, cv -> UniqueSensorRenderers.renderYearProgress(s, cv) })
        register(createDef("calendar.iso.week", Category.CALENDAR, "ISO Week & Julian Day", "Exact calendar metrics and week number", setOf(SizeBucket.S), TapAction.OPEN_CALENDAR, false) { c, s, cv -> CalendarRenderers.renderDayDateCard(s, cv) })
        register(createDef("calendar.minimal.split", Category.CALENDAR, "Split Calendar Card", "Vertical divided date and month indicator", setOf(SizeBucket.S), TapAction.OPEN_CALENDAR, false) { c, s, cv -> CalendarRenderers.renderDayDateCard(s, cv) })
        register(createDef("calendar.timeline.dots", Category.CALENDAR, "Timeline Dots", "Hourly matrix timeline of daily appointments", setOf(SizeBucket.L), TapAction.OPEN_CALENDAR, false) { c, s, cv -> CalendarRenderers.renderAgendaEvents(s, cv, 4) })

        // --- 3. BATTERY (8 widgets) ---
        register(createDef("battery.ring", Category.BATTERY, "Battery Dot Ring", "Circular 24-dot dial with center percentage", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_BATTERY, true) { c, s, cv -> BatteryRenderers.renderBatteryRing(c, s, cv) })
        register(createDef("battery.segmented", Category.BATTERY, "Segmented Battery Bar", "20-dot segmented level with temperature", setOf(SizeBucket.M), TapAction.OPEN_BATTERY, true) { c, s, cv -> BatteryRenderers.renderSegmentedBar(c, s, cv) })
        register(createDef("battery.combo.devices", Category.BATTERY, "Device Ecosystem Combo", "Phone, Earbuds, and Watch battery gauges", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_BATTERY, true) { c, s, cv -> BatteryRenderers.renderComboDevices(c, s, cv) })
        register(createDef("battery.percentage", Category.BATTERY, "Bold Battery %", "Large dot-matrix capacity readout", setOf(SizeBucket.S), TapAction.OPEN_BATTERY, true) { c, s, cv -> BatteryRenderers.renderBatteryRing(c, s, cv) })
        register(createDef("battery.charging.active", Category.BATTERY, "Charging Indicator", "Active charging state and voltage readout", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_BATTERY, false) { c, s, cv -> BatteryRenderers.renderSegmentedBar(c, s, cv) })
        register(createDef("battery.health.temp", Category.BATTERY, "Battery Health & Temp", "Thermal metrics and cell health indicator", setOf(SizeBucket.M), TapAction.OPEN_BATTERY, false) { c, s, cv -> BatteryRenderers.renderSegmentedBar(c, s, cv) })
        register(createDef("battery.screen.on", Category.BATTERY, "Screen-On Time", "Daily usage runtime & discharge rate", setOf(SizeBucket.M), TapAction.OPEN_BATTERY, false) { c, s, cv -> BatteryRenderers.renderSegmentedBar(c, s, cv) })
        register(createDef("battery.minimal.pill", Category.BATTERY, "Mini Battery Pill", "Compact capsule battery status", setOf(SizeBucket.S), TapAction.OPEN_BATTERY, false) { c, s, cv -> BatteryRenderers.renderBatteryRing(c, s, cv) })

        // --- 4. WEATHER (10 widgets) ---
        register(createDef("weather.current.temp", Category.WEATHER, "Current Temperature", "Real-time temp with dot icon and stale guard", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_WEATHER, true, true) { c, s, cv -> WeatherRenderers.renderCurrentWeather(c, s, cv) })
        register(createDef("weather.5day.strip", Category.WEATHER, "5-Day Forecast Strip", "5-day outlook with daily high/low dots", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_WEATHER, true, true) { c, s, cv -> WeatherRenderers.renderForecast5Day(c, s, cv) })
        register(createDef("weather.sunrise.arc", Category.WEATHER, "Sun Tracker Arc", "Sunrise and sunset dot arc tracking daylight", setOf(SizeBucket.M), TapAction.OPEN_WEATHER, true) { c, s, cv -> WeatherRenderers.renderSunriseSunsetArc(s, cv) })
        register(createDef("weather.city.london", Category.WEATHER, "London Weather", "City preset weather for London", setOf(SizeBucket.M), TapAction.OPEN_WEATHER, true, true) { c, s, cv -> WeatherRenderers.renderCurrentWeather(c, s, cv, "London") })
        register(createDef("weather.city.nyc", Category.WEATHER, "New York Weather", "City preset weather for New York", setOf(SizeBucket.M), TapAction.OPEN_WEATHER, false, true) { c, s, cv -> WeatherRenderers.renderCurrentWeather(c, s, cv, "New York") })
        register(createDef("weather.city.tokyo", Category.WEATHER, "Tokyo Weather", "City preset weather for Tokyo", setOf(SizeBucket.M), TapAction.OPEN_WEATHER, false, true) { c, s, cv -> WeatherRenderers.renderCurrentWeather(c, s, cv, "Tokyo") })
        register(createDef("weather.moon.phase", Category.WEATHER, "Moon Phase Tracker", "Current lunar phase dot matrix depiction", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_WEATHER, false) { c, s, cv -> UniqueSensorRenderers.renderLunarMoonPhase(s, cv) })
        register(createDef("weather.precipitation", Category.WEATHER, "Precipitation Bar", "Hourly rain probability dot run", setOf(SizeBucket.M), TapAction.OPEN_WEATHER, false, true) { c, s, cv -> WeatherRenderers.renderForecast5Day(c, s, cv) })
        register(createDef("weather.feels.humidity", Category.WEATHER, "Humidity & Feels Like", "Thermal comfort and atmospheric moisture", setOf(SizeBucket.M), TapAction.OPEN_WEATHER, false, true) { c, s, cv -> WeatherRenderers.renderCurrentWeather(c, s, cv) })
        register(createDef("weather.uv.index", Category.WEATHER, "UV Index Warning", "Solar radiation level & sun safety badge", setOf(SizeBucket.S), TapAction.OPEN_WEATHER, false) { c, s, cv -> WeatherRenderers.renderCurrentWeather(c, s, cv) })

        // --- 5. SYSTEM & SENSORS (12 widgets) ---
        register(createDef("sensor.compass.heading", Category.SYSTEM, "Digital Compass & Heading", "16-dot compass rose with live degree angle", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, true) { c, s, cv -> UniqueSensorRenderers.renderCompassHeading(s, cv) })
        register(createDef("sensor.ram.meter", Category.SYSTEM, "System RAM Vitals", "Real Android memory usage in dot font & 20-dot bar", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT, true) { c, s, cv -> UniqueSensorRenderers.renderRamMeter(c, s, cv) })
        register(createDef("sensor.storage.bar", Category.SYSTEM, "Storage Breakdown", "Internal storage capacity and free GB bar", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT, true) { c, s, cv -> UniqueSensorRenderers.renderStorageBar(s, cv) })
        register(createDef("sensor.visualizer.eq", Category.SYSTEM, "Audio Spectrum Visualizer", "8-band frequency columns with peak hold dots", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT, true) { c, s, cv -> UniqueSensorRenderers.renderVisualizer(s, cv) })
        register(createDef("sensor.world.dual", Category.SYSTEM, "Dual Capsule World Time", "Split capsule time for London (GMT) & NYC (EST)", setOf(SizeBucket.M), TapAction.OPEN_CLOCK, true) { c, s, cv -> UniqueSensorRenderers.renderDualCapsule(s, cv) })
        register(createDef("sensor.year.progress", Category.SYSTEM, "Annual Year Progress", "Day of year countdown and 25-dot annual run", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_CALENDAR, true) { c, s, cv -> UniqueSensorRenderers.renderYearProgress(s, cv) })
        register(createDef("sensor.lunar.moon", Category.SYSTEM, "Lunar Phase Matrix", "Dot matrix moon phase with illumination readout", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_WEATHER, true) { c, s, cv -> UniqueSensorRenderers.renderLunarMoonPhase(s, cv) })
        register(createDef("sensor.fitness.rings", Category.SYSTEM, "Concentric Activity Rings", "Dual dot rings for Steps and Active Calories", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, true) { c, s, cv -> UniqueSensorRenderers.renderFitnessActivityRings(c, s, cv) })
        register(createDef("sensor.stopwatch.chrono", Category.SYSTEM, "Dot Chronometer", "Precision split chronometer with lap tag", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, true) { c, s, cv -> UniqueSensorRenderers.renderChronographStopwatch(c, s, cv) })
        register(createDef("sensor.glyph.lights", Category.SYSTEM, "Glyph Light Interface", "Nothing signature glyph matrix pattern", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, true) { c, s, cv -> UniqueSensorRenderers.renderGlyphMatrix(s, cv) })
        register(createDef("sensor.crypto.ticker", Category.SYSTEM, "Crypto Market Ticker", "BTC real-time price with 16-dot trend sparkline", setOf(SizeBucket.M), TapAction.DEFAULT, true) { c, s, cv -> UniqueSensorRenderers.renderCryptoTicker(s, cv) })
        register(createDef("sensor.battery.quad", Category.SYSTEM, "Ecosystem Quad Dock", "4-up battery matrix for Phone, Ear(2), Watch, CMF", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_BATTERY, true) { c, s, cv -> UniqueSensorRenderers.renderDeviceEcosystemQuad(c, s, cv) })

        // --- 6. QUICK TOGGLES (10 widgets) ---
        register(createDef("toggle.wifi", Category.TOGGLES, "Wi-Fi Toggle", "Wi-Fi state and connection ring", setOf(SizeBucket.S), TapAction.DEFAULT, true) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "WI-FI", true, "W") })
        register(createDef("toggle.bluetooth", Category.TOGGLES, "Bluetooth Toggle", "Bluetooth state and pairing status", setOf(SizeBucket.S), TapAction.DEFAULT, true) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "BLUETOOTH", true, "B") })
        register(createDef("toggle.torch", Category.TOGGLES, "Flashlight / Torch", "Instant torch activator", setOf(SizeBucket.S), TapAction.DEFAULT, true) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "TORCH", false, "T") })
        register(createDef("toggle.dnd", Category.TOGGLES, "Do Not Disturb", "Silence notifications and calls", setOf(SizeBucket.S), TapAction.DEFAULT, true) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "DND", false, "D") })
        register(createDef("toggle.darkmode", Category.TOGGLES, "Dark Mode Switch", "System dark / light mode toggle", setOf(SizeBucket.S), TapAction.DEFAULT, true) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "DARK MODE", true, "M") })
        register(createDef("toggle.grid.2x2", Category.TOGGLES, "2x2 Quick Action Grid", "Wi-Fi, Bluetooth, Torch, DND quadrant grid", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT, true) { c, s, cv -> ToggleRenderers.render2x2Grid(s, cv) })
        register(createDef("toggle.rotation", Category.TOGGLES, "Auto-Rotate Lock", "Screen orientation lock toggle", setOf(SizeBucket.S), TapAction.DEFAULT, false) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "ROTATION", true, "R") })
        register(createDef("toggle.datasaver", Category.TOGGLES, "Data Saver", "Background data usage limiter", setOf(SizeBucket.S), TapAction.DEFAULT, false) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "DATA SAVER", false, "S") })
        register(createDef("toggle.hotspot", Category.TOGGLES, "Hotspot", "Mobile tethering and hotspot toggle", setOf(SizeBucket.S), TapAction.DEFAULT, false) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "HOTSPOT", false, "H") })
        register(createDef("toggle.airplane", Category.TOGGLES, "Airplane Mode", "Radio connectivity lock", setOf(SizeBucket.S), TapAction.DEFAULT, false) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "AIRPLANE", false, "A") })

        // --- 6. CONTACTS & SHORTCUTS (8 widgets) ---
        register(createDef("contact.favorite.single", Category.CONTACTS, "Favorite Contact", "One-tap direct contact card", setOf(SizeBucket.S), TapAction.OPEN_MAIN, true) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "ALEX", true, "A") })
        register(createDef("contact.duo", Category.CONTACTS, "Duo Contacts", "Two favorite contacts speed dial", setOf(SizeBucket.M), TapAction.OPEN_MAIN, true) { c, s, cv -> ToggleRenderers.render2x2Grid(s, cv, true, true, false, false) })
        register(createDef("contact.quad", Category.CONTACTS, "4-Up Contact Grid", "Quick reach 4-contact dock", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_MAIN, false) { c, s, cv -> ToggleRenderers.render2x2Grid(s, cv, true, true, true, true) })
        register(createDef("shortcut.direct.dial", Category.CONTACTS, "Emergency Speed Dial", "Direct phone dialer trigger", setOf(SizeBucket.S), TapAction.OPEN_MAIN, true) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "EMERGENCY", false, "E") })
        register(createDef("shortcut.direct.sms", Category.CONTACTS, "Instant Message", "One-tap message compose", setOf(SizeBucket.S), TapAction.OPEN_MAIN, false) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "MESSAGE", true, "M") })
        register(createDef("shortcut.apps.4", Category.CONTACTS, "4-App Dot Tray", "Minimalist app launch matrix", setOf(SizeBucket.M), TapAction.OPEN_MAIN, false) { c, s, cv -> ToggleRenderers.render2x2Grid(s, cv) })
        register(createDef("shortcut.apps.6", Category.CONTACTS, "6-App Quick Dock", "High efficiency app dock", setOf(SizeBucket.L), TapAction.OPEN_MAIN, false) { c, s, cv -> ToggleRenderers.render2x2Grid(s, cv) })
        register(createDef("shortcut.web.favorite", Category.CONTACTS, "Web Bookmark", "Direct browser link shortcut", setOf(SizeBucket.S), TapAction.OPEN_MAIN, false) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "GITHUB", true, "G") })

        // --- 7. MEDIA (4 widgets) ---
        register(createDef("media.nowplaying.compact", Category.MEDIA, "Now Playing Compact", "Track name and dot progress", setOf(SizeBucket.M), TapAction.DEFAULT, true) { c, s, cv -> MediaAndFunRenderers.renderNowPlaying(s, cv) })
        register(createDef("media.nowplaying.art", Category.MEDIA, "Now Playing Card", "Full album & artist playback card", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT, true) { c, s, cv -> MediaAndFunRenderers.renderNowPlaying(s, cv) })
        register(createDef("media.controls.bar", Category.MEDIA, "Media Controls Strip", "Prev, Play/Pause, Next dot targets", setOf(SizeBucket.M), TapAction.DEFAULT, false) { c, s, cv -> MediaAndFunRenderers.renderNowPlaying(s, cv) })
        register(createDef("media.volume.dots", Category.MEDIA, "Volume Level Dots", "15-dot master audio level indicator", setOf(SizeBucket.M), TapAction.DEFAULT, false) { c, s, cv -> MediaAndFunRenderers.renderNowPlaying(s, cv) })

        // --- 8. PRODUCTIVITY (8 widgets) ---
        register(createDef("prod.notes.pin", Category.PRODUCTIVITY, "Notes Pin", "Pinned quick memo and thoughts", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_MAIN, true) { c, s, cv -> ProductivityRenderers.renderNotesPin(c, s, cv) })
        register(createDef("prod.todo.3", Category.PRODUCTIVITY, "To-Do Checklist (3)", "Daily task checklist with checkmarks", setOf(SizeBucket.M), TapAction.OPEN_MAIN, true) { c, s, cv -> ProductivityRenderers.renderTodoList(s, cv, 3) })
        register(createDef("prod.todo.5", Category.PRODUCTIVITY, "To-Do Checklist (5)", "Extended 5-item daily checklist", setOf(SizeBucket.L), TapAction.OPEN_MAIN, false) { c, s, cv -> ProductivityRenderers.renderTodoList(s, cv, 5) })
        register(createDef("prod.step.counter", Category.PRODUCTIVITY, "Daily Step Counter", "Live step metrics with 20-dot progress run", setOf(SizeBucket.M), TapAction.OPEN_MAIN, true) { c, s, cv -> ProductivityRenderers.renderStepCounter(c, s, cv) })
        register(createDef("prod.water.tracker", Category.PRODUCTIVITY, "Hydration Tracker", "8-glass daily water intake dots", setOf(SizeBucket.M), TapAction.OPEN_MAIN, true) { c, s, cv -> ProductivityRenderers.renderWaterTracker(c, s, cv) })
        register(createDef("prod.focus.timer", Category.PRODUCTIVITY, "Focus Timer", "Pomodoro work session tracker", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, true) { c, s, cv -> ProductivityRenderers.renderFocusTimer(c, s, cv) })
        register(createDef("prod.habit.dots", Category.PRODUCTIVITY, "Habit Matrix Streak", "Weekly habit completion dot grid", setOf(SizeBucket.M), TapAction.OPEN_MAIN, false) { c, s, cv -> CalendarRenderers.renderWeekProgress(s, cv) })
        register(createDef("prod.countdown.timer", Category.PRODUCTIVITY, "Countdown Pill", "Target minutes remaining indicator", setOf(SizeBucket.S), TapAction.DEFAULT, true) { c, s, cv -> ProductivityRenderers.renderCountdownTimer(c, s, cv) })

        // --- 9. PHOTO (4 widgets) ---
        register(createDef("photo.single.frame", Category.PHOTO, "Dot Border Photo Frame", "Aesthetic minimal framed photo", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT, true) { c, s, cv -> MediaAndFunRenderers.renderDailyQuote(s, cv) })
        register(createDef("photo.clock.overlay", Category.PHOTO, "Photo + Clock Overlay", "Wallpaper frame with dot time badge", setOf(SizeBucket.L), TapAction.OPEN_CLOCK, false) { c, s, cv -> ClockRenderers.renderDigitalClock(s, cv) })
        register(createDef("photo.duo.collage", Category.PHOTO, "Duo Split Photo", "Two minimal framed photos", setOf(SizeBucket.M), TapAction.DEFAULT, false) { c, s, cv -> MediaAndFunRenderers.renderDailyQuote(s, cv) })
        register(createDef("photo.monochrome.art", Category.PHOTO, "Monochrome Halftone Art", "Dot matrix stylized art frame", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT, false) { c, s, cv -> MediaAndFunRenderers.renderDailyQuote(s, cv) })

        // --- 10. FUN & LAB (4 widgets) ---
        register(createDef("fun.snake.dot", Category.FUN, "Retro Dot Snake", "Classic retro dot matrix snake on screen", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT, true) { c, s, cv -> MediaAndFunRenderers.renderRetroSnake(c, s, cv) })
        register(createDef("fun.quote.daily", Category.FUN, "Daily Quote", "Inspiring minimalist thought in dot font", setOf(SizeBucket.M), TapAction.DEFAULT, true) { c, s, cv -> MediaAndFunRenderers.renderDailyQuote(s, cv) })
        register(createDef("fun.dice.roll", Category.FUN, "Dice Roll Pair", "Tap to roll a pair of dot-matrix dice", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, true) { c, s, cv -> MediaAndFunRenderers.renderDiceRoll(c, s, cv) })
        register(createDef("fun.pattern.generator", Category.FUN, "Ambient Dot Matrix", "Procedural geometric dot matrix rhythm", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT, false) { c, s, cv -> MediaAndFunRenderers.renderRetroSnake(c, s, cv) })
    }

    private fun register(def: WidgetDefinition) {
        widgetsList.add(def)
    }

    private fun createDef(
        id: String,
        category: Category,
        title: String,
        description: String,
        sizes: Set<SizeBucket>,
        defaultTap: TapAction,
        isFree: Boolean,
        requiresNetwork: Boolean = false,
        renderFn: (Context, WidgetSpec, Canvas) -> Unit
    ): WidgetDefinition {
        return object : WidgetDefinition {
            override val id = id
            override val category = category
            override val title = title
            override val description = description
            override val sizes = sizes
            override val defaultTap = defaultTap
            override val isFree = isFree
            override val requiresNetwork = requiresNetwork

            override fun render(context: Context, spec: WidgetSpec, canvas: Canvas) {
                renderFn(context, spec, canvas)
            }
        }
    }

    fun getAll(): List<WidgetDefinition> = widgetsList

    fun getByCategory(category: Category): List<WidgetDefinition> {
        return if (category == Category.ALL) widgetsList else widgetsList.filter { it.category == category }
    }

    fun getById(id: String): WidgetDefinition? {
        return widgetsList.find { it.id == id }
    }

    fun search(query: String): List<WidgetDefinition> {
        if (query.isBlank()) return widgetsList
        val q = query.trim().lowercase()
        return widgetsList.filter {
            it.title.lowercase().contains(q) ||
            it.description.lowercase().contains(q) ||
            it.category.title.lowercase().contains(q)
        }
    }
}
