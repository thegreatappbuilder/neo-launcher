package com.neo.launcher

import android.app.WallpaperManager
import android.content.SharedPreferences
import android.graphics.Canvas
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.neo.launcher.fx.FxRenderer

/** Wallpaper live: aceleași efecte, pe tot sistemul (inclusiv ecranul de blocare). */
class NeoWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = NeoEngine()

    inner class NeoEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val handler = Handler(Looper.getMainLooper())
        private val prefs = Prefs(this@NeoWallpaperService)
        private var renderer: FxRenderer? = null
        private var visible = false
        private var sw = 0
        private var sh = 0
        private var last = 0L
        private val start = System.nanoTime()
        private val frame = Runnable { drawFrame() }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            prefs.sp.registerOnSharedPreferenceChangeListener(this)
            build()
        }

        private fun build() {
            renderer = FxRenderer.create(NeoTheme.byId(prefs.themeId), resources.displayMetrics.density).also {
                if (sw > 0) it.setSize(sw, sh)
            }
        }

        override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
            if (key == "theme") {
                build()
                if (visible) drawFrame()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            sw = width
            sh = height
            renderer?.setSize(width, height)
            drawFrame()
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            handler.removeCallbacks(frame)
            if (v) {
                last = 0L
                drawFrame()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(frame)
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            handler.removeCallbacks(frame)
            prefs.sp.unregisterOnSharedPreferenceChangeListener(this)
            super.onDestroy()
        }

        override fun onTouchEvent(event: MotionEvent) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) renderer?.touch(event.x, event.y)
            super.onTouchEvent(event)
        }

        override fun onCommand(
            action: String?, x: Int, y: Int, z: Int, extras: Bundle?, resultRequested: Boolean
        ): Bundle? {
            if (action == WallpaperManager.COMMAND_TAP) renderer?.touch(x.toFloat(), y.toFloat())
            return null
        }

        private fun drawFrame() {
            handler.removeCallbacks(frame)
            val r = renderer ?: return
            if (sw == 0) return
            val holder = surfaceHolder
            val level = prefs.fxLevel
            val now = System.nanoTime()
            val dt = if (last == 0L || level == 0) 0f else ((now - last) / 1e9f).coerceAtMost(0.1f)
            last = now
            var c: Canvas? = null
            try {
                c = try {
                    holder.lockHardwareCanvas()
                } catch (e: Exception) {
                    holder.lockCanvas()
                }
                if (c != null) r.draw(c, (now - start) / 1e9f, dt)
            } catch (_: Exception) {
            } finally {
                if (c != null) try {
                    holder.unlockCanvasAndPost(c)
                } catch (_: Exception) {
                }
            }
            if (visible && level > 0) handler.postDelayed(frame, if (level >= 2) 16L else 40L)
        }
    }
}
