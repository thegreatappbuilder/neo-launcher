package com.neo.launcher

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var neo: NeoTheme
    private val previews = ArrayList<FxView>()
    private var scroll: ScrollView? = null
    private var savedScroll = 0

    private fun dp(v: Float) = UiKit.dp(this, v)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        UiKit.edgeToEdge(window)
        build()
    }

    override fun onResume() {
        super.onResume()
        previews.forEach { it.running = true }
    }

    override fun onPause() {
        previews.forEach { it.running = false }
        super.onPause()
    }

    private fun rebuild() {
        savedScroll = scroll?.scrollY ?: 0
        build()
    }

    private fun build() {
        neo = NeoTheme.byId(prefs.themeId)
        previews.clear()
        window.decorView.setBackgroundColor(neo.bg)

        val sv = ScrollView(this).apply {
            setBackgroundColor(neo.bg)
            isFillViewport = true
            isVerticalScrollBarEnabled = false
        }
        scroll = sv
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        sv.addView(col, FrameLayout.LayoutParams(-1, -2))

        // antet
        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        head.addView(UiKit.chip(this, neo, "◀").apply { setOnClickListener { finish() } })
        head.addView(UiKit.label(this, neo, "SETĂRI SISTEM", 20f, neo.text).apply {
            letterSpacing = 0.2f
            setPadding(dp(16f), 0, 0, 0)
        })
        col.addView(head, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8f) })

        // teme
        section(col, "MOD VIZUAL")
        if (prefs.themeLocked) note(col, "🔒  Tema este setată de administrator.")
        note(col, "${NeoTheme.ALL.size} teme. Atinge una ca s-o activezi.")
        // grilă pe două coloane
        NeoTheme.ALL.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEachIndexed { i, t ->
                row.addView(themeCard(t), LinearLayout.LayoutParams(0, -2, 1f).apply {
                    if (i == 1) leftMargin = dp(10f)
                })
            }
            if (pair.size == 1) row.addView(View(this), LinearLayout.LayoutParams(0, 0, 1f).apply { leftMargin = dp(10f) })
            col.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10f) })
        }

        // efecte
        section(col, "EFECTE SPECIALE")
        col.addView(choiceRow(listOf("Oprit", "Eco", "Maxim"), prefs.fxLevel) {
            prefs.fxLevel = it
            rebuild()
        })
        note(col, "Eco = 30 cadre/s (economisește bateria). Maxim = 60 cadre/s.")

        // fundal
        section(col, "FUNDAL ECRAN PRINCIPAL")
        col.addView(choiceRow(listOf("Efecte NEO", "Wallpaper-ul meu"), prefs.bgMode) {
            prefs.bgMode = it
            rebuild()
        })
        col.addView(button("✦  ACTIVEAZĂ WALLPAPER LIVE NEO") { setLiveWallpaper() })
        note(col, "Wallpaper-ul live pune efectele și pe ecranul de blocare. Când e activ, launcher-ul îl folosește automat.")

        // launcher
        section(col, "LAUNCHER")
        col.addView(button("⌂  SETEAZĂ NEO CA ECRAN PRINCIPAL") {
            val i = Intent(Settings.ACTION_HOME_SETTINGS)
            try {
                startActivity(i)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        })

        // server
        section(col, "SERVER ADMIN")
        val url = EditText(this).apply {
            setText(prefs.serverUrl)
            setHint("https://proiect-default-rtdb.firebaseio.com")
            setSingleLine()
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setTextColor(neo.text)
            setHintTextColor(neo.dim)
            background = UiKit.box(this@SettingsActivity, neo, 150, 200)
            setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
            imeOptions = EditorInfo.IME_ACTION_DONE
        }
        col.addView(url, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10f) })
        col.addView(button("⟳  SALVEAZĂ ȘI SINCRONIZEAZĂ") {
            prefs.serverUrl = url.text.toString()
            toast("Se sincronizează…")
            ConfigRepo.refresh(this) { c ->
                toast(if (c != null) "Sincronizat cu serverul ✓" else "Nu mă pot conecta. Verifică adresa.")
                if (c != null) rebuild()
            }
        })
        note(col, "Adresa e completată automat la build. O schimbi aici doar dacă muți serverul.")

        col.addView(UiKit.label(this, neo, "NEO Launcher v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", 11f, neo.dim, glow = false).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(28f), 0, dp(12f))
        })

        sv.setOnApplyWindowInsetsListener { _, ins ->
            padForInsets(col, ins)
            ins
        }
        setContentView(sv)
        sv.requestApplyInsets()
        val y = savedScroll
        sv.post { sv.scrollTo(0, y) }
    }

    @Suppress("DEPRECATION")
    private fun padForInsets(col: LinearLayout, ins: WindowInsets) {
        col.setPadding(dp(18f), ins.systemWindowInsetTop + dp(14f), dp(18f), ins.systemWindowInsetBottom + dp(20f))
    }

    private fun section(col: LinearLayout, title: String) {
        col.addView(UiKit.label(this, neo, "//  $title", 12f, neo.secondary).apply {
            letterSpacing = 0.25f
            setPadding(0, dp(22f), 0, dp(10f))
        })
    }

    private fun note(col: LinearLayout, text: String) {
        col.addView(UiKit.label(this, neo, text, 12f, neo.dim, glow = false).apply {
            setPadding(dp(2f), dp(6f), dp(2f), dp(4f))
        })
    }

    private fun button(text: String, onClick: () -> Unit): TextView =
        UiKit.label(this, neo, text, 13f, neo.text).apply {
            background = UiKit.box(this@SettingsActivity, neo, 160, 230, 1.4f)
            gravity = Gravity.CENTER
            letterSpacing = 0.12f
            setPadding(dp(14f), dp(14f), dp(14f), dp(14f))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8f) }
            setOnClickListener { onClick() }
            UiKit.pressFx(this)
        }

    private fun choiceRow(options: List<String>, selected: Int, onPick: (Int) -> Unit): LinearLayout {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        options.forEachIndexed { i, label ->
            val on = i == selected
            val chip = UiKit.label(this, neo, label, 13f, if (on) neo.bg else neo.text, glow = !on).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(12f), 0, dp(12f))
                background = if (on) {
                    GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(neo.primary, neo.secondary)).apply {
                        cornerRadius = dp(UiKit.radiusDp(neo) + 2f).toFloat()
                    }
                } else {
                    UiKit.box(this@SettingsActivity, neo, 140, 150)
                }
                setOnClickListener { onPick(i) }
                UiKit.pressFx(this)
            }
            row.addView(chip, LinearLayout.LayoutParams(0, -2, 1f).apply {
                if (i > 0) leftMargin = dp(8f)
            })
        }
        return row
    }

    private fun themeCard(t: NeoTheme): View {
        val selected = t.id == neo.id
        val locked = prefs.themeLocked
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.box(this@SettingsActivity, t, 200, if (selected) 255 else 80, if (selected) 2.2f else 1f)
            setPadding(dp(7f), dp(7f), dp(7f), dp(10f))
            if (locked && !selected) alpha = 0.45f
        }
        val preview = FxView(this).apply {
            setNeoTheme(t)
            level = 1
            background = GradientDrawable().apply { cornerRadius = dp(UiKit.radiusDp(t) + 2f).toFloat() }
            clipToOutline = true
        }
        previews.add(preview)
        card.addView(preview, LinearLayout.LayoutParams(-1, dp(118f)))
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(3f), dp(8f), dp(3f), 0)
        }
        texts.addView(UiKit.label(this, t, (if (selected) "● " else "") + t.title, 14f, t.primary).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        })
        texts.addView(UiKit.label(this, t, t.subtitle, 11f, t.dim, glow = false).apply {
            maxLines = 2
            minLines = 2
            ellipsize = TextUtils.TruncateAt.END
        })
        card.addView(texts)
        card.setOnClickListener {
            when {
                selected -> Unit
                locked -> toast("Tema este blocată de administrator")
                else -> {
                    prefs.themeId = t.id
                    rebuild()
                }
            }
        }
        return card
    }

    private fun setLiveWallpaper() {
        val i = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, NeoWallpaperService::class.java))
        try {
            startActivity(i)
        } catch (e: Exception) {
            try {
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            } catch (e2: Exception) {
                toast("Telefonul nu permite wallpaper live")
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
