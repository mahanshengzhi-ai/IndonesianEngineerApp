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
        hasAudio: (String) -> Boolean,
        onPlay: (String) -> Unit,
        onOpenPronunciation: () -> Unit,
        onOpenVocabulary: () -> Unit,
        onOpenSentences: () -> Unit,
        onOpenScenes: () -> Unit,
        onOpenPractice: () -> Unit,
        onMarkLearned: (String) -> Unit
    ): LinearLayout {
        val root = Ui.page(context)
        root.addView(Ui.title(context, "学习"))
        root.addView(Ui.subtitle(context, "先建立发音感觉，再进入词汇、句型和真实场景。").apply {
            setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 18))
        })

        module(root, context, "01  发音入门", "26 个字母、常见组合与印尼语发音规律", repository.letters().size.toString() + " 个字母", onOpenPronunciation)
        module(root, context, "02  核心词汇", "打开词卡，搜索你今天真正要用的词", repository.vocabulary().count().toString() + " 个词", onOpenVocabulary)
        module(root, context, "03  常用句型", "40 句现场可以直接套用的表达", repository.sentences().size.toString() + " 个句型", onOpenSentences)
        module(root, context, "04  真实场景", "从第一次到工地到日报会议，完整走完一次交流", repository.scenes().size.toString() + " 个场景", onOpenScenes)

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
            val available = hasAudio(first.indonesian)
            val audioButton = Ui.button(context, if (available) "听一下" else "暂无内置语音") {
                if (available) onPlay(first.indonesian)
            }
            audioButton.isEnabled = available
            word.addView(audioButton.apply { setPadding(0, Ui.dp(context, 12), 0, 0) })
            word.addView(Ui.button(context, "我会了") {
                onMarkLearned(first.id)
            })
            root.addView(Ui.card(context, word))
        }

        root.addView(Ui.card(context, Ui.button(context, "去练习") { onOpenPractice() }))
        return root
    }

    private fun module(
        root: LinearLayout,
        context: Context,
        title: String,
        desc: String,
        count: String,
        onClick: (() -> Unit)? = null
    ) {
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        box.addView(Ui.label(context, title))
        box.addView(Ui.body(context, desc).apply { setPadding(0, Ui.dp(context, 5), 0, 0) })
        box.addView(Ui.label(context, count).apply { setPadding(0, Ui.dp(context, 9), 0, 0) })
        if (onClick != null) {
            box.addView(Ui.button(context, "打开") { onClick() }.apply {
                setPadding(0, Ui.dp(context, 10), 0, 0)
            })
        }
        root.addView(Ui.card(context, box))
    }
}
