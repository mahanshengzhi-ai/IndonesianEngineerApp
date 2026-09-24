package com.mahanshengzhi.indonesianengineer.ui

import android.content.Context
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.LearningRepository

object PronunciationPage {
    fun build(
        context: Context,
        repository: LearningRepository,
        hasAudio: (String) -> Boolean,
        onPlay: (String) -> Unit
    ): LinearLayout {
        val root = Ui.page(context)
        root.addView(Ui.title(context, "发音入门"))
        root.addView(Ui.subtitle(context, "先把耳朵训练出来。这里不做中文谐音，只给真实字母名、发音提示、示例和可用真人音频。").apply {
            setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 18))
        })

        val rules = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        rules.addView(Ui.label(context, "中国用户重点"))
        rules.addView(Ui.body(context, "c / j / g / ng / ny / sy / kh / h / r / e").apply {
            textSize = 19f
            setPadding(0, Ui.dp(context, 6), 0, 0)
        })
        rules.addView(Ui.body(context, "注意 e 的实际读法会因词形变化；r 要靠听和模仿建立感觉。").apply {
            setPadding(0, Ui.dp(context, 7), 0, 0)
        })
        root.addView(Ui.card(context, rules))

        repository.letters().forEach { letter ->
            val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            box.addView(Ui.label(context, letter.letter + " · " + letter.name))
            box.addView(Ui.body(context, letter.hint).apply {
                setPadding(0, Ui.dp(context, 5), 0, 0)
            })
            box.addView(Ui.body(context, "示例： " + letter.example).apply {
                setTextColor(ContextCompat.getColor(context, R.color.teal_primary))
                setPadding(0, Ui.dp(context, 5), 0, 0)
            })

            val available = hasAudio(letter.example)
            val audio = Ui.button(context, if (available) "听示例" else "暂无内置语音") {
                if (available) onPlay(letter.example)
            }
            audio.isEnabled = available
            box.addView(audio.apply { setPadding(0, Ui.dp(context, 10), 0, 0) })
            root.addView(Ui.card(context, box))
        }

        return root
    }
}
