package com.mahanshengzhi.indonesianengineer.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.mahanshengzhi.indonesianengineer.R

object UiKit {
    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    fun pageScroll(context: Context): Pair<ScrollView, LinearLayout> {
        val scroll = ScrollView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFillViewport = true
            setBackgroundColor(ContextCompat.getColor(context, R.color.page_background))
        }
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 22), dp(context, 22), dp(context, 22), dp(context, 26))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        scroll.addView(body)
        return scroll to body
    }

    fun title(context: Context, value: String): TextView =
        TextView(context).apply {
            text = value
            textSize = 28f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            includeFontPadding = true
        }

    fun kicker(context: Context, value: String): TextView =
        TextView(context).apply {
            text = value
            textSize = 12f
            letterSpacing = 0.12f
            setTextColor(ContextCompat.getColor(context, R.color.teal_primary))
            includeFontPadding = true
        }

    fun subtitle(context: Context, value: String): TextView =
        TextView(context).apply {
            text = value
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            includeFontPadding = true
            setLineSpacing(0f, 1.18f)
        }

    fun section(context: Context, value: String): TextView =
        TextView(context).apply {
            text = value
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            includeFontPadding = true
        }

    fun body(context: Context, value: String, size: Float = 16f): TextView =
        TextView(context).apply {
            text = value
            textSize = size
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            includeFontPadding = true
            setLineSpacing(0f, 1.22f)
        }

    fun muted(context: Context, value: String, size: Float = 14f): TextView =
        TextView(context).apply {
            text = value
            textSize = size
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            includeFontPadding = true
            setLineSpacing(0f, 1.20f)
        }

    fun card(context: Context, background: Int? = null): MaterialCardView =
        MaterialCardView(context).apply {
            radius = dp(context, 18).toFloat()
            cardElevation = 0f
            strokeWidth = dp(context, 1)
            strokeColor = ContextCompat.getColor(context, R.color.card_stroke)
            setCardBackgroundColor(
                ContextCompat.getColor(context, background ?: android.R.color.white)
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(context, 14)
            }
        }

    fun cardContent(context: Context): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 18), dp(context, 18), dp(context, 18), dp(context, 18))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

    fun primaryButton(context: Context, value: String): MaterialButton =
        MaterialButton(context).apply {
            text = value
            isAllCaps = false
            textSize = 15f
            cornerRadius = dp(context, 14)
            minHeight = dp(context, 48)
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.teal_primary)
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(context, 8) }
        }

    fun secondaryButton(context: Context, value: String): MaterialButton =
        MaterialButton(context).apply {
            text = value
            isAllCaps = false
            textSize = 14f
            cornerRadius = dp(context, 12)
            minHeight = dp(context, 42)
            setTextColor(ContextCompat.getColor(context, R.color.teal_deep))
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.teal_light)
            )
        }

    fun warmButton(context: Context, value: String): MaterialButton =
        MaterialButton(context).apply {
            text = value
            isAllCaps = false
            textSize = 14f
            cornerRadius = dp(context, 12)
            minHeight = dp(context, 42)
            setTextColor(ContextCompat.getColor(context, R.color.warm_orange))
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.warm_orange_light)
            )
        }

    fun addGap(parent: LinearLayout, context: Context, height: Int = 10) {
        parent.addView(
            View(context),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(context, height)
            )
        )
    }

    fun progressRow(
        context: Context,
        parent: LinearLayout,
        label: String,
        current: Int,
        total: Int
    ) {
        val wrap = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(context, 4), 0, dp(context, 10))
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val labelView = body(context, label, 14f)
        row.addView(labelView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(muted(context, current.toString() + " / " + total, 13f))
        wrap.addView(row)

        val bar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = total.coerceAtLeast(1)
            progress = current.coerceIn(0, max)
            progressTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.teal_primary)
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(context, 7)
            ).apply { topMargin = dp(context, 5) }
        }
        wrap.addView(bar)
        parent.addView(wrap)
    }

    fun buttonRow(context: Context): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

    fun addWeightedButton(
        row: LinearLayout,
        button: View,
        context: Context,
        weight: Float,
        endMargin: Int = 0
    ) {
        row.addView(
            button,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                weight
            ).apply {
                if (endMargin > 0) rightMargin = dp(context, endMargin)
            }
        )
    }
}
