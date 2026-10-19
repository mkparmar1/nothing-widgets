package com.example.data.billing

import android.content.Context

/**
 * Single place the app asks "may this user save a widget?".
 *
 * Product decision: Dot Widgets is fully free. Every widget, size and theme is unlocked for
 * everyone, so this always returns `true`. It is kept as the one seam that Save / Add-to-home-screen
 * paths consult, so if a paid tier is ever introduced only this implementation changes — nothing
 * else in the app. No Google Play Billing dependency is shipped.
 */
object Entitlements {
    fun hasFullAccess(context: Context): Boolean = true
}
