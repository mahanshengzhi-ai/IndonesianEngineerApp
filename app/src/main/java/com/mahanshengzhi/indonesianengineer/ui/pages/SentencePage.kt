package com.mahanshengzhi.indonesianengineer.ui.pages

import android.view.View
import android.widget.LinearLayout
import com.mahanshengzhi.indonesianengineer.MainActivity
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.LearningRepository
import com.mahanshengzhi.indonesianengineer.data.ProgressStore
import com.mahanshengzhi.indonesianengineer.model.SentencePattern
import com.mahanshengzhi.indonesianengineer.ui.UiKit

class SentencePage(
    private val activity: MainActivity,
    private val repository: LearningRepository,
    private val progressStore: ProgressStore
) {
    fun build(): View {
        val pair = UiKit.pageScroll(activity)
        val body = pair.second

        body.addView(UiKit.title(activity, "核心句型"))
        body.addView(
            UiKit.subtitle(
                activity,
                "40 个现场高频表达。每句都先理解用途，再听发音，再自己说。"
            )
        )
        UiKit.addGap(body, activity, 12)

        repository.sentences().forEachIndexed { index, sentence ->
            addSentenceCard(body, index + 1, sentence)
        }

        return pair.first
    }

    private fun addSentenceCard(
        parent: LinearLayout,
        number: Int,
        sentence: SentencePattern
    ) {
        val background = if (number % 3 == 1) R.color.teal_light else R.color.card_blue
        val card = UiKit.card(activity, background)
        val content = UiKit.cardContent(activity)

        content.addView(UiKit.kicker(activity, "句型 " + number))
        content.addView(UiKit.body(activity, sentence.indonesian, 20f))
        UiKit.addGap(content, activity, 4)
        content.addView(UiKit.body(activity, sentence.chinese, 16f))
        UiKit.addGap(content, activity, 4)
        content.addView(UiKit.muted(activity, "场景：" + sentence.scene, 13f))
        UiKit.addGap(content, activity, 6)
        content.addView(UiKit.body(activity, "说明：" + sentence.explanation, 14f))
        UiKit.addGap(content, activity, 6)
        content.addView(UiKit.muted(activity, "例句：", 13f))
        content.addView(UiKit.body(activity, sentence.example, 15f))

        val actions = UiKit.buttonRow(activity)
        if (activity.hasAudio(sentence.indonesian)) {
            val audio = UiKit.secondaryButton(activity, "听发音")
            audio.setOnClickListener { activity.playAudio(sentence.indonesian) }
            UiKit.addWeightedButton(actions, audio, activity, 0.5f, 6)
        }

        val know = UiKit.primaryButton(activity, "我会了")
        know.setOnClickListener {
            progressStore.markDailySentence()
            know.text = "已加入今日进度"
            know.isEnabled = false
        }
        UiKit.addWeightedButton(actions, know, activity, 0.5f)

        content.addView(actions)
        card.addView(content)
        parent.addView(card)
    }
}
