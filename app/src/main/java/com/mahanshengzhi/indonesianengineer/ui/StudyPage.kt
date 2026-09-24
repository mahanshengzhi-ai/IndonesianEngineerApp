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
        dailyWords: Int,
        dailySentences: Int,
        dailyScene: Int,
        dailyQuiz: Int,
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
        root.addView(Ui.subtitle(
            context,
            "把今天的 15 分钟拆成很小的几步，每一步都能在工地和生活里派上用场。"
        ).apply {
            setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 16))
        })

        val today = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        today.addView(Ui.label(context, "今天的学习路径"))
        today.addView(Ui.body(
            context,
            "词汇 " + dailyWords.coerceAtMost(10) + "/10 · 句型 " +
                dailySentences.coerceAtMost(5) + "/5 · 场景 " +
                dailyScene.coerceAtMost(1) + "/1 · 挑战 " +
                (if (dailyQuiz > 0) 1 else 0) + "/1"
        ).apply {
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.teal_primary))
            setPadding(0, Ui.dp(context, 7), 0, 0)
        })
        root.addView(Ui.card(context, today))

        module(
            root,
            context,
            "01 · 发音入门",
            "26 个字母 + 常见组合。先训练耳朵，不靠中文谐音硬记。",
            repository.letters().size.toString() + " 个字母",
            onOpenPronunciation
        )
        module(
            root,
            context,
            "02 · 工程词汇",
            "按中文、印尼语和分类找词，只学今天真正需要的内容。",
            repository.vocabulary().count().toString() + " 个词",
            onOpenVocabulary
        )
        module(
            root,
            context,
            "03 · 核心句型",
            "40 个可以直接套进现场沟通的完整表达。",
            repository.sentences().size.toString() + " 个句型",
            onOpenSentences
        )
        module(
            root,
            context,
            "04 · 真实场景",
            "从第一次到工地到日报会议，用 A/B 对话把词真正用起来。",
            repository.scenes().size.toString() + " 个场景",
            onOpenScenes
        )

        val first = repository.vocabulary().firstOrNull()
        if (first != null) {
            val word = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            word.addView(Ui.label(context, "今天先学一个"))
            word.addView(Ui.body(context, first.indonesian).apply {
                textSize = 25f
                setTextColor(ContextCompat.getColor(context, R.color.teal_primary))
                setPadding(0, Ui.dp(context, 6), 0, 0)
            })
            word.addView(Ui.body(context, first.chinese).apply {
                setPadding(0, Ui.dp(context, 4), 0, Ui.dp(context, 10))
            })
            val available = hasAudio(first.indonesian)
            word.addView(if (available) {
                Ui.button(context, "先听一遍") { onPlay(first.indonesian) }
            } else {
                Ui.outlineButton(context, "暂无内置语音") {}
            })
            word.addView(Ui.outlineButton(context, "去词卡继续学") {
                onOpenVocabulary()
            }.apply {
                setPadding(0, Ui.dp(context, 8), 0, 0)
            })
            word.addView(Ui.button(context, "我会了") {
                onMarkLearned(first.id)
            }.apply {
                setPadding(0, Ui.dp(context, 8), 0, 0)
            })
            root.addView(Ui.card(context, word))
        }

        root.addView(
            Ui.secondaryButton(context, "完成后去做 10 题挑战") { onOpenPractice() }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    Ui.dp(context, 48)
                ).apply { bottomMargin = Ui.dp(context, 8) }
            }
        )

        return root
    }

    private fun module(
        root: LinearLayout,
        context: Context,
        title: String,
        desc: String,
        count: String,
        onClick: () -> Unit
    ) {
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        box.addView(Ui.label(context, title))
        box.addView(Ui.body(context, desc).apply {
            setPadding(0, Ui.dp(context, 5), 0, 0)
        })
        box.addView(Ui.label(context, count).apply {
            setPadding(0, Ui.dp(context, 9), 0, Ui.dp(context, 9))
        })
        box.addView(Ui.outlineButton(context, "打开") { onClick() })
        root.addView(Ui.card(context, box))
    }
}
