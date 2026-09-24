package com.mahanshengzhi.indonesianengineer.ui

import android.content.Context
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.LearningRepository

object StudyPage {
    fun build(
        context: Context,
        repository: LearningRepository,
        onPlay: (String) -> Unit,
        onMarkLearned: (String) -> Unit
    ): LinearLayout {
        val root = Ui.page(context)
        root.addView(Ui.title(context, "学习"))
        root.addView(Ui.subtitle(context, "先建立发音感觉，再进入词汇、句型和真实场景。").apply {
            setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 18))
        })

        addModule(root, context, "01  发音入门", "26 个字母、常见组合与印尼语发音规律", repository.letters().size.toString() + " 个字母")
        addModule(root, context, "02  核心词汇", "按分类和场景学习，不把全部词一次性堆出来", repository.vocabulary().count().toString() + " 个词")
        addModule(root, context, "03  常用句型", "40 句现场可以直接套用的表达", repository.sentences().size.toString() + " 个句型")
        addModule(root, context, "04  真实场景", "从第一次到工地到日报会议，完整学习交流过程", repository.scenes().size.toString() + " 个场景")

        val tip = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        tip.addView(Ui.label(context, "学习顺序"))
        tip.addView(Ui.body(context, "字母发音 → 核心词汇 → 核心句型 → 真实场景").apply { textSize = 18f })
        tip.addView(Ui.body(context, "每次只学一小组，学完马上进入回忆和应用。").apply {
            setPadding(0, Ui.dp(context, 8), 0, 0)
        })
        root.addView(Ui.card(context, tip))

        val first = repository.vocabulary().firstOrNull()
        if (first != null) {
            val word = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            word.addView(Ui.label(context, "今日先学一个"))
            word.addView(Ui.body(context, first.indonesian).apply {
                textSize = 26f
                setTextColor(ContextCompat.getColor(context, R.color.teal_primary))
                setPadding(0, Ui.dp(context, 7), 0, 0)
            })
            word.addView(Ui.body(context, first.chinese).apply { setPadding(0, Ui.dp(context, 4), 0, 0) })
            word.addView(Ui.button(context, if (first.indonesian.isNotBlank()) "听一下" else "暂无内置语音") {
                onPlay(first.indonesian)
                onMarkLearned(first.id)
            }).apply { setPadding(0, Ui.dp(context, 12), 0, 0) })
            root.addView(Ui.card(context, word))
        }
        return root
    }

    private fun addModule(root: LinearLayout, context: Context, title: String, desc: String, count: String) {
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        box.addView(Ui.label(context, title))
        box.addView(Ui.body(context, desc).apply { setPadding(0, Ui.dp(context, 5), 0, 0) })
        box.addView(Ui.label(context, count).apply { setPadding(0, Ui.dp(context, 9), 0, 0) })
        root.addView(Ui.card(context, box))
    }
}
