package com.bharathsimha.androidutilityapp.hotspot

import android.content.Context
import android.os.Build
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Best-effort control of a Wi-Fi tethering session requested by this app.
 *
 * Android only exposes TetheringManager from API 36. A request created by the
 * app can be stopped with the matching request. Hotspots started externally
 * continue to use the notification fallback.
 */
class HotspotAutoOffController(private val context: Context) {
    private val executor: Executor = Executors.newSingleThreadExecutor()

    fun isAppManaged(): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_APP_MANAGED, false)

    fun tryStart(onResult: (Boolean) -> Unit) {
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

            val request = buildWifiRequest()
            tetheringManager.startTethering(
                request,
                executor,
                object : android.net.TetheringManager.StartTetheringCallback {
                    override fun onTetheringStarted() {
                        setAppManaged(true)
                        onResult(true)
                    }

                    override fun onTetheringFailed(error: Int) {
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

    fun tryTurnOff(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < 36 || !isAppManaged()) {
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

            tetheringManager.stopTethering(
                buildWifiRequest(),
                executor,
                object : android.net.TetheringManager.StopTetheringCallback {
                    override fun onStopTetheringSucceeded() {
                        setAppManaged(false)
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

    private fun buildWifiRequest(): android.net.TetheringManager.TetheringRequest =
        android.net.TetheringManager.TetheringRequest.Builder(
            android.net.TetheringManager.TETHERING_WIFI
        ).build()

    private fun setAppManaged(value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_APP_MANAGED, value)
            .apply()
    }

    companion object {
        private const val PREFS = "hotspot_control"
        private const val KEY_APP_MANAGED = "app_managed"
    }
}
