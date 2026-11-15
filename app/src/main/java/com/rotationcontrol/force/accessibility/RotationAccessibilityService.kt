package com.rotationcontrol.force.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.rotationcontrol.force.data.RotationMode
import com.rotationcontrol.force.data.RotationStore
import com.rotationcontrol.force.service.RotationForegroundService

class RotationAccessibilityService : AccessibilityService() {
    private var store: RotationStore? = null
    private var lastForegroundPackage: String? = null
    private var lastAppliedModeId: Int? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        store = RotationStore(applicationContext)
        lastForegroundPackage = null
        lastAppliedModeId = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val foreground = event.packageName?.toString() ?: return
        // System UI is not an app profile target. Returning to our own UI restores the global mode.
        if (foreground == "com.android.systemui") return
        val state = store ?: return
        if (!state.serviceActive) return

        val mode = when {
            isPackageInstaller(foreground) -> RotationMode.AUTO
            foreground == packageName -> state.lastMode
            else -> state.profiles()[foreground]?.let(RotationMode::fromId) ?: state.lastMode
        }
        if (foreground == lastForegroundPackage && mode.id == lastAppliedModeId) return
        lastForegroundPackage = foreground
        lastAppliedModeId = mode.id

        try {
            RotationForegroundService.start(applicationContext, mode)
        } catch (error: RuntimeException) {
            // The foreground service may be unavailable while the OS is transitioning lifecycle state.
            lastAppliedModeId = null
            Log.w(TAG, "Unable to update rotation mode for foreground app", error)
        }
    }

    override fun onInterrupt() {
        lastForegroundPackage = null
        lastAppliedModeId = null
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        store = null
        lastForegroundPackage = null
        lastAppliedModeId = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        store = null
        lastForegroundPackage = null
        lastAppliedModeId = null
        super.onDestroy()
    }

    private fun isPackageInstaller(packageName: String): Boolean {
        val normalized = packageName.lowercase()
        return normalized in KNOWN_INSTALLERS ||
            normalized.contains("packageinstaller") ||
            (normalized.contains("package") && normalized.contains("installer"))
    }

    private companion object {
        const val TAG = "RotationAccessibility"
        val KNOWN_INSTALLERS = setOf(
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.samsung.android.packageinstaller",
            "com.miui.packageinstaller",
            "com.miui.global.packageinstaller",
            "com.oplus.packageinstaller",
            "com.coloros.packageinstaller",
            "com.huawei.packageinstaller",
            "com.lge.packageinstaller"
        )
    }
}
