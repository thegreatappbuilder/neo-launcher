package com.neo.launcher

import android.content.Context
import android.content.SharedPreferences

class Prefs(ctx: Context) {
    val sp: SharedPreferences = ctx.applicationContext.getSharedPreferences("neo", Context.MODE_PRIVATE)

    var themeId: String
        get() = sp.getString("theme", null) ?: NeoTheme.CYBER.id
        set(v) = sp.edit().putString("theme", v).apply()

    /** 0 = oprit, 1 = eco (30fps), 2 = maxim (60fps) */
    var fxLevel: Int
        get() = sp.getInt("fx", 2)
        set(v) = sp.edit().putInt("fx", v).apply()

    /** 0 = efecte NEO pe ecranul principal, 1 = wallpaper-ul utilizatorului */
    var bgMode: Int
        get() = sp.getInt("bg", 0)
        set(v) = sp.edit().putInt("bg", v).apply()

    var serverUrl: String
        get() = sp.getString("server", null)?.takeIf { it.isNotBlank() } ?: BuildConfig.FIREBASE_DB_URL
        set(v) = sp.edit().putString("server", v.trim()).apply()

    var themeLocked: Boolean
        get() = sp.getBoolean("locked", false)
        set(v) = sp.edit().putBoolean("locked", v).apply()

    var adminTheme: String?
        get() = sp.getString("adminTheme", null)
        set(v) = sp.edit().putString("adminTheme", v).apply()

    var dock: List<String>
        get() = (sp.getString("dock", "") ?: "").split("|").filter { it.isNotBlank() }
        set(v) = sp.edit().putString("dock", v.joinToString("|")).apply()

    var dockInitialized: Boolean
        get() = sp.getBoolean("dockInit", false)
        set(v) = sp.edit().putBoolean("dockInit", v).apply()

    var configJson: String?
        get() = sp.getString("config", null)
        set(v) = sp.edit().putString("config", v).apply()

    var lastPopupAny: Long
        get() = sp.getLong("popAny", 0L)
        set(v) = sp.edit().putLong("popAny", v).apply()

    var updateDismissed: Int
        get() = sp.getInt("updDismissed", 0)
        set(v) = sp.edit().putInt("updDismissed", v).apply()

    fun lastPopup(id: String): Long = sp.getLong("pop_t_$id", 0L)
    fun setLastPopup(id: String, t: Long) = sp.edit().putLong("pop_t_$id", t).apply()
    fun popupCount(id: String): Int = sp.getInt("pop_n_$id", 0)
    fun incPopupCount(id: String) = sp.edit().putInt("pop_n_$id", popupCount(id) + 1).apply()
}
