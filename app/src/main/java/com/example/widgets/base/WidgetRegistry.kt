package com.example.widgets.base

import android.content.Context
import android.graphics.Canvas
import com.example.core.bitmap.SizeBucket
import com.example.core.time.TimeFormat
import com.example.widgets.collection.CollectionWidgets
import com.example.widgets.catalogue.BatteryRenderers
import com.example.widgets.catalogue.CalendarExtraRenderers
import com.example.widgets.catalogue.CalendarRenderers
import com.example.widgets.catalogue.ClockRenderers
import com.example.widgets.catalogue.ClockStyleRenderers
import com.example.widgets.catalogue.DeviceInfoRenderers
import com.example.widgets.catalogue.ExclusiveRenderers
import com.example.widgets.catalogue.FinanceRenderers
import com.example.widgets.catalogue.HealthRenderers
import com.example.widgets.catalogue.InsightRenderers
import com.example.widgets.catalogue.InteractiveRenderers
import com.example.widgets.catalogue.LearnRenderers
import com.example.core.config.WidgetOption
import com.example.widgets.catalogue.DeepVariantRenderers
import com.example.widgets.catalogue.LiveDataRenderers
import com.example.widgets.catalogue.TravelRenderers
import com.example.widgets.catalogue.MediaAndFunRenderers
import com.example.widgets.catalogue.MediaRenderers
import com.example.widgets.catalogue.PhotoRenderers
import com.example.widgets.catalogue.ProductivityRenderers
import com.example.widgets.catalogue.ShortcutRenderers
import com.example.widgets.catalogue.SmartHomeRenderers
import com.example.widgets.catalogue.SportsRenderers
import com.example.widgets.catalogue.ToggleRenderers
import com.example.widgets.catalogue.UniqueSensorRenderers
import com.example.widgets.catalogue.UtilityRenderers
import com.example.widgets.catalogue.WeatherRenderers
import com.example.ui.permissions.AppPermission

/**
 * Glanceable widgets that also suit the lock screen / Always-On Display (no tapping needed,
 * no live sensor, no network or permission), on top of the ones registered with lockSafe = true. Top-level (not in the object)
 * because register() runs from WidgetRegistry's init block, before the object's own properties exist.
 */
private val LOCK_SAFE_EXTRA = setOf(
    "clock.dotdigital.s", "clock.dotdigital.m", "clock.analog.ring", "clock.stacked.hhmm",
    "clock.binary", "clock.flip.style", "clock.text.words", "clock.minimal.pill",
    "clock.world.london", "clock.world.tokyo", "clock.world.nyc", "clock.seconds.live",
    "calendar.day.date", "calendar.iso.week", "calendar.minimal.split", "calendar.week.progress",
    "calendar.year.progress", "battery.ring", "battery.percentage", "battery.minimal.pill",
    "battery.charging.active", "sensor.glyph.lights", "weather.moon.phase", "sensor.lunar.moon",
    "weather.golden.hour",
    "prod.focus.timer", "prod.countdown.timer"
)

object WidgetRegistry {

    private val widgetsList = mutableListOf<WidgetDefinition>()

