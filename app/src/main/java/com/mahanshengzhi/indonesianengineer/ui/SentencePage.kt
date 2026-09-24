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
        onPlay: (String) -> Unit,
        onSentenceDone: () -> Unit
    ): LinearLayout {
        val root = Ui.page(context)
        root.addView(Ui.title(context, "核心句型"))
        root.addView(Ui.subtitle(
            context,
            "40 个可以直接搬到现场沟通里的完整表达。先听一句，再自己说一句。"
        ).apply {
            setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 18))
        })

        repository.sentences().forEachIndexed { index, sentence ->
            val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            box.addView(Ui.label(
                context,
                String.format("%02d", index + 1) + " · " + sentence.scene
            ))
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
            val audio = if (available) {
                Ui.button(context, "听一下") { onPlay(sentence.indonesian) }
            } else {
                Ui.outlineButton(context, "暂无内置语音") {}
            }
            audio.isEnabled = available
            box.addView(audio.apply {
                setPadding(0, Ui.dp(context, 12), 0, 0)
            })

            val done = Ui.secondaryButton(context, "我会用这句") {}
            done.setOnClickListener {
                onSentenceDone()
                done.text = "已记录今天练习"
                done.isEnabled = false
            }
            box.addView(done.apply {
                setPadding(0, Ui.dp(context, 8), 0, 0)
            })

            root.addView(Ui.card(context, box))
        }

        return root
    }
}
