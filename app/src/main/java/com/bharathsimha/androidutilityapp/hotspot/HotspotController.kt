package com.bharathsimha.androidutilityapp.hotspot

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Best-effort hotspot controller.
 *
 * Android does not expose a general public API that lets an ordinary third-party
 * app switch off a hotspot that was created by the system Settings UI. The
 * public TetheringManager API added in API 36 controls tethering requests, but
 * it does not provide a general-purpose "turn off the user's existing hotspot"
 * operation for ordinary apps.
 *
 * Therefore this first version provides a safe scheduled action and opens the
 * system wireless settings when direct control is unavailable.
 */
class HotspotController(private val context: Context) {
    fun openHotspotSettings() {
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
