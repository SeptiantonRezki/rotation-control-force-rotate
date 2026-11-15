package com.rotationcontrol.force.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class RotationStore(context: Context) {
    private val deviceContext = context.createDeviceProtectedStorageContext()
    private val prefs = deviceContext.getSharedPreferences("rotation_state", Context.MODE_PRIVATE)

    var serviceActive: Boolean
        get() = prefs.getBoolean(KEY_ACTIVE, true)
        set(value) { prefs.edit().putBoolean(KEY_ACTIVE, value).apply() }
    var lastMode: RotationMode
        get() = RotationMode.fromId(prefs.getInt(KEY_MODE, RotationMode.AUTO.id))
        private set(value) { prefs.edit().putInt(KEY_MODE, value.id).apply() }
    var packageInventoryDisclosureAccepted: Boolean
        get() = prefs.getBoolean(KEY_PACKAGE_DISCLOSURE, false)
        set(value) { prefs.edit().putBoolean(KEY_PACKAGE_DISCLOSURE, value).apply() }
    var pauseRotationDuringCalls: Boolean
        get() = prefs.getBoolean(KEY_PAUSE_DURING_CALLS, false)
        set(value) { prefs.edit().putBoolean(KEY_PAUSE_DURING_CALLS, value).apply() }

    fun choose(mode: RotationMode) {
        lastMode = mode
        val now = System.currentTimeMillis()
        val history = try { JSONArray(prefs.getString(KEY_HISTORY, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
        val updated = JSONArray().put(JSONObject().put("mode", mode.id).put("time", now))
        for (i in 0 until minOf(history.length(), 9)) updated.put(history.getJSONObject(i))
        prefs.edit().putLong(KEY_TIMESTAMP, now).putString(KEY_HISTORY, updated.toString()).apply()
    }

    fun profiles(): Map<String, Int> {
        val json = try { JSONObject(prefs.getString(KEY_PROFILES, "{}") ?: "{}") } catch (_: Exception) { JSONObject() }
        return buildMap { json.keys().forEach { key -> put(key, json.optInt(key, -1)) } }
    }
    fun setProfile(packageName: String, modeId: Int?) {
        val json = try { JSONObject(prefs.getString(KEY_PROFILES, "{}") ?: "{}") } catch (_: Exception) { JSONObject() }
        if (modeId == null) json.remove(packageName) else json.put(packageName, modeId)
        prefs.edit().putString(KEY_PROFILES, json.toString()).apply()
    }

    companion object {
        const val KEY_ACTIVE = "KEY_SERVICE_ACTIVE_STATE"
        const val KEY_MODE = "KEY_LAST_GLOBAL_ROTATION_MODE"
        const val KEY_TIMESTAMP = "KEY_LAST_ROTATION_TIMESTAMP"
        const val KEY_HISTORY = "KEY_ROTATION_HISTORY_LIST"
        const val KEY_PROFILES = "per_app_rotation_profiles"
        private const val KEY_PACKAGE_DISCLOSURE = "package_inventory_disclosure_accepted"
        private const val KEY_PAUSE_DURING_CALLS = "pause_rotation_during_calls"
    }
}
