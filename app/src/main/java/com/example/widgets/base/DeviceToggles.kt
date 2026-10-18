package com.example.widgets.base

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
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

    fun wifiSettingsIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_WIFI)
        else Intent(Settings.ACTION_WIFI_SETTINGS)

    fun bluetoothSettingsIntent(): Intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)

    fun darkModeSettingsIntent(): Intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)

    fun dndAccessSettingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
}
