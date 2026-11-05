package com.neo.launcher.fx

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import com.neo.launcher.NeoTheme
import com.neo.launcher.UiKit
import java.util.Locale
import java.util.Random
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Interfață de navă: salt prin stele, radar cu baleiaj, ținte și colțuri HUD. */
class HudFx(d: Float, theme: NeoTheme) : FxRenderer(d, theme) {
    private val n = 220
    private val sx = FloatArray(n)
    private val sy = FloatArray(n)
    private val sz = FloatArray(n)
    private val rnd = Random()
    private val bg = Paint()
    private val star = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val sweep = Paint(Paint.ANTI_ALIAS_FLAG)
    private val blip = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.MONOSPACE }
    private val ripples = Ripples(theme.secondary, d)
    private val blipA = FloatArray(6)
    private val blipD = FloatArray(6)
    private var boost = 1f
    private var rcx = 0f
    private var rcy = 0f
    private var rr = 0f

    init {
        for (i in 0 until n) resetStar(i, true)
        for (i in 0 until 6) {
            blipA[i] = rnd.nextFloat() * 360f
            blipD[i] = 0.25f + rnd.nextFloat() * 0.7f
        }
    }

    private fun resetStar(i: Int, anyZ: Boolean) {
        sx[i] = rnd.nextFloat() * 2f - 1f
        sy[i] = rnd.nextFloat() * 2f - 1f
        sz[i] = if (anyZ) 0.05f + rnd.nextFloat() * 0.95f else 1f
    }

    override fun onSize() {
        bg.shader = RadialGradient(
            w / 2f, h * 0.45f, max(w, h) * 0.8f,
            intArrayOf(Color.parseColor("#06142B"), Color.parseColor("#000814"), Color.BLACK),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP
        )
        rcx = w / 2f
        rcy = h * 0.56f
        rr = min(w, h) * 0.4f
        sweep.shader = SweepGradient(
            rcx, rcy,
            intArrayOf(UiKit.alpha(theme.primary, 0), UiKit.alpha(theme.primary, 0), UiKit.alpha(theme.primary, 140)),
            floatArrayOf(0f, 0.75f, 1f)
        )
        text.textSize = 10f * d
    }

    override fun draw(c: Canvas, t: Float, dt: Float) {
        c.drawRect(0f, 0f, w, h, bg)
        boost += (1f - boost) * min(1f, dt * 1.5f)

        // stele
        val cx = w / 2f
        val cy = h * 0.45f
        val f = min(w, h) * 0.3f
        val v = 0.12f * speed * boost
        for (i in 0 until n) {
            val z = sz[i] - dt * v
            if (z <= 0.03f) {
                resetStar(i, false)
                continue
            }
            sz[i] = z
            val x = cx + sx[i] / z * f
            val y = cy + sy[i] / z * f
            if (x < 0f || x > w || y < 0f || y > h) {
                resetStar(i, false)
                continue
            }
            val pz = z + 0.015f * boost
            val px = cx + sx[i] / pz * f
            val py = cy + sy[i] / pz * f
            val k = 1f - z
            star.color = if (i % 5 == 0) theme.secondary else Color.WHITE
            star.alpha = (k * 255).toInt().coerceIn(30, 255)
            star.strokeWidth = (0.6f + k * 2.4f) * d
            c.drawLine(px, py, x, y, star)
        }

        // radar
        ring.color = theme.secondary
        ring.strokeWidth = d
        for (k in 1..4) {
            ring.alpha = 45 + k * 15
            c.drawCircle(rcx, rcy, rr * k / 4f, ring)
        }
        ring.alpha = 60
        c.drawLine(rcx - rr, rcy, rcx + rr, rcy, ring)
        c.drawLine(rcx, rcy - rr, rcx, rcy + rr, ring)

        ring.color = theme.primary
        ring.alpha = 150
        for (k in 0 until 72) {
            val a = Math.toRadians(k * 5.0)
            val ca = cos(a).toFloat()
            val sa = sin(a).toFloat()
            val r1 = rr * 1.04f
            val r2 = rr * (if (k % 6 == 0) 1.11f else 1.06f)
            c.drawLine(rcx + ca * r1, rcy + sa * r1, rcx + ca * r2, rcy + sa * r2, ring)
        }

        val ang = (t * 70f * speed) % 360f
        c.save()
        c.rotate(ang, rcx, rcy)
        c.drawCircle(rcx, rcy, rr, sweep)
        ring.alpha = 230
        ring.strokeWidth = 1.5f * d
        c.drawLine(rcx, rcy, rcx + rr, rcy, ring)
        c.restore()

        // ținte care se aprind când trece baleiajul
        blip.color = theme.primary
        for (i in 0 until 6) {
            val diff = (((ang - blipA[i]) % 360f) + 360f) % 360f
            val a = (1f - diff / 300f).coerceIn(0f, 1f)
            if (a > 0f) {
                val rad = Math.toRadians(blipA[i].toDouble())
                val bx = rcx + cos(rad).toFloat() * rr * blipD[i]
                val by = rcy + sin(rad).toFloat() * rr * blipD[i]
                blip.alpha = (a * 255).toInt()
                c.drawCircle(bx, by, (3f + 3f * a) * d, blip)
            }
        }

        // colțuri HUD
        ring.strokeWidth = 2f * d
        ring.alpha = 200
        val m = 14f * d
        val l = 26f * d
        c.drawLine(m, m + l, m, m, ring); c.drawLine(m, m, m + l, m, ring)
        c.drawLine(w - m - l, m, w - m, m, ring); c.drawLine(w - m, m, w - m, m + l, ring)
        c.drawLine(m, h - m - l, m, h - m, ring); c.drawLine(m, h - m, m + l, h - m, ring)
        c.drawLine(w - m - l, h - m, w - m, h - m, ring); c.drawLine(w - m, h - m - l, w - m, h - m, ring)

        text.color = theme.primary
        text.alpha = 170
        val sync = String.format(Locale.US, "%.1f", 97f + sin(t) * 2f)
        c.drawText("SECTOR ${(t * 3f).toInt() % 99}  ·  SYNC $sync%  ·  WARP ${"%.1f".format(Locale.US, boost)}", rcx - rr, rcy + rr * 1.25f, text)

        ripples.draw(c, dt)
    }

    override fun touch(x: Float, y: Float) {
        ripples.add(x, y)
        boost = 5f
    }
}
