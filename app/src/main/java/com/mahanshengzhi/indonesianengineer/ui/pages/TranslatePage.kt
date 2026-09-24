package com.mahanshengzhi.indonesianengineer.ui.pages

import android.text.InputType
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mahanshengzhi.indonesianengineer.MainActivity
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.ProgressStore
import com.mahanshengzhi.indonesianengineer.translation.TranslationEngine
import com.mahanshengzhi.indonesianengineer.ui.UiKit

class TranslatePage(
    private val activity: MainActivity,
    private val engine: TranslationEngine,
    private val progressStore: ProgressStore
) {
    private lateinit var input: TextInputEditText
    private lateinit var result: TextView
    private lateinit var status: TextView
    private lateinit var translateButton: MaterialButton
    private var chineseToIndonesian = true

    fun build(): View {
        val pair = UiKit.pageScroll(activity)
        val body = pair.second

        body.addView(UiKit.title(activity, "翻译"))
        body.addView(
            UiKit.subtitle(
                activity,
                "输入文字即可。第一次使用会下载语言模型，完成后可以离线翻译。"
            )
        )
        UiKit.addGap(body, activity, 12)

        val group = MaterialButtonToggleGroup(activity).apply {
            isSingleSelection = true
        }

        val zh = UiKit.secondaryButton(activity, "中文 → 印尼语").apply {
            id = View.generateViewId()
        }
        val id = UiKit.secondaryButton(activity, "印尼语 → 中文").apply {
            id = View.generateViewId()
        }

        group.addView(zh)
        group.addView(id)
        group.check(zh.id)
        body.addView(group)

        UiKit.addGap(body, activity, 10)
        status = UiKit.muted(activity, "模型状态：未准备", 13f)
        body.addView(status)
        UiKit.addGap(body, activity, 8)

        val inputLayout = TextInputLayout(activity).apply {
            hint = "输入要翻译的文字"
        }

        input = TextInputEditText(activity).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 4
            maxLines = 7
            gravity = android.view.Gravity.TOP
        }
        inputLayout.addView(input)
        body.addView(inputLayout)

        translateButton = UiKit.primaryButton(activity, "翻译")
        translateButton.isEnabled = false
        body.addView(translateButton)

        val resultCard = UiKit.card(activity, R.color.card_green)
        val resultContent = UiKit.cardContent(activity)
        result = UiKit.body(activity, "结果会显示在这里。", 20f)
        resultContent.addView(result)

        val copy = UiKit.secondaryButton(activity, "复制结果")
        copy.setOnClickListener {
            val manager = activity.getSystemService(
                android.content.Context.CLIPBOARD_SERVICE
            ) as android.content.ClipboardManager
            manager.setPrimaryClip(
                android.content.ClipData.newPlainText("translation", result.text)
            )
        }
        resultContent.addView(copy)
        resultCard.addView(resultContent)
        body.addView(resultCard)

        val prepare = UiKit.warmButton(activity, "准备离线模型")
        prepare.setOnClickListener { prepareModel() }
        body.addView(prepare)

        zh.setOnClickListener {
            chineseToIndonesian = true
            prepareState("中文 → 印尼语")
        }

        id.setOnClickListener {
            chineseToIndonesian = false
            prepareState("印尼语 → 中文")
        }

        translateButton.setOnClickListener {
            val text = input.text?.toString()?.trim().orEmpty()
            if (text.isBlank()) return@setOnClickListener

            status.text = "模型状态：翻译中…"
            engine.translate(
                text,
                onSuccess = {
                    result.text = it
                    progressStore.incrementTranslation()
                    progressStore.recordTranslation(text)
                    status.text = "模型状态：已就绪 · 可离线"
                },
                onError = {
                    status.text = "模型状态：失败，请重新准备模型"
                }
            )
        }

        addHistory(body)
        prepareState("中文 → 印尼语")
        return pair.first
    }

    private fun prepareState(direction: String) {
        translateButton.isEnabled = false
        status.text = "模型状态：" + direction + " · 未准备"
        engine.close()

        if (chineseToIndonesian) {
            engine.prepareChineseToIndonesian()
        } else {
            engine.prepareIndonesianToChinese()
        }
    }

    private fun prepareModel() {
        status.text = "模型状态：正在准备…"
        translateButton.isEnabled = false

        engine.download(
            onReady = {
                status.text = "模型状态：已就绪 · 可离线"
                translateButton.isEnabled = true
            },
            onError = {
                status.text = "模型状态：准备失败，请重试"
            }
        )
    }

    private fun addHistory(parent: LinearLayout) {
        val card = UiKit.card(activity)
        val content = UiKit.cardContent(activity)
        content.addView(UiKit.section(activity, "最近 6 条"))
        UiKit.addGap(content, activity, 6)

        val history = progressStore.recentTranslations().take(6)
        if (history.isEmpty()) {
            content.addView(UiKit.muted(activity, "还没有翻译记录。", 14f))
        } else {
            history.forEach {
                content.addView(UiKit.muted(activity, it, 14f))
                UiKit.addGap(content, activity, 4)
            }
        }

        card.addView(content)
        parent.addView(card)
    }
}
