package com.mahanshengzhi.indonesianengineer.ui

import android.content.Context
import android.content.res.ColorStateList
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
        setPadding(dp(context, 20), dp(context, 22), dp(context, 20), dp(context, 28))
    }

    fun scroll(context: Context, content: View): ScrollView =
        ScrollView(context).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(content)
        }

    fun title(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        textSize = 28f
        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        includeFontPadding = true
        typeface = Typeface.create("sans", Typeface.BOLD)
    }

    fun sectionTitle(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        textSize = 20f
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
            strokeWidth = if (strokeColor == null) 0 else dp(context, 1)
            strokeColor?.let { setStrokeColor(it) }
            setContentPadding(dp(context, 18), dp(context, 17), dp(context, 18), dp(context, 17))
            addView(content)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(context, 12)
            }
        }

    fun button(
        context: Context,
        text: String,
        onClick: () -> Unit
    ): MaterialButton = MaterialButton(context).apply {
        this.text = text
        setOnClickListener { onClick() }
        minHeight = dp(context, 48)
        cornerRadius = dp(context, 14)
        insetTop = 0
        insetBottom = 0
        setTextSize(15f)
        setAllCaps(false)
    }

    fun secondaryButton(
        context: Context,
        text: String,
        onClick: () -> Unit
    ): MaterialButton = button(context, text, onClick).apply {
        backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, R.color.warm_orange_light)
        )
        setTextColor(ContextCompat.getColor(context, R.color.warm_orange))
        strokeWidth = 0
    }

    fun outlineButton(
        context: Context,
        text: String,
        onClick: () -> Unit
    ): MaterialButton = button(context, text, onClick).apply {
        backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, android.R.color.transparent)
        )
        setTextColor(ContextCompat.getColor(context, R.color.teal_primary))
        strokeWidth = dp(context, 1)
        strokeColor = ColorStateList.valueOf(
            ContextCompat.getColor(context, R.color.teal_primary)
        )
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

    fun metric(
        context: Context,
        value: String,
        label: String,
        weight: Float = 1f
    ): LinearLayout {
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        box.addView(TextView(context).apply {
            text = value
            textSize = 22f
            setTextColor(ContextCompat.getColor(context, R.color.teal_deep))
            typeface = Typeface.create("sans", Typeface.BOLD)
        })
        box.addView(label(context, label).apply {
            setPadding(0, dp(context, 2), 0, 0)
        })
        box.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            weight
        )
        return box
    }

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
