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
import kotlin.math.sin

/** Oraș neon: zgârie-nori pe două planuri, ferestre care se aprind și mașini zburătoare. */
class NeonCityFx(d: Float, theme: NeoTheme) : FxRenderer(d, theme) {
    private class Building(val x: Float, val bw: Float, val bh: Float, val cols: Int, val rows: Int, val sign: Int, val lit: BooleanArray)
    private class Car(var x: Float, val y: Float, val v: Float, val col: Int)

    private val rnd = Random(5)
    private val sky = Paint()
    private val moon = Paint(Paint.ANTI_ALIAS_FLAG)
    private val far = Paint().apply { color = Color.parseColor("#120A2E") }
    private val near = Paint().apply { color = Color.parseColor("#06030F") }
    private val win = Paint()
    private val sign = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val car = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val fog = Paint()
    private val farB = ArrayList<Building>()
    private val nearB = ArrayList<Building>()
    private val cars = ArrayList<Car>()
    private var span = 1f
    private var toggleT = 0f
    private val ripples = Ripples(theme.secondary, d)

    override fun onSize() {
        sky.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(Color.parseColor("#05021A"), Color.parseColor("#2A0B4A"), Color.parseColor("#7A1F6E")),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
        )
        moon.shader = RadialGradient(
            w * 0.75f, h * 0.18f, 70f * d,
            intArrayOf(Color.argb(255, 255, 220, 240), Color.argb(120, 255, 120, 210), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
        )
        fog.shader = LinearGradient(
            0f, h * 0.55f, 0f, h,
            intArrayOf(Color.TRANSPARENT, UiKit.alpha(theme.primary, 60)), null, Shader.TileMode.CLAMP
        )
        span = w * 2f
        build(farB, 0.32f, 0.5f, 24f)
        build(nearB, 0.22f, 0.38f, 34f)
        cars.clear()
        for (k in 0 until 7) {
            cars.add(
                Car(
                    rnd.nextFloat() * w, h * (0.35f + rnd.nextFloat() * 0.3f),
                    (40f + rnd.nextFloat() * 120f) * d * if (rnd.nextBoolean()) 1f else -1f,
                    if (k % 2 == 0) theme.primary else theme.secondary
                )
            )
        }
    }

    private fun build(list: ArrayList<Building>, minH: Float, maxH: Float, minW: Float) {
        list.clear()
        var x = 0f
        while (x < span) {
            val bw = (minW + rnd.nextFloat() * 50f) * d
            val bh = h * (minH + rnd.nextFloat() * (maxH - minH))
            val cols = (bw / (9f * d)).toInt().coerceAtLeast(1)
            val rows = (bh / (12f * d)).toInt().coerceAtLeast(1)
            val lit = BooleanArray(cols * rows) { rnd.nextFloat() < 0.28f }
            list.add(Building(x, bw, bh, cols, rows, if (rnd.nextFloat() < 0.3f) rnd.nextInt(2) + 1 else 0, lit))
            x += bw + rnd.nextFloat() * 6f * d
        }
    }

    private fun drawLayer(c: Canvas, list: ArrayList<Building>, offset: Float, body: Paint, winAlpha: Int, t: Float) {
        for (copy in 0..1) {
            val shift = -offset + copy * span
            for (b in list) {
                val left = b.x + shift
                if (left > w || left + b.bw < 0f) continue
                val top = h - b.bh
                c.drawRect(left, top, left + b.bw, h, body)
                val cw = b.bw / b.cols
                for (r in 0 until b.rows) for (q in 0 until b.cols) {
                    if (!b.lit[r * b.cols + q]) continue
                    win.color = if ((r + q) % 5 == 0) theme.secondary else Color.parseColor("#FFD9A0")
                    win.alpha = winAlpha
                    val wx = left + q * cw + cw * 0.25f
                    val wy = top + 6f * d + r * 12f * d
                    if (wy > h - 4f * d) continue
                    c.drawRect(wx, wy, wx + cw * 0.5f, wy + 5f * d, win)
                }
                if (b.sign > 0) {
                    sign.color = if (b.sign == 1) theme.primary else theme.secondary
                    val on = sin(t * 3f + b.x) > -0.6f
                    sign.alpha = if (on) 240 else 60
                    sign.strokeWidth = 3f * d
                    c.drawLine(left + b.bw * 0.15f, top - 3f * d, left + b.bw * 0.85f, top - 3f * d, sign)
                    sign.alpha = if (on) 70 else 20
                    sign.strokeWidth = 10f * d
                    c.drawLine(left + b.bw * 0.15f, top - 3f * d, left + b.bw * 0.85f, top - 3f * d, sign)
                }
            }
        }
    }

    override fun draw(c: Canvas, t: Float, dt: Float) {
        c.drawRect(0f, 0f, w, h, sky)
        c.drawCircle(w * 0.75f, h * 0.18f, 70f * d, moon)
        val farOff = (t * 6f * d * speed) % span
        val nearOff = (t * 16f * d * speed) % span
        drawLayer(c, farB, farOff, far, 110, t)
        for (car0 in cars) {
            car0.x += car0.v * dt * speed
            if (car0.x > w + 30f * d) car0.x = -30f * d
            if (car0.x < -30f * d) car0.x = w + 30f * d
            val dir = if (car0.v > 0) -1f else 1f
            car.color = car0.col
            car.alpha = 60
            car.strokeWidth = 6f * d
            c.drawLine(car0.x, car0.y, car0.x + dir * 26f * d, car0.y, car)
            car.alpha = 255
            car.strokeWidth = 2.5f * d
            c.drawLine(car0.x, car0.y, car0.x + dir * 8f * d, car0.y, car)
        }
        c.drawRect(0f, h * 0.55f, w, h, fog)
        drawLayer(c, nearB, nearOff, near, 210, t)

        if (dt > 0f) {
            toggleT -= dt
            if (toggleT <= 0f) {
                toggleT = 0.12f
                val list = if (rnd.nextBoolean()) farB else nearB
                if (list.isNotEmpty()) {
                    val b = list[rnd.nextInt(list.size)]
                    val i = rnd.nextInt(b.lit.size)
                    b.lit[i] = !b.lit[i]
                }
            }
        }
        ripples.draw(c, dt)
    }

    override fun touch(x: Float, y: Float) {
        ripples.add(x, y)
        for (b in nearB) for (i in b.lit.indices) if (rnd.nextFloat() < 0.15f) b.lit[i] = !b.lit[i]
    }
}
