package com.mahanshengzhi.indonesianengineer.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.ProgressStore

object ProfilePage {
    private const val VOCABULARY_TOTAL = 3281

    fun build(context: Context, store: ProgressStore): LinearLayout {
        val root = Ui.page(context)

        root.addView(Ui.title(context, "我的"))
        root.addView(Ui.subtitle(
            context,
            "看见已经学会的内容，也看见下一步该做什么。"
        ).apply {
            setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 18))
        })

        val learned = store.learnedTotal().coerceIn(0, VOCABULARY_TOTAL)
        val percent = learned * 100 / VOCABULARY_TOTAL

        val hero = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val left = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        left.addView(Ui.label(context, "工程词汇掌握"))
        left.addView(Ui.body(context, learned.toString() + " / " + VOCABULARY_TOTAL).apply {
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(context, R.color.teal_deep))
            setPadding(0, Ui.dp(context, 4), 0, 0)
        })
        left.addView(Ui.label(context, percent.toString() + "% 已掌握").apply {
            setPadding(0, Ui.dp(context, 3), 0, 0)
        })
        hero.addView(left)

        hero.addView(
            Ui.secondaryButton(context, "连续 " + store.streak() + " 天") {}
                .apply {
                    minWidth = Ui.dp(context, 86)
                    minHeight = Ui.dp(context, 42)
                }
        )
        root.addView(Ui.card(context, hero, ContextCompat.getColor(context, R.color.teal_light)))

        val progressBar = ProgressBar(
            context,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = VOCABULARY_TOTAL
            progress = learned
            isIndeterminate = false
        }
        root.addView(Ui.card(context, progressBar))

        val today = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        today.addView(Ui.label(context, "今天"))
        val row1 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, Ui.dp(context, 9), 0, 0)
        }
        row1.addView(Ui.metric(context, store.dailyWords().toString(), "新词"))
        row1.addView(Ui.metric(context, store.dailyFlashcards().toString(), "闪卡"))
        row1.addView(Ui.metric(context, store.dailySentences().toString(), "句型"))
        row1.addView(Ui.metric(context, store.dailyScene().toString(), "场景"))
        today.addView(row1)
        today.addView(Ui.body(
            context,
            "听力 " + store.dailyAudio() +
                " 次 · 挑战 " + store.dailyQuiz() +
                " 次 · 翻译 " + store.dailyTranslation() + " 句"
        ).apply {
            setPadding(0, Ui.dp(context, 12), 0, 0)
        })
        root.addView(Ui.card(context, today))

        val cumulative = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        cumulative.addView(Ui.metric(context, store.checkins().toString(), "累计打卡"))
        cumulative.addView(Ui.metric(context, store.bestQuiz().toString(), "最佳挑战"))
        cumulative.addView(Ui.metric(context, store.learnedTotal().toString(), "累计掌握"))
        root.addView(Ui.card(context, cumulative))

        val suggestion = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        suggestion.addView(Ui.label(context, "下一步"))
        suggestion.addView(Ui.sectionTitle(
            context,
            "10 个词 + 5 个句型 + 1 个场景"
        ).apply {
            setPadding(0, Ui.dp(context, 5), 0, Ui.dp(context, 4))
        })
        suggestion.addView(Ui.body(
            context,
            "完成后做一组 10 题挑战。今天没做完也没关系，明天从剩下的一小步继续。"
        ))
        root.addView(Ui.card(context, suggestion))

        return root
    }
}
