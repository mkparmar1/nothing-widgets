package com.example.data.battery

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class DeviceBattery(
    val name: String,
    /** 0..100, or null if the device doesn't report its battery. */
    val level: Int?
)

/**
 * Battery levels of connected Bluetooth devices (earbuds, watches, speakers). Android has no
 * public API for this, so it uses the hidden BluetoothDevice getters that the system's own
 * Bluetooth settings rely on; devices that don't report a level show as unknown.
 */
object BluetoothBatteryRepository {

    fun hasPermission(context: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    /** Connected devices, or null without permission. Empty if Bluetooth is off or nothing is connected. */
    @SuppressLint("MissingPermission")
    fun connectedDevices(context: Context): List<DeviceBattery>? {
        if (!hasPermission(context)) return null
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter ?: return emptyList()
        return try {
            if (!adapter.isEnabled) return emptyList()
            adapter.bondedDevices.orEmpty()
                .filter { isConnected(it) }
                .map { DeviceBattery(it.aliasCompat ?: "DEVICE", batteryLevel(it)) }
        } catch (_: SecurityException) {
            null
        }
    }

    private fun isConnected(device: BluetoothDevice): Boolean =
        runCatching { device.javaClass.getMethod("isConnected").invoke(device) as Boolean }.getOrDefault(false)

    private fun batteryLevel(device: BluetoothDevice): Int? =
        runCatching { (device.javaClass.getMethod("getBatteryLevel").invoke(device) as Int) }.getOrNull()
            ?.takeIf { it in 0..100 }

    private val BluetoothDevice.aliasCompat: String?
        @SuppressLint("MissingPermission") get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) alias else name
}
