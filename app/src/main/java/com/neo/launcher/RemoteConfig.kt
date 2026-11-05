package com.neo.launcher

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.util.concurrent.Executors

data class PopupCfg(
    val enabled: Boolean,
    val id: String,
    val tag: String,
    val title: String,
    val message: String,
    val imageUrl: String,
    val buttonText: String,
    val url: String,
    val everyHours: Double,
    val delaySec: Int,
    val maxShows: Int
)

/** Tot ce poate fi schimbat din panoul de admin (nodul /launcher din Firebase). */
data class RemoteConfig(
    val brand: String,
    val theme: String,
    val forceTheme: Boolean,
    val bannerOn: Boolean,
    val bannerText: String,
    val bannerUrl: String,
    val popup: PopupCfg,
    val updateCode: Int,
    val updateUrl: String,
    val updateMsg: String,
    val updateForce: Boolean
) {
    companion object {
        const val DEFAULT_BRAND = "NEO // LAUNCHER"

        fun parse(json: String?): RemoteConfig {
            val o = try {
                JSONObject(json ?: "{}")
            } catch (e: Exception) {
                JSONObject()
            }
            val b = o.optJSONObject("banner") ?: JSONObject()
            val p = o.optJSONObject("popup") ?: JSONObject()
            val u = o.optJSONObject("update") ?: JSONObject()
            return RemoteConfig(
                brand = str(o, "brand"),
                theme = str(o, "theme"),
                forceTheme = o.optBoolean("forceTheme", false),
                bannerOn = b.optBoolean("enabled", false),
                bannerText = str(b, "text"),
                bannerUrl = str(b, "url"),
                popup = PopupCfg(
                    enabled = p.optBoolean("enabled", false),
                    id = str(p, "id"),
                    tag = str(p, "tag"),
                    title = str(p, "title"),
                    message = str(p, "message"),
                    imageUrl = str(p, "imageUrl"),
                    buttonText = str(p, "buttonText"),
                    url = str(p, "url"),
                    everyHours = p.optDouble("everyHours", 6.0),
                    delaySec = p.optInt("delaySec", 2),
                    maxShows = p.optInt("maxShows", 0)
                ),
                updateCode = u.optInt("versionCode", 0),
                updateUrl = str(u, "url"),
                updateMsg = str(u, "message"),
                updateForce = u.optBoolean("force", false)
            )
        }

        private fun str(o: JSONObject, k: String): String = if (o.isNull(k)) "" else o.optString(k, "").trim()
    }
}

object ConfigRepo {
    private val exec = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun cached(ctx: Context): RemoteConfig = RemoteConfig.parse(Prefs(ctx).configJson)

    private fun base(p: Prefs): String? = p.serverUrl.trim().trimEnd('/').takeIf { it.startsWith("https://") }

    /** Citește /launcher.json din Firebase Realtime Database (REST, fără SDK). */
    fun refresh(ctx: Context, cb: (RemoteConfig?) -> Unit) {
        val p = Prefs(ctx)
        val url = base(p)
        if (url == null) {
            cb(null)
            return
        }
        exec.execute {
            val txt: String? = try {
                val t = Http.get("$url/launcher.json").trim()
                if (t == "null" || t.isEmpty()) "{}" else {
                    JSONObject(t)
                    t
                }
            } catch (e: Exception) {
                null
            }
            main.post {
                if (txt == null) {
                    cb(null)
                } else {
                    p.configJson = txt
                    val cfg = RemoteConfig.parse(txt)
                    applyTheme(p, cfg)
                    cb(cfg)
                }
            }
        }
    }

    private fun applyTheme(p: Prefs, cfg: RemoteConfig) {
        if (NeoTheme.ALL.none { it.id == cfg.theme }) {
            p.themeLocked = false
            return
        }
        if (cfg.forceTheme) {
            p.themeLocked = true
            if (p.themeId != cfg.theme) p.themeId = cfg.theme
        } else {
            p.themeLocked = false
            // tema implicită din admin se aplică o singură dată, când o schimbi
            if (p.adminTheme != cfg.theme) {
                p.adminTheme = cfg.theme
                p.themeId = cfg.theme
            }
        }
    }

    /** Numără afișările / click-urile popup-ului (vizibile în admin). */
    fun track(ctx: Context, popupId: String, field: String) {
        val url = base(Prefs(ctx)) ?: return
        val id = popupId.replace(Regex("[.#\$\\[\\]/]"), "_")
        exec.execute {
            try {
                Http.put("$url/stats/$id/$field.json", "{\".sv\":{\"increment\":1}}")
            } catch (_: Exception) {
            }
        }
    }
}
