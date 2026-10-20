package com.rob.glasses

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import org.json.JSONObject
import java.util.UUID

/**
 * The classic-Bluetooth line to the MemoMind glasses ("door 2"), the same one
 * the official app uses. Bytes are passed to and from the screens unchanged.
 */
class GlassesLink(private val act: MainActivity) {

    @Volatile private var socket: BluetoothSocket? = null

    private val doors = listOf(
        "00001101-0000-1000-8000-00805f9b34fb", // serial port, worked from Chrome
        "00007033-0000-1000-8000-00805f9b34fb"  // the official app's command line
    )

    private fun tell(fn: String, arg: String? = null) {
        act.js("window.$fn && window.$fn(" + (if (arg == null) "" else JSONObject.quote(arg)) + ")")
    }

    @SuppressLint("MissingPermission")
    fun connect() {
        Thread {
            try {
                close()
                val adapter = act.getSystemService(BluetoothManager::class.java)?.adapter
                    ?: throw Exception("This phone has no Bluetooth.")
                if (Build.VERSION.SDK_INT >= 31 &&
                    act.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                ) throw Exception("Please allow Nearby devices, then tap Connect.")
                if (!adapter.isEnabled) throw Exception("Bluetooth is off. Turn it on, then tap Connect.")
                val device = adapter.bondedDevices.firstOrNull { (it.name ?: "").startsWith("MemoMind", ignoreCase = true) }
                    ?: throw Exception("Your MemoMind glasses aren't paired with this phone.")
                var lastError: Exception? = null
                for (uuid in doors) {
                    try {
                        val s = device.createRfcommSocketToServiceRecord(UUID.fromString(uuid))
                        s.connect()
                        socket = s
                        break
                    } catch (e: Exception) { lastError = e }
                }
                val s = socket ?: throw Exception("Couldn't reach the glasses. Close the MemoMind app and try again. (" + (lastError?.message ?: "") + ")")
                tell("onGlassesOpen", device.name ?: "MemoMind One")
                val input = s.inputStream
                val buf = ByteArray(2048)
                try {
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        if (n > 0) tell("onGlassesBytes", Base64.encodeToString(buf, 0, n, Base64.NO_WRAP))
                    }
                } catch (_: Exception) { }
                socket = null
                tell("onGlassesClosed")
            } catch (e: Exception) {
                tell("onGlassesError", e.message ?: e.toString())
            }
        }.start()
    }

    @Synchronized
    fun write(bytes: ByteArray) {
        try {
            socket?.outputStream?.apply { write(bytes); flush() }
        } catch (_: Exception) {
            close()
        }
    }

    fun close() {
        try { socket?.close() } catch (_: Exception) { }
        socket = null
    }
}
