package com.neo.launcher.fx

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import com.neo.launcher.NeoTheme
import com.neo.launcher.UiKit
import java.util.Random
import kotlin.math.min
import kotlin.math.sin

/** Aurora boreală: perdele de lumină care unduiesc deasupra munților. Atingerea le intensifică. */
class AuroraFx(d: Float, theme: NeoTheme) : FxRenderer(d, theme) {
    private val rnd = Random(11)
    private val sky = Paint()
    private val star = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val mountain = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#01050B") }
    private val mountain2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#04111D") }
    private val bands = Array(3) { Paint(Paint.ANTI_ALIAS_FLAG) }
    private val path = Path()
    private val ridge = Path()
    private val ridge2 = Path()
    private val ripples = Ripples(theme.primary, d)
    private val ns = 90
    private val sx = FloatArray(ns)
    private val sy = FloatArray(ns)
    private val sp = FloatArray(ns)
    private var glow = 0f

    override fun onSize() {
        sky.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(Color.parseColor("#01030A"), Color.parseColor("#04162A"), Color.parseColor("#0A2A3A")),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP
        )
        val cols = intArrayOf(theme.primary, theme.secondary, Color.parseColor("#4FD8FF"))
        for (k in 0 until 3) {
            val top = h * (0.12f + k * 0.1f)
            bands[k].shader = LinearGradient(
                0f, top - 60f * d, 0f, top + h * 0.32f,
                intArrayOf(UiKit.alpha(cols[k], 0), UiKit.alpha(cols[k], 150), UiKit.alpha(cols[k], 40), UiKit.alpha(cols[k], 0)),
                floatArrayOf(0f, 0.25f, 0.6f, 1f), Shader.TileMode.CLAMP
            )
        }
        for (i in 0 until ns) {
            sx[i] = rnd.nextFloat() * w
            sy[i] = rnd.nextFloat() * h * 0.75f
            sp[i] = rnd.nextFloat() * 6.28f
        }
        buildRidge(ridge, h * 0.80f, 0.14f, 3)
        buildRidge(ridge2, h * 0.74f, 0.12f, 5)
    }

    private fun buildRidge(p: Path, base: Float, amp: Float, peaks: Int) {
        p.reset()
        p.moveTo(0f, h)
        p.lineTo(0f, base)
        val step = w / (peaks * 2)
        for (i in 1..peaks * 2) {
            val up = i % 2 == 1
            val y = if (up) base - h * amp * (0.5f + rnd.nextFloat() * 0.5f) else base + rnd.nextFloat() * 20f * d
            p.lineTo(step * i, y)
        }
        p.lineTo(w, h)
        p.close()
    }

    override fun draw(c: Canvas, t: Float, dt: Float) {
        c.drawRect(0f, 0f, w, h, sky)
        for (i in 0 until ns) {
            star.alpha = (120 + 120 * sin(t * 1.5f + sp[i])).toInt().coerceIn(0, 255)
            c.drawCircle(sx[i], sy[i], 1.1f * d, star)
        }
        glow += (0f - glow) * min(1f, dt * 0.8f)
        val boost = 1f + glow
        for (k in 0 until 3) {
            val top = h * (0.12f + k * 0.1f)
            val amp = (34f + k * 10f) * d * boost
            val len = h * (0.22f + 0.05f * k) * (0.85f + 0.3f * glow)
            val f1 = 0.006f / d
            val f2 = 0.017f / d
            val ts = t * speed * (0.35f + k * 0.12f)
            path.reset()
            var x = -10f * d
            path.moveTo(x, top + sin(x * f1 + ts + k) * amp)
            while (x <= w + 10f * d) {
                path.lineTo(x, top + sin(x * f1 + ts + k) * amp + sin(x * f2 - ts * 1.7f) * amp * 0.35f)
                x += 12f * d
            }
            while (x >= -10f * d) {
                x -= 12f * d
                path.lineTo(x, top + len + sin(x * f1 * 0.8f + ts * 0.8f + k) * amp * 1.2f)
            }
            path.close()
            bands[k].alpha = (200 * (0.7f + 0.3f * sin(t * 0.6f + k * 2f)) * min(1.6f, boost)).toInt().coerceIn(0, 255)
            c.drawPath(path, bands[k])
        }
        c.drawPath(ridge2, mountain2)
        c.drawPath(ridge, mountain)
        ripples.draw(c, dt)
    }

    override fun touch(x: Float, y: Float) {
        ripples.add(x, y)
        glow = 1.2f
    }
}
