package com.mahanshengzhi.indonesianengineer.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.LearningRepository
import com.mahanshengzhi.indonesianengineer.data.ProgressStore
import com.mahanshengzhi.indonesianengineer.model.SentencePattern
import com.mahanshengzhi.indonesianengineer.model.Vocabulary

object QuizPage {
    private data class Question(
        val title: String,
        val prompt: String,
        val answer: String,
        val options: List<String>
    )

    fun build(
        context: Context,
        repository: LearningRepository,
        store: ProgressStore,
        onFinish: () -> Unit
    ): LinearLayout {
        val vocab = repository.vocabulary().toList().shuffled()
        val sentences = repository.sentences().shuffled()
        val questions = buildQuestions(vocab, sentences)

        val root = Ui.page(context)
        val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        root.addView(content)

        var current = 0
        var score = 0
        var selected: String? = null
        var answeredCorrectly = false

        fun render() {
            content.removeAllViews()

            if (current >= questions.size) {
                store.updateBestQuiz(score)
                store.incrementQuiz()

                val result = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                result.addView(Ui.label(context, "挑战完成"))
                result.addView(Ui.title(context, "本组 " + score + " / " + questions.size).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, Ui.dp(context, 6), 0, Ui.dp(context, 8))
                })
                result.addView(Ui.body(
                    context,
                    "正确率 " + (score * 100 / questions.size) + "%"
                ).apply {
                    gravity = Gravity.CENTER
                    textSize = 18f
                    setPadding(0, 0, 0, Ui.dp(context, 14))
                })
                result.addView(Ui.body(
                    context,
                    "最佳成绩 " + store.bestQuiz() + " / 10"
                ).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, Ui.dp(context, 14))
                })
                result.addView(Ui.button(context, "返回练习") { onFinish() })
                content.addView(Ui.card(context, result))
                return
            }

            val question = questions[current]

            content.addView(ProgressBar(
                context,
                null,
                android.R.attr.progressBarStyleHorizontal
            ).apply {
                max = questions.size
                progress = current
                isIndeterminate = false
            })

            content.addView(Ui.label(
                context,
                "第 " + (current + 1) + " / " + questions.size
            ).apply {
                setPadding(0, Ui.dp(context, 10), 0, Ui.dp(context, 3))
            })

            content.addView(Ui.title(context, question.title).apply {
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, Ui.dp(context, 10))
            })

            val questionCard = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
            questionCard.addView(Ui.label(context, "请选出对应表达"))
            questionCard.addView(Ui.body(context, question.prompt).apply {
                gravity = Gravity.CENTER
                textSize = 24f
                setPadding(0, Ui.dp(context, 14), 0, Ui.dp(context, 16))
            })
            content.addView(Ui.card(context, questionCard))

            question.options.forEach { option ->
                val button = Ui.outlineButton(context, option) {}
                button.setOnClickListener {
                    if (selected != null) return@setOnClickListener
                    selected = option
                    answeredCorrectly = option == question.answer
                    if (answeredCorrectly) score++
                    render()
                }
                if (selected != null) {
                    button.isEnabled = false
                    if (option == question.answer) {
                        button.setTextColor(
                            ContextCompat.getColor(context, R.color.teal_primary)
                        )
                    }
                }
                content.addView(button.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        Ui.dp(context, 50)
                    ).apply {
                        bottomMargin = Ui.dp(context, 8)
                    }
                })
            }

            if (selected != null) {
                val feedback = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                }
                feedback.addView(Ui.label(
                    context,
                    if (answeredCorrectly) "答对了" else "这题再记一下"
                ))
                feedback.addView(Ui.body(
                    context,
                    if (answeredCorrectly) {
                        "很好。把这个表达再默念一遍。"
                    } else {
                        "正确答案是：" + question.answer
                    }
                ).apply {
                    setPadding(0, Ui.dp(context, 5), 0, Ui.dp(context, 10))
                })
                content.addView(Ui.card(context, feedback))

                content.addView(Ui.button(
                    context,
                    if (current + 1 == questions.size) "查看结果" else "下一题"
                ) {
                    current++
                    selected = null
                    answeredCorrectly = false
                    render()
                })
            }
        }

        render()
        return root
    }

    private fun buildQuestions(
        vocab: List<Vocabulary>,
        sentences: List<SentencePattern>
    ): List<Question> {
        val result = mutableListOf<Question>()

        repeat(5) { index ->
            val q = vocab[index]
            val distractors = vocab.drop(index + 1).take(3)
            val reverse = index >= 3

            result += Question(
                title = if (reverse) "词汇 · 印尼语 → 中文" else "词汇 · 中文 → 印尼语",
                prompt = if (reverse) q.indonesian else q.chinese,
                answer = if (reverse) q.chinese else q.indonesian,
                options = if (reverse) {
                    optionList(q.chinese, distractors.map { it.chinese })
                } else {
                    optionList(q.indonesian, distractors.map { it.indonesian })
                }
            )
        }

        repeat(5) { index ->
            val q = sentences[index]
            val distractors = sentences.drop(index + 1).take(3)
            val reverse = index >= 2

            result += Question(
                title = if (reverse) "句型 · 印尼语 → 中文" else "句型 · 中文 → 印尼语",
                prompt = if (reverse) q.indonesian else q.chinese,
                answer = if (reverse) q.chinese else q.indonesian,
                options = if (reverse) {
                    optionList(q.chinese, distractors.map { it.chinese })
                } else {
                    optionList(q.indonesian, distractors.map { it.indonesian })
                }
            )
        }

        return result.shuffled()
    }

    private fun optionList(answer: String, distractors: List<String>): List<String> =
        (distractors.take(3) + answer).shuffled()
}
