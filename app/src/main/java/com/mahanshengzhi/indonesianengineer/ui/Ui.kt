package com.mahanshengzhi.indonesianengineer.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.mahanshengzhi.indonesianengineer.R

object Ui {
    fun page(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(context, 24), dp(context, 26), dp(context, 24), dp(context, 28))
    }

    fun scroll(context: Context, content: View): ScrollView =
        ScrollView(context).apply {
            isFillViewport = true
            addView(content)
        }

    fun title(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        textSize = 28f
        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        includeFontPadding = true
        typeface = Typeface.create("sans", Typeface.BOLD)
    }

    fun subtitle(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        textSize = 15.5f
        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        includeFontPadding = true
        setLineSpacing(0f, 1.2f)
    }

    fun card(context: Context, content: View, strokeColor: Int? = null): MaterialCardView =
        MaterialCardView(context).apply {
            radius = dp(context, 18).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            strokeColor?.let { setStrokeColor(it) }
            setContentPadding(dp(context, 20), dp(context, 18), dp(context, 20), dp(context, 18))
            addView(content)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(context, 14) }
        }

    fun button(context: Context, text: String, onClick: () -> Unit): MaterialButton =
        MaterialButton(context).apply {
            this.text = text
            setOnClickListener { onClick() }
            minHeight = dp(context, 50)
            cornerRadius = dp(context, 14)
            insetTop = 0
            insetBottom = 0
            setTextSize(15f)
        }

    fun body(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        textSize = 16f
        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        includeFontPadding = true
        setLineSpacing(0f, 1.2f)
    }

    fun label(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        textSize = 12.5f
        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        includeFontPadding = true
    }

    fun row(context: Context, gapDp: Int = 10): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, 0, 0, dp(context, gapDp))
    }

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
