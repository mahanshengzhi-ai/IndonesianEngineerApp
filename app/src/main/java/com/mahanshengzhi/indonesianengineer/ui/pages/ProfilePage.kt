package com.mahanshengzhi.indonesianengineer.ui.pages

import android.view.View
import android.widget.LinearLayout
import com.mahanshengzhi.indonesianengineer.MainActivity
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.ProgressStore
import com.mahanshengzhi.indonesianengineer.ui.UiKit

class ProfilePage(
    private val activity: MainActivity,
    private val progressStore: ProgressStore
) {
    fun build(): View {
        val pair = UiKit.pageScroll(activity)
        val body = pair.second

        body.addView(UiKit.title(activity, "我的"))
        body.addView(UiKit.subtitle(activity, "把学习做成每天 15 分钟的小习惯。"))
        UiKit.addGap(body, activity, 12)

        addMainProgress(body)
        addDaily(body)
        addBadges(body)
        addSuggestion(body)

        return pair.first
    }

    private fun addMainProgress(parent: LinearLayout) {
        val card = UiKit.card(activity, R.color.teal_light)
        val content = UiKit.cardContent(activity)

        content.addView(UiKit.section(activity, "我的学习进度"))
        UiKit.addGap(content, activity, 8)
        content.addView(
            UiKit.body(
                activity,
                "已掌握 " + progressStore.learnedTotal() + " / 3574 个词",
                20f
            )
        )
        UiKit.progressRow(
            activity,
            content,
            "词汇",
            progressStore.learnedTotal().coerceAtMost(3574),
            3574
        )
        content.addView(
            UiKit.body(
                activity,
                "连续学习 " + progressStore.streak() + " 天",
                16f
            )
        )
        content.addView(
            UiKit.body(
                activity,
                "累计打卡 " + progressStore.checkins() + " 次",
                16f
            )
        )
        content.addView(
            UiKit.body(
                activity,
                "最佳测验 " + progressStore.bestQuiz() + "/100",
                16f
            )
        )

        card.addView(content)
        parent.addView(card)
    }

    private fun addDaily(parent: LinearLayout) {
        val card = UiKit.card(activity)
        val content = UiKit.cardContent(activity)

        content.addView(UiKit.section(activity, "今天的轨迹"))
        UiKit.addGap(content, activity, 6)
        content.addView(UiKit.body(activity, "新词：" + progressStore.dailyWords() + " 个", 15f))
        content.addView(UiKit.body(activity, "句型：" + progressStore.dailySentences() + " 个", 15f))
        content.addView(UiKit.body(activity, "场景：" + progressStore.dailyScene() + " 个", 15f))
        content.addView(UiKit.body(activity, "听音频：" + progressStore.dailyAudio() + " 次", 15f))
        content.addView(UiKit.body(activity, "小测：" + progressStore.dailyQuiz() + " 次", 15f))
        content.addView(UiKit.body(activity, "翻译：" + progressStore.dailyTranslation() + " 句", 15f))

        card.addView(content)
        parent.addView(card)
    }

    private fun addBadges(parent: LinearLayout) {
        val card = UiKit.card(activity, R.color.warm_orange_light)
        val content = UiKit.cardContent(activity)

        content.addView(UiKit.section(activity, "已解锁的小成就"))
        UiKit.addGap(content, activity, 7)

        val badges = mutableListOf<String>()
        if (progressStore.checkins() >= 1) badges += "第一次打卡"
        if (progressStore.streak() >= 3) badges += "连续 3 天"
        if (progressStore.learnedTotal() >= 50) badges += "掌握 50 词"
        if (progressStore.learnedTotal() >= 100) badges += "掌握 100 词"
        if (progressStore.dailyScene() >= 1) badges += "完成今天场景"

        if (badges.isEmpty()) {
            content.addView(
                UiKit.muted(
                    activity,
                    "第一个徽章就在今天：先打卡，再学 10 个词。",
                    14f
                )
            )
        } else {
            badges.forEach {
                content.addView(UiKit.body(activity, "• " + it, 15f))
            }
        }

        card.addView(content)
        parent.addView(card)
    }

    private fun addSuggestion(parent: LinearLayout) {
        val card = UiKit.card(activity, R.color.card_blue)
        val content = UiKit.cardContent(activity)

        content.addView(UiKit.section(activity, "明天也照这个节奏"))
        UiKit.addGap(content, activity, 6)
        content.addView(
            UiKit.body(
                activity,
                "10 个词 + 5 个句型 + 1 个场景 + 10 题挑战。",
                16f
            )
        )

        val start = UiKit.primaryButton(activity, "继续今天的学习")
        start.setOnClickListener {
            activity.openLearnModule(MainActivity.LearnModule.VOCABULARY)
        }
        content.addView(start)

        card.addView(content)
        parent.addView(card)
    }
}
