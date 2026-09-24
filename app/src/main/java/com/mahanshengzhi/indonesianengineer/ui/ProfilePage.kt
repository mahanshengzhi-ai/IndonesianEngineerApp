package com.mahanshengzhi.indonesianengineer.ui

import android.content.Context
import android.widget.LinearLayout
import com.mahanshengzhi.indonesianengineer.data.ProgressStore

object ProfilePage {
    fun build(context: Context, store: ProgressStore): LinearLayout {
        val root = Ui.page(context)
        root.addView(Ui.title(context, "我的"))
        root.addView(Ui.subtitle(context, "把学习变成每天都能完成的一小段。").apply {
            setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 18))
        })

        add(root, context, "累计掌握", store.learnedTotal().toString() + " / 3600 个词")
        add(root, context, "连续学习", store.streak().toString() + " 天")
        add(root, context, "累计打卡", store.checkins().toString() + " 次")
        add(root, context, "今日学习", store.dailyWords().toString() + " 词 · 听读 " + store.dailyAudio() + " 次 · 测验 " + store.dailyQuiz() + " 次 · 翻译 " + store.dailyTranslation() + " 句")
        add(root, context, "学习建议", "10 个词 + 5 个句型 + 1 个场景 + 1 组 10 题挑战")

        return root
    }

    private fun add(root: LinearLayout, context: Context, title: String, value: String) {
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        box.addView(Ui.label(context, title))
        box.addView(Ui.body(context, value).apply {
            textSize = 18f
            setPadding(0, Ui.dp(context, 6), 0, 0)
        })
        root.addView(Ui.card(context, box))
    }
}
