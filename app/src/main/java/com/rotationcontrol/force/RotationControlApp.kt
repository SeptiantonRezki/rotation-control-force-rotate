package com.rotationcontrol.force

import android.app.Application
import com.google.android.gms.ads.MobileAds
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.ump.ConsentInformation
import com.google.android.ump.UserMessagingPlatform

class RotationControlApp : Application() {
    lateinit var consentInformation: ConsentInformation
        private set
    @Volatile private var adsInitializationStarted = false

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        // Activity requests consent update and presents any required form before loading ads.
    }

    @Synchronized
    fun initializeAds() {
        if (adsInitializationStarted) return
        adsInitializationStarted = true
        MobileAds.initialize(this)
    }
}