    init {
        // --- 1. CLOCK (14 widgets) ---
        register(createDef("clock.dotdigital.s", Category.CLOCK, "Digital Dot Clock S", "Minimalist dot-matrix digital clock", setOf(SizeBucket.S), TapAction.OPEN_CLOCK, lockSafe = true) { c, s, cv -> ClockRenderers.renderDigitalClock(s, cv) })
        register(createDef("clock.dotdigital.m", Category.CLOCK, "Digital Dot Clock M", "Wide dot-matrix digital clock with date", setOf(SizeBucket.M), TapAction.OPEN_CLOCK, lockSafe = true) { c, s, cv -> ClockRenderers.renderDigitalClock(s, cv) })
        register(createDef("clock.dotdigital.l", Category.CLOCK, "Digital Dot Clock L", "Hero size dot-matrix digital clock", setOf(SizeBucket.L), TapAction.OPEN_CLOCK) { c, s, cv -> ClockRenderers.renderDigitalClock(s, cv) })
        register(createDef("clock.analog.ring", Category.CLOCK, "Analog Dot Ring", "Circular dot-matrix analog face with live hands", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_CLOCK) { c, s, cv -> ClockRenderers.renderAnalogRing(s, cv) })
        register(createDef("clock.stacked.hhmm", Category.CLOCK, "Stacked HH/MM", "Vertical stacked hours and minutes in dot-matrix", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_CLOCK) { c, s, cv -> ClockRenderers.renderStackedTime(s, cv) })
        register(createDef("clock.seconds.bar", Category.CLOCK, "Hour Progress Clock", "Digital clock with a dot run for the minutes of the hour", setOf(SizeBucket.M), TapAction.OPEN_CLOCK) { c, s, cv -> ClockRenderers.renderSecondsBarClock(s, cv) })
        register(createDef("clock.binary", Category.CLOCK, "Binary BCD Clock", "Pure binary dot matrix time readout", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_CLOCK) { c, s, cv -> ClockRenderers.renderBinaryClock(s, cv) })
        register(createDef("clock.world.london", Category.CLOCK, "World Clock: London", "Timezone override for London (GMT)", setOf(SizeBucket.M), TapAction.OPEN_CLOCK) { c, s, cv -> ClockRenderers.renderDigitalClock(s, cv, timeZoneId = "Europe/London") })
        register(createDef("clock.world.tokyo", Category.CLOCK, "World Clock: Tokyo", "Timezone override for Tokyo (JST)", setOf(SizeBucket.M), TapAction.OPEN_CLOCK) { c, s, cv -> ClockRenderers.renderDigitalClock(s, cv, timeZoneId = "Asia/Tokyo") })
        register(createDef("clock.world.nyc", Category.CLOCK, "World Clock: New York", "Timezone override for New York (EST)", setOf(SizeBucket.M), TapAction.OPEN_CLOCK) { c, s, cv -> ClockRenderers.renderDigitalClock(s, cv, timeZoneId = "America/New_York") })
        register(createDef("clock.world.custom", Category.CLOCK, "World Clock: Any City", "Pick any time zone for a second city's time", setOf(SizeBucket.M), TapAction.OPEN_CLOCK) { c, s, cv -> ClockRenderers.renderDigitalClock(s, cv) })
        register(createDef("clock.flip.style", Category.CLOCK, "Flip-Style Dot Clock", "Split-flap dot digital aesthetic", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_CLOCK) { c, s, cv -> ClockStyleRenderers.renderFlipClock(s, cv) })
        register(createDef("clock.text.words", Category.CLOCK, "Text Matrix Clock", "Spelled out time representation", setOf(SizeBucket.M), TapAction.OPEN_CLOCK) { c, s, cv -> ClockStyleRenderers.renderWordClock(s, cv) })
        register(createDef("clock.seconds.live", Category.CLOCK, "Precision Seconds", "Live hh:mm:ss second-accurate clock", setOf(SizeBucket.M), TapAction.OPEN_CLOCK) { c, s, cv -> ClockRenderers.renderDigitalClock(s, cv, defaultTime = TimeFormat.H24_SECONDS) })
        register(createDef("clock.minimal.pill", Category.CLOCK, "Capsule Time Pill", "Ultra-condensed dot time badge", setOf(SizeBucket.S), TapAction.OPEN_CLOCK) { c, s, cv -> ClockStyleRenderers.renderCapsulePill(s, cv) })
        register(createDef("clock.glyph.ring", Category.CLOCK, "Glyph-Reactive Clock", "Dot time inside a segmented accent ring that lights up while charging", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_CLOCK) { c, s, cv -> ExclusiveRenderers.renderGlyphClock(c, s, cv) })

        // --- 2. DATE & CALENDAR (10 widgets) ---
        register(createDef("calendar.day.date", Category.CALENDAR, "Day & Date Card", "Bold day number with weekday tag", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_CALENDAR, lockSafe = true, options = listOf(
            com.example.core.config.WidgetOption.Toggle("show_weekday", "Show weekday tag", default = true)
        )) { c, s, cv -> CalendarRenderers.renderDayDateCard(s, cv) })
        register(createDef("calendar.month.grid", Category.CALENDAR, "Month Dot Matrix", "Full month calendar matrix with accented day dot", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_CALENDAR) { c, s, cv -> CalendarRenderers.renderMonthGrid(s, cv) })
        register(createDef("calendar.agenda.3", Category.CALENDAR, "Today Agenda (3)", "Upcoming schedule events in high contrast", setOf(SizeBucket.M), TapAction.OPEN_CALENDAR, requiredPermission = AppPermission.Calendar) { c, s, cv -> CalendarExtraRenderers.renderAgenda(c, s, cv, 3) })
        register(createDef("calendar.agenda.5", Category.CALENDAR, "Today Agenda (5)", "Full day agenda timeline", setOf(SizeBucket.L), TapAction.OPEN_CALENDAR, requiredPermission = AppPermission.Calendar) { c, s, cv -> CalendarExtraRenderers.renderAgenda(c, s, cv, 5) })
        register(createDef("calendar.week.progress", Category.CALENDAR, "Week Progress Bar", "7-dot day-by-day week run", setOf(SizeBucket.M), TapAction.OPEN_CALENDAR) { c, s, cv -> CalendarRenderers.renderWeekProgress(s, cv) })
        register(createDef("calendar.countdown.date", Category.CALENDAR, "Event Countdown", "Days remaining countdown pill", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_CALENDAR) { c, s, cv -> CalendarExtraRenderers.renderEventCountdown(c, s, cv) })
        register(createDef("calendar.year.progress", Category.CALENDAR, "Year Progress Tracker", "Annual dot timeline run", setOf(SizeBucket.M), TapAction.OPEN_CALENDAR) { c, s, cv -> UniqueSensorRenderers.renderYearProgress(s, cv) })
        register(createDef("calendar.iso.week", Category.CALENDAR, "ISO Week & Julian Day", "Exact calendar metrics and week number", setOf(SizeBucket.S), TapAction.OPEN_CALENDAR) { c, s, cv -> CalendarExtraRenderers.renderIsoWeek(s, cv) })
        register(createDef("calendar.minimal.split", Category.CALENDAR, "Split Calendar Card", "Vertical divided date and month indicator", setOf(SizeBucket.S), TapAction.OPEN_CALENDAR) { c, s, cv -> CalendarExtraRenderers.renderSplitCard(s, cv) })
        register(createDef("calendar.timeline.dots", Category.CALENDAR, "Timeline Dots", "Hourly matrix timeline of daily appointments", setOf(SizeBucket.L), TapAction.OPEN_CALENDAR, requiredPermission = AppPermission.Calendar) { c, s, cv -> CalendarExtraRenderers.renderTimeline(c, s, cv) })
        register(createDef("calendar.life.dots", Category.CALENDAR, "Life in Dots", "The whole day as 144 minute-dots that fill as time passes", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_CALENDAR) { c, s, cv -> ExclusiveRenderers.renderLifeInDots(c, s, cv) })

        // --- 3. BATTERY (8 widgets) ---
        register(createDef("battery.ring", Category.BATTERY, "Battery Dot Ring", "Circular 24-dot dial with center percentage", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_BATTERY, lockSafe = true, options = listOf(
            com.example.core.config.WidgetOption.Slider("ring_dots", "Ring dot count", min = 12, max = 36, step = 4, default = 24, unit = "dots"),
            com.example.core.config.WidgetOption.ColorRule("warn_low", "Low-battery warning", warnRatio = 0.2f, default = true)
        )) { c, s, cv -> BatteryRenderers.renderBatteryRing(c, s, cv) })
        register(createDef("battery.segmented", Category.BATTERY, "Segmented Battery Bar", "20-dot segmented level with temperature", setOf(SizeBucket.M), TapAction.OPEN_BATTERY, options = listOf(
            com.example.core.config.WidgetOption.Slider("segments", "Segment count", min = 10, max = 30, step = 2, default = 20, unit = "dots")
        )) { c, s, cv -> BatteryRenderers.renderSegmentedBar(c, s, cv) })
        register(createDef("battery.combo.devices", Category.BATTERY, "Device Ecosystem Combo", "Phone, Earbuds, and Watch battery gauges", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_BATTERY, requiredPermission = AppPermission.NearbyDevices) { c, s, cv -> LiveDataRenderers.renderDeviceBatteries(c, s, cv, 3) })
        register(createDef("battery.percentage", Category.BATTERY, "Bold Battery %", "Large dot-matrix capacity readout", setOf(SizeBucket.S), TapAction.OPEN_BATTERY) { c, s, cv -> BatteryRenderers.renderBatteryRing(c, s, cv) })
        register(createDef("battery.charging.active", Category.BATTERY, "Charging Indicator", "Active charging state and voltage readout", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_BATTERY) { c, s, cv -> DeviceInfoRenderers.renderChargingIndicator(c, s, cv) })
        register(createDef("battery.health.temp", Category.BATTERY, "Battery Health & Temp", "Thermal metrics and cell health indicator", setOf(SizeBucket.M), TapAction.OPEN_BATTERY) { c, s, cv -> DeviceInfoRenderers.renderBatteryHealth(c, s, cv) })
        register(createDef("battery.screen.on", Category.BATTERY, "Screen-On Time", "Daily usage runtime & discharge rate", setOf(SizeBucket.M), TapAction.OPEN_BATTERY, requiredPermission = AppPermission.UsageAccess) { c, s, cv -> LiveDataRenderers.renderScreenTime(c, s, cv) })
        register(createDef("battery.screen.week", Category.BATTERY, "Weekly Screen Time", "7-day screen-time bar chart with an average", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_BATTERY, requiredPermission = AppPermission.UsageAccess) { c, s, cv -> LiveDataRenderers.renderWeeklyScreenTime(c, s, cv) })
        register(createDef("battery.minimal.pill", Category.BATTERY, "Mini Battery Pill", "Compact capsule battery status", setOf(SizeBucket.S), TapAction.OPEN_BATTERY) { c, s, cv -> BatteryRenderers.renderBatteryRing(c, s, cv) })
        register(createDef("battery.fuel.gauge", Category.BATTERY, "Battery Fuel Gauge", "Horizontal dot gauge with time-to-full or time-left", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_BATTERY) { c, s, cv -> ExclusiveRenderers.renderBatteryFuelGauge(c, s, cv) })

        // --- 4. WEATHER (10 widgets) ---
        register(createDef("weather.current.temp", Category.WEATHER, "Current Temperature", "Real-time temp with dot icon and stale guard", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_WEATHER, true, AppPermission.Location, options = listOf(
            com.example.core.config.WidgetOption.Toggle("show_details", "Show high/low line", default = true)
        )) { c, s, cv -> WeatherRenderers.renderCurrentWeather(c, s, cv) })
        register(createDef("weather.5day.strip", Category.WEATHER, "5-Day Forecast Strip", "5-day outlook with daily high/low dots", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_WEATHER, true, AppPermission.Location) { c, s, cv -> WeatherRenderers.renderForecast5Day(c, s, cv) })
        register(createDef("weather.hourly.strip", Category.WEATHER, "Hourly Forecast Strip", "Next hours with temperature, icon and rain chance", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_WEATHER, true, AppPermission.Location) { c, s, cv -> WeatherRenderers.renderHourlyForecast(c, s, cv) })
        register(createDef("weather.sunrise.arc", Category.WEATHER, "Sun Tracker Arc", "Sunrise and sunset dot arc tracking daylight", setOf(SizeBucket.M), TapAction.OPEN_WEATHER, requiredPermission = AppPermission.Location) { c, s, cv -> WeatherRenderers.renderSunriseSunsetArc(c, s, cv) })
        register(createDef("weather.city.london", Category.WEATHER, "London Weather", "City preset weather for London", setOf(SizeBucket.M), TapAction.OPEN_WEATHER, true, AppPermission.Location) { c, s, cv -> WeatherRenderers.renderCurrentWeather(c, s, cv, "London") })
        register(createDef("weather.city.nyc", Category.WEATHER, "New York Weather", "City preset weather for New York", setOf(SizeBucket.M), TapAction.OPEN_WEATHER, true, AppPermission.Location) { c, s, cv -> WeatherRenderers.renderCurrentWeather(c, s, cv, "New York") })
        register(createDef("weather.city.tokyo", Category.WEATHER, "Tokyo Weather", "City preset weather for Tokyo", setOf(SizeBucket.M), TapAction.OPEN_WEATHER, true, AppPermission.Location) { c, s, cv -> WeatherRenderers.renderCurrentWeather(c, s, cv, "Tokyo") })
        register(createDef("weather.moon.phase", Category.WEATHER, "Moon Phase Tracker", "Current lunar phase dot matrix depiction", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_WEATHER) { c, s, cv -> UniqueSensorRenderers.renderLunarMoonPhase(s, cv) })
        register(createDef("weather.precipitation", Category.WEATHER, "Precipitation Bar", "Hourly rain probability dot run", setOf(SizeBucket.M), TapAction.OPEN_WEATHER, true, AppPermission.Location) { c, s, cv -> WeatherRenderers.renderForecast5Day(c, s, cv, showPrecipitation = true) })
        register(createDef("weather.feels.humidity", Category.WEATHER, "Humidity & Feels Like", "Thermal comfort and atmospheric moisture", setOf(SizeBucket.M), TapAction.OPEN_WEATHER, true, AppPermission.Location) { c, s, cv -> WeatherRenderers.renderComfort(c, s, cv) })
        register(createDef("weather.uv.index", Category.WEATHER, "UV Index Warning", "Solar radiation level & sun safety badge", setOf(SizeBucket.S), TapAction.OPEN_WEATHER, requiredPermission = AppPermission.Location) { c, s, cv -> WeatherRenderers.renderUvIndex(c, s, cv) })

        // --- 5. SYSTEM & SENSORS (12 widgets) ---
        register(createDef("sensor.ram.meter", Category.SYSTEM, "System RAM Vitals", "Real Android memory usage in dot font & 20-dot bar", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT) { c, s, cv -> UniqueSensorRenderers.renderRamMeter(c, s, cv) })
        register(createDef("sensor.storage.bar", Category.SYSTEM, "Storage Breakdown", "Internal storage capacity and free GB bar", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT) { c, s, cv -> UniqueSensorRenderers.renderStorageBar(s, cv) })
        register(createDef("sensor.visualizer.eq", Category.SYSTEM, "Soundwave", "Dot soundwave and title of the song that is playing", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT, requiredPermission = AppPermission.MediaAccess) { c, s, cv -> MediaRenderers.renderSoundwave(c, s, cv) })
        register(createDef("sensor.world.dual", Category.SYSTEM, "Dual Capsule World Time", "Split capsule time for London (GMT) & NYC (EST)", setOf(SizeBucket.M), TapAction.OPEN_CLOCK) { c, s, cv -> UniqueSensorRenderers.renderDualCapsule(s, cv) })
        register(createDef("sensor.year.progress", Category.SYSTEM, "Annual Year Progress", "Day of year countdown and 25-dot annual run", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_CALENDAR) { c, s, cv -> UniqueSensorRenderers.renderYearProgress(s, cv) })
        register(createDef("sensor.lunar.moon", Category.SYSTEM, "Lunar Phase Matrix", "Dot matrix moon phase with illumination readout", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_WEATHER) { c, s, cv -> UniqueSensorRenderers.renderLunarMoonPhase(s, cv) })
        register(createDef("sensor.fitness.rings", Category.SYSTEM, "Concentric Activity Rings", "Dual dot rings for Steps and Active Calories", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, requiredPermission = AppPermission.Activity) { c, s, cv -> UniqueSensorRenderers.renderFitnessActivityRings(c, s, cv) })
        register(createDef("sensor.stopwatch.chrono", Category.SYSTEM, "Dot Chronometer", "Precision split chronometer with lap tag", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT) { c, s, cv -> UniqueSensorRenderers.renderChronographStopwatch(c, s, cv) })
        register(createDef("sensor.glyph.lights", Category.SYSTEM, "Glyph Battery", "Nothing-style segmented glyph ring showing battery level", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT) { c, s, cv -> DeviceInfoRenderers.renderGlyphBattery(c, s, cv) })
        register(createDef("sensor.compass.heading", Category.SYSTEM, "Compass", "Dot ring with cardinal marks and a needle at the last sampled heading", setOf(SizeBucket.S, SizeBucket.M, SizeBucket.L), TapAction.DEFAULT) { c, s, cv -> UniqueSensorRenderers.renderCompass(c, s, cv) })
        register(createDef("sensor.crypto.ticker", Category.SYSTEM, "Crypto Market Ticker", "BTC real-time price with 16-dot trend sparkline", setOf(SizeBucket.M), TapAction.DEFAULT) { c, s, cv -> LiveDataRenderers.renderCrypto(c, s, cv) })
        register(createDef("sensor.battery.quad", Category.SYSTEM, "Ecosystem Quad Dock", "4-up battery matrix for Phone, Ear(2), Watch, CMF", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_BATTERY, requiredPermission = AppPermission.NearbyDevices) { c, s, cv -> LiveDataRenderers.renderDeviceBatteries(c, s, cv, 4) })

        // --- 6. QUICK TOGGLES (10 widgets) ---
        register(createDef("toggle.wifi", Category.TOGGLES, "Wi-Fi Toggle", "Wi-Fi state and connection ring", setOf(SizeBucket.S), TapAction.DEFAULT) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "WI-FI", DeviceToggles.isWifiOn(c), "W") })
        register(createDef("toggle.bluetooth", Category.TOGGLES, "Bluetooth Toggle", "Bluetooth state and pairing status", setOf(SizeBucket.S), TapAction.DEFAULT) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "BLUETOOTH", DeviceToggles.isBluetoothOn(c), "B") })
        register(createDef("toggle.torch", Category.TOGGLES, "Flashlight / Torch", "Instant torch activator", setOf(SizeBucket.S), TapAction.DEFAULT) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "TORCH", DeviceToggles.isTorchOn(c), "T") })
        register(createDef("toggle.dnd", Category.TOGGLES, "Do Not Disturb", "Silence notifications and calls", setOf(SizeBucket.S), TapAction.DEFAULT, requiredPermission = AppPermission.DndAccess) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "DND", DeviceToggles.isDndOn(c), "D") })
        register(createDef("toggle.darkmode", Category.TOGGLES, "Dark Mode Switch", "System dark / light mode toggle", setOf(SizeBucket.S), TapAction.DEFAULT) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "DARK MODE", DeviceToggles.isDarkModeOn(c), "M") })
        register(createDef("toggle.grid.2x2", Category.TOGGLES, "2x2 Quick Action Grid", "Wi-Fi, Bluetooth, Torch, DND quadrant grid", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT) { c, s, cv -> ToggleRenderers.render2x2Grid(s, cv, DeviceToggles.isWifiOn(c), DeviceToggles.isBluetoothOn(c), DeviceToggles.isTorchOn(c), DeviceToggles.isDndOn(c)) })
        register(createDef("toggle.rotation", Category.TOGGLES, "Auto-Rotate Lock", "Screen orientation lock toggle", setOf(SizeBucket.S), TapAction.DEFAULT) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "ROTATION", DeviceToggles.isAutoRotateOn(c), "R") })
        register(createDef("toggle.datasaver", Category.TOGGLES, "Data Saver", "Background data usage limiter", setOf(SizeBucket.S), TapAction.DEFAULT) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "DATA SAVER", DeviceToggles.isDataSaverOn(c), "S") })
        register(createDef("toggle.hotspot", Category.TOGGLES, "Hotspot", "Mobile tethering and hotspot toggle", setOf(SizeBucket.S), TapAction.DEFAULT) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "HOTSPOT", DeviceToggles.isHotspotOn(c), "H") })
        register(createDef("toggle.airplane", Category.TOGGLES, "Airplane Mode", "Radio connectivity lock", setOf(SizeBucket.S), TapAction.DEFAULT) { c, s, cv -> ToggleRenderers.renderSingleToggle(s, cv, "AIRPLANE", DeviceToggles.isAirplaneModeOn(c), "A") })

        // --- 6. CONTACTS & SHORTCUTS (8 widgets) ---
        register(createDef("contact.favorite.single", Category.CONTACTS, "Favorite Contact", "One-tap direct contact card", setOf(SizeBucket.S), TapAction.OPEN_MAIN, requiredPermission = AppPermission.Contacts) { c, s, cv -> ShortcutRenderers.render(c, s, cv, "contact.favorite.single") })
        register(createDef("contact.duo", Category.CONTACTS, "Duo Contacts", "Two favorite contacts speed dial", setOf(SizeBucket.M), TapAction.OPEN_MAIN, requiredPermission = AppPermission.Contacts) { c, s, cv -> ShortcutRenderers.render(c, s, cv, "contact.duo") })
        register(createDef("contact.quad", Category.CONTACTS, "4-Up Contact Grid", "Quick reach 4-contact dock", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_MAIN, requiredPermission = AppPermission.Contacts) { c, s, cv -> ShortcutRenderers.render(c, s, cv, "contact.quad") })
        register(createDef("shortcut.direct.dial", Category.CONTACTS, "Emergency Speed Dial", "Direct phone dialer trigger", setOf(SizeBucket.S), TapAction.OPEN_MAIN) { c, s, cv -> ShortcutRenderers.render(c, s, cv, "shortcut.direct.dial") })
        register(createDef("shortcut.direct.sms", Category.CONTACTS, "Instant Message", "One-tap message compose", setOf(SizeBucket.S), TapAction.OPEN_MAIN) { c, s, cv -> ShortcutRenderers.render(c, s, cv, "shortcut.direct.sms") })
        register(createDef("shortcut.apps.4", Category.CONTACTS, "4-App Dot Tray", "Minimalist app launch matrix", setOf(SizeBucket.M), TapAction.OPEN_MAIN) { c, s, cv -> ShortcutRenderers.render(c, s, cv, "shortcut.apps.4") })
        register(createDef("shortcut.apps.6", Category.CONTACTS, "6-App Quick Dock", "High efficiency app dock", setOf(SizeBucket.L), TapAction.OPEN_MAIN) { c, s, cv -> ShortcutRenderers.render(c, s, cv, "shortcut.apps.6") })
        register(createDef("utility.folder.grid", Category.CONTACTS, "App Folder", "Eight pinned apps in one folder tile", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_MAIN) { c, s, cv -> ShortcutRenderers.render(c, s, cv, "utility.folder.grid") })
        register(createDef("shortcut.web.favorite", Category.CONTACTS, "Web Bookmark", "Direct browser link shortcut", setOf(SizeBucket.S), TapAction.OPEN_MAIN) { c, s, cv -> ShortcutRenderers.render(c, s, cv, "shortcut.web.favorite") })

        // --- 7. MEDIA (4 widgets) ---
        register(createDef("media.nowplaying.compact", Category.MEDIA, "Now Playing Compact", "Track name and dot progress", setOf(SizeBucket.M), TapAction.DEFAULT, requiredPermission = AppPermission.MediaAccess) { c, s, cv -> MediaRenderers.renderCompact(c, s, cv) })
        register(createDef("media.nowplaying.art", Category.MEDIA, "Now Playing Card", "Full album & artist playback card", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT, requiredPermission = AppPermission.MediaAccess) { c, s, cv -> MediaRenderers.renderArtCard(c, s, cv) })
        register(createDef("media.controls.bar", Category.MEDIA, "Media Controls Strip", "Prev, Play/Pause, Next dot targets", setOf(SizeBucket.M), TapAction.DEFAULT, requiredPermission = AppPermission.MediaAccess) { c, s, cv -> MediaRenderers.renderControls(c, s, cv) })
        register(createDef("media.volume.dots", Category.MEDIA, "Volume Level Dots", "15-dot master audio level indicator", setOf(SizeBucket.M), TapAction.DEFAULT) { c, s, cv -> DeviceInfoRenderers.renderVolumeLevels(c, s, cv) })

        // --- 8. PRODUCTIVITY (8 widgets) ---
        register(createDef("prod.search.bar", Category.PRODUCTIVITY, "Search Bar", "Tap-to-search pill that opens web search", setOf(SizeBucket.M), TapAction.OPEN_SEARCH) { c, s, cv -> UtilityRenderers.renderSearchBar(c, s, cv) })
        register(createDef("prod.notes.pin", Category.PRODUCTIVITY, "Notes Pin", "Pinned quick memo and thoughts", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_MAIN) { c, s, cv -> ProductivityRenderers.renderNotesPin(c, s, cv) })
        register(createDef("prod.todo.3", Category.PRODUCTIVITY, "To-Do Checklist (3)", "Daily task checklist with checkmarks", setOf(SizeBucket.M), TapAction.OPEN_MAIN) { c, s, cv -> ProductivityRenderers.renderTodoList(c, s, cv, 3) })
        register(createDef("prod.todo.5", Category.PRODUCTIVITY, "To-Do Checklist (5)", "Extended 5-item daily checklist", setOf(SizeBucket.L), TapAction.OPEN_MAIN) { c, s, cv -> ProductivityRenderers.renderTodoList(c, s, cv, 5) })

        // --- Phase 10: scrollable lists (RemoteViewsService rows) and the swipeable Smart Stack ---
        register(createDef(CollectionWidgets.TODO, Category.PRODUCTIVITY, "Scrolling To-Do", "Every task in a scrolling list; tap a task to tick it off", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_MAIN) { c, s, cv -> CollectionWidgets.renderList(c, s, cv, CollectionWidgets.TODO) })
        register(createDef(CollectionWidgets.SHOPPING, Category.PRODUCTIVITY, "Shopping List", "Scrolling shopping list; tap an item to check it", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_MAIN) { c, s, cv -> CollectionWidgets.renderList(c, s, cv, CollectionWidgets.SHOPPING) })
        register(createDef(CollectionWidgets.HABITS, Category.PRODUCTIVITY, "Habit List", "Several habits with streaks; tap one to mark today done", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_MAIN) { c, s, cv -> CollectionWidgets.renderList(c, s, cv, CollectionWidgets.HABITS) })
        register(createDef(CollectionWidgets.WEEK_AGENDA, Category.CALENDAR, "Week Agenda (Scroll)", "Your events for the next 7 days in a scrolling list", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_CALENDAR, requiredPermission = AppPermission.Calendar) { c, s, cv -> CollectionWidgets.renderList(c, s, cv, CollectionWidgets.WEEK_AGENDA) })
        register(createDef(CollectionWidgets.FAVORITES, Category.CONTACTS, "Favourite Contacts (Scroll)", "Your starred contacts; tap one to call", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_MAIN, requiredPermission = AppPermission.Contacts) { c, s, cv -> CollectionWidgets.renderList(c, s, cv, CollectionWidgets.FAVORITES) })
        register(createDef(CollectionWidgets.SMART_STACK, Category.PRODUCTIVITY, "Smart Stack", "Up to 5 of your widgets in one spot; swipe between them", setOf(SizeBucket.S, SizeBucket.M, SizeBucket.L), TapAction.OPEN_MAIN) { c, s, cv -> CollectionWidgets.renderStackBackground(c, s, cv, CollectionWidgets.stackItems(s.configJson)) })

        // --- Phase 10.3: multi-button widgets (tap grid matches InteractiveRenderers) ---
        register(createDef("prod.tally.counter", Category.PRODUCTIVITY, "Tally Counter", "Count anything: tap − or +", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_MAIN) { c, s, cv -> InteractiveRenderers.renderTally(c, s, cv) })
        register(createDef("health.mood.log", Category.HEALTH, "Mood Log", "Tap how you feel today; see your week", setOf(SizeBucket.M), TapAction.OPEN_MAIN) { c, s, cv -> InteractiveRenderers.renderMood(c, s, cv) })
        register(createDef("prod.timer.presets", Category.PRODUCTIVITY, "Quick Timer", "One-tap 1, 5, 10 or 25 minute countdown", setOf(SizeBucket.M), TapAction.OPEN_MAIN) { c, s, cv -> InteractiveRenderers.renderTimerPresets(c, s, cv) })
        register(createDef("toggle.volume.steps", Category.TOGGLES, "Volume Steps", "Set media volume to mute, 33, 66 or 100% in one tap", setOf(SizeBucket.M), TapAction.OPEN_MAIN) { c, s, cv -> InteractiveRenderers.renderVolumeSteps(c, s, cv) })

        // --- Phase 11: data widgets (no new permissions; data use + top apps use Usage access) ---
        register(createDef("sys.next.alarm", Category.CLOCK, "Next Alarm", "Your next alarm and how long until it rings", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_CLOCK, lockSafe = true) { c, s, cv -> InsightRenderers.renderNextAlarm(c, s, cv) })
        register(createDef("sys.network.speed", Category.SYSTEM, "Network Speed", "Download and upload speed over the last minute", setOf(SizeBucket.M), TapAction.OPEN_MAIN) { c, s, cv -> InsightRenderers.renderNetworkSpeed(c, s, cv) })
        register(createDef("sys.thermal.uptime", Category.SYSTEM, "Thermal & Uptime", "Phone temperature state and time since restart", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_MAIN) { c, s, cv -> InsightRenderers.renderThermalUptime(c, s, cv) })
        register(createDef("battery.history.24h", Category.BATTERY, "Battery History", "Your battery level over the last 24 hours", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_BATTERY) { c, s, cv -> InsightRenderers.renderBatteryHistory(c, s, cv) })
        register(createDef("weather.air.quality", Category.WEATHER, "Air Quality", "Live AQI with PM2.5 and PM10 for your city", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_WEATHER, requiresNetwork = true) { c, s, cv -> InsightRenderers.renderAirQuality(c, s, cv) })
        register(createDef("weather.golden.hour", Category.WEATHER, "Golden Hour", "Golden and blue hour times for photos today", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_WEATHER) { c, s, cv -> InsightRenderers.renderGoldenHour(c, s, cv) })
        register(createDef("sys.top.apps", Category.SYSTEM, "Top Apps Today", "The apps you used most today", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_MAIN, requiredPermission = AppPermission.UsageAccess) { c, s, cv -> InsightRenderers.renderTopApps(c, s, cv) })
        register(createDef("sys.mobile.data", Category.SYSTEM, "Mobile Data Today", "Mobile data used since midnight", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_MAIN, requiredPermission = AppPermission.UsageAccess) { c, s, cv -> InsightRenderers.renderMobileData(c, s, cv) })
        register(createDef("calendar.india.festivals", Category.CALENDAR, "Festivals & Holidays", "Upcoming Indian festivals and national holidays", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_CALENDAR, lockSafe = true) { c, s, cv -> InsightRenderers.renderFestivals(c, s, cv) })
        register(createDef("calendar.india.tithi", Category.CALENDAR, "Tithi", "Today's Hindu lunar day and paksha", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_CALENDAR, lockSafe = true) { c, s, cv -> InsightRenderers.renderTithi(s, cv) })
        register(createDef("prod.step.counter", Category.PRODUCTIVITY, "Daily Step Counter", "Live step metrics with 20-dot progress run", setOf(SizeBucket.M), TapAction.OPEN_MAIN, requiredPermission = AppPermission.Activity, options = listOf(
            com.example.core.config.WidgetOption.Slider("goal", "Daily step goal", min = 1000, max = 30000, step = 1000, default = 10000, unit = "steps"),
            com.example.core.config.WidgetOption.Toggle("show_pct", "Show % of goal", default = true)
        )) { c, s, cv -> ProductivityRenderers.renderStepCounter(c, s, cv) })
        register(createDef("prod.water.tracker", Category.PRODUCTIVITY, "Hydration Tracker", "8-glass daily water intake dots", setOf(SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            com.example.core.config.WidgetOption.Slider("glasses_goal", "Daily glasses", min = 4, max = 16, step = 1, default = 8, unit = "glasses")
        )) { c, s, cv -> ProductivityRenderers.renderWaterTracker(c, s, cv) })
        register(createDef("prod.focus.timer", Category.PRODUCTIVITY, "Focus Timer", "Pomodoro work session tracker", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT) { c, s, cv -> ProductivityRenderers.renderFocusTimer(c, s, cv) })
        register(createDef("prod.habit.dots", Category.PRODUCTIVITY, "Habit Matrix Streak", "Weekly habit completion dot grid", setOf(SizeBucket.M), TapAction.OPEN_MAIN) { c, s, cv -> CalendarExtraRenderers.renderHabitMatrix(c, s, cv) })
        register(createDef("prod.countdown.timer", Category.PRODUCTIVITY, "Countdown Pill", "Target minutes remaining indicator", setOf(SizeBucket.S), TapAction.DEFAULT) { c, s, cv -> ProductivityRenderers.renderCountdownTimer(c, s, cv) })
        register(createDef("prod.week.dots", Category.PRODUCTIVITY, "Week in Dots", "7-column heatmap of the week's elapsed hours", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_CALENDAR) { c, s, cv -> ExclusiveRenderers.renderWeekInDots(c, s, cv) })
        register(createDef("prod.focus.companion", Category.PRODUCTIVITY, "Focus Companion", "Focus-timer dot ring with a silent streak row", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT) { c, s, cv -> ExclusiveRenderers.renderFocusCompanion(c, s, cv) })

        // --- 9. PHOTO (4 widgets) ---
        register(createDef("photo.single.frame", Category.PHOTO, "Dot Border Photo Frame", "Aesthetic minimal framed photo", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT) { c, s, cv -> PhotoRenderers.renderFrame(c, s, cv) })
        register(createDef("photo.clock.overlay", Category.PHOTO, "Photo + Clock Overlay", "Wallpaper frame with dot time badge", setOf(SizeBucket.L), TapAction.OPEN_CLOCK) { c, s, cv -> PhotoRenderers.renderClockOverlay(c, s, cv) })
        register(createDef("photo.duo.collage", Category.PHOTO, "Duo Split Photo", "Two minimal framed photos", setOf(SizeBucket.M), TapAction.DEFAULT) { c, s, cv -> PhotoRenderers.renderDuo(c, s, cv) })
        register(createDef("photo.monochrome.art", Category.PHOTO, "Monochrome Halftone Art", "Dot matrix stylized art frame", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT) { c, s, cv -> PhotoRenderers.renderHalftone(c, s, cv) })

        // --- 10. FUN & LAB (4 widgets) ---
        register(createDef("fun.snake.dot", Category.FUN, "Retro Dot Snake", "Classic retro dot matrix snake on screen", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT) { c, s, cv -> MediaAndFunRenderers.renderRetroSnake(c, s, cv) })
        register(createDef("fun.quote.daily", Category.FUN, "Daily Quote", "Inspiring minimalist thought in dot font", setOf(SizeBucket.M), TapAction.DEFAULT) { c, s, cv -> MediaAndFunRenderers.renderDailyQuote(s, cv) })
        register(createDef("fun.dice.roll", Category.FUN, "Dice Roll Pair", "Tap to roll a pair of dot-matrix dice", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT) { c, s, cv -> MediaAndFunRenderers.renderDiceRoll(c, s, cv) })
        register(createDef("fun.pattern.generator", Category.FUN, "Ambient Dot Matrix", "Procedural geometric dot matrix rhythm", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT) { c, s, cv -> DeviceInfoRenderers.renderAmbientPattern(s, cv) })
        register(createDef("fun.orbit.ambient", Category.FUN, "Orbit Ambient", "Slow concentric dot rings drifting with the clock", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT) { c, s, cv -> ExclusiveRenderers.renderOrbitAmbient(s, cv) })

        // --- 11. FINANCE (12 widgets) ---
        register(createDef("finance.crypto.tick", Category.FINANCE, "Crypto Ticker", "Live coin price with 24h move", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            com.example.core.config.WidgetOption.Text("coin", "Coin symbol", default = "BTC"),
            com.example.core.config.WidgetOption.Toggle("up_down_colour", "Colour down moves", default = true)
        )) { c, s, cv -> FinanceRenderers.renderCryptoTick(c, s, cv) })
        register(createDef("finance.crypto.spark", Category.FINANCE, "Crypto Sparkline", "Coin price with an intraday dot sparkline", setOf(SizeBucket.M), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            com.example.core.config.WidgetOption.Text("coin", "Coin symbol", default = "BTC"),
            com.example.core.config.WidgetOption.Choice.of("range", "Timeframe", listOf("24H", "7D", "30D"), "24H"),
            com.example.core.config.WidgetOption.Toggle("fill", "Fill under the line", default = true)
        )) { c, s, cv -> FinanceRenderers.renderCryptoSpark(c, s, cv) })
        register(createDef("finance.crypto.portfolio", Category.FINANCE, "Crypto Portfolio", "Your holdings with live BTC valuation", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            com.example.core.config.WidgetOption.Text("currency", "Display currency", default = "USD"),
            com.example.core.config.WidgetOption.Toggle("show_units", "Show units", default = true)
        )) { c, s, cv -> FinanceRenderers.renderPortfolio(c, s, cv) })
        register(createDef("finance.stock.tick", Category.FINANCE, "Stock Ticker", "Last-known share price with change", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            com.example.core.config.WidgetOption.Text("symbol", "Ticker symbol", default = "AAPL"),
            com.example.core.config.WidgetOption.Choice.of("market", "Market", listOf("US", "LSE", "NSE"), "US")
        )) { c, s, cv -> FinanceRenderers.renderStockTick(c, s, cv) })
        register(createDef("finance.stock.spark", Category.FINANCE, "Stock Sparkline", "Share price with a trend sparkline", setOf(SizeBucket.M), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            com.example.core.config.WidgetOption.Text("symbol", "Ticker symbol", default = "AAPL"),
            com.example.core.config.WidgetOption.Choice.of("range", "Range", listOf("1D", "1W", "1M"), "1M")
        )) { c, s, cv -> FinanceRenderers.renderStockSpark(c, s, cv) })
        register(createDef("finance.currency.pair", Category.FINANCE, "FX Rate Pair", "Live exchange rate for a currency pair", setOf(SizeBucket.M), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            com.example.core.config.WidgetOption.Text("base", "Base currency", default = "USD"),
            com.example.core.config.WidgetOption.Text("quote", "Quote currency", default = "INR"),
            com.example.core.config.WidgetOption.Toggle("inverse", "Invert pair", default = false)
        )) { c, s, cv -> FinanceRenderers.renderCurrencyPair(c, s, cv) })
        register(createDef("finance.currency.multi", Category.FINANCE, "Multi-FX Board", "Up to six currencies against a base", setOf(SizeBucket.L), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            com.example.core.config.WidgetOption.Text("base", "Base currency", default = "USD"),
            com.example.core.config.WidgetOption.Text("pairs", "Currencies (comma sep)", default = "EUR,GBP,JPY,INR,AUD,CAD")
        )) { c, s, cv -> FinanceRenderers.renderCurrencyMulti(c, s, cv) })
        register(createDef("finance.market.status", Category.FINANCE, "Market Open/Closed", "Exchange trading-hours status", setOf(SizeBucket.S), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Choice.of("exchange", "Exchange", listOf("NYSE", "LSE", "NSE", "TSE"), "NYSE"),
            com.example.core.config.WidgetOption.Toggle("holiday_aware", "Holiday aware", default = true)
        )) { c, s, cv -> FinanceRenderers.renderMarketStatus(c, s, cv) })
        register(createDef("finance.spend.daily", Category.FINANCE, "Daily Spend Meter", "Spend today versus your daily budget", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Slider("budget", "Daily budget", min = 100, max = 20000, step = 100, default = 2000, unit = "₹"),
            com.example.core.config.WidgetOption.Toggle("show_bar", "Show progress bar", default = true)
        )) { c, s, cv -> FinanceRenderers.renderSpendDaily(c, s, cv) })
        register(createDef("finance.budget.ring", Category.FINANCE, "Budget Ring", "Spend against budget as a dot ring", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Slider("warn_pct", "Warn at % of budget", min = 50, max = 100, step = 5, default = 80, unit = "%"),
            com.example.core.config.WidgetOption.Choice.of("period", "Period", listOf("DAILY", "WEEKLY", "MONTHLY"), "DAILY")
        )) { c, s, cv -> FinanceRenderers.renderBudgetRing(c, s, cv) })
        register(createDef("finance.savings.goal", Category.FINANCE, "Savings Goal Dots", "Progress toward a savings target", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Slider("goal", "Goal amount", min = 10000, max = 1000000, step = 10000, default = 100000, unit = "₹"),
            com.example.core.config.WidgetOption.Text("target_date", "Target date", default = "")
        )) { c, s, cv -> FinanceRenderers.renderSavingsGoal(c, s, cv) })
        register(createDef("finance.payday.countdown", Category.FINANCE, "Payday Countdown", "Days until the next salary date", setOf(SizeBucket.S), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Slider("payday_dom", "Payday (day of month)", min = 1, max = 28, step = 1, default = 1, unit = ""),
            com.example.core.config.WidgetOption.Toggle("show_month", "Show month name", default = false)
        )) { c, s, cv -> FinanceRenderers.renderPaydayCountdown(c, s, cv) })

        // --- 12. HEALTH & FITNESS (12 widgets) ---
        register(createDef("health.steps.ring", Category.HEALTH, "Steps Ring", "Today's steps as a dot ring", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_MAIN, requiredPermission = AppPermission.Activity, options = listOf(
            com.example.core.config.WidgetOption.Slider("goal", "Daily step goal", min = 1000, max = 30000, step = 1000, default = 10000, unit = "steps"),
            com.example.core.config.WidgetOption.Choice.of("style", "Style", listOf("ring", "text"), "ring")
        )) { c, s, cv -> HealthRenderers.renderStepsRing(c, s, cv) })
        register(createDef("health.steps.week", Category.HEALTH, "7-Day Steps", "This week's step columns", setOf(SizeBucket.M), TapAction.OPEN_MAIN, requiredPermission = AppPermission.Activity, options = listOf(
            com.example.core.config.WidgetOption.Slider("goal", "Daily step goal", min = 1000, max = 30000, step = 1000, default = 10000, unit = "steps"),
            com.example.core.config.WidgetOption.Toggle("show_goal", "Show goal line", default = true)
        )) { c, s, cv -> HealthRenderers.renderStepsWeek(c, s, cv) })
        register(createDef("health.water.dots", Category.HEALTH, "Hydration Dots", "Glass-by-glass hydration grid", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            com.example.core.config.WidgetOption.Slider("glasses_goal", "Daily glasses", min = 4, max = 16, step = 1, default = 8, unit = "glasses"),
            com.example.core.config.WidgetOption.Choice.of("glass_size", "Glass size", listOf("200ml", "250ml", "330ml"), "250ml")
        )) { c, s, cv -> HealthRenderers.renderWaterDots(c, s, cv) })
        register(createDef("health.sleep.last", Category.HEALTH, "Last Night Sleep", "Last night's sleep duration", setOf(SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            com.example.core.config.WidgetOption.Slider("goal_hours", "Sleep goal", min = 5, max = 10, step = 1, default = 8, unit = "h"),
            com.example.core.config.WidgetOption.Toggle("show_goal", "Show sleep goal", default = true)
        )) { c, s, cv -> HealthRenderers.renderSleepLast(c, s, cv) })
        register(createDef("health.sleep.week", Category.HEALTH, "Sleep Week", "Seven nights of sleep as bars", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_MAIN, options = listOf(
            com.example.core.config.WidgetOption.Slider("goal_hours", "Sleep goal", min = 5, max = 10, step = 1, default = 8, unit = "h"),
            com.example.core.config.WidgetOption.Toggle("show_avg", "Show average", default = true)
        )) { c, s, cv -> HealthRenderers.renderSleepWeek(c, s, cv) })
        register(createDef("health.heart.rate", Category.HEALTH, "Heart Rate", "Resting heart rate with a pulse trace", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            com.example.core.config.WidgetOption.Toggle("resting", "Resting vs live", default = true),
            com.example.core.config.WidgetOption.Toggle("show_trace", "Show pulse trace", default = true)
        )) { c, s, cv -> HealthRenderers.renderHeartRate(c, s, cv) })
        register(createDef("health.workout.streak", Category.HEALTH, "Workout Streak", "Consecutive workout days and week dots", setOf(SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            com.example.core.config.WidgetOption.Slider("weekly_target", "Weekly target", min = 1, max = 7, step = 1, default = 5, unit = "days"),
            com.example.core.config.WidgetOption.Toggle("show_week", "Show week dots", default = true)
        )) { c, s, cv -> HealthRenderers.renderWorkoutStreak(c, s, cv) })
        register(createDef("health.calories.ring", Category.HEALTH, "Calories Ring", "Active calories against a goal ring", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            com.example.core.config.WidgetOption.Slider("goal", "Calorie goal", min = 100, max = 3000, step = 50, default = 600, unit = "kcal"),
            com.example.core.config.WidgetOption.Choice.of("kind", "Active vs total", listOf("active", "total", "basal"), "active")
        )) { c, s, cv -> HealthRenderers.renderCaloriesRing(c, s, cv) })
        register(createDef("health.weight.trend", Category.HEALTH, "Weight Trend", "Recent weight entries as a trend line", setOf(SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            com.example.core.config.WidgetOption.Choice.of("unit", "Unit", listOf("KG", "LB"), "KG"),
            com.example.core.config.WidgetOption.Slider("goal_kg", "Goal weight", min = 40, max = 150, step = 1, default = 70, unit = "kg")
        )) { c, s, cv -> HealthRenderers.renderWeightTrend(c, s, cv) })
        register(createDef("health.meditation", Category.HEALTH, "Mindfulness Minutes", "Today's mindful minutes against a goal", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            com.example.core.config.WidgetOption.Slider("goal", "Daily goal", min = 1, max = 60, step = 1, default = 10, unit = "min"),
            com.example.core.config.WidgetOption.Toggle("show_zero", "Show when zero", default = false)
        )) { c, s, cv -> HealthRenderers.renderMeditation(c, s, cv) })
        register(createDef("health.stand.hours", Category.HEALTH, "Stand Hours", "Hours you moved today as a ring", setOf(SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            com.example.core.config.WidgetOption.Slider("goal", "Daily goal", min = 1, max = 24, step = 1, default = 12, unit = "hours"),
            com.example.core.config.WidgetOption.Choice.of("style", "Style", listOf("ring", "bar"), "ring")
        )) { c, s, cv -> HealthRenderers.renderStandHours(c, s, cv) })
        register(createDef("health.body.battery", Category.HEALTH, "Body Battery", "Energy level as a fuel gauge", setOf(SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            com.example.core.config.WidgetOption.Choice.of("formula", "Formula", listOf("BALANCED", "STRESS", "SLEEP"), "BALANCED"),
            com.example.core.config.WidgetOption.Toggle("show_charge", "Show charge label", default = true)
        )) { c, s, cv -> HealthRenderers.renderBodyBattery(c, s, cv) })

        // --- 13. SMART HOME (10 widgets) ---
        register(createDef("home.device.toggle", Category.SMART_HOME, "Device Toggle", "On/off ring for one Home Assistant entity", setOf(SizeBucket.S), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            com.example.core.config.WidgetOption.Text("entity", "Entity id", default = "switch.lamp"),
            com.example.core.config.WidgetOption.Text("label", "Label", default = "DEVICE")
        )) { c, s, cv -> SmartHomeRenderers.renderDeviceToggle(c, s, cv) })
        register(createDef("home.room.status", Category.SMART_HOME, "Room Status Grid", "Multiple entities as a status grid", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            com.example.core.config.WidgetOption.Text("entities", "Entity ids (comma sep)", default = "light.living_room,light.kitchen")
        )) { c, s, cv -> SmartHomeRenderers.renderRoomStatus(c, s, cv) })
        register(createDef("home.lights.count", Category.SMART_HOME, "Lights On Counter", "How many lights are on right now", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            com.example.core.config.WidgetOption.Text("entities", "Light entity ids", default = "light.living_room,light.bedroom"),
            com.example.core.config.WidgetOption.Text("label", "Label", default = "LIGHTS")
        )) { c, s, cv -> SmartHomeRenderers.renderLightsCount(c, s, cv) })
        register(createDef("home.thermostat", Category.SMART_HOME, "Thermostat Dial", "Current temperature as a dial", setOf(SizeBucket.M), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            com.example.core.config.WidgetOption.Text("entity", "Climate entity", default = "climate.thermostat"),
            com.example.core.config.WidgetOption.Choice.of("unit", "Unit", listOf("°C", "°F"), "°C")
        )) { c, s, cv -> SmartHomeRenderers.renderThermostat(c, s, cv) })
        register(createDef("home.scene.button", Category.SMART_HOME, "Scene Button", "One-tap scene trigger", setOf(SizeBucket.S), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            com.example.core.config.WidgetOption.Text("scene", "Scene name", default = "MOVIE NIGHT"),
            com.example.core.config.WidgetOption.Text("entity", "Scene entity", default = "scene.movie_night")
        )) { c, s, cv -> SmartHomeRenderers.renderSceneButton(c, s, cv) })
        register(createDef("home.energy.today", Category.SMART_HOME, "Energy Today", "Today's energy consumption", setOf(SizeBucket.M), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            com.example.core.config.WidgetOption.Text("entity", "Energy sensor", default = "sensor.energy_today"),
            com.example.core.config.WidgetOption.Text("currency", "Unit", default = "kWh")
        )) { c, s, cv -> SmartHomeRenderers.renderEnergyToday(c, s, cv) })
        register(createDef("home.sensor.temp", Category.SMART_HOME, "Room Temperature", "A room temperature sensor", setOf(SizeBucket.S), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            com.example.core.config.WidgetOption.Text("entity", "Temperature sensor", default = "sensor.room_temperature"),
            com.example.core.config.WidgetOption.Toggle("show_name", "Show sensor name", default = true)
        )) { c, s, cv -> SmartHomeRenderers.renderSensorTemp(c, s, cv) })
        register(createDef("home.humidity", Category.SMART_HOME, "Humidity Gauge", "A humidity sensor with a dot bar", setOf(SizeBucket.S), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            com.example.core.config.WidgetOption.Text("entity", "Humidity sensor", default = "sensor.humidity"),
            com.example.core.config.WidgetOption.Toggle("show_bar", "Show dot bar", default = true)
        )) { c, s, cv -> SmartHomeRenderers.renderHumidity(c, s, cv) })
        register(createDef("home.door.status", Category.SMART_HOME, "Door/Window Status", "Open/closed state of doors and windows", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            com.example.core.config.WidgetOption.Text("entities", "Contact sensors", default = "binary_sensor.front_door,binary_sensor.window")
        )) { c, s, cv -> SmartHomeRenderers.renderDoorStatus(c, s, cv) })
        register(createDef("home.presence", Category.SMART_HOME, "Presence Tracker", "Who is home right now", setOf(SizeBucket.S), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            com.example.core.config.WidgetOption.Text("entities", "Person entities", default = "person.alex,person.sam")
        )) { c, s, cv -> SmartHomeRenderers.renderPresence(c, s, cv) })

        // --- 14. SPORTS & LIVE (10 widgets) ---
        register(createDef("sports.score.live", Category.SPORTS, "Live Score", "Followed team's live or final score", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Text("team", "Team", default = ""),
            com.example.core.config.WidgetOption.Text("league", "League", default = "LIVE SCORE")
        )) { c, s, cv -> SportsRenderers.renderLiveScore(c, s, cv) })
        register(createDef("sports.fixture.next", Category.SPORTS, "Next Fixture", "Next match with kickoff time", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Text("team", "Team", default = ""),
            com.example.core.config.WidgetOption.Toggle("show_time", "Show kickoff time", default = true)
        )) { c, s, cv -> SportsRenderers.renderNextFixture(c, s, cv) })
        register(createDef("sports.table.mini", Category.SPORTS, "Mini League Table", "Compact league standings", setOf(SizeBucket.L), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Text("league", "League", default = "LEAGUE"),
            com.example.core.config.WidgetOption.Slider("rows", "Rows shown", min = 3, max = 12, step = 1, default = 8, unit = "rows")
        )) { c, s, cv -> SportsRenderers.renderMiniTable(c, s, cv) })
        register(createDef("sports.form.guide", Category.SPORTS, "Form Guide", "Recent W/D/L results as dots", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Text("team", "Team", default = ""),
            com.example.core.config.WidgetOption.Slider("games", "Games shown", min = 3, max = 10, step = 1, default = 6, unit = "games")
        )) { c, s, cv -> SportsRenderers.renderFormGuide(c, s, cv) })
        register(createDef("sports.countdown.match", Category.SPORTS, "Match Countdown", "Days until the next match", setOf(SizeBucket.S), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Text("team", "Team", default = ""),
            com.example.core.config.WidgetOption.Toggle("show_opponent", "Show opponent", default = true)
        )) { c, s, cv -> SportsRenderers.renderMatchCountdown(c, s, cv) })
        register(createDef("sports.f1.next.race", Category.SPORTS, "Next F1 Race", "Next Grand Prix on the calendar", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Choice.of("series", "Series", listOf("F1", "F2", "MotoGP"), "F1"),
            com.example.core.config.WidgetOption.Toggle("show_circuit", "Show circuit", default = true)
        )) { c, s, cv -> SportsRenderers.renderF1NextRace(c, s, cv) })
        register(createDef("sports.cricket.overs", Category.SPORTS, "Cricket Score", "Runs/wickets with overs context", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Text("match", "Match id", default = ""),
            com.example.core.config.WidgetOption.Slider("overs", "Overs", min = 5, max = 50, step = 5, default = 20, unit = "ov")
        )) { c, s, cv -> SportsRenderers.renderCricketOvers(c, s, cv) })
        register(createDef("sports.tennis.sets", Category.SPORTS, "Tennis Sets", "Current sets score", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Text("match", "Match id", default = ""),
            com.example.core.config.WidgetOption.Choice.of("best_of", "Best of", listOf("3", "5"), "3")
        )) { c, s, cv -> SportsRenderers.renderTennisSets(c, s, cv) })
        register(createDef("sports.standings.dot", Category.SPORTS, "Standings Dots", "League table as dot bars", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Text("league", "League", default = "LEAGUE"),
            com.example.core.config.WidgetOption.Slider("rows", "Rows shown", min = 3, max = 10, step = 1, default = 6, unit = "rows")
        )) { c, s, cv -> SportsRenderers.renderStandingsDots(c, s, cv) })
        register(createDef("sports.highlights.link", Category.SPORTS, "Highlights Link", "Tap to open sports highlights", setOf(SizeBucket.S), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Text("label", "Label", default = "WATCH HIGHLIGHTS"),
            com.example.core.config.WidgetOption.Text("url", "URL", default = "")
        )) { c, s, cv -> SportsRenderers.renderHighlightsLink(c, s, cv) })

        // --- 15. LEARNING & FOCUS (10 widgets) ---
        register(createDef("learn.pomodoro.ring", Category.LEARNING, "Pomodoro Ring", "Focus-timer remaining time in a dot ring", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Slider("work_min", "Work preset", min = 15, max = 60, step = 5, default = 25, unit = "min"),
            com.example.core.config.WidgetOption.Slider("break_min", "Break preset", min = 3, max = 15, step = 1, default = 5, unit = "min")
        )) { c, s, cv -> LearnRenderers.renderPomodoroRing(c, s, cv) })
        register(createDef("learn.study.sessions", Category.LEARNING, "Study Sessions", "Sessions completed today against a target", setOf(SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            com.example.core.config.WidgetOption.Slider("target", "Daily target", min = 1, max = 12, step = 1, default = 3, unit = "sessions"),
            com.example.core.config.WidgetOption.Toggle("show_bar", "Show progress bar", default = true)
        )) { c, s, cv -> LearnRenderers.renderStudySessions(c, s, cv) })
        register(createDef("learn.flashcard.daily", Category.LEARNING, "Word of the Day", "A new vocabulary word every day", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Choice.of("language", "Language", listOf("en", "es", "fr"), "en"),
            com.example.core.config.WidgetOption.Toggle("show_meaning", "Show meaning", default = true)
        )) { c, s, cv -> LearnRenderers.renderWordOfDay(c, s, cv) })
        register(createDef("learn.reading.goal", Category.LEARNING, "Reading Goal", "Pages (or minutes) read today", setOf(SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            com.example.core.config.WidgetOption.Slider("goal", "Daily goal", min = 5, max = 200, step = 5, default = 20, unit = ""),
            com.example.core.config.WidgetOption.Choice.of("unit", "Unit", listOf("pages", "mins"), "pages")
        )) { c, s, cv -> LearnRenderers.renderReadingGoal(c, s, cv) })
        register(createDef("learn.streak.days", Category.LEARNING, "Learning Streak", "Consecutive learning days", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            com.example.core.config.WidgetOption.Slider("min_streak", "Goal streak", min = 3, max = 100, step = 1, default = 7, unit = "days"),
            com.example.core.config.WidgetOption.Toggle("show_goal", "Show goal", default = true)
        )) { c, s, cv -> LearnRenderers.renderLearningStreak(c, s, cv) })
        register(createDef("learn.quiz.daily", Category.LEARNING, "Daily Quiz", "A daily quiz question from the bundled list", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Choice.of("category", "Category", listOf("SCIENCE", "HISTORY", "GEOGRAPHY", "MATH"), "SCIENCE"),
            com.example.core.config.WidgetOption.Toggle("show_options", "Show answer options", default = true)
        )) { c, s, cv -> LearnRenderers.renderDailyQuiz(c, s, cv) })
        register(createDef("learn.focus.total", Category.LEARNING, "Total Focus Time", "Accumulated focus time", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Choice.of("period", "Period", listOf("today", "week", "all"), "today"),
            com.example.core.config.WidgetOption.Toggle("show_label", "Show label", default = true)
        )) { c, s, cv -> LearnRenderers.renderFocusTotal(c, s, cv) })
        register(createDef("learn.course.progress", Category.LEARNING, "Course Progress", "Modules completed in a course", setOf(SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            com.example.core.config.WidgetOption.Text("course", "Course name", default = "COURSE"),
            com.example.core.config.WidgetOption.Slider("modules", "Total modules", min = 1, max = 60, step = 1, default = 12, unit = "modules")
        )) { c, s, cv -> LearnRenderers.renderCourseProgress(c, s, cv) })
        register(createDef("learn.notes.last", Category.LEARNING, "Last Note Snippet", "The most recent note text", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            com.example.core.config.WidgetOption.Toggle("show_title", "Show title", default = true),
            com.example.core.config.WidgetOption.Toggle("show_body", "Show body", default = true)
        )) { c, s, cv -> LearnRenderers.renderLastNote(c, s, cv) })
        register(createDef("learn.timer.exam", Category.LEARNING, "Exam Countdown", "Days until an exam", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            com.example.core.config.WidgetOption.Text("exam", "Exam name", default = "EXAM"),
            com.example.core.config.WidgetOption.Toggle("show_date", "Show date", default = false)
        )) { c, s, cv -> LearnRenderers.renderExamCountdown(c, s, cv) })

        // --- 16. TRAVEL & WORLD (10 widgets) ---
        register(createDef("travel.clock.multi", Category.TRAVEL, "Multi-City Clock", "Up to four world cities at once", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Text("zones", "Zones (comma-separated)", default = "Europe/London,Asia/Tokyo,America/New_York"),
            com.example.core.config.WidgetOption.Slider("count", "Visible cities", min = 2, max = 4, step = 1, default = 3, unit = "cities")
        )) { c, s, cv -> TravelRenderers.renderMultiCityClock(c, s, cv) })
        register(createDef("travel.clock.city", Category.TRAVEL, "World City Clock", "A single city's time", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Text("city", "City zone", default = "Europe/London"),
            com.example.core.config.WidgetOption.Toggle("h24", "24-hour clock", default = true)
        )) { c, s, cv -> TravelRenderers.renderSingleWorldCity(c, s, cv) })
        register(createDef("travel.weather.city", Category.TRAVEL, "City Weather", "Current weather for a chosen city", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Text("city", "City name", default = "London"),
            com.example.core.config.WidgetOption.Toggle("show_city", "Show city name", default = true)
        )) { c, s, cv -> TravelRenderers.renderWeatherCity(c, s, cv) })
        register(createDef("travel.sun.times", Category.TRAVEL, "Sunrise & Sunset", "Today's rise and set for your location", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Toggle("show_arc", "Show arc", default = false),
            com.example.core.config.WidgetOption.Toggle("h24", "24-hour clock", default = true)
        )) { c, s, cv -> TravelRenderers.renderSunTimes(c, s, cv) })
        register(createDef("travel.flight.countdown", Category.TRAVEL, "Flight Countdown", "Time until your next flight", setOf(SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            com.example.core.config.WidgetOption.Text("flight", "Flight code", default = "FLIGHT"),
            com.example.core.config.WidgetOption.Toggle("show_time", "Show departure time", default = true)
        )) { c, s, cv -> TravelRenderers.renderFlightCountdown(c, s, cv) })
        register(createDef("travel.packing.list", Category.TRAVEL, "Packing List", "Tick off packing items", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_MAIN, options = listOf(
            com.example.core.config.WidgetOption.Slider("max_items", "Items shown", min = 3, max = 10, step = 1, default = 6, unit = "items"),
            com.example.core.config.WidgetOption.Toggle("show_checkboxes", "Show checkboxes", default = true)
        )) { c, s, cv -> TravelRenderers.renderPackingCheck(c, s, cv) })
        register(createDef("travel.trip.countdown", Category.TRAVEL, "Trip Countdown", "Days until your trip", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            com.example.core.config.WidgetOption.Text("destination", "Destination", default = "TRIP"),
            com.example.core.config.WidgetOption.Toggle("show_date", "Show date", default = false)
        )) { c, s, cv -> TravelRenderers.renderTripCountdown(c, s, cv) })
        register(createDef("travel.currency.tip", Category.TRAVEL, "Travel Currency", "Exchange rate between two currencies", setOf(SizeBucket.M), TapAction.DEFAULT, requiresNetwork = false, options = listOf(
            com.example.core.config.WidgetOption.Text("home", "Home currency", default = "USD"),
            com.example.core.config.WidgetOption.Text("away", "Away currency", default = "EUR")
        )) { c, s, cv -> TravelRenderers.renderCurrencyTip(c, s, cv) })
        register(createDef("travel.tz.diff", Category.TRAVEL, "Time Difference", "Hours between two zones", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Text("home", "Home zone", default = "UTC"),
            com.example.core.config.WidgetOption.Text("away", "Away zone", default = "Europe/London")
        )) { c, s, cv -> TravelRenderers.renderTzDiff(c, s, cv) })
        register(createDef("travel.day.length", Category.TRAVEL, "Daylight Length", "Hours of daylight today", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            com.example.core.config.WidgetOption.Toggle("show_progress", "Show progress", default = true),
            com.example.core.config.WidgetOption.Toggle("h24", "24-hour clock", default = true)
        )) { c, s, cv -> TravelRenderers.renderDayLength(c, s, cv) })

        // --- 17. DEEP VARIANTS (40) --- depth for the staple families.
        // Clock (+8)
        register(createDef("clock.tally.marks", Category.CLOCK, "Hour Tally", "A 24-dot tally of the current hour", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Text("zone", "Zone (optional)", default = ""),
            WidgetOption.Toggle("show_track", "Show track", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderClockTallyMarks(c, s, cv) })
        register(createDef("clock.radial.24", Category.CLOCK, "24-Hour Radial", "A 24-dot dial with the hour lit", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Text("zone", "Zone (optional)", default = ""),
            WidgetOption.Toggle("show_time", "Show time", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderClockRadial24(c, s, cv) })
        register(createDef("clock.week.arc", Category.CLOCK, "Week Progress", "Day of week plus week-of-year", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Toggle("show_letters", "Show day letters", default = true),
            WidgetOption.Toggle("monday_first", "Monday first", default = false)
        )) { c, s, cv -> DeepVariantRenderers.renderClockWeekArc(c, s, cv) })
        register(createDef("clock.dual.meridiem", Category.CLOCK, "Meridiem Split", "AM/PM with the active half lit", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Text("zone", "Zone (optional)", default = ""),
            WidgetOption.Toggle("show_dots", "Show indicator", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderClockDualMeridiem(c, s, cv) })
        register(createDef("clock.iso.dots", Category.CLOCK, "ISO Week Dots", "Week number with a weekday run", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Toggle("show_week_label", "Show week label", default = true),
            WidgetOption.Toggle("show_run", "Show weekday run", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderClockIsoDots(c, s, cv) })
        register(createDef("clock.moon.time", Category.CLOCK, "Time & Moon", "The clock with the current moon phase", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Text("zone", "Zone (optional)", default = ""),
            WidgetOption.Toggle("show_phase_name", "Show phase name", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderClockMoonTime(c, s, cv) })
        register(createDef("clock.hijri", Category.CLOCK, "Dual Calendar Clock", "Clock with an alternate lunar date", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Text("zone", "Zone (optional)", default = ""),
            WidgetOption.Toggle("show_gregorian", "Show Gregorian date", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderClockHijri(c, s, cv) })
        register(createDef("clock.segment.largewaves", Category.CLOCK, "Segmented Clock", "Large segmented digits with a seconds run", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Text("zone", "Zone (optional)", default = ""),
            WidgetOption.Toggle("show_seconds", "Show seconds run", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderClockSegmentLarge(c, s, cv) })
        // Weather (+6)
        register(createDef("weather.wind.compass", Category.WEATHER, "Wind Compass", "Wind speed with a direction tag", setOf(SizeBucket.M), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            WidgetOption.Text("direction", "Direction label", default = "N"),
            WidgetOption.Toggle("show_unit", "Show unit", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderWeatherWindCompass(c, s, cv) })
        register(createDef("weather.pressure.gauge", Category.WEATHER, "Pressure Gauge", "Sea-level pressure with a scale", setOf(SizeBucket.M), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            WidgetOption.Toggle("show_scale", "Show scale", default = true),
            WidgetOption.Toggle("show_unit", "Show unit", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderWeatherPressureGauge(c, s, cv) })
        register(createDef("weather.visibility", Category.WEATHER, "Visibility", "Estimated visibility in kilometres", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            WidgetOption.Toggle("show_unit", "Show unit", default = true),
            WidgetOption.Toggle("show_label", "Show label", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderWeatherVisibility(c, s, cv) })
        register(createDef("weather.dewpoint", Category.WEATHER, "Dew Point", "Dew point with a comfort band", setOf(SizeBucket.M), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            WidgetOption.Toggle("show_band", "Show comfort band", default = true),
            WidgetOption.Toggle("show_unit", "Show unit", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderWeatherDewpoint(c, s, cv) })
        register(createDef("weather.aqi.dial", Category.WEATHER, "Air Quality", "An estimated AQI dial and band", setOf(SizeBucket.M), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            WidgetOption.Toggle("show_band", "Show band", default = true),
            WidgetOption.Toggle("show_scale", "Show scale", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderWeatherAqiDial(c, s, cv) })
        register(createDef("weather.precip.bar", Category.WEATHER, "Precipitation", "Rain chance with an hourly bar run", setOf(SizeBucket.M), TapAction.DEFAULT, requiresNetwork = true, options = listOf(
            WidgetOption.Toggle("show_bars", "Show hourly bars", default = true),
            WidgetOption.Toggle("show_label", "Show label", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderWeatherPrecipBar(c, s, cv) })
        // Battery (+6)
        register(createDef("battery.temp.gauge", Category.BATTERY, "Battery Temp", "Battery temperature with a gauge", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Toggle("show_status", "Show status", default = true),
            WidgetOption.Toggle("show_gauge", "Show gauge", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderBatteryTempGauge(c, s, cv) })
        register(createDef("battery.cycles", Category.BATTERY, "Charge Cycles", "Lifetime charge cycles when reported", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Toggle("show_label", "Show label", default = true),
            WidgetOption.Toggle("show_when_unknown", "Show placeholder", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderBatteryCycles(c, s, cv) })
        register(createDef("battery.charge.eta", Category.BATTERY, "Charge ETA", "Estimated time to full", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Toggle("show_level", "Show current level", default = true),
            WidgetOption.Toggle("show_label", "Show label", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderBatteryChargeEta(c, s, cv) })
        register(createDef("battery.drain.rate", Category.BATTERY, "Drain Estimate", "Rough time left at light use", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Toggle("show_label", "Show label", default = true),
            WidgetOption.Toggle("show_rate", "Show rate note", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderBatteryDrainRate(c, s, cv) })
        register(createDef("battery.health.percent", Category.BATTERY, "Battery Health", "Battery health status", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Toggle("show_state", "Show state", default = true),
            WidgetOption.Toggle("show_label", "Show label", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderBatteryHealthPercent(c, s, cv) })
        register(createDef("battery.multi.devices", Category.BATTERY, "Device Battery", "This phone's level and plug type", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Toggle("show_plug", "Show plug type", default = true),
            WidgetOption.Toggle("show_label", "Show label", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderBatteryMultiDevices(c, s, cv) })
        // Calendar (+6)
        register(createDef("calendar.month.heat", Category.CALENDAR, "Month Heatmap", "The month as a dot grid", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT, options = listOf(
            WidgetOption.Toggle("monday_first", "Monday first", default = true),
            WidgetOption.Toggle("show_past", "Dim past days", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderCalendarMonthHeat(c, s, cv) })
        register(createDef("calendar.week.numbers", Category.CALENDAR, "Week Number", "Week of year against the total", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Toggle("show_total", "Show total weeks", default = true),
            WidgetOption.Toggle("show_label", "Show label", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderCalendarWeekNumbers(c, s, cv) })
        register(createDef("calendar.birthdays", Category.CALENDAR, "Birthday", "Days until the saved birthday/event", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            WidgetOption.Text("name", "Name", default = ""),
            WidgetOption.Toggle("show_days", "Show days left", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderCalendarBirthdays(c, s, cv) })
        register(createDef("calendar.moon.month", Category.CALENDAR, "Moon This Month", "Moon illumination across the month", setOf(SizeBucket.M, SizeBucket.L), TapAction.DEFAULT, options = listOf(
            WidgetOption.Toggle("show_scores", "Show phase scores", default = true),
            WidgetOption.Toggle("show_today", "Show today's phase", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderCalendarMoonMonth(c, s, cv) })
        register(createDef("calendar.quarter.progress", Category.CALENDAR, "Quarter Progress", "How far through the quarter", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Toggle("show_percent", "Show percent", default = true),
            WidgetOption.Toggle("show_bar", "Show progress bar", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderCalendarQuarterProgress(c, s, cv) })
        register(createDef("calendar.countdown.multi", Category.CALENDAR, "Multi Countdown", "Event countdown plus year-end", setOf(SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            WidgetOption.Toggle("show_year_end", "Show year-end", default = true),
            WidgetOption.Toggle("show_labels", "Show labels", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderCalendarCountdownMulti(c, s, cv) })
        // Productivity (+4)
        register(createDef("prod.habit.month", Category.PRODUCTIVITY, "Habit Month Matrix", "A 35-day habit heatmap", setOf(SizeBucket.M, SizeBucket.L), TapAction.OPEN_MAIN, options = listOf(
            WidgetOption.Toggle("show_count", "Show days logged", default = true),
            WidgetOption.Toggle("newest_first", "Newest first", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderProdHabitMonthMatrix(c, s, cv) })
        register(createDef("prod.project.burndown", Category.PRODUCTIVITY, "Task Burndown", "Remaining tasks with a burndown line", setOf(SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            WidgetOption.Toggle("show_ratio", "Show ratio", default = true),
            WidgetOption.Toggle("show_line", "Show burndown line", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderProdProjectBurndown(c, s, cv) })
        register(createDef("prod.meeting.count", Category.PRODUCTIVITY, "Meeting Count", "Meetings on today's calendar", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            WidgetOption.Slider("count", "Meetings", min = 0, max = 12, step = 1, default = 0, unit = "meetings"),
            WidgetOption.Toggle("show_label", "Show label", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderProdMeetingCount(c, s, cv) })
        register(createDef("prod.inbox.count", Category.PRODUCTIVITY, "Inbox Count", "Pending to-do items", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_MAIN, options = listOf(
            WidgetOption.Toggle("show_label", "Show label", default = true),
            WidgetOption.Toggle("celebrate_zero", "Celebrate inbox zero", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderProdInboxCount(c, s, cv) })
        // System / sensor (+6)
        register(createDef("sensor.cpu.load", Category.SYSTEM, "CPU Cores", "Active CPU cores with a scale", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Toggle("show_scale", "Show scale", default = true),
            WidgetOption.Toggle("show_label", "Show label", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderSensorCpuLoad(c, s, cv) })
        register(createDef("sensor.ram.spark", Category.SYSTEM, "RAM Load", "Memory pressure with a sparkline", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Toggle("show_spark", "Show sparkline", default = true),
            WidgetOption.Toggle("show_detail", "Show used/total", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderSensorRamSpark(c, s, cv) })
        register(createDef("sensor.net.updown", Category.SYSTEM, "Network Link", "Active connection type", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Toggle("show_state", "Show state", default = true),
            WidgetOption.Toggle("show_label", "Show label", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderSensorNetUpDown(c, s, cv) })
        register(createDef("sensor.uptime", Category.SYSTEM, "Device Uptime", "Time since last boot", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Toggle("show_label", "Show label", default = true),
            WidgetOption.Toggle("show_days", "Prefer days", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderSensorUptime(c, s, cv) })
        register(createDef("sensor.storage.free", Category.SYSTEM, "Storage Free", "Free internal storage with a bar", setOf(SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Toggle("show_bar", "Show usage bar", default = true),
            WidgetOption.Toggle("show_unit", "Show unit", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderSensorStorageFree(c, s, cv) })
        register(createDef("sensor.net.speedtag", Category.SYSTEM, "Link Speed Tag", "Current link tech (5G/LTE/Wi-Fi)", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, options = listOf(
            WidgetOption.Toggle("show_label", "Show label", default = true),
            WidgetOption.Toggle("show_state", "Show state", default = true)
        )) { c, s, cv -> DeepVariantRenderers.renderSensorNetSpeedTag(c, s, cv) })
    }

    private fun register(def: WidgetDefinition) {
        widgetsList.add(
            if (def.id in LOCK_SAFE_EXTRA && !def.lockSafe) object : WidgetDefinition by def {
                override val lockSafe = true
            } else def
        )
    }

    private fun createDef(
        id: String,
        category: Category,
        title: String,
        description: String,
        sizes: Set<SizeBucket>,
        defaultTap: TapAction,
        requiresNetwork: Boolean = false,
        requiredPermission: AppPermission? = null,
        lockSafe: Boolean = false,
        options: List<com.example.core.config.WidgetOption> = emptyList(),
        renderFn: (Context, WidgetSpec, Canvas) -> Unit
    ): WidgetDefinition {
        return object : WidgetDefinition {
            override val id = id
            override val category = category
            override val title = title
            override val description = description
            override val sizes = sizes
            override val defaultTap = defaultTap
            override val requiresNetwork = requiresNetwork
            override val requiredPermission = requiredPermission
            override val lockSafe = lockSafe
            // Every widget also gets the universal options (text size, corner dot).
            override val options = options + com.example.core.config.UniversalOptions.ALL

            override fun render(context: Context, spec: WidgetSpec, canvas: Canvas) {
                renderFn(context, spec, canvas)
            }

            override fun describe(context: Context): String = WidgetDescription.of(context, id, title)
        }
    }

    fun getAll(): List<WidgetDefinition> = widgetsList

    fun getByCategory(category: Category): List<WidgetDefinition> {
        return if (category == Category.ALL) widgetsList else widgetsList.filter { it.category == category }
    }

    fun getById(id: String): WidgetDefinition? {
        return widgetsList.find { it.id == id }
    }

    /** Widgets suitable for the Android 14+ lock screen / Always-On Display. */
    fun lockSafeDefs(): List<WidgetDefinition> = widgetsList.filter { it.lockSafe }

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

/** Human-readable TalkBack descriptions for the main widget families. */
object WidgetDescription {
    fun of(context: Context, id: String, fallback: String): String = try {
        when {
            id.startsWith("clock.") || id.startsWith("sensor.world") -> {
                val cal = java.util.Calendar.getInstance()
                String.format(
                    java.util.Locale.US, "Time %02d:%02d",
                    cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE)
                )
            }
            id.startsWith("battery.") || id == "sensor.battery.quad" -> {
                val b = com.example.data.battery.BatteryHelper.getBatteryStatus(context)
                "Battery ${b.level} percent" + if (b.isCharging) ", charging" else ""
            }
            id.startsWith("weather.") -> {
                val w = com.example.data.weather.WeatherRepository.getWeatherInstant(context)
                "${w.cityName} ${w.temperatureC} degrees, ${w.description}"
            }
            id.startsWith("media.") || id == "sensor.visualizer.eq" -> {
                val now = com.example.data.media.MediaRepository.nowPlaying(context)
                if (now != null) {
                    if (now.isPlaying) "Now playing ${now.title} by ${now.artist}"
                    else "Paused: ${now.title} by ${now.artist}"
                } else "Nothing playing"
            }
            id == "sensor.compass.heading" -> {
                val heading = com.example.data.sensor.CompassRepository.headingDegrees(context)
                if (heading == null) "Compass"
                else "Compass heading ${heading} degrees ${com.example.data.sensor.CompassRepository.cardinal(heading)}"
            }
            id.startsWith("finance.crypto.tick") || id.startsWith("finance.crypto.spark") -> {
                val q = com.example.data.market.CryptoRepository.quote(context)
                if (q == null) "Crypto price" else "${q.symbol} ${q.price.toInt()} ${q.currency}"
            }
            id.startsWith("finance.") -> "Finance"
            id.startsWith("health.steps.") -> {
                val s = com.example.data.activity.StepRepository.todaySteps(context)
                if (s == null) "Steps" else "$s steps today"
            }
            id.startsWith("health.") -> "Health"
            id.startsWith("home.") -> "Smart home"
            id.startsWith("sports.") -> "Sports"
            id.startsWith("learn.") -> "Learning"
            id.startsWith("travel.") -> "Travel"
            id == "sensor.stopwatch.chrono" -> "Stopwatch"
            id == "prod.focus.timer" -> "Focus timer"
            id == "prod.countdown.timer" -> "Countdown timer"
            id.startsWith("calendar.agenda") || id == "calendar.timeline.dots" -> "Today's agenda"
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
