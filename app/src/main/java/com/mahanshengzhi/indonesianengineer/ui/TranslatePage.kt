package com.mahanshengzhi.indonesianengineer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.ProgressStore
import com.mahanshengzhi.indonesianengineer.data.TranslationRecord
import com.mahanshengzhi.indonesianengineer.translation.TranslationDirection
import com.mahanshengzhi.indonesianengineer.translation.TranslationEngine

object TranslatePage {
    private const val EMPTY_RESULT = "翻译结果会显示在这里"
    private const val MAX_INPUT_LENGTH = 1000

    fun build(
        context: Context,
        engine: TranslationEngine,
        store: ProgressStore
    ): LinearLayout {
        val root = Ui.page(context)

        root.addView(Ui.title(context, "翻译"))
        root.addView(Ui.subtitle(
            context,
            "中文 ↔ 印尼语文字翻译。首次使用准备模型，下载完成后可在设备端继续离线使用。"
        ).apply {
            setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 14))
        })

        var direction = TranslationDirection.CHINESE_TO_INDONESIAN

        val status = Ui.label(context, "模型状态：正在准备")
        status.setTextColor(ContextCompat.getColor(context, R.color.teal_primary))
        status.setPadding(0, Ui.dp(context, 2), 0, Ui.dp(context, 2))

        val zhToId = Ui.button(context, TranslationDirection.CHINESE_TO_INDONESIAN.label) {}
        val idToZh = Ui.outlineButton(context, TranslationDirection.INDONESIAN_TO_CHINESE.label) {}

        val directionHint = Ui.label(context, "选择方向")
        directionHint.setPadding(0, 0, 0, Ui.dp(context, 6))

        val directionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        directionRow.addView(
            zhToId,
            LinearLayout.LayoutParams(0, Ui.dp(context, 48), 1f)
        )
        directionRow.addView(
            idToZh,
            LinearLayout.LayoutParams(0, Ui.dp(context, 48), 1f).apply {
                leftMargin = Ui.dp(context, 8)
            }
        )

        val input = EditText(context).apply {
            hint = "例如：请确认混凝土浇筑时间"
            minLines = 5
            maxLines = 8
            gravity = Gravity.TOP
            textSize = 17f
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setPadding(
                Ui.dp(context, 14),
                Ui.dp(context, 13),
                Ui.dp(context, 14),
                Ui.dp(context, 13)
            )
        }

        val inputCount = Ui.label(context, "0 / " + MAX_INPUT_LENGTH)
        inputCount.gravity = Gravity.END

        val result = TextView(context).apply {
            text = EMPTY_RESULT
            textSize = 19f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            includeFontPadding = true
            setLineSpacing(0f, 1.2f)
            minLines = 4
            typeface = Typeface.create("sans", Typeface.NORMAL)
        }

        fun setStatus(message: String, positive: Boolean) {
            status.text = "模型状态：" + message
            status.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (positive) R.color.teal_primary else R.color.warm_orange
                )
            )
        }

        fun styleDirectionButtons() {
            val selected = direction == TranslationDirection.CHINESE_TO_INDONESIAN
            val primary = ContextCompat.getColor(context, R.color.teal_primary)
            val transparent = ContextCompat.getColor(context, android.R.color.transparent)

            zhToId.backgroundTintList = ColorStateList.valueOf(
                if (selected) primary else transparent
            )
            idToZh.backgroundTintList = ColorStateList.valueOf(
                if (selected) transparent else primary
            )
            zhToId.strokeWidth = if (selected) 0 else Ui.dp(context, 1)
            idToZh.strokeWidth = if (selected) Ui.dp(context, 1) else 0
            zhToId.textColorForState(selected, context)
            idToZh.textColorForState(!selected, context)
        }

        fun clearAll() {
            input.setText("")
            result.text = EMPTY_RESULT
            inputCount.text = "0 / " + MAX_INPUT_LENGTH
        }

        fun prepareCurrentDirection() {
            setStatus("正在准备 " + direction.label, true)
            engine.prepare(direction)
            engine.download(
                onReady = {
                    setStatus("已就绪 · 可离线翻译", true)
                },
                onError = {
                    setStatus("未准备好 · 首次使用请连接网络", false)
                }
            )
        }

        lateinit var translateButton: MaterialButton
        translateButton = Ui.button(context, "翻译") {
            val text = input.text?.toString()?.trim().orEmpty()
            if (text.isEmpty()) {
                Toast.makeText(context, "先输入一段文字", Toast.LENGTH_SHORT).show()
                return@button
            }

            setStatus("正在翻译", true)
            translateButton.isEnabled = false
            engine.prepare(direction)

            engine.download(
                onReady = {
                    engine.translate(
                        text,
                        onSuccess = { translated ->
                            result.text = translated
                            store.incrementTranslation()
                            store.recordTranslation(direction, text, translated)
                            setStatus("已就绪 · 可离线翻译", true)
                            translateButton.isEnabled = true
                        },
                        onError = {
                            setStatus("翻译失败，请稍后再试", false)
                            translateButton.isEnabled = true
                        }
                    )
                },
                onError = {
                    setStatus("模型下载失败，请连接网络后重试", false)
                    translateButton.isEnabled = true
                }
            )
        }

        val clearButton = Ui.outlineButton(context, "清空") {
            clearAll()
        }

        val copyButton = Ui.secondaryButton(context, "复制结果") {
            val text = result.text?.toString().orEmpty()
            if (text.isBlank() || text == EMPTY_RESULT) {
                Toast.makeText(context, "还没有可复制的翻译结果", Toast.LENGTH_SHORT).show()
                return@secondaryButton
            }

            val clipboard =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(
                ClipData.newPlainText("translation", text)
            )
            Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
        }

        zhToId.setOnClickListener {
            if (direction != TranslationDirection.CHINESE_TO_INDONESIAN) {
                direction = TranslationDirection.CHINESE_TO_INDONESIAN
                styleDirectionButtons()
                result.text = EMPTY_RESULT
                prepareCurrentDirection()
            }
        }

        idToZh.setOnClickListener {
            if (direction != TranslationDirection.INDONESIAN_TO_CHINESE) {
                direction = TranslationDirection.INDONESIAN_TO_CHINESE
                styleDirectionButtons()
                result.text = EMPTY_RESULT
                prepareCurrentDirection()
            }
        }

        input.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                val currentLength = s?.length ?: 0
                inputCount.text = currentLength.toString() + " / " + MAX_INPUT_LENGTH
            }

            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })

        root.addView(Ui.card(context, status))
        root.addView(directionHint)
        root.addView(directionRow)
        root.addView(Ui.card(context, input))
        root.addView(inputCount)

        root.addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(
                    translateButton,
                    LinearLayout.LayoutParams(0, Ui.dp(context, 48), 1f)
                )
                addView(
                    clearButton,
                    LinearLayout.LayoutParams(0, Ui.dp(context, 48), 1f).apply {
                        leftMargin = Ui.dp(context, 8)
                    }
                )
            }.apply {
                setPadding(0, Ui.dp(context, 8), 0, Ui.dp(context, 12))
            }
        )

        val resultTitle = Ui.label(context, "翻译结果")
        root.addView(resultTitle)
        root.addView(Ui.card(context, result))
        root.addView(copyButton.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(context, 48)
            ).apply {
                bottomMargin = Ui.dp(context, 10)
            }
        })

        val history = store.recentTranslationRecords()
        if (history.isNotEmpty()) {
            root.addView(Ui.sectionTitle(context, "最近 6 条").apply {
                setPadding(0, Ui.dp(context, 6), 0, Ui.dp(context, 4))
            })
            root.addView(Ui.label(context, "点击历史记录可重新查看原文和译文").apply {
                setPadding(0, 0, 0, Ui.dp(context, 8))
            })

            history.forEach { record ->
                root.addView(
                    historyCard(context, record, input, result) {
                        direction = record.direction
                        styleDirectionButtons()
                        prepareCurrentDirection()
                    }
                )
            }
        }

        styleDirectionButtons()
        prepareCurrentDirection()
        return root
    }

    private fun historyCard(
        context: Context,
        record: TranslationRecord,
        input: EditText,
        result: TextView,
        onDirectionChanged: () -> Unit
    ): android.view.View {
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setOnClickListener {
                input.setText(record.input)
                input.setSelection(input.text?.length ?: 0)
                result.text = record.output
                onDirectionChanged()
            }
        }

        box.addView(Ui.label(context, record.direction.label))
        box.addView(Ui.body(context, record.input).apply {
            setPadding(0, Ui.dp(context, 5), 0, 0)
        })
        box.addView(Ui.body(context, "→ " + record.output).apply {
            textSize = 14.5f
            setTextColor(ContextCompat.getColor(context, R.color.teal_primary))
            setPadding(0, Ui.dp(context, 5), 0, 0)
        })

        return Ui.card(context, box)
    }

    private fun MaterialButton.textColorForState(selected: Boolean, context: Context) {
        setTextColor(
            ContextCompat.getColor(
                context,
                if (selected) android.R.color.white else R.color.teal_primary
            )
        )
    }
}
