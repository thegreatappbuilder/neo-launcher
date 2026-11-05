package com.neo.launcher.fx

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.neo.launcher.NeoTheme
import java.util.Random
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Synthwave: soare cu dungi, grilă în perspectivă, linii de scanare și glitch. */
class CyberFx(d: Float, theme: NeoTheme) : FxRenderer(d, theme) {
    private val rnd = Random()
    private val sky = Paint()
    private val ground = Paint().apply { color = Color.parseColor("#07000F") }
    private val sun = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glow = Paint()
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val scan = Paint().apply { color = Color.argb(38, 0, 0, 0) }
    private val glitch = Paint()
    private val starP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val ripples = Ripples(theme.secondary, d)
    private val stars = FloatArray(120)
    private var horizon = 0f
    private var sunR = 0f
    private var nextGlitch = 2.5f
    private var glitchT = 0f
    private var gY = 0f
    private var gH = 0f
    private var gDx = 0f

    override fun onSize() {
        horizon = h * 0.6f
        sunR = min(w, h) * 0.3f
        sky.shader = LinearGradient(
            0f, 0f, 0f, horizon,
            intArrayOf(Color.parseColor("#0B0016"), Color.parseColor("#2A0845"), Color.parseColor("#7A1E6C")),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP
        )
        sun.shader = LinearGradient(
            0f, horizon - sunR * 1.6f, 0f, horizon,
            intArrayOf(Color.parseColor("#FFE76A"), Color.parseColor("#FF6AC1"), Color.parseColor("#FF2BD6")),
            null, Shader.TileMode.CLAMP
        )
        glow.shader = RadialGradient(
            w / 2f, horizon, max(w, h) * 0.6f,
            intArrayOf(Color.argb(120, 255, 43, 214), Color.TRANSPARENT), null, Shader.TileMode.CLAMP
        )
        for (i in 0 until 60) {
            stars[i * 2] = rnd.nextFloat() * w
            stars[i * 2 + 1] = rnd.nextFloat() * horizon * 0.75f
        }
    }

    override fun draw(c: Canvas, t: Float, dt: Float) {
        val cx = w / 2f
        c.drawRect(0f, 0f, w, horizon, sky)

        for (i in 0 until 60) {
            starP.alpha = (130 + 120 * sin(t * 2f + i)).toInt().coerceIn(0, 255)
            c.drawCircle(stars[i * 2], stars[i * 2 + 1], 1.2f * d, starP)
        }

        // soarele cu dungi care coboară
        val cy = horizon - sunR * 0.55f
        c.save()
        c.clipRect(0f, 0f, w, horizon)
        c.drawCircle(cx, cy, sunR, sun)
        val gap = sunR * 0.13f
        val shift = (t * 10f * d * speed) % gap
        var y = cy + shift
        var th = 1.5f * d
        while (y < cy + sunR) {
            c.drawRect(cx - sunR, y, cx + sunR, y + th, sky)
            y += gap
            th += 1.6f * d
        }
        c.restore()

        c.drawRect(0f, horizon, w, h, ground)
        c.drawRect(0f, 0f, w, h, glow)

        // grila în perspectivă
        val gh = h - horizon
        grid.color = theme.primary
        grid.strokeWidth = 1.5f * d
        val phase = (t * 0.55f * speed) % 1f
        for (i in 0 until 16) {
            val z = (i + phase) / 16f
            val yy = horizon + gh * z * z
            grid.alpha = (30 + 225 * z).toInt().coerceAtMost(255)
            c.drawLine(0f, yy, w, yy, grid)
        }
        grid.alpha = 190
        for (i in -14..14) {
            c.drawLine(cx + i * w * 0.045f, horizon, cx + i * w * 0.24f, h, grid)
        }
        grid.color = theme.secondary
        grid.strokeWidth = 2f * d
        c.drawLine(0f, horizon, w, horizon, grid)

        ripples.draw(c, dt)

        // linii de scanare
        var sy = 0f
        val step = 3f * d
        while (sy < h) {
            c.drawRect(0f, sy, w, sy + d, scan)
            sy += step
        }

        // glitch ocazional
        if (dt > 0f) {
            nextGlitch -= dt
            if (nextGlitch <= 0f) {
                glitchT = 0.18f
                nextGlitch = 2f + rnd.nextFloat() * 5f
                gY = rnd.nextFloat() * h
                gH = (8f + rnd.nextFloat() * 40f) * d
                gDx = (rnd.nextFloat() - 0.5f) * 40f * d
            }
        }
        if (glitchT > 0f) {
            glitchT -= dt
            glitch.color = theme.secondary
            glitch.alpha = 70
            c.drawRect(gDx, gY, w + gDx, gY + gH, glitch)
            glitch.color = theme.primary
            glitch.alpha = 60
            c.drawRect(-gDx, gY + gH * 0.4f, w - gDx, gY + gH * 1.2f, glitch)
        }
    }

    override fun touch(x: Float, y: Float) {
        ripples.add(x, y)
        glitchT = 0.12f
        gY = y - 20f * d
        gH = 30f * d
        gDx = 12f * d
    }
}
