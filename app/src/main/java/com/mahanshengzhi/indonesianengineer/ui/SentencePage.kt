package com.mahanshengzhi.indonesianengineer.ui

import android.content.Context
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.LearningRepository

object SentencePage {
    fun build(
        context: Context,
        repository: LearningRepository,
        hasAudio: (String) -> Boolean,
        onPlay: (String) -> Unit
    ): LinearLayout {
        val root = Ui.page(context)
        root.addView(Ui.title(context, "句型卡"))
        root.addView(Ui.subtitle(context, "40 个可以直接搬到现场沟通里的完整表达。").apply {
            setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 18))
        })

        repository.sentences().forEachIndexed { index, sentence ->
            val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            box.addView(Ui.label(context, String.format("%02d", index + 1) + " · " + sentence.scene))
            box.addView(Ui.body(context, sentence.indonesian).apply {
                textSize = 21f
                setTextColor(ContextCompat.getColor(context, R.color.teal_primary))
                setPadding(0, Ui.dp(context, 6), 0, 0)
            })
            box.addView(Ui.body(context, sentence.chinese).apply {
                setPadding(0, Ui.dp(context, 4), 0, 0)
            })
            if (sentence.explanation.isNotBlank()) {
                box.addView(Ui.label(context, sentence.explanation).apply {
                    setPadding(0, Ui.dp(context, 7), 0, 0)
                })
            }

            val available = hasAudio(sentence.indonesian)
            val audio = Ui.button(context, if (available) "听一下" else "暂无内置语音") {
                if (available) onPlay(sentence.indonesian)
            }
            audio.isEnabled = available
            box.addView(audio.apply { setPadding(0, Ui.dp(context, 12), 0, 0) })
            root.addView(Ui.card(context, box))
        }
        return root
    }
}
