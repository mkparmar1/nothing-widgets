package com.example.widgets.base

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.core.bitmap.SizeBucket
import com.example.data.media.MediaRepository
import com.example.data.shortcuts.ShortcutStore
import com.example.widgets.catalogue.ShortcutRenderers

/** Tap behaviour for widgets with several buttons or a special single action. */
object WidgetTapTargets {

    const val ACTION_MEDIA_COMMAND = "com.aistudio.dotwidgets.ACTION_MEDIA_COMMAND"
    const val ACTION_STOPWATCH_RESET = "com.aistudio.dotwidgets.ACTION_STOPWATCH_RESET"
    const val EXTRA_MEDIA_COMMAND = "media_command"

    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    /**
     * Rows of tap targets matching the widget's drawing, or null for widgets with a single tap
     * target. Applied with [applyRegions].
     */
    fun regions(context: Context, appWidgetId: Int, widgetId: String, size: SizeBucket): List<List<PendingIntent>>? {
        if (widgetId == "sensor.stopwatch.chrono") {
            return listOf(
                listOf(
                    action(context, appWidgetId, "com.aistudio.dotwidgets.ACTION_STOPWATCH_TOGGLE", 47),
                    action(context, appWidgetId, ACTION_STOPWATCH_RESET, 48)
                )
            )
        }
        if (widgetId == "media.controls.bar") {
            return listOf(
                listOf(
                    mediaCommand(context, appWidgetId, MediaRepository.Command.PREVIOUS),
                    mediaCommand(context, appWidgetId, MediaRepository.Command.PLAY_PAUSE),
                    mediaCommand(context, appWidgetId, MediaRepository.Command.NEXT)
                )
            )
        }
        if (widgetId in ShortcutStore.WIDGETS) {
            val slots = ShortcutStore.get(context, widgetId)
            val (rows, cols) = ShortcutRenderers.grid(widgetId, size)
            return List(rows) { r ->
                List(cols) { c ->
                    val i = r * cols + c
                    // Empty slots open the app so the user can fill them in.
                    val intent = slots.getOrNull(i)?.intent(context) ?: openApp(context)
                    PendingIntent.getActivity(context, appWidgetId * 100 + 30 + i, intent, FLAGS)
                }
            }
        }
        return null
    }

    /** Special single tap action, or null to use the widget's normal tap action. */
    fun single(context: Context, appWidgetId: Int, widgetId: String): PendingIntent? = when (widgetId) {
        "media.nowplaying.compact" -> mediaCommand(context, appWidgetId, MediaRepository.Command.PLAY_PAUSE)
        "media.nowplaying.art", "sensor.visualizer.eq" -> {
            val player = MediaRepository.nowPlaying(context)?.appPackage?.let { context.packageManager.getLaunchIntentForPackage(it) }
            PendingIntent.getActivity(context, appWidgetId * 100 + 45, (player ?: openApp(context)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), FLAGS)
        }
        "media.volume.dots" -> {
            val panel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_VOLUME) else Intent(Settings.ACTION_SOUND_SETTINGS)
            PendingIntent.getActivity(context, appWidgetId * 100 + 46, panel.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), FLAGS)
        }
        else -> null
    }

    /** Fills the dynamic region container with rows of tap cells. */
    fun applyRegions(context: Context, views: RemoteViews, rows: List<List<PendingIntent>>) {
        views.removeAllViews(R.id.dynamic_regions)
        rows.forEach { cells ->
            val row = RemoteViews(context.packageName, R.layout.widget_region_row)
            cells.forEach { pi ->
                val cell = RemoteViews(context.packageName, R.layout.widget_region_cell)
                cell.setOnClickPendingIntent(R.id.region_cell, pi)
                row.addView(R.id.region_row, cell)
            }
            views.addView(R.id.dynamic_regions, row)
        }
        views.setViewVisibility(R.id.dynamic_regions, View.VISIBLE)
    }

    private fun mediaCommand(context: Context, appWidgetId: Int, command: MediaRepository.Command): PendingIntent {
        val intent = Intent(ACTION_MEDIA_COMMAND).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_MEDIA_COMMAND, command.name)
        }
        return PendingIntent.getBroadcast(context, appWidgetId * 100 + 40 + command.ordinal, intent, FLAGS)
    }

    private fun action(context: Context, appWidgetId: Int, action: String, slot: Int): PendingIntent =
        PendingIntent.getBroadcast(context, appWidgetId * 100 + slot, Intent(action).setPackage(context.packageName), FLAGS)

    private fun openApp(context: Context) = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
