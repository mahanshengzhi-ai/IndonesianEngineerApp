package com.mahanshengzhi.indonesianengineer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.translation.TranslationEngine
import com.mahanshengzhi.indonesianengineer.data.ProgressStore

object TranslatePage {
    fun build(
        context: Context,
        engine: TranslationEngine,
        store: ProgressStore
    ): LinearLayout {
        val root = Ui.page(context)
        root.addView(Ui.title(context, "翻译"))
        root.addView(Ui.subtitle(context, "中文 ↔ 印尼语文字翻译。第一次使用需要准备模型，之后可离线。").apply {
            setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 18))
        })

        var toIndonesian = true
        val source = EditText(context).apply {
            hint = "输入中文或印尼语"
            minLines = 4
            gravity = Gravity.TOP
            setTextSize(17f)
            includeFontPadding = true
            setPadding(Ui.dp(context, 14), Ui.dp(context, 12), Ui.dp(context, 14), Ui.dp(context, 12))
        }
        val result = TextView(context).apply {
            text = "翻译结果会显示在这里"
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            includeFontPadding = true
            setLineSpacing(0f, 1.2f)
        }
        val status = Ui.label(context, "模型状态：准备中")

        val direction = MaterialButton(context).apply {
            text = "中文 → 印尼语"
            setOnClickListener {
                toIndonesian = !toIndonesian
                text = if (toIndonesian) "中文 → 印尼语" else "印尼语 → 中文"
                if (toIndonesian) engine.prepareChineseToIndonesian() else engine.prepareIndonesianToChinese()
                status.text = "模型状态：需要检查"
            }
        }

        val translate = Ui.button(context, "翻译") {
            val input = source.text.toString().trim()
            if (input.isEmpty()) {
                Toast.makeText(context, "先输入一段文字", Toast.LENGTH_SHORT).show()
                return@button
            }
            status.text = "模型状态：准备模型"
            engine.download(
                onReady = {
                    status.text = "模型状态：已就绪 · 离线可用"
                    engine.translate(
                        input,
                        onSuccess = {
                            result.text = it
                            store.incrementTranslation()
                            store.recordTranslation(input)
                        },
                        onError = { status.text = "模型状态：翻译失败" }
                    )
                },
                onError = { status.text = "模型状态：模型下载失败，请检查网络" }
            )
        }

        engine.prepareChineseToIndonesian()
        root.addView(Ui.card(context, status))
        root.addView(Ui.card(context, direction))
        root.addView(Ui.card(context, source))
        root.addView(Ui.card(context, result))

        val actions = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(translate, LinearLayout.LayoutParams(0, Ui.dp(context, 50), 1f).apply {
            rightMargin = Ui.dp(context, 8)
        })
        actions.addView(Ui.button(context, "复制结果") {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("translation", result.text))
            Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
        }, LinearLayout.LayoutParams(0, Ui.dp(context, 50), 1f))
        root.addView(actions)

        val history = store.recentTranslations()
        if (history.isNotEmpty()) {
            val h = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            h.addView(Ui.label(context, "最近 6 条"))
            h.addView(Ui.body(context, history.joinToString("\n\n")).apply { setPadding(0, Ui.dp(context, 6), 0, 0) })
            root.addView(Ui.card(context, h))
        }

        return root
    }
}
