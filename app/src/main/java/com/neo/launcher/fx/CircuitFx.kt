package com.neo.launcher.fx

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.neo.launcher.NeoTheme
import java.util.Random
import kotlin.math.abs
import kotlin.math.hypot

/** Circuit cuantic: trasee electronice prin care aleargă impulsuri de energie. Atingerea trimite impulsuri noi. */
class CircuitFx(d: Float, theme: NeoTheme) : FxRenderer(d, theme) {
    private class Trace(val xs: FloatArray, val ys: FloatArray, val len: Float, val path: Path)
    private class Pulse(val trace: Int, var s: Float, val v: Float, val hot: Boolean)

    private val rnd = Random(21)
    private val traces = ArrayList<Trace>()
    private val pulses = ArrayList<Pulse>()
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private val node = Paint(Paint.ANTI_ALIAS_FLAG)
    private val chip = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val head = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dots = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ripples = Ripples(theme.primary, d)
    private val tmp = FloatArray(2)
    private var cell = 1f
    private var spawnT = 0f

    override fun onSize() {
        cell = 26f * d
        traces.clear()
        pulses.clear()
        val cols = (w / cell).toInt()
        val rows = (h / cell).toInt()
        repeat(34) {
            var cxi = rnd.nextInt(cols + 1)
            var cyi = rnd.nextInt(rows + 1)
            var dir = rnd.nextInt(4)
            val steps = 6 + rnd.nextInt(14)
            val xs = FloatArray(steps + 1)
            val ys = FloatArray(steps + 1)
            xs[0] = cxi * cell
            ys[0] = cyi * cell
            var count = 1
            for (s in 0 until steps) {
                if (rnd.nextFloat() < 0.3f) dir = (dir + if (rnd.nextBoolean()) 1 else 3) % 4
                val run = 1 + rnd.nextInt(3)
                when (dir) {
                    0 -> cxi += run
                    1 -> cyi += run
                    2 -> cxi -= run
                    else -> cyi -= run
                }
                cxi = cxi.coerceIn(0, cols)
                cyi = cyi.coerceIn(0, rows)
                val nx = cxi * cell
                val ny = cyi * cell
                if (nx == xs[count - 1] && ny == ys[count - 1]) continue
                xs[count] = nx
                ys[count] = ny
                count++
            }
            if (count < 3) return@repeat
            val fx = xs.copyOf(count)
            val fy = ys.copyOf(count)
            var len = 0f
            val p = Path()
            p.moveTo(fx[0], fy[0])
            for (k in 1 until count) {
                len += hypot(fx[k] - fx[k - 1], fy[k] - fy[k - 1])
                p.lineTo(fx[k], fy[k])
            }
            traces.add(Trace(fx, fy, len, p))
        }
        line.strokeWidth = 1.6f * d
        chip.strokeWidth = 1.4f * d
        head.color = Color.WHITE
        dots.color = theme.primary
        dots.alpha = 40
    }

    private fun pointAt(tr: Trace, dist: Float, out: FloatArray) {
        var rest = dist.coerceIn(0f, tr.len)
        for (k in 1 until tr.xs.size) {
            val seg = abs(tr.xs[k] - tr.xs[k - 1]) + abs(tr.ys[k] - tr.ys[k - 1])
            if (rest <= seg || k == tr.xs.size - 1) {
                val f = if (seg > 0f) (rest / seg).coerceIn(0f, 1f) else 0f
                out[0] = tr.xs[k - 1] + (tr.xs[k] - tr.xs[k - 1]) * f
                out[1] = tr.ys[k - 1] + (tr.ys[k] - tr.ys[k - 1]) * f
                return
            }
            rest -= seg
        }
    }

    override fun draw(c: Canvas, t: Float, dt: Float) {
        c.drawColor(theme.bg)
        // grilă de puncte
        var gy = 0f
        while (gy <= h) {
            var gx = 0f
            while (gx <= w) {
                c.drawCircle(gx, gy, 0.9f * d, dots)
                gx += cell
            }
            gy += cell
        }
        line.color = theme.primary
        line.alpha = 55
        node.color = theme.primary
        for (tr in traces) {
            c.drawPath(tr.path, line)
            node.alpha = 120
            c.drawCircle(tr.xs[0], tr.ys[0], 2.6f * d, node)
            c.drawCircle(tr.xs[tr.xs.size - 1], tr.ys[tr.ys.size - 1], 2.6f * d, node)
        }
        // „cipuri” care pulsează
        chip.color = theme.secondary
        chip.alpha = (90 + 60 * kotlin.math.sin(t * 2f)).toInt()
        for (k in 0 until 3) {
            val x = w * (0.2f + k * 0.3f)
            val y = h * (0.25f + (k % 2) * 0.4f)
            c.drawRect(x - cell, y - cell * 0.7f, x + cell, y + cell * 0.7f, chip)
        }

        if (traces.isEmpty()) return
        spawnT -= dt * speed
        if (spawnT <= 0f && pulses.size < 24) {
            spawnT = 0.15f + rnd.nextFloat() * 0.3f
            pulses.add(Pulse(rnd.nextInt(traces.size), 0f, (120f + rnd.nextFloat() * 200f) * d, rnd.nextFloat() < 0.25f))
        }
        val it = pulses.iterator()
        while (it.hasNext()) {
            val p = it.next()
            val tr = traces[p.trace]
            p.s += p.v * dt * speed
            if (p.s - 60f * d > tr.len) {
                it.remove()
                continue
            }
            val col = if (p.hot) theme.secondary else theme.primary
            for (k in 6 downTo 1) {
                pointAt(tr, p.s - k * 9f * d, tmp)
                node.color = col
                node.alpha = (200 * (1f - k / 7f)).toInt()
                c.drawCircle(tmp[0], tmp[1], (2.8f - k * 0.25f) * d, node)
            }
            pointAt(tr, p.s, tmp)
            node.alpha = 90
            c.drawCircle(tmp[0], tmp[1], 9f * d, node)
            c.drawCircle(tmp[0], tmp[1], 2.6f * d, head)
        }
        ripples.draw(c, dt)
    }

    override fun touch(x: Float, y: Float) {
        ripples.add(x, y)
        if (traces.isEmpty()) return
        // impulsuri din traseele cele mai apropiate de deget
        val order = traces.indices.sortedBy { i ->
            val tr = traces[i]
            hypot(tr.xs[0] - x, tr.ys[0] - y)
        }
        for (i in order.take(6)) pulses.add(Pulse(i, 0f, 260f * d, true))
    }
}
