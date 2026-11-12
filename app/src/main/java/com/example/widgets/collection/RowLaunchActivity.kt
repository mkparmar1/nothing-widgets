package com.example.widgets.collection

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import com.example.widgets.collection.CollectionWidgetService.Companion.EXTRA_ROW_ACTION

/**
 * Invisible hop for list-widget row taps that open another app (dial a favourite, open an agenda
 * event). Android 14+ forbids a widget's mutable row template from being an implicit intent, so
 * the template targets this explicit, non-exported activity, which then opens the real target.
 */
class RowLaunchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent.data
        val action = when (intent.getStringExtra(EXTRA_ROW_ACTION)) {
            RowAction.CALL.name -> Intent.ACTION_DIAL
            RowAction.OPEN_CALENDAR.name -> Intent.ACTION_VIEW
            else -> null
        }
        if (action != null && data != null) {
            try {
                startActivity(Intent(action, data))
            } catch (_: ActivityNotFoundException) {
                // No dialler or calendar app; nothing to open.
            }
        }
        finish()
    }
}
