package com.mahanshengzhi.indonesianengineer.ui.pages

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.mahanshengzhi.indonesianengineer.MainActivity
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.LearningRepository
import com.mahanshengzhi.indonesianengineer.data.ProgressStore
import com.mahanshengzhi.indonesianengineer.model.SentencePattern
import com.mahanshengzhi.indonesianengineer.model.Vocabulary
import com.mahanshengzhi.indonesianengineer.ui.UiKit

class QuizPage(
    private val activity: MainActivity,
    private val repository: LearningRepository,
    private val progressStore: ProgressStore
) {
    private data class Question(
        val type: String,
        val prompt: String,
        val answer: String,
        val options: List<String>,
        val audioText: String?
    )

    private var questions: List<Question> = emptyList()
    private var index = 0
    private var score = 0
    private var answered = false
    private lateinit var body: LinearLayout
    private lateinit var status: TextView

    fun build(): View {
        val pair = UiKit.pageScroll(activity)
        body = pair.second
        body.addView(UiKit.title(activity, "10 题挑战"))
        status = UiKit.subtitle(activity, "正在准备题目…")
        body.addView(status)
        UiKit.addGap(body, activity, 12)

        activity.backgroundExecutor.execute {
            val words = repository.vocabulary().take(20).toList()
            val sentences = repository.sentences().take(10)
            val result = buildQuestions(words, sentences)

            activity.runOnUiThread {
                questions = result
                index = 0
                score = 0
                answered = false
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

    private fun buildQuestions(
        words: List<Vocabulary>,
        sentences: List<SentencePattern>
    ): List<Question> {
        val result = mutableListOf<Question>()
        val wordPool = words.map { it.indonesian }
        val sentencePool = sentences.map { it.indonesian }

        words.take(5).forEach { word ->
            val options = (
                listOf(word.indonesian) +
                    wordPool.filter { it != word.indonesian }.shuffled().take(3)
            ).shuffled()

            result.add(
                Question(
                    "词汇",
                    "“" + word.chinese + "”对应哪个印尼语？",
                    word.indonesian,
                    options,
                    null
                )
            )
        }

        sentences.take(5).forEach { sentence ->
            val options = (
                listOf(sentence.indonesian) +
                    sentencePool.filter { it != sentence.indonesian }.shuffled().take(3)
            ).shuffled()

            result.add(
                Question(
                    "句型",
                    "下面哪一句表达“" + sentence.chinese + "”？",
                    sentence.indonesian,
                    options,
                    sentence.indonesian
                )
            )
        }

        return result
    }

    private fun render() {
        clearDynamicContent()

        if (questions.isEmpty() || index >= questions.size) {
            renderResult()
            return
        }

        val question = questions[index]
        status.text = (index + 1).toString() + " / " + questions.size +
            " · 已答对 " + score + " 题"

        val cardBackground = if (index % 2 == 0) R.color.teal_light else R.color.card_blue
        val card = UiKit.card(activity, cardBackground)
        val content = UiKit.cardContent(activity)

        content.addView(UiKit.kicker(activity, question.type))
        UiKit.addGap(content, activity, 5)
        content.addView(UiKit.body(activity, question.prompt, 19f))
        UiKit.addGap(content, activity, 8)

        if (question.audioText != null && activity.hasAudio(question.audioText)) {
            val audio = UiKit.secondaryButton(activity, "先听一句")
            audio.setOnClickListener { activity.playAudio(question.audioText) }
            content.addView(audio)
            UiKit.addGap(content, activity, 4)
        }

        val optionButtons = mutableListOf<MaterialButton>()
        val nextButton = UiKit.primaryButton(
            activity,
            if (index == questions.lastIndex) "看成绩" else "下一题"
        )
        nextButton.isEnabled = false

        question.options.forEach { option ->
            val button = UiKit.secondaryButton(activity, option)
            button.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = UiKit.dp(activity, 8)
            }

            button.setOnClickListener {
                if (answered) return@setOnClickListener

                answered = true
                if (option == question.answer) {
                    score += 1
                    button.setBackgroundColor(activity.getColor(R.color.teal_light))
                    button.setTextColor(activity.getColor(R.color.teal_deep))
                } else {
                    button.setBackgroundColor(Color.rgb(250, 234, 224))
                    button.setTextColor(activity.getColor(R.color.warm_orange))
                    optionButtons
                        .firstOrNull { it.text.toString() == question.answer }
                        ?.setBackgroundColor(activity.getColor(R.color.teal_light))
                }

                optionButtons.forEach { it.isEnabled = false }
                nextButton.isEnabled = true
            }

            optionButtons.add(button)
            content.addView(button)
        }

        nextButton.setOnClickListener {
            index += 1
            answered = false
            render()
        }
        content.addView(nextButton)

        card.addView(content)
        body.addView(card)
    }

    private fun renderResult() {
        progressStore.incrementQuiz()

        val percent = if (questions.isEmpty()) 0 else score * 100 / questions.size
        progressStore.updateBestQuiz(percent)

        status.text = "本组完成"

        val card = UiKit.card(activity, R.color.teal_light)
        val content = UiKit.cardContent(activity)

        content.addView(UiKit.section(activity, "本次成绩"))
        UiKit.addGap(content, activity, 7)
        content.addView(UiKit.body(activity, percent.toString() + " 分", 32f))
        content.addView(
            UiKit.body(
                activity,
                "答对 " + score + " / " + questions.size + " 题",
                16f
            )
        )
        UiKit.addGap(content, activity, 8)
        content.addView(
            UiKit.muted(
                activity,
                "最佳成绩：" + progressStore.bestQuiz() + "/100",
                14f
            )
        )

        val retry = UiKit.primaryButton(activity, "再来一组")
        retry.setOnClickListener {
            index = 0
            score = 0
            answered = false
            render()
        }
        content.addView(retry)

        card.addView(content)
        body.addView(card)
    }
}
