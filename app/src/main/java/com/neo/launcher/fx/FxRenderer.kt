package com.neo.launcher.fx

import android.graphics.Canvas
import android.graphics.Paint
import com.neo.launcher.NeoTheme

/** Motorul comun pentru efecte: același cod desenează în launcher și în wallpaper-ul live. */
abstract class FxRenderer(protected val d: Float, protected val theme: NeoTheme) {
    protected var w = 1f
    protected var h = 1f
    var speed = 1f

    fun setSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        if (width.toFloat() == w && height.toFloat() == h) return
        w = width.toFloat()
        h = height.toFloat()
        onSize()
    }

    protected open fun onSize() {}

    /** t = secunde de la pornire, dt = secunde de la cadrul anterior. */
    abstract fun draw(c: Canvas, t: Float, dt: Float)

    open fun touch(x: Float, y: Float) {}

    companion object {
        fun create(theme: NeoTheme, density: Float): FxRenderer = when (theme.id) {
            "hologram" -> HologramFx(density, theme)
            "hud" -> HudFx(density, theme)
            "matrix" -> MatrixFx(density, theme)
            else -> CyberFx(density, theme)
        }
    }
}

/** Unde de lumină la atingere. */
class Ripples(private val color: Int, private val d: Float) {
    private class R(val x: Float, val y: Float) {
        var age = 0f
    }

    private val list = ArrayList<R>()
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    fun add(x: Float, y: Float) {
        if (list.size < 8) list.add(R(x, y))
    }

    fun draw(c: Canvas, dt: Float) {
        val it = list.iterator()
        while (it.hasNext()) {
            val r = it.next()
            r.age += dt
            if (r.age > 1.1f) {
                it.remove()
                continue
            }
            val k = r.age / 1.1f
            p.color = color
            p.strokeWidth = 2.5f * d * (1f - k) + 0.5f * d
            p.alpha = ((1f - k) * 255).toInt()
            c.drawCircle(r.x, r.y, (16f + k * 170f) * d, p)
            p.alpha = ((1f - k) * 130).toInt()
            c.drawCircle(r.x, r.y, (8f + k * 100f) * d, p)
        }
    }
}
