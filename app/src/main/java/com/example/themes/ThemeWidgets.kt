package com.example.themes

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.AudioManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import com.example.core.bitmap.SizeBucket
import com.example.core.config.UniversalOptions
import com.example.core.config.WidgetOption
import com.example.data.battery.BatteryHelper
import com.example.data.media.MediaRepository
import com.example.ui.permissions.AppPermission
import com.example.widgets.base.Category
import com.example.widgets.base.DeviceToggles
import com.example.widgets.base.TapAction
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetSpec
import com.example.widgets.base.WidgetStateManager
import com.example.widgets.base.WidgetTapTargets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The control-centre set drawn with the selected theme pack. Ids keep the "toggle.", "fun." and
 * "battery." prefixes that WidgetRenderEngine treats as dynamic, so state changes redraw at once.
 */
object ThemeWidgets {

    private const val QUICK = "toggle.theme.quick"
    private const val MUSIC = "fun.theme.music"
    private const val FOCUS = "prod.focus.timer.theme"
    private const val GREETING = "fun.theme.greeting"
    private const val LEVELS = "toggle.theme.levels"
    private const val BUTTONS = "toggle.theme.buttons"
    private const val CLOCK = "fun.theme.clock"
    private const val DATE = "fun.theme.date"
    private const val BATTERY = "battery.theme.level"

    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    val ALL: List<WidgetDefinition> = listOf(
        def(QUICK, "Theme Quick Toggles", "Wi-Fi, Bluetooth, torch and Do Not Disturb", setOf(SizeBucket.S), TapAction.DEFAULT, draw = ::quickToggles),
        def(MUSIC, "Theme Music", "Now playing with play controls", setOf(SizeBucket.S, SizeBucket.M), TapAction.DEFAULT, AppPermission.MediaAccess, ::music),
        def(FOCUS, "Theme Focus Pill", "Focus timer, tap to start or pause", setOf(SizeBucket.M), TapAction.DEFAULT, draw = ::focus),
        def(GREETING, "Theme Greeting Pill", "A greeting for the time of day", setOf(SizeBucket.M), TapAction.OPEN_MAIN, draw = ::greeting),
        def(LEVELS, "Theme Brightness & Volume", "Two levels, tap to open the system controls", setOf(SizeBucket.S), TapAction.DEFAULT, draw = ::levels),
        def(BUTTONS, "Theme Quick Buttons", "Torch, timer, calculator and camera", setOf(SizeBucket.M), TapAction.DEFAULT, draw = ::buttons),
        def(CLOCK, "Theme Clock", "Time and date on a themed tile", setOf(SizeBucket.S, SizeBucket.M), TapAction.OPEN_CLOCK, draw = ::clock),
        def(DATE, "Theme Date", "Today's date on a themed tile", setOf(SizeBucket.S), TapAction.OPEN_CALENDAR, draw = ::date),
        def(BATTERY, "Theme Battery", "Battery level on a themed tile", setOf(SizeBucket.S), TapAction.OPEN_BATTERY, draw = ::battery)
    )

    private fun def(
        id: String,
        title: String,
        description: String,
        sizes: Set<SizeBucket>,
        tap: TapAction,
        permission: AppPermission? = null,
        draw: (Context, ThemeCanvas) -> Unit
    ): WidgetDefinition = object : WidgetDefinition {
        override val id = id
        override val category = Category.THEMES
        override val title = title
        override val description = description
        override val sizes = sizes
        override val defaultTap = tap
        override val requiredPermission = permission
        override val options: List<WidgetOption> = UniversalOptions.ALL

        override fun render(context: Context, spec: WidgetSpec, canvas: Canvas) {
            draw(context, ThemeCanvas(context, spec, canvas, ThemeRepository.forConfig(context, spec.configJson)))
        }
    }

    // ------------------------------------------------------------------ drawing

