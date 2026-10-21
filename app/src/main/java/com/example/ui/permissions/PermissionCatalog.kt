package com.example.ui.permissions

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.example.data.media.MediaListenerService
import com.example.widgets.base.DeviceToggles
import com.example.widgets.base.WidgetAlarmScheduler

/**
 * Every permission the widgets can use, in the order they're offered. Runtime permissions
 * show Android's Allow/Deny dialog; settings permissions open the matching system page.
 */
sealed class AppPermission(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    abstract fun isGranted(context: Context): Boolean

    /** Not applicable on this Android version (treated as granted and hidden). */
    open fun isApplicable(): Boolean = true

    abstract class Runtime(id: String, title: String, description: String, icon: ImageVector, val permissions: Array<String>) :
        AppPermission(id, title, description, icon) {
        override fun isGranted(context: Context) = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    abstract class SettingsPage(id: String, title: String, description: String, icon: ImageVector) :
        AppPermission(id, title, description, icon) {
        abstract fun intent(context: Context): Intent?
    }

    object ExactAlarms : SettingsPage(
        "exact_alarms", "Alarms & reminders",
        "Updates clock widgets exactly on the minute.", Icons.Default.Alarm
    ) {
        override fun isGranted(context: Context) = WidgetAlarmScheduler.canScheduleExactAlarms(context)
        override fun intent(context: Context) = WidgetAlarmScheduler.exactAlarmSettingsIntent(context)
        override fun isApplicable() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    object DndAccess : SettingsPage(
        "dnd", "Do Not Disturb access",
        "Lets the DND toggle widget switch Do Not Disturb on and off.", Icons.Default.DoNotDisturbOn
    ) {
        override fun isGranted(context: Context) = DeviceToggles.hasDndAccess(context)
        override fun intent(context: Context) = DeviceToggles.dndAccessSettingsIntent()
    }

    object Location : Runtime(
        "location", "Location",
        "Shows weather and sunrise for where you are. Only your approximate area is used.",
        Icons.Default.LocationOn, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
    )

    object Calendar : Runtime(
        "calendar", "Calendar",
        "Shows your real events in the agenda and timeline widgets.",
        Icons.Default.CalendarMonth, arrayOf(Manifest.permission.READ_CALENDAR)
    )

    object Contacts : Runtime(
        "contacts", "Contacts",
        "Lets the favourite-contact widgets show a contact's name and photo.",
        Icons.Default.Person, arrayOf(Manifest.permission.READ_CONTACTS)
    )

    object Activity : Runtime(
        "activity", "Physical activity",
        "Counts your steps for the step counter and activity rings.",
        Icons.AutoMirrored.Filled.DirectionsWalk,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) arrayOf(Manifest.permission.ACTIVITY_RECOGNITION) else emptyArray()
    ) {
        override fun isApplicable() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    object NearbyDevices : Runtime(
        "bluetooth", "Nearby devices",
        "Shows battery levels of your connected earbuds and watch.",
        Icons.Default.Bluetooth,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(Manifest.permission.BLUETOOTH_CONNECT) else emptyArray()
    ) {
        override fun isApplicable() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    object MediaAccess : SettingsPage(
        "media", "Notification access",
        "Lets the media widgets show and control the song that's playing. Dot Widgets doesn't read your notifications.",
        Icons.Default.MusicNote
    ) {
        override fun isGranted(context: Context): Boolean {
            val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
            val me = ComponentName(context, MediaListenerService::class.java)
            return enabled.split(":").any { ComponentName.unflattenFromString(it) == me }
        }

        override fun intent(context: Context) = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }

    object UsageAccess : SettingsPage(
        "usage", "Usage access",
        "Reads today's app usage on this phone for the Screen-On Time, Top Apps Today and Mobile Data Today widgets. It never leaves your phone.", Icons.Default.PhoneAndroid
    ) {
        override fun isGranted(context: Context): Boolean {
            val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            @Suppress("DEPRECATION")
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            } else {
                ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }

        override fun intent(context: Context) = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    companion object {
        val ALL: List<AppPermission> by lazy {
            listOf(ExactAlarms, DndAccess, Location, Contacts, Calendar, Activity, MediaAccess, UsageAccess, NearbyDevices)
                .filter { it.isApplicable() }
        }

        fun missing(context: Context) = ALL.filterNot { it.isGranted(context) }
    }
}
