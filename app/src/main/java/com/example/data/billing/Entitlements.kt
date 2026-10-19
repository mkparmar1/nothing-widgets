package com.example.data.billing

import android.content.Context

/**
 * Single place the app asks "may this user save a widget?". Everyone has full access until Google
 * Play Billing is wired up; only the Save / Add-to-home-screen paths consult this, so swapping in a
 * real check (₹159/month, 7-day trial) changes nothing else.
 */
object Entitlements {
    fun hasFullAccess(context: Context): Boolean = true
}
