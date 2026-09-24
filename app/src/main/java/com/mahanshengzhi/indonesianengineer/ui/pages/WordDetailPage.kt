package com.mahanshengzhi.indonesianengineer.ui.pages

import android.view.View
import android.widget.LinearLayout
import com.mahanshengzhi.indonesianengineer.MainActivity
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.ProgressStore
import com.mahanshengzhi.indonesianengineer.model.Vocabulary
import com.mahanshengzhi.indonesianengineer.ui.UiKit

class WordDetailPage(
    private val activity: MainActivity,
    private val item: Vocabulary,
    private val progressStore: ProgressStore
) {
    fun build(): View {
        val pair = UiKit.pageScroll(activity)
        val body = pair.second

        body.addView(UiKit.kicker(activity, item.category.uppercase()))
        body.addView(UiKit.title(activity, item.indonesian))
        body.addView(UiKit.subtitle(activity, item.chinese))
        UiKit.addGap(body, activity, 12)

        val meaning = UiKit.card(activity, R.color.teal_light)
        val content = UiKit.cardContent(activity)
        content.addView(UiKit.section(activity, "这个词在哪里用？"))
        UiKit.addGap(content, activity, 5)
        content.addView(UiKit.body(activity, "场景：" + item.scene, 15f))

        if (item.example.isNotBlank()) {
            UiKit.addGap(content, activity, 8)
            content.addView(UiKit.muted(activity, "例句", 13f))
            content.addView(UiKit.body(activity, item.example, 18f))
            if (item.exampleChinese.isNotBlank()) {
                content.addView(UiKit.body(activity, item.exampleChinese, 14f))
            }
        }

        meaning.addView(content)
        body.addView(meaning)

        val actions = UiKit.buttonRow(activity)

        if (activity.hasAudio(item.indonesian)) {
            val audio = UiKit.secondaryButton(activity, "听这个词")
            audio.setOnClickListener { activity.playAudio(item.indonesian) }
            UiKit.addWeightedButton(actions, audio, activity, 0.5f, 6)
        }

        val learn = UiKit.primaryButton(
            activity,
            if (progressStore.isLearned(item.id)) "已掌握" else "我会了"
        )
        learn.setOnClickListener {
            progressStore.markLearned(item.id)
            progressStore.markDailyWord()
            learn.text = "已掌握"
        }
        UiKit.addWeightedButton(actions, learn, activity, 0.5f)
        body.addView(actions)

        val review = UiKit.card(activity)
        val reviewContent = UiKit.cardContent(activity)
        reviewContent.addView(UiKit.section(activity, "记忆提示"))
        UiKit.addGap(reviewContent, activity, 5)
        reviewContent.addView(
            UiKit.body(
                activity,
                "先看中文 → 自己回忆印尼语 → 听一次 → 再闭眼说一遍。",
                15f
            )
        )

        val repeat = UiKit.secondaryButton(activity, "再听一遍")
        repeat.setOnClickListener { activity.playAudio(item.indonesian) }
        reviewContent.addView(repeat)

        review.addView(reviewContent)
        body.addView(review)

        return pair.first
    }
}
