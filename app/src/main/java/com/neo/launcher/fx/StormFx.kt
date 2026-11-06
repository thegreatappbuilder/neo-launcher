package com.neo.launcher.fx

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.neo.launcher.NeoTheme
import java.util.Random
import kotlin.math.max

/** Furtună electrică: nori, ploaie și fulgere. Atingerea trimite un fulger exact unde ai apăsat. */
class StormFx(d: Float, theme: NeoTheme) : FxRenderer(d, theme) {
    private class Cloud(var x: Float, val y: Float, val r: Float, val v: Float, val paint: Paint)

    private val rnd = Random()
    private val sky = Paint()
    private val rain = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val bolt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val flash = Paint()
    private val clouds = ArrayList<Cloud>()
    private val boltPath = Path()
    private val nr = 220
    private val rx = FloatArray(nr)
    private val ry = FloatArray(nr)
    private val rv = FloatArray(nr)
    private val rl = FloatArray(nr)
    private var flashA = 0f
    private var boltA = 0f
    private var next = 3f
    private val ripples = Ripples(theme.secondary, d)

    override fun onSize() {
        sky.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(Color.parseColor("#141A2A"), Color.parseColor("#0A0E18"), Color.parseColor("#04060B")),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        clouds.clear()
        for (k in 0 until 9) {
            val r = (90f + rnd.nextFloat() * 120f) * d
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    0f, 0f, r,
                    intArrayOf(Color.argb(150, 70, 82, 105), Color.argb(70, 40, 48, 66), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP
                )
            }
            clouds.add(Cloud(rnd.nextFloat() * w, h * (0.02f + rnd.nextFloat() * 0.22f), r, (6f + rnd.nextFloat() * 14f) * d, p))
        }
        for (i in 0 until nr) resetDrop(i, rnd.nextFloat() * h)
        rain.strokeWidth = 1.1f * d
    }

    private fun resetDrop(i: Int, y: Float) {
        rx[i] = rnd.nextFloat() * (w + h * 0.3f)
        ry[i] = y
        rv[i] = (650f + rnd.nextFloat() * 450f) * d
        rl[i] = (10f + rnd.nextFloat() * 16f) * d
    }

    private fun strike(x: Float, yEnd: Float) {
        boltPath.reset()
        var px = x + (rnd.nextFloat() - 0.5f) * 60f * d
        var py = 0f
        boltPath.moveTo(px, py)
        val target = max(yEnd, h * 0.3f)
        while (py < target) {
            py += (14f + rnd.nextFloat() * 22f) * d
            px += (rnd.nextFloat() - 0.5f) * 36f * d + (x - px) * 0.08f
            boltPath.lineTo(px, py)
            if (rnd.nextFloat() < 0.14f) {
                // ramificație
                var bx = px
                var by = py
                repeat(3 + rnd.nextInt(4)) {
                    bx += (rnd.nextFloat() - 0.3f) * 28f * d * if (rnd.nextBoolean()) 1f else -1f
                    by += (12f + rnd.nextFloat() * 16f) * d
                    boltPath.lineTo(bx, by)
                }
                boltPath.moveTo(px, py)
            }
        }
        boltA = 1f
        flashA = 1f
    }

    override fun draw(c: Canvas, t: Float, dt: Float) {
        c.drawRect(0f, 0f, w, h, sky)
        for (cl in clouds) {
            cl.x += cl.v * dt * speed
            if (cl.x - cl.r > w) cl.x = -cl.r
            c.save()
            c.translate(cl.x, cl.y)
            c.scale(1.6f, 0.7f)
            c.drawCircle(0f, 0f, cl.r, cl.paint)
            c.restore()
        }

        if (dt > 0f) {
            next -= dt
            if (next <= 0f) {
                next = 3f + rnd.nextFloat() * 6f
                strike(rnd.nextFloat() * w, h * (0.5f + rnd.nextFloat() * 0.4f))
            }
        }

        if (flashA > 0f) {
            flash.color = Color.argb((flashA * 110).toInt().coerceIn(0, 255), 200, 220, 255)
            c.drawRect(0f, 0f, w, h, flash)
            flashA -= dt * 3.5f
        }
        if (boltA > 0f) {
            val flicker = if ((t * 30f).toInt() % 3 == 0) 0.6f else 1f
            bolt.color = theme.primary
            bolt.alpha = (boltA * 120 * flicker).toInt().coerceIn(0, 255)
            bolt.strokeWidth = 9f * d
            c.drawPath(boltPath, bolt)
            bolt.color = Color.WHITE
            bolt.alpha = (boltA * 255 * flicker).toInt().coerceIn(0, 255)
            bolt.strokeWidth = 2.2f * d
            c.drawPath(boltPath, bolt)
            boltA -= dt * 2.2f
        }

        rain.color = theme.secondary
        for (i in 0 until nr) {
            ry[i] += rv[i] * dt * speed
            rx[i] -= rv[i] * 0.18f * dt * speed
            if (ry[i] > h) resetDrop(i, -rl[i])
            rain.alpha = 70 + (i % 4) * 25
            c.drawLine(rx[i], ry[i], rx[i] + rl[i] * 0.18f, ry[i] - rl[i], rain)
        }
        ripples.draw(c, dt)
    }

    override fun touch(x: Float, y: Float) {
        ripples.add(x, y)
        strike(x, y)
    }
}
