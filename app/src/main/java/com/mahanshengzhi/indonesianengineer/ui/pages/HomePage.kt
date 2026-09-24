package com.mahanshengzhi.indonesianengineer.ui.pages

import android.view.View
import android.widget.LinearLayout
import com.mahanshengzhi.indonesianengineer.MainActivity
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.LearningRepository
import com.mahanshengzhi.indonesianengineer.data.ProgressStore
import com.mahanshengzhi.indonesianengineer.model.SentencePattern
import com.mahanshengzhi.indonesianengineer.ui.UiKit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomePage(
    private val activity: MainActivity,
    private val repository: LearningRepository,
    private val progressStore: ProgressStore
) {
    fun build(): View {
        val pair = UiKit.pageScroll(activity)
        val body = pair.second

        body.addView(UiKit.kicker(activity, "BAHASA INDONESIA · ENGINEER"))
        body.addView(UiKit.title(activity, "印尼语工程员学习"))
        body.addView(UiKit.subtitle(activity, "每天 15 分钟，先听懂，再开口"))
        UiKit.addGap(body, activity, 16)

        addStats(body)
        addToday(body)
        addSentence(body)
        addMethod(body)

        return pair.first
    }

    private fun addStats(parent: LinearLayout) {
        val card = UiKit.card(activity, R.color.teal_light)
        val content = UiKit.cardContent(activity)
        content.addView(UiKit.section(activity, "这套课为现场沟通服务"))
        UiKit.addGap(content, activity, 6)
        content.addView(
            UiKit.body(
                activity,
                "3,574 个工程词汇  ·  40 个核心句型  ·  10 个真实场景",
                15f
            )
        )
        card.addView(content)
        parent.addView(card)
    }

    private fun addToday(parent: LinearLayout) {
        val card = UiKit.card(activity, R.color.card_green)
        val content = UiKit.cardContent(activity)
        val dateText = SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(Date())
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        content.addView(UiKit.section(activity, "今天学什么？"))
        UiKit.addGap(content, activity, 4)
        content.addView(UiKit.muted(activity, dateText, 13f))
        UiKit.addGap(content, activity, 10)

        UiKit.progressRow(activity, content, "新词", progressStore.dailyWords(), 10)
        UiKit.progressRow(activity, content, "句型", progressStore.dailySentences(), 5)
        UiKit.progressRow(activity, content, "场景", progressStore.dailyScene(), 1)
        UiKit.progressRow(activity, content, "小测", progressStore.dailyQuiz(), 1)

        val start = UiKit.primaryButton(activity, "开始今天学习")
        val checkIn = UiKit.warmButton(
            activity,
            if (progressStore.lastCheckIn() == todayKey) "已打卡" else "今日打卡"
        )

        val row = UiKit.buttonRow(activity)
        UiKit.addWeightedButton(row, start, activity, 0.65f, 8)
        UiKit.addWeightedButton(row, checkIn, activity, 0.35f)

        start.setOnClickListener {
            activity.openLearnModule(MainActivity.LearnModule.PRONUNCIATION)
        }
        checkIn.setOnClickListener {
            progressStore.checkIn()
            checkIn.text = "已打卡"
        }

        content.addView(row)
        card.addView(content)
        parent.addView(card)
    }

    private fun addSentence(parent: LinearLayout) {
        val sentence: SentencePattern = repository.sentences().firstOrNull() ?: return
        val card = UiKit.card(activity, R.color.card_blue)
        val content = UiKit.cardContent(activity)

        content.addView(UiKit.section(activity, "今天一句"))
        UiKit.addGap(content, activity, 8)
        content.addView(UiKit.body(activity, sentence.indonesian, 22f))
        UiKit.addGap(content, activity, 4)
        content.addView(UiKit.body(activity, sentence.chinese, 16f))
        UiKit.addGap(content, activity, 6)
        content.addView(UiKit.muted(activity, sentence.scene, 13f))

        if (activity.hasAudio(sentence.indonesian)) {
            val audio = UiKit.secondaryButton(activity, "听发音")
            audio.setOnClickListener { activity.playAudio(sentence.indonesian) }
            content.addView(audio)
        }

        card.addView(content)
        parent.addView(card)
    }

    private fun addMethod(parent: LinearLayout) {
        val card = UiKit.card(activity)
        val content = UiKit.cardContent(activity)

        content.addView(UiKit.section(activity, "15 分钟怎么学"))
        UiKit.addGap(content, activity, 6)
        content.addView(
            UiKit.body(
                activity,
                "先看中文理解 → 自己说一遍 → 听标准发音 → 跟读 → 闪卡回忆 → 做 10 题小测。",
                15f
            )
        )
        UiKit.addGap(content, activity, 8)

        val row = UiKit.buttonRow(activity)
        val learn = UiKit.secondaryButton(activity, "开始学词")
        val practice = UiKit.secondaryButton(activity, "来一组闪卡")
        UiKit.addWeightedButton(row, learn, activity, 0.5f, 6)
        UiKit.addWeightedButton(row, practice, activity, 0.5f)

        learn.setOnClickListener {
            activity.openLearnModule(MainActivity.LearnModule.VOCABULARY)
        }
        practice.setOnClickListener {
            activity.openPracticeModule(MainActivity.PracticeModule.FLASHCARD)
        }

        content.addView(row)
        card.addView(content)
        parent.addView(card)
    }
}
