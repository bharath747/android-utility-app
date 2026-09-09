package com.bharathsimha.androidutilityapp.hotspot

import android.content.Context
import android.os.Build
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Attempts to stop a Wi-Fi tethering request created through the Android 16+
 * TetheringManager API.
 *
 * Important: Android's API stops a matching TetheringRequest. It does not provide
 * a general-purpose switch for a hotspot that was manually enabled in Settings.
 * The caller must therefore keep the notification fallback for ordinary devices.
 */
class HotspotAutoOffController(private val context: Context) {
    private val executor: Executor = Executors.newSingleThreadExecutor()

    fun tryTurnOff(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < 36) {
            onResult(false)
            return
        }

        try {
            val tetheringManager =
                context.getSystemService(android.net.TetheringManager::class.java)
                    ?: run {
                        onResult(false)
                        return
                    }

            val request = android.net.TetheringManager.TetheringRequest.Builder(
                android.net.TetheringManager.TETHERING_WIFI
            ).build()

            tetheringManager.stopTethering(
                request,
                executor,
                object : android.net.TetheringManager.StopTetheringCallback {
                    override fun onStopTetheringSucceeded() {
                        onResult(true)
                    }

                    override fun onStopTetheringFailed(error: Int) {
                        onResult(false)
                    }
                }
            )
        } catch (_: SecurityException) {
            onResult(false)
        } catch (_: UnsupportedOperationException) {
            onResult(false)
        } catch (_: Exception) {
            onResult(false)
        }
    }
}
