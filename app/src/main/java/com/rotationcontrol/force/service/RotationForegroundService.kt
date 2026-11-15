package com.rotationcontrol.force.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Surface
import androidx.core.content.ContextCompat
import com.rotationcontrol.force.R
import com.rotationcontrol.force.data.RotationMode
import com.rotationcontrol.force.data.RotationStore
import com.rotationcontrol.force.ui.MainActivity

class RotationForegroundService : Service() {
    private lateinit var store: RotationStore
    private var sensorListener: android.view.OrientationEventListener? = null
    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: TelephonyCallback? = null
    private var legacyPhoneStateListener: PhoneStateListener? = null
    private var requestedMode = RotationMode.AUTO
    private var callInProgress = false
    private var callPauseActive = false

    override fun onCreate() {
        super.onCreate()
        store = RotationStore(this)
        telephonyManager = getSystemService(TelephonyManager::class.java)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        requestedMode = RotationMode.fromId(
            intent?.getIntExtra(EXTRA_MODE, store.lastMode.id) ?: store.lastMode.id
        )
        val active = intent?.getBooleanExtra(EXTRA_ACTIVE, store.serviceActive) ?: store.serviceActive
        startAsForeground(requestedMode)
        if (!active) {
            unregisterCallListener()
            stopSensor()
            setSystemAutoRotate()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        refreshCallListener()
        applyMode(requestedMode)
        return START_STICKY
    }

    private fun startAsForeground(mode: RotationMode) {
        val open = mainActivityPendingIntent()
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_rotation)
            .setContentTitle("Rotation Control: Force Rotate")
            .setContentText("Rotation service running · ${mode.title}")
            .setContentIntent(open)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Tap to Open App & Change Rotation", open).build())
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun applyMode(mode: RotationMode) {
        requestedMode = mode
        if (!Settings.System.canWrite(this)) return

        val shouldPauseForCall = store.pauseRotationDuringCalls &&
            hasPhoneStatePermission() && callInProgress &&
            mode != RotationMode.AUTO && mode != RotationMode.GUARD
        callPauseActive = shouldPauseForCall
        val appliedMode = if (shouldPauseForCall) RotationMode.AUTO else mode
        applyOrientation(appliedMode)
        updateNotification(mode, shouldPauseForCall)
    }

    private fun applyOrientation(mode: RotationMode) {
        when (mode) {
            RotationMode.AUTO, RotationMode.GUARD -> {
                stopSensor()
                writeRotationSetting(Settings.System.ACCELEROMETER_ROTATION, 1)
            }
            RotationMode.LANDSCAPE -> fixed(Surface.ROTATION_90)
            RotationMode.PORTRAIT -> fixed(Surface.ROTATION_0)
            RotationMode.REVERSE_PORTRAIT -> fixed(Surface.ROTATION_180)
            RotationMode.REVERSE_LANDSCAPE -> fixed(Surface.ROTATION_270)
            RotationMode.SENSOR_LANDSCAPE -> observeRange(landscape = true)
            RotationMode.SENSOR_PORTRAIT -> observeRange(landscape = false)
            RotationMode.LOCK_CURRENT -> fixed(currentRotation())
        }
    }

    private fun refreshCallListener() {
        if (!store.serviceActive || !store.pauseRotationDuringCalls || !hasPhoneStatePermission()) {
            val wasPaused = callPauseActive
            unregisterCallListener()
            if (wasPaused) applyMode(requestedMode)
            return
        }
        if (telephonyCallback != null || legacyPhoneStateListener != null) return

        val manager = telephonyManager ?: return
        try {
            callInProgress = manager.callState != TelephonyManager.CALL_STATE_IDLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) = handleCallState(state)
                }
                manager.registerTelephonyCallback(mainExecutor, callback)
                telephonyCallback = callback
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        // The phone number is intentionally ignored; only call state is needed.
                        handleCallState(state)
                    }
                }
                @Suppress("DEPRECATION")
                manager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                legacyPhoneStateListener = listener
            }
        } catch (error: SecurityException) {
            callInProgress = false
            Log.w(TAG, "Call-state permission was revoked; call bypass is inactive", error)
        } catch (error: RuntimeException) {
            callInProgress = false
            Log.w(TAG, "Unable to register call-state listener", error)
        }
    }

    private fun handleCallState(state: Int) {
        val inCall = state == TelephonyManager.CALL_STATE_RINGING ||
            state == TelephonyManager.CALL_STATE_OFFHOOK
        if (callInProgress == inCall) return
        callInProgress = inCall
        applyMode(requestedMode)
    }

    private fun hasPhoneStatePermission() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.READ_PHONE_STATE
    ) == PackageManager.PERMISSION_GRANTED

    private fun unregisterCallListener() {
        val manager = telephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { callback ->
                try {
                    manager?.unregisterTelephonyCallback(callback)
                } catch (error: RuntimeException) {
                    Log.d(TAG, "Call-state callback was already unregistered", error)
                }
            }
            telephonyCallback = null
        }
        legacyPhoneStateListener?.let { listener ->
            try {
                @Suppress("DEPRECATION")
                manager?.listen(listener, PhoneStateListener.LISTEN_NONE)
            } catch (error: RuntimeException) {
                Log.d(TAG, "Legacy call-state listener was already unregistered", error)
            }
            legacyPhoneStateListener = null
        }
        callInProgress = false
        callPauseActive = false
    }

    private fun fixed(rotation: Int) {
        stopSensor()
        writeRotationSetting(Settings.System.ACCELEROMETER_ROTATION, 0)
        writeRotationSetting(Settings.System.USER_ROTATION, rotation)
    }

    private fun setSystemAutoRotate() {
        if (Settings.System.canWrite(this)) {
            writeRotationSetting(Settings.System.ACCELEROMETER_ROTATION, 1)
        }
    }

    private fun writeRotationSetting(key: String, value: Int) {
        try {
            Settings.System.putInt(contentResolver, key, value)
        } catch (error: SecurityException) {
            Log.w(TAG, "Unable to update rotation setting $key", error)
        }
    }

    private fun currentRotation(): Int = try {
        Settings.System.getInt(contentResolver, Settings.System.USER_ROTATION, Surface.ROTATION_0)
    } catch (_: Exception) {
        Surface.ROTATION_0
    }

    private fun observeRange(landscape: Boolean) {
        writeRotationSetting(Settings.System.ACCELEROMETER_ROTATION, 0)
        stopSensor()
        sensorListener = object : android.view.OrientationEventListener(this) {
            override fun onOrientationChanged(degrees: Int) {
                if (degrees == ORIENTATION_UNKNOWN) return
                val target = if (landscape) {
                    when (degrees) {
                        in 60..120 -> Surface.ROTATION_90
                        in 240..300 -> Surface.ROTATION_270
                        else -> return
                    }
                } else {
                    when (degrees) {
                        in 0..30, in 330..359 -> Surface.ROTATION_0
                        in 150..210 -> Surface.ROTATION_180
                        else -> return
                    }
                }
                writeRotationSetting(Settings.System.USER_ROTATION, target)
            }
        }.also { if (it.canDetectOrientation()) it.enable() }
    }

    private fun stopSensor() {
        sensorListener?.disable()
        sensorListener = null
    }

    private fun updateNotification(mode: RotationMode, pausedForCall: Boolean) {
        val message = if (pausedForCall) {
            "Phone call detected · rotation paused · ${mode.title}"
        } else {
            "Rotation service running · ${mode.title}"
        }
        val open = mainActivityPendingIntent()
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_rotation)
            .setContentTitle("Rotation Control: Force Rotate")
            .setContentText(message)
            .setOngoing(true)
            .setContentIntent(open)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(Notification.Action.Builder(null, "Tap to Open App & Change Rotation", open).build())
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun mainActivityPendingIntent() = PendingIntent.getActivity(
        this,
        MAIN_ACTIVITY_REQUEST_CODE,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Rotation service", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onDestroy() {
        unregisterCallListener()
        stopSensor()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_MODE = "mode_id"
        const val EXTRA_ACTIVE = "active"
        private const val TAG = "RotationForeground"
        private const val CHANNEL_ID = "rotation_service"
        private const val NOTIFICATION_ID = 42
        private const val MAIN_ACTIVITY_REQUEST_CODE = 7

        fun start(context: android.content.Context, mode: RotationMode, active: Boolean = true) {
            val intent = Intent(context, RotationForegroundService::class.java)
                .putExtra(EXTRA_MODE, mode.id)
                .putExtra(EXTRA_ACTIVE, active)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
        }
    }
}
