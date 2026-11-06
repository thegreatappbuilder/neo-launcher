package com.neo.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max
import kotlin.math.roundToInt

object UiKit {
    fun dp(ctx: Context, v: Float): Int = (v * ctx.resources.displayMetrics.density).roundToInt()

    fun alpha(color: Int, a: Int): Int =
        Color.argb(a.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    fun radiusDp(neo: NeoTheme): Float = neo.radius

    fun box(
        ctx: Context,
        neo: NeoTheme,
        fillAlpha: Int = 140,
        strokeAlpha: Int = 200,
        strokeDp: Float = 1.2f,
        strokeColor: Int = neo.primary,
        radius: Float = radiusDp(neo)
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(ctx, radius).toFloat()
        setColor(alpha(neo.bg, fillAlpha))
        setStroke(max(1, dp(ctx, strokeDp)), alpha(strokeColor, strokeAlpha))
    }

    fun iconFrame(ctx: Context, neo: NeoTheme): GradientDrawable =
        if (neo.roundIcons) {
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(alpha(Color.WHITE, 26))
                setStroke(dp(ctx, 1f), alpha(neo.primary, 170))
            }
        } else {
            box(ctx, neo, 120, 190, 1.2f, neo.primary, if (neo.id == "cyberpunk") 12f else radiusDp(neo) + 4f)
        }

    fun label(ctx: Context, neo: NeoTheme, value: String, sp: Float, color: Int, glow: Boolean = true): TextView =
        TextView(ctx).apply {
            text = value
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
            setTextColor(color)
            typeface = neo.typeface
            if (glow) setShadowLayer(dp(ctx, 8f).toFloat(), 0f, 0f, alpha(neo.primary, 210))
        }

    fun chip(ctx: Context, neo: NeoTheme, value: String): TextView =
        label(ctx, neo, value, 12f, neo.text).apply {
            background = box(ctx, neo, 120, 220)
            setPadding(dp(ctx, 14f), dp(ctx, 7f), dp(ctx, 14f), dp(ctx, 7f))
            letterSpacing = 0.15f
            gravity = Gravity.CENTER
            pressFx(this)
        }

    /** Iconițe verzi, în stil terminal, pentru tema Matrix. */
    val matrixFilter: ColorMatrixColorFilter by lazy {
        val m = ColorMatrix()
        m.setSaturation(0f)
        m.postConcat(
            ColorMatrix(
                floatArrayOf(
                    0.25f, 0f, 0f, 0f, 0f,
                    0f, 1.15f, 0f, 0f, 12f,
                    0f, 0f, 0.4f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        ColorMatrixColorFilter(m)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun pressFx(v: View) {
        v.setOnTouchListener { view, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(90).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    view.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
            }
            false
        }
    }

    @Suppress("DEPRECATION")
    fun edgeToEdge(w: Window) {
        w.statusBarColor = Color.TRANSPARENT
        w.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false)
        } else {
            w.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }
    }
}

/** O iconiță de aplicație cu ramă neon. */
class IconCell(
    ctx: Context,
    neo: NeoTheme,
    showLabel: Boolean,
    press: Boolean,
    sizeDp: Float = 56f
) : LinearLayout(ctx) {
    val frame = FrameLayout(ctx)
    val iconView = ImageView(ctx)
    val label = TextView(ctx)
    private val isMatrix = neo.id == "matrix"

    init {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        val s = UiKit.dp(ctx, sizeDp)
        val pad = UiKit.dp(ctx, 9f)
        frame.background = UiKit.iconFrame(ctx, neo)
        frame.setPadding(pad, pad, pad, pad)
        frame.addView(iconView, FrameLayout.LayoutParams(-1, -1))
        addView(frame, LinearLayout.LayoutParams(s, s))
        label.textSize = 11f
        label.setTextColor(neo.text)
        label.typeface = neo.typeface
        label.maxLines = 1
        label.ellipsize = TextUtils.TruncateAt.END
        label.gravity = Gravity.CENTER
        label.setShadowLayer(UiKit.dp(ctx, 6f).toFloat(), 0f, 0f, UiKit.alpha(neo.primary, 220))
        if (showLabel) addView(label, LinearLayout.LayoutParams(-1, -2).apply { topMargin = UiKit.dp(ctx, 6f) })
        if (press) UiKit.pressFx(this)
    }

    fun bind(a: AppItem, copy: Boolean = false) {
        val dr = if (copy) a.icon.constantState?.newDrawable()?.mutate() ?: a.icon else a.icon
        iconView.setImageDrawable(dr)
        if (isMatrix) iconView.colorFilter = UiKit.matrixFilter
        label.text = a.label
        contentDescription = a.label
    }

    companion object {
        fun drawerButton(ctx: Context, neo: NeoTheme): IconCell =
            IconCell(ctx, neo, false, true).apply {
                iconView.setImageDrawable(GridIcon(neo.primary))
                contentDescription = "Toate aplicațiile"
            }
    }
}

/** Iconița 3x3 pentru butonul de aplicații. */
class GridIcon(col: Int) : Drawable() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val step = b.width() / 3f
        val r = step * 0.2f
        for (i in 0..2) for (j in 0..2) {
            canvas.drawCircle(b.left + step * (i + 0.5f), b.top + step * (j + 0.5f), r, p)
        }
    }

    override fun setAlpha(alpha: Int) {
        p.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        p.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
