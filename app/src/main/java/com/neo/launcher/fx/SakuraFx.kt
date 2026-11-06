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
import kotlin.math.min
import kotlin.math.sin

/** Sakura la lună plină: ramuri de cireș, lună mare și petale care plutesc. Atingerea stârnește o rafală. */
class SakuraFx(d: Float, theme: NeoTheme) : FxRenderer(d, theme) {
    private val rnd = Random(9)
    private val sky = Paint()
    private val moon = Paint(Paint.ANTI_ALIAS_FLAG)
    private val moonHalo = Paint(Paint.ANTI_ALIAS_FLAG)
    private val branch = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#0A0410")
    }
    private val bloom = Paint(Paint.ANTI_ALIAS_FLAG)
    private val petal = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0B0512") }
    private val petalPath = Path()
    private val hillPath = Path()
    private val branches = ArrayList<FloatArray>()
    private val blossoms = ArrayList<FloatArray>()
    private val n = 70
    private val px = FloatArray(n)
    private val py = FloatArray(n)
    private val pv = FloatArray(n)
    private val pr = FloatArray(n)
    private val prv = FloatArray(n)
    private val ps = FloatArray(n)
    private var gust = 0f
    private val ripples = Ripples(theme.primary, d)

    override fun onSize() {
        sky.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(Color.parseColor("#0D0620"), Color.parseColor("#3A1840"), Color.parseColor("#8A3D5E")),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP
        )
        val mr = min(w, h) * 0.22f
        moon.shader = RadialGradient(
            w * 0.68f, h * 0.3f, mr,
            intArrayOf(Color.parseColor("#FFF6E6"), Color.parseColor("#FFE2B8")), null, Shader.TileMode.CLAMP
        )
        moonHalo.shader = RadialGradient(
            w * 0.68f, h * 0.3f, mr * 2.4f,
            intArrayOf(Color.argb(110, 255, 210, 200), Color.TRANSPARENT), null, Shader.TileMode.CLAMP
        )
        branches.clear()
        blossoms.clear()
        grow(-10f * d, h * 0.08f, 0.35f, 120f * d, 10f * d, 0)
        grow(w + 10f * d, h * 0.02f, 2.6f, 90f * d, 8f * d, 0)

        hillPath.reset()
        hillPath.moveTo(0f, h)
        hillPath.lineTo(0f, h * 0.88f)
        hillPath.quadTo(w * 0.3f, h * 0.8f, w * 0.6f, h * 0.87f)
        hillPath.quadTo(w * 0.85f, h * 0.92f, w, h * 0.85f)
        hillPath.lineTo(w, h)
        hillPath.close()

        petalPath.reset()
        petalPath.moveTo(0f, -1f)
        petalPath.quadTo(0.9f, -0.6f, 0.15f, 1f)
        petalPath.lineTo(0f, 0.7f)
        petalPath.lineTo(-0.15f, 1f)
        petalPath.quadTo(-0.9f, -0.6f, 0f, -1f)
        petalPath.close()

        for (i in 0 until n) reset(i, rnd.nextFloat() * h)
    }

    /** Ramură recursivă: segmente salvate ca [x1,y1,x2,y2,grosime]. */
    private fun grow(x: Float, y: Float, angle: Float, len: Float, thick: Float, depth: Int) {
        val x2 = x + kotlin.math.cos(angle) * len
        val y2 = y + kotlin.math.sin(angle) * len
        branches.add(floatArrayOf(x, y, x2, y2, thick))
        if (depth >= 4 || len < 18f * d) {
            repeat(3) {
                blossoms.add(floatArrayOf(x2 + (rnd.nextFloat() - 0.5f) * 18f * d, y2 + (rnd.nextFloat() - 0.5f) * 18f * d, (4f + rnd.nextFloat() * 4f) * d))
            }
            return
        }
        grow(x2, y2, angle + 0.25f + rnd.nextFloat() * 0.3f, len * 0.72f, thick * 0.65f, depth + 1)
        grow(x2, y2, angle - 0.35f - rnd.nextFloat() * 0.3f, len * 0.68f, thick * 0.6f, depth + 1)
        if (rnd.nextFloat() < 0.5f) {
            blossoms.add(floatArrayOf(x2, y2, (4f + rnd.nextFloat() * 3f) * d))
        }
    }

    private fun reset(i: Int, y: Float) {
        px[i] = rnd.nextFloat() * w * 1.2f - w * 0.1f
        py[i] = y
        pv[i] = (18f + rnd.nextFloat() * 30f) * d
        pr[i] = rnd.nextFloat() * 360f
        prv[i] = (rnd.nextFloat() - 0.5f) * 120f
        ps[i] = (4f + rnd.nextFloat() * 4f) * d
    }

    override fun draw(c: Canvas, t: Float, dt: Float) {
        c.drawRect(0f, 0f, w, h, sky)
        val mr = min(w, h) * 0.22f
        c.drawCircle(w * 0.68f, h * 0.3f, mr * 2.4f, moonHalo)
        c.drawCircle(w * 0.68f, h * 0.3f, mr, moon)
        c.drawPath(hillPath, hill)

        // ramuri care se leagănă puțin
        val sway = sin(t * 0.6f) * 1.2f
        c.save()
        c.rotate(sway * 0.4f, 0f, 0f)
        for (b in branches) {
            branch.strokeWidth = b[4]
            c.drawLine(b[0], b[1], b[2], b[3], branch)
        }
        for ((k, bl) in blossoms.withIndex()) {
            bloom.color = if (k % 3 == 0) Color.parseColor("#FFD1E6") else theme.primary
            bloom.alpha = 230
            c.drawCircle(bl[0], bl[1], bl[2], bloom)
        }
        c.restore()

        gust += (0f - gust) * min(1f, dt * 1.2f)
        for (i in 0 until n) {
            py[i] += pv[i] * dt * speed
            px[i] += (sin(t * 0.9f + i) * 20f * d + 30f * d + gust * 260f * d) * dt * speed
            pr[i] += prv[i] * dt * (1f + gust)
            if (py[i] > h + 20f * d || px[i] > w + 30f * d) reset(i, -20f * d)
            c.save()
            c.translate(px[i], py[i])
            c.rotate(pr[i])
            val flip = 0.35f + 0.65f * kotlin.math.abs(sin(t * 1.8f + i))
            c.scale(ps[i] * flip, ps[i])
            petal.color = if (i % 4 == 0) Color.parseColor("#FFE4EF") else theme.primary
            petal.alpha = 220
            c.drawPath(petalPath, petal)
            c.restore()
        }
        ripples.draw(c, dt)
    }

    override fun touch(x: Float, y: Float) {
        ripples.add(x, y)
        gust = 1.5f
    }
}
