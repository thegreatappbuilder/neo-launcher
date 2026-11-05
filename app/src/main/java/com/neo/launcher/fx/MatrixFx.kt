package com.neo.launcher.fx

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import com.neo.launcher.NeoTheme
import java.util.Random
import kotlin.math.max
import kotlin.math.min

/** Ploaie de cod digital. Atingerea „trezește” coloanele de sub deget. */
class MatrixFx(d: Float, theme: NeoTheme) : FxRenderer(d, theme) {
    private val glyphs = "アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワン0123456789Z:=*+<>".toCharArray()
    private val rnd = Random()
    private val cell = 15f * d
    private var cols = 0
    private var rows = 0
    private var grid = CharArray(0)
    private var head = FloatArray(0)
    private var spd = FloatArray(0)
    private var len = IntArray(0)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = cell * 0.95f
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
        color = theme.primary
    }
    private val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = cell * 0.95f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#E8FFE8")
        setShadowLayer(6f * d, 0f, 0f, theme.primary)
    }
    private val vignette = Paint()
    private val ripples = Ripples(theme.primary, d)

    override fun onSize() {
        cols = (w / cell).toInt() + 1
        rows = (h / cell).toInt() + 2
        grid = CharArray(cols * rows) { glyphs[rnd.nextInt(glyphs.size)] }
        head = FloatArray(cols) { -rnd.nextFloat() * rows }
        spd = FloatArray(cols) { 6f + rnd.nextFloat() * 14f }
        len = IntArray(cols) { 6 + rnd.nextInt(22) }
        vignette.shader = RadialGradient(
            w / 2f, h / 2f, max(w, h) * 0.75f,
            intArrayOf(Color.TRANSPARENT, Color.argb(200, 0, 0, 0)),
            floatArrayOf(0.5f, 1f), Shader.TileMode.CLAMP
        )
    }

    override fun draw(c: Canvas, t: Float, dt: Float) {
        c.drawColor(Color.BLACK)
        if (cols == 0) return
        if (dt > 0f) {
            repeat((grid.size * 0.02f).toInt() + 1) {
                grid[rnd.nextInt(grid.size)] = glyphs[rnd.nextInt(glyphs.size)]
            }
        }
        for (ci in 0 until cols) {
            head[ci] += spd[ci] * dt * speed
            val hd = head[ci]
            val l = len[ci]
            if (hd - l > rows) {
                head[ci] = -rnd.nextFloat() * rows * 0.5f
                spd[ci] = 6f + rnd.nextFloat() * 14f
                len[ci] = 6 + rnd.nextInt(22)
                continue
            }
            val hr = hd.toInt()
            val x = ci * cell + cell / 2f
            for (k in 0 until l) {
                val r = hr - k
                if (r < 0 || r >= rows) continue
                val y = (r + 1) * cell
                val idx = r * cols + ci
                if (k == 0) {
                    c.drawText(grid, idx, 1, x, y, hp)
                } else {
                    p.alpha = (255 * (1f - k.toFloat() / l)).toInt().coerceIn(0, 255)
                    c.drawText(grid, idx, 1, x, y, p)
                }
            }
        }
        ripples.draw(c, dt)
        c.drawRect(0f, 0f, w, h, vignette)
    }

    override fun touch(x: Float, y: Float) {
        ripples.add(x, y)
        if (cols == 0) return
        val cc = (x / cell).toInt()
        val r = y / cell
        for (i in max(0, cc - 3)..min(cols - 1, cc + 3)) {
            head[i] = r + rnd.nextFloat() * 2f
            spd[i] = 18f + rnd.nextFloat() * 10f
        }
    }
}
