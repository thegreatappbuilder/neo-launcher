package com.neo.launcher.fx

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.neo.launcher.NeoTheme
import com.neo.launcher.UiKit
import java.util.Random
import kotlin.math.sin

/** Adâncul oceanului: raze de lumină, bule și meduze bioluminescente. Atingerea eliberează bule. */
class OceanFx(d: Float, theme: NeoTheme) : FxRenderer(d, theme) {
    private class Jelly(var x: Float, var y: Float, val r: Float, val ph: Float, val col: Int, val paint: Paint)

    private val rnd = Random()
    private val bg = Paint()
    private val ray = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bubble = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val tent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val path = Path()
    private val nb = 70
    private val bx = FloatArray(nb)
    private val by = FloatArray(nb)
    private val br = FloatArray(nb)
    private val bv = FloatArray(nb)
    private val jellies = ArrayList<Jelly>()
    private val ripples = Ripples(theme.primary, d)

    override fun onSize() {
        bg.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(Color.parseColor("#0B4A6B"), Color.parseColor("#022A44"), Color.parseColor("#000C18")),
            floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP
        )
        ray.shader = LinearGradient(
            0f, 0f, 0f, h * 0.85f,
            intArrayOf(Color.argb(70, 200, 245, 255), Color.TRANSPARENT), null, Shader.TileMode.CLAMP
        )
        bubble.strokeWidth = 1.2f * d
        tent.strokeWidth = 1.3f * d
        for (i in 0 until nb) resetBubble(i, rnd.nextFloat() * h)
        jellies.clear()
        val cols = intArrayOf(theme.primary, theme.secondary, Color.parseColor("#FF8BD8"))
        for (k in 0 until 4) {
            val r = (18f + rnd.nextFloat() * 22f) * d
            val col = cols[k % 3]
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    0f, 0f, r * 1.6f,
                    intArrayOf(UiKit.alpha(col, 200), UiKit.alpha(col, 70), UiKit.alpha(col, 0)),
                    floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
                )
            }
            jellies.add(Jelly(rnd.nextFloat() * w, h * (0.3f + rnd.nextFloat() * 0.7f), r, rnd.nextFloat() * 6f, col, p))
        }
    }

    private fun resetBubble(i: Int, y: Float) {
        bx[i] = rnd.nextFloat() * w
        by[i] = y
        br[i] = (1.5f + rnd.nextFloat() * 5f) * d
        bv[i] = (25f + rnd.nextFloat() * 50f) * d
    }

    override fun draw(c: Canvas, t: Float, dt: Float) {
        c.drawRect(0f, 0f, w, h, bg)

        // raze de lumină care se leagănă
        for (k in 0 until 6) {
            val cx = w * (0.1f + k * 0.17f) + sin(t * 0.3f + k) * 30f * d
            val top = 40f * d + k % 2 * 20f * d
            val spread = w * (0.12f + 0.05f * sin(t * 0.4f + k * 1.7f))
            path.reset()
            path.moveTo(cx - top * 0.3f, 0f)
            path.lineTo(cx + top * 0.3f, 0f)
            path.lineTo(cx + spread, h * 0.85f)
            path.lineTo(cx - spread * 0.6f, h * 0.85f)
            path.close()
            ray.alpha = (140 + 100 * sin(t * 0.7f + k * 2.1f)).toInt().coerceIn(0, 255)
            c.drawPath(path, ray)
        }

        // meduze
        for (j in jellies) {
            val pulse = 0.5f + 0.5f * sin(t * 2.2f * speed + j.ph)
            j.y -= (8f + 26f * pulse) * d * dt * speed
            j.x += sin(t * 0.4f + j.ph) * 10f * d * dt
            if (j.y < -j.r * 4f) {
                j.y = h + j.r * 2f
                j.x = rnd.nextFloat() * w
            }
            val rx = j.r * (1f + 0.12f * pulse)
            val ry = j.r * (0.85f - 0.12f * pulse)
            c.save()
            c.translate(j.x, j.y)
            c.drawArc(-rx, -ry, rx, ry, 180f, 180f, true, j.paint)
            c.restore()
            tent.color = j.col
            tent.alpha = 150
            for (m in 0 until 5) {
                path.reset()
                val sx = j.x - rx * 0.7f + m * rx * 0.35f
                path.moveTo(sx, j.y)
                var yy = j.y
                val len = j.r * (2.2f + m % 2 * 0.8f)
                while (yy < j.y + len) {
                    yy += 4f * d
                    path.lineTo(sx + sin((yy - j.y) * 0.08f / d - t * 3f + m) * 4f * d, yy)
                }
                c.drawPath(path, tent)
            }
        }

        // bule
        for (i in 0 until nb) {
            by[i] -= bv[i] * dt * speed
            if (by[i] < -10f * d) resetBubble(i, h + 10f * d)
            val x = bx[i] + sin(t * 2f + i) * 5f * d
            bubble.color = Color.WHITE
            bubble.alpha = 120
            c.drawCircle(x, by[i], br[i], bubble)
            bubble.alpha = 200
            c.drawPoint(x - br[i] * 0.35f, by[i] - br[i] * 0.35f, bubble)
        }
        ripples.draw(c, dt)
    }

    override fun touch(x: Float, y: Float) {
        ripples.add(x, y)
        for (k in 0 until 14) {
            val i = rnd.nextInt(nb)
            bx[i] = x + (rnd.nextFloat() - 0.5f) * 50f * d
            by[i] = y + (rnd.nextFloat() - 0.5f) * 30f * d
            bv[i] = (80f + rnd.nextFloat() * 120f) * d
        }
    }
}
