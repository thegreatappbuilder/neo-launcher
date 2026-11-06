package com.neo.launcher.fx

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.neo.launcher.NeoTheme
import com.neo.launcher.UiKit
import java.util.Random
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Galaxie spirală: mii de stele care se rotesc cu viteze diferite. Atingerea accelerează rotația. */
class GalaxyFx(d: Float, theme: NeoTheme) : FxRenderer(d, theme) {
    private val rnd = Random(3)
    private val n = 900
    private val rad = FloatArray(n)
    private val ang = FloatArray(n)
    private val bucket = IntArray(n)
    private val buckets = 4
    private val pts = Array(buckets) { FloatArray(n * 2) }
    private val counts = IntArray(buckets)
    private val paints = Array(buckets) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    }
    private val core = Paint()
    private val haze = Paint()
    private val bgStar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val nb = 120
    private val bgx = FloatArray(nb)
    private val bgy = FloatArray(nb)
    private val ripples = Ripples(theme.secondary, d)
    private var cx = 0f
    private var cy = 0f
    private var maxR = 1f
    private var spin = 1f
    private val tilt = 0.55f

    override fun onSize() {
        cx = w / 2f
        cy = h * 0.45f
        maxR = min(w, h) * 0.62f
        val cols = intArrayOf(Color.parseColor("#FFF4D6"), theme.primary, Color.parseColor("#FF7BB0"), theme.secondary)
        for (b in 0 until buckets) {
            paints[b].color = cols[b]
            paints[b].strokeWidth = (2.2f - b * 0.35f) * d
        }
        for (i in 0 until n) {
            val r = (rnd.nextFloat().let { it * it }) * maxR + 4f * d
            rad[i] = r
            val arm = rnd.nextInt(2) * Math.PI.toFloat()
            ang[i] = arm + r / maxR * 5.2f + (rnd.nextFloat() - 0.5f) * (0.9f - 0.5f * r / maxR)
            bucket[i] = min(buckets - 1, (r / maxR * buckets + rnd.nextFloat() * 0.8f).toInt())
        }
        core.shader = RadialGradient(
            cx, cy, maxR * 0.35f,
            intArrayOf(Color.argb(255, 255, 240, 210), Color.argb(120, 255, 180, 100), Color.TRANSPARENT),
            floatArrayOf(0f, 0.3f, 1f), Shader.TileMode.CLAMP
        )
        haze.shader = RadialGradient(
            cx, cy, maxR * 1.2f,
            intArrayOf(UiKit.alpha(theme.secondary, 70), UiKit.alpha(theme.secondary, 20), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        for (i in 0 until nb) {
            bgx[i] = rnd.nextFloat() * w
            bgy[i] = rnd.nextFloat() * h
        }
    }

    override fun draw(c: Canvas, t: Float, dt: Float) {
        c.drawColor(Color.parseColor("#03010A"))
        for (i in 0 until nb) {
            bgStar.alpha = (90 + 80 * sin(t + i)).toInt().coerceIn(0, 255)
            c.drawCircle(bgx[i], bgy[i], 0.9f * d, bgStar)
        }
        c.drawCircle(cx, cy, maxR * 1.2f, haze)
        spin += (1f - spin) * min(1f, dt * 0.7f)
        counts.fill(0)
        val base = dt * 0.25f * speed * spin
        for (i in 0 until n) {
            val r = rad[i]
            ang[i] += base * sqrt(maxR / max(r, 8f * d))
            val x = cx + cos(ang[i]) * r
            val y = cy + sin(ang[i]) * r * tilt
            val b = bucket[i]
            val k = counts[b]
            pts[b][k] = x
            pts[b][k + 1] = y
            counts[b] = k + 2
        }
        for (b in 0 until buckets) c.drawPoints(pts[b], 0, counts[b], paints[b])
        c.drawCircle(cx, cy, maxR * 0.35f, core)
        ripples.draw(c, dt)
    }

    override fun touch(x: Float, y: Float) {
        ripples.add(x, y)
        spin = 6f
    }
}
