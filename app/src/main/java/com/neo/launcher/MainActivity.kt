package com.neo.launcher

import android.app.Activity
import android.app.ActivityOptions
import android.app.Dialog
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var neo: NeoTheme
    private lateinit var fx: FxView
    private lateinit var home: LinearLayout
    private lateinit var brand: TextView
    private lateinit var clock: TextView
    private lateinit var dateTv: TextView
    private lateinit var statusTv: TextView
    private lateinit var bannerTv: TextView
    private lateinit var hintTv: TextView
    private lateinit var dock: LinearLayout
    private lateinit var drawer: FrameLayout
    private lateinit var drawerCol: LinearLayout
    private lateinit var grid: GridView
    private lateinit var search: EditText
    private lateinit var scramble: TextScramble

    private val appAdapter = AppAdapter()
    private val handler = Handler(Looper.getMainLooper())
    private var cfg: RemoteConfig = RemoteConfig.parse(null)
    private var uiKey = ""
    private var lastFetch = 0L
    private var dialog: Dialog? = null
    private var pendingPopup = false
    private var resumedNow = false
    private var wallpaperIsOurs = false
    private var downX = 0f
    private var downY = 0f
    private var downT = 0L
    private var downAtTop = true
    private val roLocale = Locale("ro", "RO")
    private val dateFmt = SimpleDateFormat("EEEE, d MMMM", roLocale)

    private val tick = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000L - System.currentTimeMillis() % 1000L)
        }
    }
    private val popupRunnable = Runnable {
        pendingPopup = false
        showPopupNow()
    }
    private val appsCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = reloadApps()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = reloadApps()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = reloadApps()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = reloadApps()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = reloadApps()
    }

    private val drawerOpen: Boolean
        get() = ::drawer.isInitialized && drawer.visibility == View.VISIBLE

    private fun dp(v: Float) = UiKit.dp(this, v)
    private fun currentKey() = "${prefs.themeId}|${prefs.fxLevel}|${prefs.bgMode}"

    // ───────────────────────── ciclu de viață ─────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        UiKit.edgeToEdge(window)
        cfg = ConfigRepo.cached(this)
        buildUi()
        try {
            getSystemService(LauncherApps::class.java)?.registerCallback(appsCallback)
        } catch (_: Exception) {
        }
        reloadApps()
        if (Build.VERSION.SDK_INT >= 33) {
            // Android 13+ (și obligatoriu pe Android 16): gestul „înapoi” închide sertarul,
            // iar pe ecranul principal nu face nimic, ca la orice launcher.
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                OnBackInvokedCallback { if (drawerOpen) closeDrawer(true) }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        resumedNow = true
        if (currentKey() != uiKey) buildUi() else updateFxVisibility()
        handler.removeCallbacks(tick)
        handler.post(tick)
        scramble.run(brandText())
        applyConfig(ConfigRepo.cached(this))
        refreshConfig(false)
    }

    override fun onPause() {
        resumedNow = false
        fx.running = false
        handler.removeCallbacks(tick)
        handler.removeCallbacks(popupRunnable)
        pendingPopup = false
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        closeDrawer(false)
    }

    override fun onDestroy() {
        try {
            getSystemService(LauncherApps::class.java)?.unregisterCallback(appsCallback)
        } catch (_: Exception) {
        }
        handler.removeCallbacksAndMessages(null)
        dialog?.dismiss()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // butonul Home apăsat când launcher-ul e deja deschis
        closeDrawer(true)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerOpen) closeDrawer(true)
    }

    // ───────────────────────── interfața ─────────────────────────

    private fun buildUi() {
        neo = NeoTheme.byId(prefs.themeId)
        uiKey = currentKey()
        val root = FrameLayout(this)

        fx = FxView(this).apply {
            setNeoTheme(neo)
            level = prefs.fxLevel
        }
        root.addView(fx, FrameLayout.LayoutParams(-1, -1))

        home = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.argb(120, 0, 0, 0), Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(170, 0, 0, 0))
            )
            setOnLongClickListener {
                openSettings()
                true
            }
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        brand = UiKit.label(this, neo, "", 13f, neo.secondary).apply {
            letterSpacing = 0.25f
            maxLines = 1
        }
        scramble = TextScramble(brand)
        top.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(UiKit.chip(this, neo, "◢ SYS").apply { setOnClickListener { openSettings() } })
        home.addView(top, LinearLayout.LayoutParams(-1, -2))

        clock = UiKit.label(this, neo, "", if (neo.id == "hologram") 84f else 72f, neo.text).apply {
            includeFontPadding = false
            setPadding(0, dp(28f), 0, 0)
        }
        home.addView(clock)
        dateTv = UiKit.label(this, neo, "", 15f, neo.primary).apply { letterSpacing = 0.12f }
        home.addView(dateTv)
        statusTv = UiKit.label(this, neo, "", 11f, neo.dim, glow = false).apply {
            letterSpacing = 0.12f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(8f), 0, 0)
        }
        home.addView(statusTv)

        bannerTv = UiKit.label(this, neo, "", 13f, neo.text).apply {
            background = UiKit.box(this@MainActivity, neo, 170, 230, 1.2f, neo.secondary)
            setPadding(dp(14f), dp(10f), dp(14f), dp(10f))
            visibility = View.GONE
        }
        home.addView(bannerTv, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(22f) })

        home.addView(View(this), LinearLayout.LayoutParams(-1, 0, 1f))

        hintTv = UiKit.label(this, neo, "︽   glisează în sus", 11f, neo.dim, glow = false).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.2f
            setOnClickListener { openDrawer() }
        }
        hintTv.animate().alpha(0.35f).setDuration(1400).withEndAction { pulseHint() }.start()
        home.addView(hintTv, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10f) })

        dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = UiKit.box(this@MainActivity, neo, 110, 160)
            setPadding(dp(6f), dp(10f), dp(6f), dp(10f))
        }
        home.addView(dock, LinearLayout.LayoutParams(-1, -2))
        root.addView(home, FrameLayout.LayoutParams(-1, -1))

        // sertarul de aplicații
        drawer = FrameLayout(this).apply {
            setBackgroundColor(UiKit.alpha(neo.bg, 230))
            visibility = View.GONE
            isClickable = true
        }
        drawerCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val handle = UiKit.label(this, neo, "▼   APLICAȚII", 12f, neo.secondary).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.3f
            setPadding(0, dp(12f), 0, dp(12f))
            setOnClickListener { closeDrawer(true) }
        }
        drawerCol.addView(handle, LinearLayout.LayoutParams(-1, -2))

        search = EditText(this).apply {
            setHint("›  caută aplicații_")
            setSingleLine()
            textSize = 15f
            setTextColor(neo.text)
            setHintTextColor(neo.dim)
            typeface = neo.typeface
            background = UiKit.box(this@MainActivity, neo, 150, 210)
            setPadding(dp(16f), dp(12f), dp(16f), dp(12f))
            imeOptions = EditorInfo.IME_ACTION_GO
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    appAdapter.filter(s?.toString() ?: "")
                }
            })
            setOnEditorActionListener { _, _, _ ->
                appAdapter.items.firstOrNull()?.let { launch(it, null) }
                true
            }
        }
        drawerCol.addView(search, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(6f), 0, dp(6f), dp(14f)) })

        grid = GridView(this).apply {
            numColumns = 4
            verticalSpacing = dp(18f)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            selector = ColorDrawable(Color.TRANSPARENT)
            isVerticalScrollBarEnabled = false
            clipToPadding = false
            adapter = appAdapter
            setOnItemClickListener { _, v, pos, _ ->
                val a = appAdapter.items.getOrNull(pos) ?: return@setOnItemClickListener
                v.animate().scaleX(0.86f).scaleY(0.86f).setDuration(80).withEndAction {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    launch(a, v)
                }.start()
            }
            setOnItemLongClickListener { _, v, pos, _ ->
                appAdapter.items.getOrNull(pos)?.let { appMenu(it, v) }
                true
            }
        }
        drawerCol.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))
        drawer.addView(drawerCol, FrameLayout.LayoutParams(-1, -1))
        root.addView(drawer, FrameLayout.LayoutParams(-1, -1))

        root.setOnApplyWindowInsetsListener { _, ins ->
            applyInsets(ins)
            ins
        }
        setContentView(root)
        root.requestApplyInsets()

        updateFxVisibility()
        renderApps()
        applyConfig(cfg, false)
        brand.text = brandText()
        updateClock()
    }

    private fun pulseHint() {
        if (!::hintTv.isInitialized) return
        val target = if (hintTv.alpha < 0.6f) 1f else 0.35f
        hintTv.animate().alpha(target).setDuration(1400).withEndAction { pulseHint() }.start()
    }

    @Suppress("DEPRECATION")
    private fun applyInsets(ins: WindowInsets) {
        val t = ins.systemWindowInsetTop
        val b = ins.systemWindowInsetBottom
        home.setPadding(dp(20f), t + dp(14f), dp(20f), b + dp(16f))
        drawerCol.setPadding(dp(14f), t + dp(4f), dp(14f), 0)
        grid.setPadding(0, 0, 0, b + dp(24f))
    }

    private fun updateFxVisibility() {
        wallpaperIsOurs = try {
            WallpaperManager.getInstance(this).wallpaperInfo?.packageName == packageName
        } catch (e: Exception) {
            false
        }
        val show = prefs.bgMode == 0 && !wallpaperIsOurs
        fx.visibility = if (show) View.VISIBLE else View.GONE
        fx.running = show
    }

    private fun brandText() = cfg.brand.ifBlank { RemoteConfig.DEFAULT_BRAND }

    private fun updateClock() {
        if (!::clock.isInitialized) return
        val now = Calendar.getInstance()
        val hm = String.format(Locale.US, "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        val sec = String.format(Locale.US, " %02d", now.get(Calendar.SECOND))
        val sb = SpannableStringBuilder(hm).append(sec)
        sb.setSpan(RelativeSizeSpan(0.3f), hm.length, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(ForegroundColorSpan(neo.primary), hm.length, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        clock.text = sb
        dateTv.text = dateFmt.format(now.time).uppercase(roLocale)

        val bm = getSystemService(BatteryManager::class.java)
        val bat = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val charging = bm?.isCharging == true
        val online = try {
            getSystemService(ConnectivityManager::class.java)?.activeNetwork != null
        } catch (e: Exception) {
            false
        }
        statusTv.text = buildString {
            append("◉ BAT ").append(bat).append('%')
            if (charging) append(" ⚡")
            append("   ·   ").append(if (online) "ONLINE" else "OFFLINE")
            append("   ·   ").append(AppRepo.apps.size).append(" APL")
        }
        if (neo.id == "cyberpunk" && now.get(Calendar.SECOND) % 9 == 0) glitch(clock)
    }

    private fun glitch(v: View) {
        v.animate().cancel()
        v.translationX = dp(5f).toFloat()
        v.alpha = 0.55f
        v.animate().translationX(-dp(3f).toFloat()).setDuration(50).withEndAction {
            v.animate().translationX(0f).alpha(1f).setDuration(70).start()
        }.start()
    }

    // ───────────────────────── aplicații ─────────────────────────

    private fun reloadApps() {
        AppRepo.load(this) { list ->
            if (!prefs.dockInitialized && list.isNotEmpty()) {
                prefs.dock = AppRepo.defaultDock(this, list)
                prefs.dockInitialized = true
            }
            if (!isDestroyed) renderApps()
        }
    }

    private fun renderApps() {
        appAdapter.filter(search.text?.toString() ?: "")
        dock.removeAllViews()
        val apps = AppRepo.apps
        val slots = ArrayList<View>()
        for (key in prefs.dock) {
            val a = apps.firstOrNull { it.key == key } ?: continue
            val cell = IconCell(this, neo, false, true)
            cell.bind(a, copy = true)
            cell.setOnClickListener { launch(a, cell.frame) }
            cell.setOnLongClickListener {
                appMenu(a, cell)
                true
            }
            slots.add(cell)
        }
        val drawerBtn = IconCell.drawerButton(this, neo).apply { setOnClickListener { openDrawer() } }
        slots.add(minOf(2, slots.size), drawerBtn)
        for (v in slots) dock.addView(v, LinearLayout.LayoutParams(0, -2, 1f))
        updateClock()
    }

    private fun launch(a: AppItem, v: View?) {
        try {
            val la = getSystemService(LauncherApps::class.java)
            var bounds: Rect? = null
            var opts: Bundle? = null
            if (v != null && v.width > 0) {
                val loc = IntArray(2)
                v.getLocationOnScreen(loc)
                bounds = Rect(loc[0], loc[1], loc[0] + v.width, loc[1] + v.height)
                opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.width, v.height).toBundle()
            }
            la.startMainActivity(a.component, a.user, bounds, opts)
        } catch (e: Exception) {
            toast("Nu pot deschide ${a.label}")
        }
    }

    private fun appMenu(a: AppItem, anchor: View) {
        val inDock = prefs.dock.contains(a.key)
        val pm = PopupMenu(this, anchor)
        pm.menu.add(0, 1, 0, if (inDock) "Scoate din dock" else "Adaugă în dock")
        pm.menu.add(0, 2, 1, "Info aplicație")
        pm.menu.add(0, 3, 2, "Dezinstalează")
        pm.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> toggleDock(a)
                2 -> safeStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${a.pkg}")))
                3 -> safeStart(Intent(Intent.ACTION_DELETE, Uri.parse("package:${a.pkg}")))
            }
            true
        }
        pm.show()
    }

    private fun toggleDock(a: AppItem) {
        val list = prefs.dock.toMutableList()
        if (list.contains(a.key)) {
            list.remove(a.key)
        } else {
            if (list.size >= 4) {
                toast("Dock-ul e plin (max. 4). Scoate întâi o aplicație.")
                return
            }
            list.add(a.key)
        }
        prefs.dock = list
        prefs.dockInitialized = true
        renderApps()
    }

    // ───────────────────────── sertar și gesturi ─────────────────────────

    private fun openDrawer() {
        if (drawerOpen) return
        drawer.visibility = View.VISIBLE
        drawer.alpha = 0f
        drawer.translationY = home.height * 0.25f
        drawer.animate().alpha(1f).translationY(0f).setDuration(260).setInterpolator(DecelerateInterpolator()).start()
        home.animate().alpha(0f).scaleX(0.94f).scaleY(0.94f).setDuration(220).start()
        grid.setSelection(0)
    }

    private fun closeDrawer(animate: Boolean) {
        if (!drawerOpen) return
        hideKeyboard()
        if (search.text.isNotEmpty()) search.setText("")
        drawer.animate().cancel()
        home.animate().cancel()
        if (animate) {
            drawer.animate().alpha(0f).translationY(home.height * 0.2f).setDuration(200)
                .withEndAction { drawer.visibility = View.GONE }.start()
            home.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(240).start()
        } else {
            drawer.visibility = View.GONE
            home.alpha = 1f
            home.scaleX = 1f
            home.scaleY = 1f
        }
    }

    private fun gridAtTop(): Boolean {
        if (grid.childCount == 0) return true
        val first = grid.getChildAt(0)
        return grid.firstVisiblePosition == 0 && first.top >= grid.paddingTop - dp(2f)
    }

    private fun hideKeyboard() {
        try {
            getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(search.windowToken, 0)
        } catch (_: Exception) {
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
                downT = ev.eventTime
                downAtTop = !drawerOpen || gridAtTop()
                if (!drawerOpen) {
                    if (fx.visibility == View.VISIBLE) {
                        fx.touchFx(ev.x, ev.y)
                    } else if (wallpaperIsOurs) {
                        try {
                            WallpaperManager.getInstance(this).sendWallpaperCommand(
                                window.decorView.windowToken, WallpaperManager.COMMAND_TAP,
                                ev.rawX.toInt(), ev.rawY.toInt(), 0, null
                            )
                        } catch (_: Exception) {
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                val dx = ev.rawX - downX
                val dy = ev.rawY - downY
                if (ev.eventTime - downT < 500 && abs(dy) > dp(80f) && abs(dy) > abs(dx) * 1.5f) {
                    if (dy < 0 && !drawerOpen) openDrawer()
                    else if (dy > 0 && drawerOpen && downAtTop) closeDrawer(true)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    // ───────────────────────── config de la admin ─────────────────────────

    private fun refreshConfig(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastFetch < 5 * 60_000L) return
        lastFetch = now
        ConfigRepo.refresh(this) { c ->
            if (c == null || isFinishing || isDestroyed) return@refresh
            cfg = c
            if (currentKey() != uiKey) buildUi()
            applyConfig(c)
        }
    }

    private fun applyConfig(c: RemoteConfig, popups: Boolean = true) {
        cfg = c
        val b = brandText()
        if (brand.tag != b) {
            brand.tag = b
            scramble.run(b)
        }
        if (c.bannerOn && c.bannerText.isNotBlank()) {
            bannerTv.visibility = View.VISIBLE
            bannerTv.text = "▶  ${c.bannerText}"
            bannerTv.setOnClickListener { if (c.bannerUrl.isNotBlank()) openUrl(c.bannerUrl) }
        } else {
            bannerTv.visibility = View.GONE
        }
        if (popups) schedulePopup()
    }

    private fun needsUpdate(c: RemoteConfig) =
        !BuildConfig.IS_PLAY && c.updateCode > BuildConfig.VERSION_CODE && c.updateUrl.startsWith("http") &&
            (c.updateForce || prefs.updateDismissed != c.updateCode)

    private fun popupDue(p: PopupCfg): Boolean {
        if (BuildConfig.IS_PLAY) return false // Google Play nu permite popup-uri neașteptate
        if (!p.enabled || p.id.isBlank() || (p.title.isBlank() && p.imageUrl.isBlank())) return false
        val now = System.currentTimeMillis()
        if (now - prefs.lastPopupAny < 45_000L) return false
        if (p.maxShows > 0 && prefs.popupCount(p.id) >= p.maxShows) return false
        val last = prefs.lastPopup(p.id)
        return last == 0L || now - last >= (p.everyHours * 3_600_000.0).toLong()
    }

    private fun schedulePopup() {
        if (!resumedNow || pendingPopup || dialog?.isShowing == true) return
        val delay = when {
            needsUpdate(cfg) -> 1200L
            popupDue(cfg.popup) -> (cfg.popup.delaySec.coerceIn(0, 120) * 1000L).coerceAtLeast(700L)
            else -> return
        }
        pendingPopup = true
        handler.postDelayed(popupRunnable, delay)
    }

    private fun showPopupNow() {
        if (!resumedNow || isFinishing || dialog?.isShowing == true) return
        val c = cfg
        if (needsUpdate(c)) {
            dialog = NeoDialog.show(
                this, neo, "Actualizare", "Versiune nouă disponibilă",
                c.updateMsg.ifBlank { "Descarcă ultima versiune NEO Launcher." }, null, "Descarcă",
                cancelable = !c.updateForce,
                onCta = { openUrl(c.updateUrl) },
                onDismiss = { if (!c.updateForce) prefs.updateDismissed = c.updateCode }
            )
            return
        }
        val p = c.popup
        if (!popupDue(p)) return
        val now = System.currentTimeMillis()
        prefs.setLastPopup(p.id, now)
        prefs.lastPopupAny = now
        prefs.incPopupCount(p.id)
        ConfigRepo.track(this, p.id, "views")
        dialog = NeoDialog.show(
            this, neo, p.tag.ifBlank { "Ofertă" }, p.title, p.message, p.imageUrl,
            p.buttonText.ifBlank { "Deschide" },
            cancelable = true,
            onCta = {
                ConfigRepo.track(this, p.id, "clicks")
                if (p.url.isNotBlank()) openUrl(p.url)
            },
            onDismiss = {}
        )
    }

    // ───────────────────────── utilitare ─────────────────────────

    private fun openSettings() = safeStart(Intent(this, SettingsActivity::class.java))

    private fun openUrl(url: String) =
        safeStart(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    private fun safeStart(i: Intent) {
        try {
            startActivity(i)
        } catch (e: Exception) {
            toast("Acțiune indisponibilă pe acest telefon")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    inner class AppAdapter : BaseAdapter() {
        var items: List<AppItem> = emptyList()
            private set

        fun filter(q: String) {
            val query = q.trim()
            items = if (query.isEmpty()) AppRepo.apps else AppRepo.apps.filter { it.label.contains(query, ignoreCase = true) }
            notifyDataSetChanged()
        }

        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val cell = (convertView as? IconCell)?.takeIf { it.tag == neo.id }
                ?: IconCell(this@MainActivity, neo, true, false).also { it.tag = neo.id }
            cell.bind(items[position])
            return cell
        }
    }
}
