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
import kotlin.math.max
import kotlin.math.sin

/** Infern de lavă: scântei care urcă, magmă care pulsează și val de căldură. Atingerea aruncă scântei. */
class InfernoFx(d: Float, theme: NeoTheme) : FxRenderer(d, theme) {
    private val rnd = Random()
    private val bg = Paint()
    private val magma = Paint()
    private val wave = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val spark = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val n = 160
    private val px = FloatArray(n)
    private val py = FloatArray(n)
    private val vx = FloatArray(n)
    private val vy = FloatArray(n)
    private val life = FloatArray(n)
    private val maxLife = FloatArray(n)
    private val size = FloatArray(n)
    private val ripples = Ripples(theme.secondary, d)

    override fun onSize() {
        bg.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(Color.parseColor("#070000"), Color.parseColor("#1E0402"), Color.parseColor("#5A0E02")),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP
        )
        magma.shader = RadialGradient(
            w / 2f, h * 1.05f, max(w, h) * 0.7f,
            intArrayOf(Color.argb(230, 255, 120, 20), Color.argb(90, 255, 40, 0), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
        )
        for (i in 0 until n) spawn(i, rnd.nextFloat() * w, h + rnd.nextFloat() * h, false)
    }

    private fun spawn(i: Int, x: Float, y: Float, burst: Boolean) {
        px[i] = x
        py[i] = y
        if (burst) {
            val a = rnd.nextFloat() * 6.283f
            val s = (60f + rnd.nextFloat() * 260f) * d
            vx[i] = kotlin.math.cos(a) * s
            vy[i] = kotlin.math.sin(a) * s - 80f * d
        } else {
            vx[i] = (rnd.nextFloat() - 0.5f) * 30f * d
            vy[i] = -(40f + rnd.nextFloat() * 120f) * d
        }
        maxLife[i] = 2f + rnd.nextFloat() * 4f
        life[i] = if (burst) maxLife[i] * 0.5f else maxLife[i]
        size[i] = (0.8f + rnd.nextFloat() * 2.4f) * d
    }

    override fun draw(c: Canvas, t: Float, dt: Float) {
        c.drawRect(0f, 0f, w, h, bg)
        magma.alpha = (200 + 55 * sin(t * 1.3f)).toInt().coerceIn(0, 255)
        c.drawRect(0f, h * 0.3f, w, h, magma)

        // val de căldură
        for (k in 0 until 5) {
            path.reset()
            val base = h * (0.7f + k * 0.065f)
            var x = 0f
            path.moveTo(0f, base)
            while (x <= w) {
                x += 14f * d
                path.lineTo(x, base + sin(x * 0.02f / d + t * (1.2f + k * 0.3f) * speed + k) * (4f + k * 2f) * d)
            }
            wave.color = if (k % 2 == 0) theme.primary else theme.secondary
            wave.alpha = 40 + k * 14
            wave.strokeWidth = (1f + k * 0.5f) * d
            c.drawPath(path, wave)
        }

        for (i in 0 until n) {
            life[i] -= dt * speed
            if (life[i] <= 0f || py[i] < -20f * d) {
                spawn(i, rnd.nextFloat() * w, h + 10f * d, false)
                continue
            }
            vx[i] += sin(t * 2f + i) * 20f * d * dt
            vy[i] -= 10f * d * dt
            px[i] += vx[i] * dt * speed
            py[i] += vy[i] * dt * speed
            val k = life[i] / maxLife[i]
            val flick = 0.7f + 0.3f * sin(t * 18f + i * 3f)
            spark.color = if (k > 0.6f) theme.secondary else if (k > 0.3f) theme.primary else Color.parseColor("#C21E00")
            spark.alpha = (255 * k * flick).toInt().coerceIn(0, 255)
            c.drawCircle(px[i], py[i], size[i] * (0.6f + k), spark)
            spark.alpha = (60 * k).toInt()
            c.drawCircle(px[i], py[i], size[i] * 3.2f, spark)
        }
        ripples.draw(c, dt)
    }

    override fun touch(x: Float, y: Float) {
        ripples.add(x, y)
        var spawned = 0
        var i = rnd.nextInt(n)
        while (spawned < 40) {
            spawn(i, x, y, true)
            i = (i + 1) % n
            spawned++
        }
    }
}
