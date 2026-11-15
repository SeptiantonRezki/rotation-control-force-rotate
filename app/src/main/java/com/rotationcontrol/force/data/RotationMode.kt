package com.rotationcontrol.force.data

enum class RotationMode(val id: Int, val title: String, val description: String) {
    GUARD(0, "Guard Mode", "Keep the current system rotation policy"),
    AUTO(1, "Auto-Rotate", "Use all four sensor directions"),
    LANDSCAPE(2, "Landscape", "Lock to landscape"),
    PORTRAIT(3, "Portrait", "Lock to portrait"),
    REVERSE_PORTRAIT(4, "Reverse Portrait", "Lock upside down"),
    SENSOR_LANDSCAPE(5, "Sensor Landscape", "Rotate between landscape directions"),
    REVERSE_LANDSCAPE(6, "Reverse Landscape", "Lock to reverse landscape"),
    SENSOR_PORTRAIT(7, "Sensor Portrait", "Rotate between portrait directions"),
    LOCK_CURRENT(8, "Lock Current", "Lock the current display orientation");

    companion object {
        fun fromId(id: Int) = entries.firstOrNull { it.id == id } ?: AUTO
    }
}
