package com.rotationcontrol.force.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.rotationcontrol.force.data.RotationStore
import com.rotationcontrol.force.service.RotationForegroundService

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_LOCKED_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val store = RotationStore(context)
        if (!store.serviceActive || !Settings.System.canWrite(context)) return
        try {
            RotationForegroundService.start(context, store.lastMode)
        } catch (error: RuntimeException) {
            // Some OS/OEM background-start policies may refuse the boot-time start. Saved state
            // remains intact and the launcher activity retries once the user opens the app.
            Log.w(TAG, "Unable to restore the rotation service during boot", error)
        }
    }

    private companion object { const val TAG = "RotationBootReceiver" }
}
