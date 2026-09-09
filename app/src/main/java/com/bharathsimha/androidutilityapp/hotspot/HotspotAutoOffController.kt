package com.bharathsimha.androidutilityapp.hotspot

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class HotspotAutoOffController(private val context: Context) {
    private val executor: Executor = Executors.newSingleThreadExecutor()

    /**
     * Attempts direct hotspot shutdown when the platform and app privileges allow it.
     * Returns true only when the request was accepted by Android. A normal third-party
     * app will usually fall back to the notification path.
     */
    fun tryTurnOff(): Boolean {
        if (Build.VERSION.SDK_INT < 36) return false
        if (!Settings.System.canWrite(context)) return false

        return try {
            val tetheringManager = context.getSystemService(android.net.TetheringManager::class.java)
            tetheringManager.stopTethering(android.net.TetheringManager.TETHERING_WIFI)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: UnsupportedOperationException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}