    private fun quickToggles(context: Context, t: ThemeCanvas) {
        val r = t.tile("tile_quad")
        val cells = listOf(
            Triple(0.30f to 0.30f, DeviceToggles.isWifiOn(context), Icons.Filled.Wifi),
            Triple(0.70f to 0.30f, DeviceToggles.isBluetoothOn(context), Icons.Filled.Bluetooth),
            Triple(0.30f to 0.70f, DeviceToggles.isTorchOn(context), Icons.Filled.FlashlightOn),
            Triple(0.70f to 0.70f, DeviceToggles.isDndOn(context), Icons.Filled.DoNotDisturbOn)
        )
        cells.forEach { (pos, on, icon) ->
            val cx = r.left + r.width() * pos.first
            val cy = r.top + r.height() * pos.second
            t.button(on, cx, cy, r.width() * 0.32f)
            t.icon(icon, cx, cy, r.width() * 0.13f, if (on) t.pack.onButtonOn else t.pack.muted)
        }
    }

    private fun music(context: Context, t: ThemeCanvas) {
        val wide = t.spec.sizeBucket != SizeBucket.S
        val r = t.tile(if (wide) "tile_wide" else "tile_square")
        val now = MediaRepository.nowPlaying(context)
        val h = r.height()
        val left = r.left + h * 0.14f
        val textWidth = r.width() - h * 0.28f
        t.text(now?.title ?: "Not playing", left, r.top + h * 0.30f, h * 0.105f, t.pack.onTile, bold = true, maxWidth = textWidth)
        t.text(now?.artist.orEmpty().ifEmpty { "Tap play to resume" }, left, r.top + h * 0.43f, h * 0.075f, t.pack.muted, maxWidth = textWidth)
        val y = r.top + h * 0.70f
        val step = r.width() / 4
        t.icon(Icons.Filled.SkipPrevious, r.left + step, y, h * 0.16f, t.pack.onTile)
        t.button(true, r.centerX(), y, h * 0.30f)
        t.icon(if (now?.isPlaying == true) Icons.Filled.Pause else Icons.Filled.PlayArrow, r.centerX(), y, h * 0.15f, t.pack.onButtonOn)
        t.icon(Icons.Filled.SkipNext, r.right - step, y, h * 0.16f, t.pack.onTile)
    }

    private fun focus(context: Context, t: ThemeCanvas) {
        val r = t.tile("tile_pill")
        val state = WidgetStateManager.getFocusTimerState(context)
        val h = r.height()
        val cx = r.left + h * 0.52f
        val cy = r.centerY()
        t.button(state.isRunning, cx, cy, h * 0.52f)
        t.icon(Icons.Filled.DarkMode, cx, cy, h * 0.24f, t.pack.onButtonOn)
        val left = r.left + h * 0.92f
        t.text("Focus", left, cy - h * 0.02f, h * 0.17f, t.pack.onTile, bold = true)
        t.text(if (state.isRunning) "Running" else "Tap to start", left, cy + h * 0.17f, h * 0.115f, t.pack.muted)
        val minutes = state.remainingSeconds / 60
        val seconds = state.remainingSeconds % 60
        t.text(String.format(Locale.US, "%02d:%02d", minutes, seconds), r.right - h * 0.42f, cy + h * 0.10f, h * 0.24f, t.pack.onTile, Paint.Align.RIGHT, bold = true, mono = true)
    }

    private fun greeting(context: Context, t: ThemeCanvas) {
        val r = t.tile("tile_pill")
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val h = r.height()
        val cx = r.left + h * 0.52f
        val cy = r.centerY()
        val day = hour in 6..17
        t.button(true, cx, cy, h * 0.52f)
        t.icon(if (day) Icons.Filled.LightMode else Icons.Filled.DarkMode, cx, cy, h * 0.24f, t.pack.onButtonOn)
        val message = when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
        val left = r.left + h * 0.92f
        val width = r.right - left - h * 0.34f
        t.text(message, left, cy - h * 0.02f, h * 0.17f, t.pack.onTile, bold = true, maxWidth = width)
        t.text("Have a good day", left, cy + h * 0.17f, h * 0.115f, t.pack.muted, maxWidth = width)
    }

