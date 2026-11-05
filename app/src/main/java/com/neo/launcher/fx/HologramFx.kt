package com.neo.launcher.fx

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.neo.launcher.NeoTheme
import com.neo.launcher.UiKit
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Sfere de sticlă care plutesc, valuri holografice, particule și o rază de lumină. */
class HologramFx(d: Float, theme: NeoTheme) : FxRenderer(d, theme) {
    private class Orb(val bx: Float, val by: Float, val r: Float, val ph: Float, val paint: Paint) {
        var ox = 0f
        var oy = 0f
        var vx = 0f
        var vy = 0f
        var lx = 0f
        var ly = 0f
    }

    private val rnd = Random()
    private val bg = Paint()
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        alpha = 45
    }
    private val hl = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(45, 255, 255, 255) }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wave = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val shimmer = Paint()
    private val shimmerM = Matrix()
    private val path = Path()
    private val orbs = ArrayList<Orb>()
    private val nd = 90
    private val dx = FloatArray(nd)
    private val dy = FloatArray(nd)
    private val dv = FloatArray(nd)
    private val dph = FloatArray(nd)
    private val ripples = Ripples(theme.secondary, d)

    override fun onSize() {
        bg.shader = LinearGradient(
            0f, 0f, w * 0.3f, h,
            intArrayOf(Color.parseColor("#050B18"), Color.parseColor("#0B1E3A"), Color.parseColor("#160A2E")),
            null, Shader.TileMode.CLAMP
        )
        edge.strokeWidth = d
        wave.strokeWidth = 1.4f * d
        orbs.clear()
        val colors = intArrayOf(theme.primary, theme.secondary, Color.parseColor("#5EF2D6"))
        for (i in 0 until 7) {
            val r = (50f + rnd.nextFloat() * 110f) * d
            val col = colors[i % 3]
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    0f, 0f, r,
                    intArrayOf(UiKit.alpha(col, 110), UiKit.alpha(col, 35), UiKit.alpha(col, 0)),
                    floatArrayOf(0f, 0.65f, 1f), Shader.TileMode.CLAMP
                )
            }
            orbs.add(Orb(rnd.nextFloat() * w, rnd.nextFloat() * h, r, rnd.nextFloat() * 6.28f, p))
        }
        for (i in 0 until nd) {
            dx[i] = rnd.nextFloat() * w
            dy[i] = rnd.nextFloat() * h
            dv[i] = (10f + rnd.nextFloat() * 40f) * d
            dph[i] = rnd.nextFloat() * 6.28f
        }
        val band = w * 0.45f
        shimmer.shader = LinearGradient(
            0f, 0f, band, band,
            intArrayOf(Color.TRANSPARENT, Color.argb(30, 255, 255, 255), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
    }

    override fun draw(c: Canvas, t: Float, dt: Float) {
        c.drawRect(0f, 0f, w, h, bg)

        // valuri holografice
        val freq = (2f * Math.PI.toFloat()) / (320f * d)
        for (k in 0 until 3) {
            path.reset()
            val base = h * (0.3f + k * 0.22f)
            val amp = (18f + k * 10f) * d
            val sp = (0.5f + k * 0.2f) * speed
            var x = 0f
            path.moveTo(0f, base + sin(t * sp + k) * amp)
            while (x <= w) {
                x += 10f * d
                path.lineTo(x, base + sin(x * freq + t * sp + k) * amp)
            }
            wave.color = if (k % 2 == 0) theme.primary else theme.secondary
            wave.alpha = 70
            c.drawPath(path, wave)
        }

        // sfere de sticlă
        val damp = (1f - 1.5f * dt).coerceIn(0f, 1f)
        for (o in orbs) {
            o.vx += -o.ox * 3f * dt
            o.vy += -o.oy * 3f * dt
            o.vx *= damp
            o.vy *= damp
            o.ox += o.vx * dt
            o.oy += o.vy * dt
            val x = o.bx + sin(t * 0.25f * speed + o.ph) * 40f * d + o.ox
            val y = o.by + cos(t * 0.18f * speed + o.ph * 1.3f) * 60f * d + o.oy
            o.lx = x
            o.ly = y
            c.save()
            c.translate(x, y)
            c.drawCircle(0f, 0f, o.r, o.paint)
            c.drawCircle(0f, 0f, o.r * 0.72f, edge)
            c.drawCircle(-o.r * 0.28f, -o.r * 0.3f, o.r * 0.12f, hl)
            c.restore()
        }

        // particule care urcă
        for (i in 0 until nd) {
            dy[i] -= dv[i] * dt * speed
            if (dy[i] < -10f) {
                dy[i] = h + 10f
                dx[i] = rnd.nextFloat() * w
            }
            val a = 0.5f + 0.5f * sin(t * 2f + dph[i])
            dot.color = if (i % 3 == 0) theme.secondary else Color.WHITE
            dot.alpha = (40 + 180 * a).toInt()
            c.drawCircle(dx[i] + sin(t + dph[i]) * 6f * d, dy[i], (0.8f + 1.4f * a) * d, dot)
        }

        // rază de lumină pe sticlă
        val period = 7f
        val pos = ((t % period) / period) * (w + h) * 1.4f - w
        shimmerM.setTranslate(pos, 0f)
        shimmer.shader?.setLocalMatrix(shimmerM)
        c.drawRect(0f, 0f, w, h, shimmer)

        ripples.draw(c, dt)
    }

    override fun touch(x: Float, y: Float) {
        ripples.add(x, y)
        val range = 300f * d
        for (o in orbs) {
            val ddx = o.lx - x
            val ddy = o.ly - y
            val dist = sqrt(ddx * ddx + ddy * ddy).coerceAtLeast(1f)
            if (dist < range) {
                val f = (1f - dist / range) * 900f * d
                o.vx += ddx / dist * f
                o.vy += ddy / dist * f
            }
        }
    }
}
