package com.mahanshengzhi.indonesianengineer.ui.pages

import android.view.View
import android.widget.LinearLayout
import com.mahanshengzhi.indonesianengineer.MainActivity
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.ProgressStore
import com.mahanshengzhi.indonesianengineer.ui.UiKit

class PracticePage(
    private val activity: MainActivity,
    private val progressStore: ProgressStore
) {
    fun build(): View {
        val pair = UiKit.pageScroll(activity)
        val body = pair.second

        body.addView(UiKit.title(activity, "练习"))
        body.addView(UiKit.subtitle(activity, "用闪卡回忆，再用 10 题挑战检验自己。"))
        UiKit.addGap(body, activity, 14)

        val stats = UiKit.card(activity, R.color.teal_light)
        val statsContent = UiKit.cardContent(activity)
        statsContent.addView(UiKit.section(activity, "今天"))
        UiKit.addGap(statsContent, activity, 6)
        statsContent.addView(
            UiKit.body(
                activity,
                "听音频 " + progressStore.dailyAudio() +
                    " 次 · 小测 " + progressStore.dailyQuiz() +
                    " 次 · 最佳成绩 " + progressStore.bestQuiz() + "/100",
                15f
            )
        )
        stats.addView(statsContent)
        body.addView(stats)

        addPracticeCard(
            body,
            "闪卡复习",
            "10 个词一组：先回忆，再看中文。掌握了就记入今天学习。",
            "开始闪卡",
            MainActivity.PracticeModule.FLASHCARD,
            R.color.card_green
        )
        addPracticeCard(
            body,
            "10 题挑战",
            "混合词汇、句型和场景理解，不需要麦克风。",
            "开始挑战",
            MainActivity.PracticeModule.QUIZ,
            R.color.card_blue
        )

        return pair.first
    }

    private fun addPracticeCard(
        parent: LinearLayout,
        title: String,
        desc: String,
        buttonText: String,
        module: MainActivity.PracticeModule,
        background: Int
    ) {
        val card = UiKit.card(activity, background)
        val content = UiKit.cardContent(activity)
        content.addView(UiKit.section(activity, title))
        UiKit.addGap(content, activity, 5)
        content.addView(UiKit.body(activity, desc, 15f))
        val button = UiKit.primaryButton(activity, buttonText)
        button.setOnClickListener { activity.openPracticeModule(module) }
        content.addView(button)
        card.addView(content)
        parent.addView(card)
    }
}
