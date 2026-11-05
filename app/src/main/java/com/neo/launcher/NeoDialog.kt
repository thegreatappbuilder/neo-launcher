package com.neo.launcher

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout

/** Popup-ul futurist folosit pentru reclame și anunțuri de actualizare. */
object NeoDialog {
    fun show(
        act: Activity,
        neo: NeoTheme,
        tag: String,
        title: String,
        message: String,
        imageUrl: String?,
        button: String,
        cancelable: Boolean,
        onCta: () -> Unit,
        onDismiss: () -> Unit
    ): Dialog {
        fun dp(v: Float) = UiKit.dp(act, v)
        val d = Dialog(act)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        d.setCancelable(cancelable)
        d.setCanceledOnTouchOutside(cancelable)

        val root = FrameLayout(act)
        val card = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.box(act, neo, 242, 255, 2f)
            setPadding(dp(20f), dp(16f), dp(20f), dp(20f))
            isClickable = true
            elevation = dp(12f).toFloat()
        }

        val head = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        head.addView(
            UiKit.label(act, neo, "◆ " + tag.uppercase(), 11f, neo.secondary).apply { letterSpacing = 0.25f },
            LinearLayout.LayoutParams(0, -2, 1f)
        )
        if (cancelable) {
            head.addView(UiKit.label(act, neo, "✕", 18f, neo.text).apply {
                setPadding(dp(14f), dp(4f), dp(4f), dp(4f))
                setOnClickListener { d.dismiss() }
            })
        }
        card.addView(head, LinearLayout.LayoutParams(-1, -2))

        imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            val iv = ImageView(act).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.CENTER_CROP
                maxHeight = dp(230f)
                visibility = View.GONE
                background = GradientDrawable().apply {
                    cornerRadius = dp(UiKit.radiusDp(neo) + 4f).toFloat()
                    setColor(Color.BLACK)
                }
                clipToOutline = true
            }
            card.addView(iv, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12f) })
            Thread {
                val bmp = Http.bitmap(url, 1000)
                act.runOnUiThread {
                    if (bmp != null && d.isShowing) {
                        iv.setImageBitmap(bmp)
                        iv.visibility = View.VISIBLE
                        iv.alpha = 0f
                        iv.animate().alpha(1f).setDuration(250).start()
                    }
                }
            }.start()
        }

        if (title.isNotBlank()) {
            card.addView(UiKit.label(act, neo, title, 21f, neo.primary).apply { setPadding(0, dp(14f), 0, 0) })
        }
        if (message.isNotBlank()) {
            card.addView(UiKit.label(act, neo, message, 14.5f, neo.text, glow = false).apply {
                setPadding(0, dp(8f), 0, 0)
                setLineSpacing(0f, 1.15f)
            })
        }

        val cta = UiKit.label(act, neo, button.uppercase(), 14f, neo.bg, glow = false).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.2f
            typeface = Typeface.create(neo.typeface, Typeface.BOLD)
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(neo.primary, neo.secondary)
            ).apply { cornerRadius = dp(UiKit.radiusDp(neo) + 2f).toFloat() }
            setPadding(0, dp(14f), 0, dp(14f))
            setOnClickListener {
                onCta()
                if (cancelable) d.dismiss()
            }
        }
        UiKit.pressFx(cta)
        card.addView(cta, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18f) })

        val width = minOf(dp(380f), act.resources.displayMetrics.widthPixels - dp(40f))
        root.addView(card, FrameLayout.LayoutParams(width, -2, Gravity.CENTER))
        if (cancelable) root.setOnClickListener { d.dismiss() }

        d.setContentView(root)
        d.setOnDismissListener { onDismiss() }
        d.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.75f)
        }
        d.show()
        d.window?.setLayout(-1, -1)

        // intrare cu efect
        card.alpha = 0f
        card.scaleX = 0.85f
        card.scaleY = 0.85f
        if (neo.id == "cyberpunk" || neo.id == "matrix") card.translationX = dp(10f).toFloat()
        card.animate().alpha(1f).scaleX(1f).scaleY(1f).translationX(0f)
            .setDuration(340).setInterpolator(OvershootInterpolator(1.2f)).start()
        return d
    }
}
