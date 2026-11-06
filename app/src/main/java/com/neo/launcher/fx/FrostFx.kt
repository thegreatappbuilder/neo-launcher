package com.neo.launcher.fx

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.neo.launcher.NeoTheme
import com.neo.launcher.UiKit
import java.util.Random
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/** Cristale de gheață: ninsoare cu fulgi de cristal care se rotesc. Atingerea împinge fulgii ca o rafală. */
class FrostFx(d: Float, theme: NeoTheme) : FxRenderer(d, theme) {
    private val rnd = Random()
    private val bg = Paint()
    private val glowTop = Paint()
    private val flake = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val n = 130
    private val x = FloatArray(n)
    private val y = FloatArray(n)
    private val vx = FloatArray(n)
    private val vy = FloatArray(n)
    private val sz = FloatArray(n)
    private val rot = FloatArray(n)
    private val rv = FloatArray(n)
    private val ripples = Ripples(theme.primary, d)

    override fun onSize() {
        bg.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(Color.parseColor("#04101F"), Color.parseColor("#0D2A4A"), Color.parseColor("#3A6E96")),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP
        )
        glowTop.shader = RadialGradient(
            w * 0.5f, 0f, max(w, h) * 0.7f,
            intArrayOf(UiKit.alpha(theme.primary, 80), Color.TRANSPARENT), null, Shader.TileMode.CLAMP
        )
        for (i in 0 until n) reset(i, rnd.nextFloat() * h)
    }

    private fun reset(i: Int, yy: Float) {
        x[i] = rnd.nextFloat() * w
        y[i] = yy
        val big = rnd.nextFloat() < 0.18f
        sz[i] = if (big) (7f + rnd.nextFloat() * 9f) * d else (1f + rnd.nextFloat() * 2.4f) * d
        vy[i] = (20f + sz[i] / d * 6f + rnd.nextFloat() * 20f) * d
        vx[i] = 0f
        rot[i] = rnd.nextFloat() * 60f
        rv[i] = (rnd.nextFloat() - 0.5f) * 60f
    }

    private fun crystal(c: Canvas, cx: Float, cy: Float, r: Float, a: Float) {
        c.save()
        c.translate(cx, cy)
        c.rotate(a)
        flake.strokeWidth = max(1f, r * 0.12f)
        for (k in 0 until 6) {
            c.drawLine(0f, 0f, 0f, -r, flake)
            c.drawLine(0f, -r * 0.55f, r * 0.28f, -r * 0.78f, flake)
            c.drawLine(0f, -r * 0.55f, -r * 0.28f, -r * 0.78f, flake)
            c.rotate(60f)
        }
        c.restore()
    }

    override fun draw(c: Canvas, t: Float, dt: Float) {
        c.drawRect(0f, 0f, w, h, bg)
        c.drawRect(0f, 0f, w, h, glowTop)
        val damp = (1f - 1.2f * dt).coerceIn(0f, 1f)
        for (i in 0 until n) {
            vx[i] *= damp
            x[i] += (vx[i] + sin(t * 0.8f + i) * 14f * d) * dt * speed
            y[i] += vy[i] * dt * speed
            rot[i] += rv[i] * dt
            if (y[i] > h + 20f * d) reset(i, -20f * d)
            if (x[i] < -30f * d) x[i] = w + 20f * d
            if (x[i] > w + 30f * d) x[i] = -20f * d
            if (sz[i] > 5f * d) {
                flake.alpha = 210
                crystal(c, x[i], y[i], sz[i], rot[i])
            } else {
                dot.alpha = (140 + 100 * sin(t * 3f + i)).toInt().coerceIn(0, 255)
                c.drawCircle(x[i], y[i], sz[i], dot)
            }
        }
        ripples.draw(c, dt)
    }

    override fun touch(tx: Float, ty: Float) {
        ripples.add(tx, ty)
        val range = 260f * d
        for (i in 0 until n) {
            val dx = x[i] - tx
            val dy = y[i] - ty
            val dist = hypot(dx, dy).coerceAtLeast(1f)
            if (dist < range) {
                val f = (1f - dist / range) * 700f * d
                vx[i] += dx / dist * f
                y[i] += dy / dist * f * 0.05f
            }
        }
    }
}
