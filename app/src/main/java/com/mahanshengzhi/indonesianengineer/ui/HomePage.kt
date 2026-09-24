package com.mahanshengzhi.indonesianengineer.ui

import android.content.Context
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.mahanshengzhi.indonesianengineer.R

object HomePage {
    fun build(
        context: Context,
        learned: Int,
        streak: Int,
        dailyWords: Int,
        dailyAudio: Int,
        dailyQuiz: Int,
        dailyTranslation: Int,
        onStart: () -> Unit,
        onCheckIn: () -> Unit,
        onOpenStudy: () -> Unit
    ): LinearLayout {
        val root = Ui.page(context)

        root.addView(Ui.label(context, "BAHASA INDONESIA · ENGINEER"))
        root.addView(Ui.title(context, "印尼语工程员学习").apply {
            setPadding(0, Ui.dp(context, 5), 0, 0)
        })
        root.addView(Ui.subtitle(context, "每天 15 分钟，先听懂，再开口").apply {
            setPadding(0, Ui.dp(context, 6), 0, Ui.dp(context, 22))
        })

        val plan = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        plan.addView(Ui.label(context, "今天学什么？"))
        plan.addView(Ui.body(context, "10 个新词 · 5 个句型 · 1 个场景 · 10 题挑战").apply {
            textSize = 19f
            setPadding(0, Ui.dp(context, 6), 0, Ui.dp(context, 14))
        })
        plan.addView(Ui.button(context, "开始今天学习") { onStart() })
        root.addView(Ui.card(context, plan, ContextCompat.getColor(context, R.color.teal_primary)))

        val progress = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        progress.addView(Ui.label(context, "今日进度"))
        progress.addView(Ui.body(context, "词汇 " + dailyWords.coerceAtMost(10) + " / 10    句型 0 / 5\n场景 0 / 1    测试 " + if (dailyQuiz > 0) 1 else 0 + " / 1"))
        progress.addView(Ui.body(context, "累计掌握 " + learned + " 个词 · 连续学习 " + streak + " 天").apply {
            setTextColor(ContextCompat.getColor(context, R.color.teal_primary))
            setPadding(0, Ui.dp(context, 10), 0, 0)
        })
        root.addView(Ui.card(context, progress))

        val focus = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        focus.addView(Ui.label(context, "15 分钟学习路径"))
        focus.addView(Ui.body(context, "听懂 → 看懂 → 跟读 → 回忆 → 应用").apply {
            textSize = 18f
            setPadding(0, Ui.dp(context, 6), 0, 0)
        })
        focus.addView(Ui.body(context, "今天先从 10 个现场高频词开始，再练一个完整场景。").apply {
            setPadding(0, Ui.dp(context, 8), 0, Ui.dp(context, 4))
        })
        focus.addView(Ui.button(context, "去学习") { onOpenStudy() })
        root.addView(Ui.card(context, focus))

        val stats = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        stats.addView(Ui.label(context, "今天已经做过"))
        stats.addView(Ui.body(context, "听读 " + dailyAudio + " 次 · 测验 " + dailyQuiz + " 次 · 翻译 " + dailyTranslation + " 句"))
        root.addView(Ui.card(context, stats))

        val check = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val left = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        left.addView(Ui.label(context, "今日打卡"))
        left.addView(Ui.body(context, "完成今天学习后记得留下一个连续学习记录。"))
        check.addView(left)
        check.addView(Ui.button(context, "打卡") { onCheckIn() }.apply {
            layoutParams = LinearLayout.LayoutParams(Ui.dp(context, 92), Ui.dp(context, 48))
        })
        root.addView(Ui.card(context, check))

        return root
    }
}
