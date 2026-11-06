package com.neo.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import com.neo.launcher.fx.FxRenderer

/** View care rulează efectele în interiorul aplicației. Se oprește singur când nu e vizibil. */
class FxView(ctx: Context) : View(ctx) {
    private var renderer: FxRenderer? = null
    private var themeId = ""
    private var last = 0L
    private val start = System.nanoTime()
    private val visRect = Rect()
    private var looping = false
    private val scrollListener = ViewTreeObserver.OnScrollChangedListener {
        if (!looping && running) invalidate()
    }

    var level = 2
    var running = true
        set(v) {
            field = v
            if (v) {
                last = 0L
                invalidate()
            }
        }

    fun setNeoTheme(theme: NeoTheme) {
        if (theme.id == themeId && renderer != null) return
        themeId = theme.id
        renderer = FxRenderer.create(theme, resources.displayMetrics.density).also {
            if (width > 0) it.setSize(width, height)
        }
        invalidate()
    }

    fun touchFx(x: Float, y: Float) {
        renderer?.touch(x, y)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnScrollChangedListener(scrollListener)
    }

    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnScrollChangedListener(scrollListener)
        looping = false
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        renderer?.setSize(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val r = renderer ?: return
        val now = System.nanoTime()
        val dt = if (last == 0L || level == 0 || !looping) 0f else ((now - last) / 1e9f).coerceAtMost(0.1f)
        last = now
        r.draw(canvas, (now - start) / 1e9f, dt)
        val visible = isShown && getGlobalVisibleRect(visRect)
        looping = running && level > 0 && visible
        if (looping) {
            if (level == 1) postInvalidateDelayed(33L) else postInvalidateOnAnimation()
        }
    }
}
