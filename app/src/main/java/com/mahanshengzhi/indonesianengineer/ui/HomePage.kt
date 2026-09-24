package com.mahanshengzhi.indonesianengineer.ui

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.view.Gravity
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
        dailySentences: Int,
        dailyScene: Int,
        checkedInToday: Boolean,
        onStart: () -> Unit,
        onCheckIn: () -> Unit,
        onOpenStudy: () -> Unit,
        onOpenPractice: () -> Unit
    ): LinearLayout {
        val root = Ui.page(context)
        val date = SimpleDateFormat("yyyy年M月d日 · EEEE", Locale.CHINA).format(Date())

        root.addView(Ui.label(context, "BAHASA INDONESIA · ENGINEER"))
        root.addView(Ui.title(context, "印尼语工程员学习").apply {
            setPadding(0, Ui.dp(context, 4), 0, 0)
        })
        root.addView(Ui.subtitle(context, "每天 15 分钟，先听懂，再开口").apply {
            setPadding(0, Ui.dp(context, 5), 0, Ui.dp(context, 10))
        })
        root.addView(Ui.label(context, date).apply {
            setTextColor(ContextCompat.getColor(context, R.color.teal_primary))
            setPadding(0, 0, 0, Ui.dp(context, 18))
        })

        val plan = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        plan.addView(Ui.label(context, "今日目标 · 15 分钟"))
        plan.addView(Ui.sectionTitle(context, "先听懂，再开口").apply {
            setPadding(0, Ui.dp(context, 5), 0, 0)
        })
        plan.addView(Ui.body(context, "10 个新词 · 5 个句型 · 1 个场景 · 10 题挑战").apply {
            setPadding(0, Ui.dp(context, 5), 0, Ui.dp(context, 13))
        })
        plan.addView(Ui.button(context, "开始今天学习") { onStart() })
        root.addView(Ui.card(context, plan, ContextCompat.getColor(context, R.color.teal_primary)))

        val progress = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        progress.addView(Ui.label(context, "今日完成"))
        val metrics = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, Ui.dp(context, 10), 0, Ui.dp(context, 10))
        }
        metrics.addView(Ui.metric(context, dailyWords.coerceAtMost(10).toString() + "/10", "新词"))
        metrics.addView(Ui.metric(context, dailySentences.coerceAtMost(5).toString() + "/5", "句型"))
        metrics.addView(Ui.metric(context, dailyScene.coerceAtMost(1).toString() + "/1", "场景"))
        metrics.addView(Ui.metric(context, (if (dailyQuiz > 0) 1 else 0).toString() + "/1", "挑战"))
        progress.addView(metrics)
        progress.addView(Ui.body(
            context,
            "听力 " + dailyAudio + " 次 · 翻译 " + dailyTranslation + " 句 · 已掌握 " + learned + " 个词"
        ))
        root.addView(Ui.card(context, progress))

        val route = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        route.addView(Ui.label(context, "15 分钟学习路径"))
        route.addView(Ui.body(context, "① 听懂 → ② 看懂 → ③ 跟读 → ④ 回忆 → ⑤ 应用").apply {
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.teal_primary))
            setPadding(0, Ui.dp(context, 6), 0, Ui.dp(context, 5))
        })
        route.addView(Ui.body(
            context,
            "今天先抓现场高频词，再用完整句型和场景把词放进真实交流里。"
        ))
        root.addView(Ui.card(context, route))

        val continueBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        continueBox.addView(Ui.label(context, "继续学习"))
        continueBox.addView(Ui.body(context, "词汇 → 句型 → 场景 → 练习").apply {
            setPadding(0, Ui.dp(context, 5), 0, Ui.dp(context, 10))
        })
        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        actions.addView(
            Ui.button(context, "继续学习") { onOpenStudy() },
            LinearLayout.LayoutParams(0, Ui.dp(context, 48), 1f)
        )
        actions.addView(
            Ui.secondaryButton(context, "去挑战") { onOpenPractice() },
            LinearLayout.LayoutParams(0, Ui.dp(context, 48), 1f).apply {
                leftMargin = Ui.dp(context, 8)
            }
        )
        continueBox.addView(actions)
        root.addView(Ui.card(context, continueBox))

        val check = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val left = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        left.addView(Ui.label(context, "连续学习"))
        left.addView(Ui.body(
            context,
            if (checkedInToday) "今天已经打卡，保持 " + streak + " 天连续学习。"
            else "已经连续学习 " + streak + " 天，今天完成后记得打卡。"
        ))
        check.addView(left)
        check.addView(
            if (checkedInToday) {
                Ui.outlineButton(context, "已打卡") {}
            } else {
                Ui.secondaryButton(context, "今日打卡") { onCheckIn() }
            }
        )
        root.addView(Ui.card(context, check))

        return root
    }
}
