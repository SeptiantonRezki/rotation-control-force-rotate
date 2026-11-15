package com.rotationcontrol.force.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.rotationcontrol.force.R
import com.rotationcontrol.force.ads.AdMobHelper
import com.rotationcontrol.force.data.RotationMode
import com.rotationcontrol.force.data.RotationStore
import com.rotationcontrol.force.databinding.ActivityMainBinding
import com.rotationcontrol.force.service.RotationForegroundService
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class MainActivity : AppCompatActivity(), AdMobHelper.ConsentCallbacks {
    private lateinit var binding: ActivityMainBinding
    private lateinit var store: RotationStore
    private var page = PAGE_CONTROL
    private var adsReady = false
    private var hadWriteSettingsPermission = false
    private var updatingServiceSwitch = false
    private var updatingCallBypassSwitch = false
    private var appProfileAdapter: AppProfileAdapter? = null
    private var appCatalog: List<AppProfileItem>? = null
    private var appCatalogExecutor: ExecutorService? = null
    private var appCatalogFuture: Future<*>? = null
    private var appCatalogGeneration = 0
    private var appCatalogLoading = false
    private val profileOverrides = mutableMapOf<String, Int?>()
    private var appSearchWatcher: android.text.TextWatcher? = null
    private val modes = RotationMode.entries

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val safeInsets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(safeInsets.left, safeInsets.top, safeInsets.right, safeInsets.bottom)
            windowInsets
        }
        ViewCompat.requestApplyInsets(binding.root)
        store = RotationStore(this)
        buildModeCards()
        setupAppList()
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navControl -> { showPage(PAGE_CONTROL); true }
                R.id.navApps -> {
                    if (store.packageInventoryDisclosureAccepted) showPage(PAGE_APPS)
                    else showPackageInventoryDisclosure()
                    store.packageInventoryDisclosureAccepted
                }
                R.id.navSettings -> { showPage(PAGE_SETTINGS); true }
                else -> false
            }
        }
        binding.serviceSwitch.setOnCheckedChangeListener { _, checked ->
            if (updatingServiceSwitch) return@setOnCheckedChangeListener
            store.serviceActive = checked
            if (checked) ensurePermissionThenStart() else RotationForegroundService.start(this, store.lastMode, false)
            refreshStatus()
        }
        binding.autoStartSwitch.isChecked = store.serviceActive
        binding.autoStartSwitch.setOnCheckedChangeListener { _, enabled -> store.serviceActive = enabled }
        binding.callBypassSwitch.isChecked = store.pauseRotationDuringCalls
        binding.callBypassSwitch.setOnCheckedChangeListener { _, enabled ->
            if (updatingCallBypassSwitch) return@setOnCheckedChangeListener
            if (!enabled) {
                store.pauseRotationDuringCalls = false
                refreshRunningService()
                return@setOnCheckedChangeListener
            }
            if (hasPhoneStatePermission()) {
                store.pauseRotationDuringCalls = true
                refreshRunningService()
            } else {
                showCallBypassDisclosure()
            }
        }
        binding.grantSettingsButton.setOnClickListener { requestWriteSettings() }
        binding.accessibilityButton.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Enable Per-App Rotation")
                .setMessage("Rotation Control uses Android Accessibility only to detect the package name of the app currently in the foreground, apply the rotation profile you selected, and temporarily use Auto-Rotate on a package installer screen. It does not read screen text or content, capture the screen, or send this information off your device. This access is optional and is used only for Per-App Rotation and installer compatibility. You can disable the service at any time in Android Accessibility settings.\n\nTap Continue to open Accessibility settings and enable it yourself.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Continue") { _, _ -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                .show()
        }
        binding.privacyOptionsButton.setOnClickListener {
            AdMobHelper.showPrivacyOptions(this, this)
        }
        binding.privacyPolicyButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
        }
        appSearchWatcher = SimpleTextWatcher { renderApps(it) }
        binding.searchApps.addTextChangedListener(appSearchWatcher)
        refreshStatus()
        showPage(PAGE_CONTROL)
        requestNotifications()
        AdMobHelper.requestConsent(this, this)
    }

    override fun onConsentFlowComplete() {
        if (!isFinishing && !isDestroyed) refreshPrivacyOptionsVisibility()
    }

    override fun onAdsReady() {
        if (isFinishing || isDestroyed) return
        adsReady = true
        refreshPrivacyOptionsVisibility()
        loadAdsForCurrentPage()
    }

    override fun onPrivacyOptionsDismissed() {
        if (!isFinishing && !isDestroyed) refreshPrivacyOptionsVisibility()
    }

    override fun onResume() {
        super.onResume()
        if (::store.isInitialized) {
            refreshStatus()
            refreshPrivacyOptionsVisibility()
            val canWriteSettings = Settings.System.canWrite(this)
            if (canWriteSettings && !hadWriteSettingsPermission && store.serviceActive) {
                RotationForegroundService.start(this, store.lastMode)
            }
            hadWriteSettingsPermission = canWriteSettings
            if (store.pauseRotationDuringCalls && !hasPhoneStatePermission()) {
                store.pauseRotationDuringCalls = false
                updateCallBypassSwitch(false)
                refreshRunningService()
            }
        }
    }

    @Deprecated("Permission callback used to handle the optional phone-state permission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_CALL_STATE_PERMISSION) return
        val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        store.pauseRotationDuringCalls = granted
        updateCallBypassSwitch(granted)
        if (granted) {
            refreshRunningService()
        } else {
            Toast.makeText(this, "Call bypass stays off because phone-state access was not granted.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        if (::binding.isInitialized) {
            appSearchWatcher?.let(binding.searchApps::removeTextChangedListener)
            appSearchWatcher = null
            binding.bottomNavigation.setOnItemSelectedListener(null)
            binding.serviceSwitch.setOnCheckedChangeListener(null)
            binding.autoStartSwitch.setOnCheckedChangeListener(null)
            binding.callBypassSwitch.setOnCheckedChangeListener(null)
            binding.grantSettingsButton.setOnClickListener(null)
            binding.accessibilityButton.setOnClickListener(null)
            binding.privacyOptionsButton.setOnClickListener(null)
            binding.privacyPolicyButton.setOnClickListener(null)
            binding.appList.adapter = null
            appProfileAdapter?.clearResources()
            appProfileAdapter = null
            appCatalogGeneration++
            appCatalogFuture?.cancel(true)
            appCatalogFuture = null
            appCatalogExecutor?.shutdownNow()
            appCatalogExecutor = null
            appCatalogLoading = false
            appCatalog = null
            profileOverrides.clear()
            ViewCompat.setOnApplyWindowInsetsListener(binding.root, null)
            AdMobHelper.destroyAdContainers(
                binding.controlNativeAd,
                binding.appsNativeAd,
                binding.settingsNativeAd,
                binding.bannerContainer
            )
        }
        super.onDestroy()
    }

    private fun showPackageInventoryDisclosure() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Per-App Rotation and app list access")
            .setMessage("To let you choose rotation rules for individual apps, Rotation Control needs visibility of apps installed on this device. QUERY_ALL_PACKAGES provides that list; Android does not show a separate runtime prompt for it. This app's rotation feature uses package names locally to display your choices and does not send the list in our app's ad requests or analytics. The permission grants package visibility to code running in this app, including included SDKs. If you continue, you can then optionally enable Accessibility access so your selected rule can be applied when an app opens.")
            .setNegativeButton("Not now") { _, _ ->
                binding.bottomNavigation.selectedItemId = currentNavigationItem()
            }
            .setPositiveButton("Continue") { _, _ ->
                store.packageInventoryDisclosureAccepted = true
                binding.bottomNavigation.selectedItemId = R.id.navApps
            }
            .setOnCancelListener {
                binding.bottomNavigation.selectedItemId = currentNavigationItem()
            }
            .show()
    }

    private fun currentNavigationItem(): Int = when (page) {
        PAGE_CONTROL -> R.id.navControl
        PAGE_APPS -> R.id.navApps
        else -> R.id.navSettings
    }

    private fun refreshPrivacyOptionsVisibility() {
        if (!::binding.isInitialized) return
        binding.privacyOptionsButton.visibility = if (adsReady && AdMobHelper.isPrivacyOptionsRequired(this)) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun updateServiceSwitch(enabled: Boolean) {
        updatingServiceSwitch = true
        binding.serviceSwitch.isChecked = enabled
        updatingServiceSwitch = false
    }

    private fun updateCallBypassSwitch(enabled: Boolean) {
        updatingCallBypassSwitch = true
        binding.callBypassSwitch.isChecked = enabled
        updatingCallBypassSwitch = false
    }

    private fun hasPhoneStatePermission() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.READ_PHONE_STATE
    ) == PackageManager.PERMISSION_GRANTED

    private fun showCallBypassDisclosure() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Pause rotation during phone calls")
            .setMessage("Rotation Control needs Android's Phone state permission to detect only when a cellular call is ringing, active, or idle. During a call it temporarily enables system auto-rotate so the dialer can use its preferred orientation, then restores your selected rotation when the call ends. The app does not read or store phone numbers, contacts, or call history. This feature is optional.")
            .setNegativeButton("Cancel") { _, _ -> updateCallBypassSwitch(false) }
            .setPositiveButton("Continue") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_PHONE_STATE),
                    REQUEST_CALL_STATE_PERMISSION
                )
            }
            .setOnCancelListener { updateCallBypassSwitch(false) }
            .show()
    }

    private fun refreshRunningService() {
        if (store.serviceActive && Settings.System.canWrite(this)) {
            RotationForegroundService.start(this, store.lastMode)
        }
    }

    private fun buildModeCards() {
        binding.modeGrid.columnCount = 3
        binding.modeGrid.clipChildren = false
        binding.modeGrid.clipToPadding = false
        modes.forEach { mode ->
            val card = TextView(this).apply {
                text = mode.title
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setTextColor(Color.parseColor("#E6FFFFFF"))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                isClickable = true
                isFocusable = true
                elevation = dp(2).toFloat()
                tag = mode.id
                background = createModeCardBackground(active = false)
                foreground = android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#22D8B4FE")),
                    null,
                    android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = dp(22).toFloat()
                        setColor(Color.WHITE)
                    }
                )
                layoutParams = android.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(104)
                    columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                    setMargins(dp(5), dp(5), dp(5), dp(5))
                }
                setOnClickListener { selectMode(mode) }
            }
            binding.modeGrid.addView(card)
        }
        refreshModeCards()
    }

    private fun setupAppList() {
        val adapter = AppProfileAdapter(this, modes) { packageName, modeId ->
            profileOverrides[packageName] = modeId
            store.setProfile(packageName, modeId)
        }
        appProfileAdapter = adapter
        binding.appList.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@MainActivity)
            // Keep the list virtualized inside the page scroller. A bounded viewport prevents
            // RecyclerView from measuring and binding the full installed-app catalog at once.
            isNestedScrollingEnabled = true
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
                addDuration = 120L
                removeDuration = 120L
                moveDuration = 120L
                changeDuration = 120L
            }
            this.adapter = adapter
        }
    }

    private fun selectMode(mode: RotationMode) {
        if (!Settings.System.canWrite(this)) { requestWriteSettings(); return }
        store.choose(mode); store.serviceActive = true; updateServiceSwitch(true)
        RotationForegroundService.start(this, mode); refreshModeCards(); refreshStatus()
    }

    private fun requestWriteSettings() {
        Toast.makeText(this, "Allow Modify system settings to control device rotation", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
    }
    private fun ensurePermissionThenStart() { if (Settings.System.canWrite(this)) RotationForegroundService.start(this, store.lastMode) else requestWriteSettings() }

    private fun refreshStatus() {
        val allowed = Settings.System.canWrite(this)
        binding.permissionStatus.text = if (allowed) "System settings access granted" else "Allow Modify system settings to enable device-wide rotation"
        binding.grantSettingsButton.visibility = if (allowed) android.view.View.GONE else android.view.View.VISIBLE
        updateServiceSwitch(store.serviceActive)
        binding.serviceStatus.text = when {
            !store.serviceActive -> "Service is off"
            allowed -> "Service active · ${store.lastMode.title}"
            else -> "Grant system settings access to start the service"
        }
        binding.serviceStatus.setTextColor(
            if (store.serviceActive && allowed) Color.parseColor("#5EEAD4")
            else Color.parseColor("#E6FFFFFF")
        )
        refreshModeCards()
    }
    private fun refreshModeCards() {
        if (!::binding.isInitialized || !::store.isInitialized) return
        for (i in 0 until binding.modeGrid.childCount) {
            val card = binding.modeGrid.getChildAt(i) as TextView
            val active = card.tag == store.lastMode.id
            card.background = createModeCardBackground(active)
            card.elevation = dp(if (active) 8 else 2).toFloat()
            card.setTextColor(Color.parseColor(if (active) "#D8B4FE" else "#E6FFFFFF"))
        }
    }

    private fun createModeCardBackground(active: Boolean): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            if (active) intArrayOf(Color.parseColor("#3D392850"), Color.parseColor("#29241445"))
            else intArrayOf(Color.parseColor("#22FFFFFF"), Color.parseColor("#171A1530"))
        ).apply {
            cornerRadius = dp(22).toFloat()
            setStroke(
                dp(if (active) 2 else 1),
                Color.parseColor(if (active) "#995EEAD4" else "#33FFFFFF")
            )
        }

    private fun showPage(destination: Int) {
        page = destination
        moveBannerToPage(destination)
        binding.controlPage.visibility = if (page == PAGE_CONTROL) android.view.View.VISIBLE else android.view.View.GONE
        binding.appsPage.visibility = if (page == PAGE_APPS) android.view.View.VISIBLE else android.view.View.GONE
        binding.settingsPage.visibility = if (page == PAGE_SETTINGS) android.view.View.VISIBLE else android.view.View.GONE
        // NavigationBarView dispatches the selection callback before it updates its
        // selected item. Setting selectedItemId here re-enters that callback and
        // recursively calls showPage() (StackOverflowError). The view updates the
        // selection itself after this callback returns; callers outside navigation
        // should select an item explicitly before/after invoking showPage().
        if (page == PAGE_APPS) renderApps(binding.searchApps.text.toString())
        loadAdsForCurrentPage()
    }

    private fun moveBannerToPage(destination: Int) {
        val targetSlot = when (destination) {
            PAGE_APPS -> binding.bannerSlotApps
            PAGE_SETTINGS -> binding.bannerSlotSettings
            else -> binding.bannerSlotControl
        }
        if (binding.bannerContainer.parent === targetSlot) return
        (binding.bannerContainer.parent as? android.view.ViewGroup)?.removeView(binding.bannerContainer)
        targetSlot.addView(
            binding.bannerContainer,
            android.widget.FrameLayout.LayoutParams(-1, -2, android.view.Gravity.CENTER)
        )
    }

    private fun loadAdsForCurrentPage() {
        if (!::binding.isInitialized || !adsReady) return
        val native = when (page) { PAGE_CONTROL -> binding.controlNativeAd; PAGE_APPS -> binding.appsNativeAd; else -> binding.settingsNativeAd }
        AdMobHelper.loadNative(this, native)
        AdMobHelper.loadBanner(this, binding.bannerContainer)
    }
    private fun renderApps(query: String) {
        if (!::binding.isInitialized) return
        val adapter = appProfileAdapter ?: return
        val normalizedQuery = query.trim()
        val catalog = appCatalog
        if (catalog == null) {
            binding.appList.visibility = android.view.View.GONE
            binding.appEmptyState.visibility = android.view.View.GONE
            binding.appLoadingState.visibility = android.view.View.VISIBLE
            if (!appCatalogLoading) loadAppCatalogAsync()
            return
        }
        binding.appLoadingState.visibility = android.view.View.GONE
        binding.appList.visibility = android.view.View.VISIBLE
        val profiles = store.profiles()
        val filtered = catalog.asSequence()
            .filter {
                normalizedQuery.isEmpty() ||
                    it.label.contains(normalizedQuery, ignoreCase = true) ||
                    it.packageName.contains(normalizedQuery, ignoreCase = true)
            }
            .map { item ->
                val selected = if (profileOverrides.containsKey(item.packageName)) {
                    profileOverrides[item.packageName]
                } else {
                    profiles[item.packageName]
                }
                item.copy(selectedModeId = selected)
            }
            .toList()
        adapter.submitList(filtered)
        binding.appEmptyState.visibility = if (filtered.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun loadAppCatalogAsync() {
        if (appCatalogLoading || isFinishing || isDestroyed) return
        appCatalogLoading = true
        val generation = ++appCatalogGeneration
        val appContext = applicationContext
        val currentPackageName = packageName
        val activityRef = WeakReference(this)
        val mainHandler = Handler(Looper.getMainLooper())
        val executor = appCatalogExecutor ?: Executors.newSingleThreadExecutor { task ->
            Thread(task, "app-catalog-loader").apply { isDaemon = true }
        }.also { appCatalogExecutor = it }
        appCatalogFuture = executor.submit {
            val catalog = runCatching {
                val pm = appContext.packageManager
                pm.getInstalledApplications(0)
                    .asSequence()
                    .filter {
                        it.packageName != currentPackageName &&
                            runCatching { pm.getLaunchIntentForPackage(it.packageName) != null }.getOrDefault(false)
                    }
                    .map { info ->
                        AppProfileItem(
                            packageName = info.packageName,
                            label = pm.getApplicationLabel(info).toString(),
                            selectedModeId = null
                        )
                    }
                    .sortedBy { it.label.lowercase(java.util.Locale.ROOT) }
                    .toList()
            }.getOrDefault(emptyList())
            mainHandler.post {
                val activity = activityRef.get() ?: return@post
                if (activity.isFinishing || activity.isDestroyed || activity.appCatalogGeneration != generation) return@post
                activity.appCatalogFuture = null
                activity.appCatalogLoading = false
                activity.appCatalog = catalog
                activity.renderApps(activity.binding.searchApps.text?.toString().orEmpty())
            }
        }
    }
    private fun requestNotifications() { if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 50) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private class SimpleTextWatcher(val changed: (String) -> Unit) : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed(s?.toString().orEmpty()) }
        override fun afterTextChanged(s: android.text.Editable?) {}
    }
    companion object {
        private const val PRIVACY_POLICY_URL = "https://septiantonrezki.github.io/rotation-control-force-rotate/privacy-policy.html"
        private const val PAGE_CONTROL = 0
        private const val PAGE_APPS = 1
        private const val PAGE_SETTINGS = 2
        private const val REQUEST_CALL_STATE_PERMISSION = 51
    }
}
