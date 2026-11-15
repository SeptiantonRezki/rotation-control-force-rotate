package com.rotationcontrol.force.ads

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import java.lang.ref.WeakReference
import com.google.android.gms.ads.*
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.rotationcontrol.force.R
import com.rotationcontrol.force.RotationControlApp
import java.util.WeakHashMap

object AdMobHelper {
    const val BANNER_ID = "ca-app-pub-3359147804072195/5253776691"
    const val NATIVE_ID = "ca-app-pub-3359147804072195/6922081582"
    private val nativeRequestTokens = WeakHashMap<FrameLayout, Any>()

    interface ConsentCallbacks {
        fun onConsentFlowComplete()
        fun onAdsReady()
        fun onPrivacyOptionsDismissed()
    }

    fun requestConsent(activity: Activity, callbacks: ConsentCallbacks) {
        val app = activity.application as RotationControlApp
        val activityRef = WeakReference(activity)
        val callbacksRef = WeakReference(callbacks)
        val params = ConsentRequestParameters.Builder().build()
        var consentFlowReported = false
        var adsReadyReported = false
        fun isActivityUsable(): Boolean {
            val current = activityRef.get() ?: return false
            return !current.isFinishing && !current.isDestroyed && callbacksRef.get() != null
        }
        fun reportConsentFlowComplete() {
            if (!isActivityUsable() || consentFlowReported) return
            consentFlowReported = true
            callbacksRef.get()?.onConsentFlowComplete()
        }
        fun reportAdsReadyIfAllowed() {
            if (!isActivityUsable() || adsReadyReported || !app.consentInformation.canRequestAds()) return
            adsReadyReported = true
            app.initializeAds()
            callbacksRef.get()?.onAdsReady()
        }
        app.consentInformation.requestConsentInfoUpdate(activity, params, {
            val current = activityRef.get()
            if (current != null && !current.isFinishing && !current.isDestroyed) {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(current) {
                    reportConsentFlowComplete()
                    reportAdsReadyIfAllowed()
                }
            }
        }, {
            // UMP retains the prior session's decision when a refresh fails.
            reportConsentFlowComplete()
            reportAdsReadyIfAllowed()
        })
        // Use a valid prior-session decision without waiting on the network. The one-shot guard
        // prevents a duplicate ad load when the update/form callback also completes.
        reportAdsReadyIfAllowed()
    }

    fun isPrivacyOptionsRequired(activity: Activity): Boolean {
        val info = (activity.application as RotationControlApp).consentInformation
        return info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    fun showPrivacyOptions(activity: Activity, callbacks: ConsentCallbacks) {
        val activityRef = WeakReference(activity)
        val callbacksRef = WeakReference(callbacks)
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            val current = activityRef.get()
            if (current != null && !current.isFinishing && !current.isDestroyed) {
                callbacksRef.get()?.onPrivacyOptionsDismissed()
            }
        }
    }

    fun loadBanner(activity: Activity, container: FrameLayout) {
        val existingAd = container.getChildAt(0) as? AdView
        if (existingAd?.adUnitId == BANNER_ID) {
            container.visibility = View.VISIBLE
            return
        }
        existingAd?.destroy()
        if (container.width <= container.paddingLeft + container.paddingRight) {
            val activityRef = WeakReference(activity)
            val containerRef = WeakReference(container)
            container.post {
                val currentActivity = activityRef.get() ?: return@post
                val currentContainer = containerRef.get() ?: return@post
                if (!currentActivity.isFinishing && !currentActivity.isDestroyed &&
                    currentContainer.width > currentContainer.paddingLeft + currentContainer.paddingRight
                ) {
                    loadBanner(currentActivity, currentContainer)
                }
            }
            return
        }
        val ad = AdView(activity)
        ad.adUnitId = BANNER_ID
        val density = activity.resources.displayMetrics.density
        val measuredAvailableWidth = container.width - container.paddingLeft - container.paddingRight
        val widthDp = (measuredAvailableWidth / density).toInt().coerceAtLeast(1)
        ad.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp))
        container.removeAllViews()
        container.addView(ad, FrameLayout.LayoutParams(-1, -2))
        ad.loadAd(AdRequest.Builder().build())
    }

    fun loadNative(activity: Activity, container: FrameLayout) {
        val requestToken = Any()
        synchronized(nativeRequestTokens) { nativeRequestTokens[container] = requestToken }
        (container.tag as? NativeAd)?.destroy()
        container.tag = null
        container.removeAllViews()
        val activityRef = WeakReference(activity)
        val containerRef = WeakReference(container)
        val loader = AdLoader.Builder(activity, NATIVE_ID)
            .forNativeAd { nativeAd ->
                val currentActivity = activityRef.get()
                val currentContainer = containerRef.get()
                if (currentActivity == null || currentContainer == null ||
                    currentActivity.isFinishing || currentActivity.isDestroyed ||
                    !isCurrentNativeRequest(currentContainer, requestToken)
                ) {
                    nativeAd.destroy()
                } else {
                    bindNative(currentActivity, currentContainer, nativeAd)
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    val currentActivity = activityRef.get() ?: return
                    val currentContainer = containerRef.get() ?: return
                    if (!currentActivity.isFinishing && !currentActivity.isDestroyed &&
                        isCurrentNativeRequest(currentContainer, requestToken)
                    ) {
                        currentContainer.visibility = View.GONE
                    }
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        loader.loadAd(AdRequest.Builder().build())
    }

    private fun bindNative(activity: Activity, container: FrameLayout, ad: NativeAd) {
        val view = LayoutInflater.from(activity).inflate(R.layout.ad_native, container, false) as NativeAdView
        view.headlineView = view.findViewById(R.id.adHeadline)
        view.bodyView = view.findViewById(R.id.adBody)
        view.callToActionView = view.findViewById(R.id.adAction)
        (view.headlineView as android.widget.TextView).text = ad.headline
        (view.bodyView as android.widget.TextView).apply { text = ad.body; visibility = if (ad.body == null) View.GONE else View.VISIBLE }
        (view.callToActionView as android.widget.Button).apply { text = ad.callToAction; visibility = if (ad.callToAction == null) View.GONE else View.VISIBLE }
        view.setNativeAd(ad)
        container.tag = ad
        container.removeAllViews(); container.addView(view); container.visibility = View.VISIBLE
    }

    fun destroyAdContainers(vararg containers: FrameLayout) {
        containers.forEach { container ->
            synchronized(nativeRequestTokens) { nativeRequestTokens.remove(container) }
            (container.tag as? NativeAd)?.destroy()
            container.tag = null
            (container.getChildAt(0) as? AdView)?.destroy()
            container.removeAllViews()
        }
    }

    private fun isCurrentNativeRequest(container: FrameLayout, token: Any): Boolean =
        synchronized(nativeRequestTokens) { nativeRequestTokens[container] === token }
}
