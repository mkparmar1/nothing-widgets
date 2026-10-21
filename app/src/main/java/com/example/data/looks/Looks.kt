package com.example.data.looks

import android.content.Context
import com.example.core.bitmap.BitmapCache
import com.example.core.render.Frame
import com.example.core.render.Skin
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.time.ClockOptions
import com.example.core.time.DotStyle
import com.example.data.db.AppDatabase
import com.example.widgets.base.WidgetProviders
import com.example.widgets.base.WidgetRenderEngine
import org.json.JSONObject

/** A preset combination of skin, accent, frame, dot style and background. */
data class Look(
    val id: String,
    val name: String,
    val description: String,
    val skin: Skin,
    val accent: AccentOption,
    val frame: Frame,
    val dotStyle: DotStyle,
    val background: BackgroundMode
)

/** One-tap Looks: restyle every placed widget at once. */
object Looks {

    val ALL = listOf(
        Look("classic", "Dot Classic", "Red-dot dot matrix on black", Skin.DOT, AccentOption.RED, Frame.HAIRLINE, DotStyle.ROUND, BackgroundMode.SOLID),
        Look("nothing", "Native Dot", "Your phone's own dot font, where it has one", Skin.NOTHING, AccentOption.RED, Frame.NONE, DotStyle.ROUND, BackgroundMode.SOLID),
        Look("glyph", "Light Strip Night", "Thin glowing light strips", Skin.GLYPH, AccentOption.PURE, Frame.NONE, DotStyle.ROUND, BackgroundMode.SOLID),
        Look("lcd", "Retro LCD", "7-segment calculator display", Skin.SEGMENT, AccentOption.AMBER, Frame.HAIRLINE, DotStyle.ROUND, BackgroundMode.SOLID),
        Look("terminal", "Terminal", "Monospace console with scan lines", Skin.TERMINAL, AccentOption.LIME, Frame.NONE, DotStyle.ROUND, BackgroundMode.SOLID),
        Look("paper", "Paper", "Ink on warm paper", Skin.PAPER, AccentOption.RED, Frame.DOTTED, DotStyle.ROUND, BackgroundMode.SOLID),
        Look("neon", "Neon", "Glowing dots and borders", Skin.NEON, AccentOption.CYAN, Frame.GLOW, DotStyle.BOLD, BackgroundMode.SOLID),
        Look("pixel", "8-Bit", "Chunky square pixels", Skin.PIXEL, AccentOption.AMBER, Frame.NONE, DotStyle.SQUARE, BackgroundMode.SOLID),
        Look("you", "Material You", "Colours from your wallpaper", Skin.DOT, AccentOption.DYNAMIC, Frame.HAIRLINE, DotStyle.ROUND, BackgroundMode.SCRIM),
        Look("glass", "Glass", "See-through, minimal dots", Skin.DOT, AccentOption.PURE, Frame.HAIRLINE, DotStyle.MINIMAL, BackgroundMode.TRANSPARENT)
    )

    /**
     * Writes the look into every placed widget's config (keeping its own content and options) and
     * redraws them all. Returns how many widgets were restyled.
     */
    suspend fun apply(context: Context, look: Look): Int {
        val dao = AppDatabase.getInstance(context).widgetInstanceDao()
        var count = 0
        for ((_, ids) in WidgetProviders.placedIds(context)) {
            for (id in ids) {
                val instance = dao.getById(id) ?: continue
                val json = runCatching { JSONObject(instance.configJson) }.getOrElse { JSONObject() }
                json.put(Skin.KEY, look.skin.name)
                    .put(Frame.KEY, look.frame.name)
                    .put(ClockOptions.KEY_DOT_STYLE, look.dotStyle.name)
                    .put("accent", look.accent.name)
                    .put("background", look.background.name)
                dao.insertOrUpdate(instance.copy(configJson = json.toString()))
                count++
            }
        }
        BitmapCache.clear()
        WidgetRenderEngine.updateAllPlacedWidgets(context, force = true)
        return count
    }
}
