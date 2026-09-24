package com.mahanshengzhi.indonesianengineer.ui

import android.content.Context
import android.widget.LinearLayout
import com.mahanshengzhi.indonesianengineer.data.ProgressStore

object PracticePage {
    fun build(context: Context, store: ProgressStore, onMarkQuiz: () -> Unit): LinearLayout {
        val root = Ui.page(context)
        root.addView(Ui.title(context, "练习"))
        root.addView(Ui.subtitle(context, "把今天学过的内容再拿出来一次，记忆会更牢。").apply {
            setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 18))
        })

        add(root, context, "闪卡复习", "看印尼语 → 回忆中文 → 翻回去确认 → 标记掌握")
        add(root, context, "10 题挑战", "中文→印尼语、印尼语→中文、场景理解、句型选择")
        add(root, context, "发音辨认", "只播放预录音频，让耳朵先建立声音和词义的连接")
        add(root, context, "最佳成绩", "当前最佳成绩：" + store.bestQuiz() + " / 10")

        root.addView(Ui.card(context, Ui.button(context, "模拟完成一组 10 题") {
            onMarkQuiz()
        }))
        return root
    }

    private fun add(root: LinearLayout, context: Context, title: String, desc: String) {
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        box.addView(Ui.label(context, title))
        box.addView(Ui.body(context, desc).apply { setPadding(0, Ui.dp(context, 6), 0, 0) })
        root.addView(Ui.card(context, box))
    }
}