    private fun levels(context: Context, t: ThemeCanvas) {
        val brightness = runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
        }.getOrDefault(0.5f)
        val audio = context.getSystemService(AudioManager::class.java)
        val volume = runCatching {
            audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        }.getOrDefault(0.5f)
        val half = t.width / 2
        capsule(t, RectF(0f, 0f, half, t.height), brightness) { cx, cy, s -> t.icon(Icons.Filled.LightMode, cx, cy, s, t.pack.onTile) }
        capsule(t, RectF(half, 0f, t.width, t.height), volume) { cx, cy, s -> t.icon(Icons.AutoMirrored.Filled.VolumeUp, cx, cy, s, t.pack.onTile) }
    }

    private fun capsule(t: ThemeCanvas, area: RectF, level: Float, icon: (Float, Float, Float) -> Unit) {
        val r = t.tile("tile_tall", area)
        val fillTop = r.top + r.height() * (1 - level.coerceIn(0.12f, 1f))
        t.canvas.save()
        t.canvas.clipRect(r.left, fillTop, r.right, r.bottom)
        t.tile("tile_tall_fill", area)
        t.canvas.restore()
        icon(r.centerX(), r.bottom - r.width() * 0.5f, r.width() * 0.34f)
    }

    private fun buttons(context: Context, t: ThemeCanvas) {
        val icons = listOf(Icons.Filled.FlashlightOn, Icons.Filled.Timer, Icons.Filled.Calculate, Icons.Filled.PhotoCamera)
        val cell = t.width / 4
        val size = minOf(cell, t.height) * 0.96f
        icons.forEachIndexed { i, icon ->
            val on = i == 0 && DeviceToggles.isTorchOn(context)
            val cx = cell * (i + 0.5f)
            t.button(on, cx, t.height / 2, size)
            t.icon(icon, cx, t.height / 2, size * 0.42f, if (on) t.pack.onButtonOn else t.pack.onButtonOff)
        }
    }

    private fun clock(context: Context, t: ThemeCanvas) {
        val wide = t.spec.sizeBucket != SizeBucket.S
        val r = t.tile(if (wide) "tile_wide" else "tile_square")
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
        val h = r.height()
        val now = Date()
        t.text(SimpleDateFormat(pattern, Locale.getDefault()).format(now), r.centerX(), r.centerY() + h * 0.08f, h * (if (wide) 0.38f else 0.27f), t.pack.onTile, Paint.Align.CENTER, bold = true, mono = true, maxWidth = r.width() * 0.8f)
        t.text(SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(now), r.centerX(), r.centerY() + h * 0.27f, h * 0.085f, t.pack.muted, Paint.Align.CENTER)
        t.cornerDot(r)
    }

    private fun date(context: Context, t: ThemeCanvas) {
        val r = t.tile("tile_square")
        val h = r.height()
        val now = Date()
        t.text(SimpleDateFormat("EEEE", Locale.getDefault()).format(now), r.centerX(), r.top + h * 0.30f, h * 0.085f, t.pack.accent, Paint.Align.CENTER, bold = true, maxWidth = r.width() * 0.76f)
        t.text(SimpleDateFormat("d", Locale.getDefault()).format(now), r.centerX(), r.centerY() + h * 0.13f, h * 0.36f, t.pack.onTile, Paint.Align.CENTER, bold = true, mono = true)
        t.text(SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(now), r.centerX(), r.bottom - h * 0.17f, h * 0.08f, t.pack.muted, Paint.Align.CENTER, maxWidth = r.width() * 0.76f)
        t.cornerDot(r)
    }

    private fun battery(context: Context, t: ThemeCanvas) {
        val r = t.tile("tile_square")
        val status = BatteryHelper.getBatteryStatus(context)
        val h = r.height()
        t.arc(r.centerX(), r.centerY(), h * 0.29f, h * 0.05f, status.level / 100f)
        t.text("${status.level}%", r.centerX(), r.centerY() + h * 0.05f, h * 0.15f, t.pack.onTile, Paint.Align.CENTER, bold = true, mono = true)
        if (status.isCharging) t.text("Charging", r.centerX(), r.centerY() + h * 0.15f, h * 0.06f, t.pack.muted, Paint.Align.CENTER)
    }

    // ------------------------------------------------------------------ taps

    /** Rows of tap cells for theme widgets; null for every other widget. */
    fun regions(context: Context, appWidgetId: Int, widgetId: String): List<List<PendingIntent>>? = when (widgetId) {
        QUICK -> listOf(
            listOf(
                activity(context, appWidgetId, 86, DeviceToggles.wifiSettingsIntent()),
                activity(context, appWidgetId, 87, DeviceToggles.bluetoothSettingsIntent())
            ),
            listOf(toggle(context, appWidgetId, 88, "TORCH"), dnd(context, appWidgetId, 89))
        )
        MUSIC -> listOf(
            listOf(openPlayer(context, appWidgetId)),
            listOf(
                media(context, appWidgetId, MediaRepository.Command.PREVIOUS),
                media(context, appWidgetId, MediaRepository.Command.PLAY_PAUSE),
                media(context, appWidgetId, MediaRepository.Command.NEXT)
            )
        )
        LEVELS -> listOf(
            listOf(
                activity(context, appWidgetId, 86, Intent(Settings.ACTION_DISPLAY_SETTINGS)),
                activity(context, appWidgetId, 87, volumePanel())
            )
        )
        BUTTONS -> listOf(
            listOf(
                toggle(context, appWidgetId, 86, "TORCH"),
                activity(context, appWidgetId, 87, timers()),
                activity(context, appWidgetId, 88, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR)),
                activity(context, appWidgetId, 89, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            )
        )
        else -> null
    }

    /** Single tap action for theme widgets that need one; null otherwise. */
    fun single(context: Context, appWidgetId: Int, widgetId: String): PendingIntent? = when (widgetId) {
        FOCUS -> PendingIntent.getBroadcast(
            context, appWidgetId * 100 + 86,
            Intent("com.aistudio.dotwidgets.ACTION_TIMER_TOGGLE").setPackage(context.packageName), FLAGS
        )
        else -> null
    }

    private fun activity(context: Context, appWidgetId: Int, slot: Int, intent: Intent): PendingIntent =
        PendingIntent.getActivity(context, appWidgetId * 100 + slot, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), FLAGS)

    private fun toggle(context: Context, appWidgetId: Int, slot: Int, name: String): PendingIntent {
        val intent = Intent("com.aistudio.dotwidgets.ACTION_TOGGLE_CLICK").apply {
            setPackage(context.packageName)
            putExtra("app_widget_id", appWidgetId)
            putExtra("toggle_name", name)
        }
        return PendingIntent.getBroadcast(context, appWidgetId * 100 + slot, intent, FLAGS)
    }

    private fun dnd(context: Context, appWidgetId: Int, slot: Int): PendingIntent =
        if (DeviceToggles.hasDndAccess(context)) toggle(context, appWidgetId, slot, "DND")
        else activity(context, appWidgetId, slot, DeviceToggles.dndAccessSettingsIntent())

    private fun media(context: Context, appWidgetId: Int, command: MediaRepository.Command): PendingIntent {
        val intent = Intent(WidgetTapTargets.ACTION_MEDIA_COMMAND).apply {
            setPackage(context.packageName)
            putExtra(WidgetTapTargets.EXTRA_MEDIA_COMMAND, command.name)
        }
        return PendingIntent.getBroadcast(context, appWidgetId * 100 + 90 + command.ordinal, intent, FLAGS)
    }

    private fun openPlayer(context: Context, appWidgetId: Int): PendingIntent {
        val player = MediaRepository.nowPlaying(context)?.appPackage?.let { context.packageManager.getLaunchIntentForPackage(it) }
        val intent = player ?: Intent(context, com.example.MainActivity::class.java)
        return activity(context, appWidgetId, 86, intent)
    }

    private fun volumePanel(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_VOLUME) else Intent(Settings.ACTION_SOUND_SETTINGS)

    private fun timers(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Intent(AlarmClock.ACTION_SHOW_TIMERS) else Intent(AlarmClock.ACTION_SHOW_ALARMS)
}
