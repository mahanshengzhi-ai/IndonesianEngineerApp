package com.mahanshengzhi.indonesianengineer.ui

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.ProgressStore

object PracticePage {
    fun build(
        context: Context,
        store: ProgressStore,
        onStartFlashcards: () -> Unit,
        onStartQuiz: () -> Unit
    ): LinearLayout {
        val root = Ui.page(context)
        root.addView(Ui.title(context, "练习"))
        root.addView(Ui.subtitle(
            context,
            "少做一点，但每一题都把今天学过的内容重新调出来。"
        ).apply {
            setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 18))
        })

        val summary = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        summary.addView(Ui.metric(context, store.dailyFlashcards().coerceAtMost(10).toString() + "/10", "今日闪卡"))
        summary.addView(Ui.metric(context, store.bestQuiz().toString() + "/10", "最佳成绩"))
        summary.addView(Ui.metric(context, store.dailyQuiz().toString(), "今日挑战"))
        root.addView(Ui.card(context, summary))

        val flash = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        flash.addView(Ui.label(context, "01 · 闪卡复习"))
        flash.addView(Ui.sectionTitle(context, "把词从记忆里捞出来").apply {
            setPadding(0, Ui.dp(context, 5), 0, 0)
        })
        flash.addView(Ui.body(
            context,
            "先看印尼语，自己想中文；不确定再翻开答案。熟练后再标记“认识了”。"
        ).apply {
            setPadding(0, Ui.dp(context, 5), 0, Ui.dp(context, 12))
        })
        flash.addView(Ui.button(context, "开始闪卡") { onStartFlashcards() })
        root.addView(Ui.card(context, flash, ContextCompat.getColor(context, R.color.teal_primary)))

        val quiz = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        quiz.addView(Ui.label(context, "02 · 10 题挑战"))
        quiz.addView(Ui.sectionTitle(context, "混合词汇与句型").apply {
            setPadding(0, Ui.dp(context, 5), 0, 0)
        })
        quiz.addView(Ui.body(
            context,
            "5 题词汇 + 5 题句型，正反方向混合。每题答完马上知道对错。"
        ).apply {
            setPadding(0, Ui.dp(context, 5), 0, Ui.dp(context, 12))
        })
        quiz.addView(Ui.secondaryButton(context, "开始挑战") { onStartQuiz() })
        root.addView(Ui.card(context, quiz))

        root.addView(Ui.card(
            context,
            Ui.body(context, "练习原则：先回忆，再看答案；错题不要急着跳过，重新读一遍印尼语。")
        ))

        return root
    }
}
