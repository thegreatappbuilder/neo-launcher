package com.neo.launcher.fx

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.neo.launcher.NeoTheme
import com.neo.launcher.UiKit
import java.util.Random
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Constelație: particule conectate prin linii. Atingerea le atrage spre deget, ca un magnet. */
class PlexusFx(d: Float, theme: NeoTheme) : FxRenderer(d, theme) {
    private val rnd = Random()
    private val n = 75
    private val x = FloatArray(n)
    private val y = FloatArray(n)
    private val vx = FloatArray(n)
    private val vy = FloatArray(n)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bg = Paint()
    private val ripples = Ripples(theme.secondary, d)
    private var link = 1f
    private var ax = 0f
    private var ay = 0f
    private var attract = 0f

    override fun onSize() {
        link = min(w, h) * 0.2f
        bg.shader = RadialGradient(
            w * 0.5f, h * 0.4f, max(w, h) * 0.8f,
            intArrayOf(Color.parseColor("#120E33"), Color.parseColor("#04030E")), null, Shader.TileMode.CLAMP
        )
        for (i in 0 until n) {
            x[i] = rnd.nextFloat() * w
            y[i] = rnd.nextFloat() * h
            vx[i] = (rnd.nextFloat() - 0.5f) * 40f * d
            vy[i] = (rnd.nextFloat() - 0.5f) * 40f * d
        }
        line.strokeWidth = 1f * d
    }

    override fun draw(c: Canvas, t: Float, dt: Float) {
        c.drawRect(0f, 0f, w, h, bg)
        attract = max(0f, attract - dt * 0.6f)
        val s = dt * speed
        for (i in 0 until n) {
            if (attract > 0f) {
                val dx = ax - x[i]
                val dy = ay - y[i]
                val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(20f * d)
                vx[i] += dx / dist * 260f * d * attract * dt
                vy[i] += dy / dist * 260f * d * attract * dt
            }
            val sp = sqrt(vx[i] * vx[i] + vy[i] * vy[i])
            val maxSp = 120f * d
            if (sp > maxSp) {
                vx[i] *= 0.97f
                vy[i] *= 0.97f
            }
            x[i] += vx[i] * s
            y[i] += vy[i] * s
            if (x[i] < 0f) { x[i] = 0f; vx[i] = -vx[i] }
            if (x[i] > w) { x[i] = w; vx[i] = -vx[i] }
            if (y[i] < 0f) { y[i] = 0f; vy[i] = -vy[i] }
            if (y[i] > h) { y[i] = h; vy[i] = -vy[i] }
        }
        val l2 = link * link
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val dx = x[i] - x[j]
                val dy = y[i] - y[j]
                val dd = dx * dx + dy * dy
                if (dd < l2) {
                    val k = 1f - sqrt(dd) / link
                    line.color = if ((i + j) % 3 == 0) theme.secondary else theme.primary
                    line.alpha = (k * 170).toInt()
                    c.drawLine(x[i], y[i], x[j], y[j], line)
                }
            }
        }
        for (i in 0 until n) {
            dot.color = if (i % 3 == 0) theme.secondary else theme.primary
            dot.alpha = 70
            c.drawCircle(x[i], y[i], 6f * d, dot)
            dot.color = Color.WHITE
            dot.alpha = 230
            c.drawCircle(x[i], y[i], 1.8f * d, dot)
        }
        if (attract > 0f) {
            dot.color = theme.secondary
            dot.alpha = (attract * 120).toInt().coerceIn(0, 255)
            c.drawCircle(ax, ay, 10f * d + 4f * d * kotlin.math.sin(t * 8f), dot)
        }
        ripples.draw(c, dt)
    }

    override fun touch(tx: Float, ty: Float) {
        ripples.add(tx, ty)
        ax = tx
        ay = ty
        attract = 1.5f
    }
}
