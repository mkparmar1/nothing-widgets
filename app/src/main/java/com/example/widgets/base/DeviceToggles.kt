package com.example.widgets.base

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Reads real device state for the toggle widgets and performs the actions Android allows
 * third-party apps to take. Wi-Fi, Bluetooth and dark mode cannot be switched directly by
 * normal apps, so those toggles open the matching system panel instead.
 */
object DeviceToggles {

    private const val TAG = "DeviceToggles"
    private const val PREFS_NAME = "dot_widgets_interactive_state"
    private const val KEY_TORCH_ON = "torch_on"

    fun isWifiOn(context: Context): Boolean = try {
        (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.isWifiEnabled == true
    } catch (_: Exception) {
        false
    }

    fun isBluetoothOn(context: Context): Boolean = try {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter?.isEnabled == true
    } catch (_: SecurityException) {
        false
    }

    fun isDndOn(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        val filter = nm.currentInterruptionFilter
        return filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
            filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    fun isDarkModeOn(context: Context): Boolean {
        val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return night == Configuration.UI_MODE_NIGHT_YES
    }

    /** Last torch state this app set or observed. Live state is refreshed whenever the torch is toggled. */
    fun isTorchOn(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_TORCH_ON, false)

    fun hasDndAccess(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        return nm.isNotificationPolicyAccessGranted
    }

    fun toggleDnd(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (!nm.isNotificationPolicyAccessGranted) return
        nm.setInterruptionFilter(
            if (isDndOn(context)) NotificationManager.INTERRUPTION_FILTER_ALL
            else NotificationManager.INTERRUPTION_FILTER_PRIORITY
        )
    }

    suspend fun toggleTorch(context: Context) {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        try {
            val cameraId = cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            // The torch may have been changed from Quick Settings, so read its live state first.
            val current = readTorchState(cm, cameraId) ?: isTorchOn(context)
            val next = !current
            cm.setTorchMode(cameraId, next)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_TORCH_ON, next).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle torch", e)
        }
    }

    /** Registering a TorchCallback immediately reports the current state of every flash unit. */
    private suspend fun readTorchState(cm: CameraManager, cameraId: String): Boolean? = withTimeoutOrNull(500) {
        suspendCancellableCoroutine { cont ->
            val callback = object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(id: String, enabled: Boolean) {
                    if (id == cameraId && cont.isActive) {
                        cm.unregisterTorchCallback(this)
                        cont.resume(enabled)
                    }
                }
            }
            cm.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
            cont.invokeOnCancellation { cm.unregisterTorchCallback(callback) }
        }
    }

    fun isAutoRotateOn(context: Context): Boolean =
        Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1

    fun isAirplaneModeOn(context: Context): Boolean =
        Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1

    fun isDataSaverOn(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return cm.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
    }

    /** There's no public hotspot API; this hidden method works on most builds and reports false otherwise. */
    fun isHotspotOn(context: Context): Boolean = try {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wm.javaClass.getMethod("isWifiApEnabled").invoke(wm) as? Boolean ?: false
    } catch (_: Exception) {
        false
    }

    fun autoRotateSettingsIntent(): Intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)

    fun airplaneSettingsIntent(): Intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)

    fun dataSaverSettingsIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) Intent(Settings.ACTION_DATA_USAGE_SETTINGS)
        else Intent(Settings.ACTION_WIRELESS_SETTINGS)

    fun hotspotSettingsIntent(): Intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)

    fun wifiSettingsIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_WIFI)
        else Intent(Settings.ACTION_WIFI_SETTINGS)

    fun bluetoothSettingsIntent(): Intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)

    fun darkModeSettingsIntent(): Intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)

    fun dndAccessSettingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
}
