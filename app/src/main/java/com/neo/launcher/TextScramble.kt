package com.neo.launcher

import android.animation.ValueAnimator
import android.widget.TextView
import java.util.Random

/** Efect „decodare”: literele aleatorii se transformă în textul final. */
class TextScramble(private val tv: TextView) {
    private val glyphs = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789#%&@$<>/\\"
    private val rnd = Random()
    private var anim: ValueAnimator? = null

    fun run(target: String, duration: Long = 900L) {
        anim?.cancel()
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            addUpdateListener { va ->
                val p = va.animatedValue as Float
                val reveal = (p * target.length).toInt()
                val sb = StringBuilder(target.length)
                for (i in target.indices) {
                    val ch = target[i]
                    sb.append(if (i < reveal || ch == ' ') ch else glyphs[rnd.nextInt(glyphs.length)])
                }
                tv.text = sb
            }
            start()
        }
    }
}
