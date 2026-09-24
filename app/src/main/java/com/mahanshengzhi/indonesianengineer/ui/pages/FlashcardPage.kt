package com.mahanshengzhi.indonesianengineer.ui.pages

import android.view.View
import android.widget.LinearLayout
import com.google.android.material.card.MaterialCardView
import com.mahanshengzhi.indonesianengineer.MainActivity
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.LearningRepository
import com.mahanshengzhi.indonesianengineer.data.ProgressStore
import com.mahanshengzhi.indonesianengineer.model.Vocabulary
import com.mahanshengzhi.indonesianengineer.ui.UiKit

class FlashcardPage(
    private val activity: MainActivity,
    private val repository: LearningRepository,
    private val progressStore: ProgressStore
) {
    private var cards: List<Vocabulary> = emptyList()
    private var position = 0
    private var revealed = false
    private lateinit var body: LinearLayout
    private lateinit var progressText: android.widget.TextView

    fun build(): View {
        val pair = UiKit.pageScroll(activity)
        body = pair.second
        body.addView(UiKit.title(activity, "闪卡复习"))
        progressText = UiKit.subtitle(activity, "正在准备今天这一组…")
        body.addView(progressText)
        UiKit.addGap(body, activity, 12)

        activity.backgroundExecutor.execute {
            val loaded = repository.vocabulary().take(10).toList()
            activity.runOnUiThread {
                cards = loaded
                position = 0
                revealed = false
                render()
            }
        }

        return pair.first
    }

    private fun clearDynamicContent() {
        while (body.childCount > 3) {
            body.removeViewAt(body.childCount - 1)
        }
    }

    private fun render() {
        clearDynamicContent()

        if (cards.isEmpty()) {
            progressText.text = "没有可用词卡"
            return
        }

        if (position >= cards.size) {
            progressText.text = "今天这一组完成了"
            val done = UiKit.card(activity, R.color.teal_light)
            val content = UiKit.cardContent(activity)
            content.addView(UiKit.section(activity, "做得很好"))
            UiKit.addGap(content, activity, 6)
            content.addView(
                UiKit.body(
                    activity,
                    "你已经完成这组 10 个词。接下来可以去做 10 题挑战。",
                    16f
                )
            )
            val next = UiKit.primaryButton(activity, "去做 10 题")
            next.setOnClickListener {
                activity.openPracticeModule(MainActivity.PracticeModule.QUIZ)
            }
            content.addView(next)
            done.addView(content)
            body.addView(done)
            return
        }

        val item = cards[position]
        progressText.text = "第 " + (position + 1) + " / " + cards.size + " 张"

        val card: MaterialCardView = UiKit.card(activity, R.color.card_green)
        val content = UiKit.cardContent(activity)
        content.addView(UiKit.kicker(activity, "工程词"))
        content.addView(UiKit.body(activity, item.indonesian, 28f))
        UiKit.addGap(content, activity, 8)

        if (revealed) {
            content.addView(UiKit.body(activity, item.chinese, 18f))
            UiKit.addGap(content, activity, 6)
            content.addView(UiKit.muted(activity, "场景：" + item.scene, 13f))
        } else {
            content.addView(
                UiKit.muted(
                    activity,
                    "先别急着看中文，先在脑子里回忆。",
                    15f
                )
            )
        }

        val hasAudio = activity.hasAudio(item.indonesian)
        val listen = UiKit.secondaryButton(
            activity,
            if (hasAudio) "听发音" else "暂无内置语音"
        )
        listen.isEnabled = hasAudio
        listen.setOnClickListener {
            if (hasAudio) activity.playAudio(item.indonesian)
        }
        content.addView(listen)

        if (!revealed) {
            val reveal = UiKit.primaryButton(activity, "查看中文")
            reveal.setOnClickListener {
                revealed = true
                render()
            }
            content.addView(reveal)
        } else {
            val actions = UiKit.buttonRow(activity)
            val know = UiKit.primaryButton(activity, "我会了")
            know.setOnClickListener {
                progressStore.markLearned(item.id)
                progressStore.markDailyWord()
                position += 1
                revealed = false
                render()
            }

            val again = UiKit.secondaryButton(activity, "再复习一次")
            again.setOnClickListener {
                revealed = false
                render()
            }

            UiKit.addWeightedButton(actions, know, activity, 0.58f, 6)
            UiKit.addWeightedButton(actions, again, activity, 0.42f)
            content.addView(actions)
        }

        card.addView(content)
        body.addView(card)
    }
}
