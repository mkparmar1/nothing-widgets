package com.example.widgets.base

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import com.example.core.bitmap.SizeBucket

/**
 * Every widget provider the app declares (home screen and lock screen), in one place so refresh,
 * alarm scheduling and the configure screen can't miss one.
 */
object WidgetProviders {

    data class Entry(
        val providerClass: Class<out AppWidgetProvider>,
        val defaultWidgetTypeId: String,
        val size: SizeBucket,
        /** Lock screen / Always-On providers only offer lock-safe designs. */
        val lockScreen: Boolean = false
    )

    val ALL = listOf(
        Entry(DotWidgetProviderS::class.java, "clock.dotdigital.s", SizeBucket.S),
        Entry(DotWidgetProviderM::class.java, "clock.dotdigital.m", SizeBucket.M),
        Entry(DotWidgetProviderL::class.java, "clock.dotdigital.l", SizeBucket.L),
        Entry(DotWidgetProviderLockS::class.java, "clock.dotdigital.s", SizeBucket.S, lockScreen = true),
        Entry(DotWidgetProviderLockM::class.java, "clock.dotdigital.m", SizeBucket.M, lockScreen = true)
    )

    /** Placed widget ids per provider entry. */
    fun placedIds(context: Context): List<Pair<Entry, IntArray>> {
        val manager = AppWidgetManager.getInstance(context) ?: return emptyList()
        return ALL.map { entry ->
            entry to (runCatching { manager.getAppWidgetIds(ComponentName(context, entry.providerClass)) }.getOrNull() ?: IntArray(0))
        }
    }

    /** Size bucket for a provider class name, or null if it isn't one of ours. */
    fun sizeForProviderClass(className: String): SizeBucket? =
        ALL.firstOrNull { it.providerClass.name == className }?.size

    fun isLockScreenProvider(className: String): Boolean =
        ALL.firstOrNull { it.providerClass.name == className }?.lockScreen == true
}
