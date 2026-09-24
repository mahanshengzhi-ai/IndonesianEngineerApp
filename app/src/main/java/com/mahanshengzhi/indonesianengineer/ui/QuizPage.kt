package com.mahanshengzhi.indonesianengineer.ui

import android.content.Context
import android.widget.LinearLayout
import android.widget.Toast
import com.mahanshengzhi.indonesianengineer.data.LearningRepository
import com.mahanshengzhi.indonesianengineer.data.ProgressStore

object QuizPage {
    fun build(
        context: Context,
        repository: LearningRepository,
        store: ProgressStore,
        onFinish: () -> Unit
    ): LinearLayout {
        val pool = repository.vocabulary().take(60).toList()
        val questions = pool.take(10)
        val root = Ui.page(context)
        val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        root.addView(content)

        var current = 0
        var score = 0

        fun render() {
            content.removeAllViews()
            if (current >= questions.size) {
                store.updateBestQuiz(score)
                content.addView(Ui.label(context, "挑战完成"))
                content.addView(Ui.title(context, "本组得分 " + score + " / 10").apply {
                    setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 12))
                })
                content.addView(Ui.body(context, "正确率 " + (score * 10) + "%").apply {
                    textSize = 18f
                })
                content.addView(Ui.card(context, Ui.button(context, "再来一组") {
                    current = 0
                    score = 0
                    render()
                }))
                content.addView(Ui.card(context, Ui.button(context, "返回练习") {
                    onFinish()
                }))
                return
            }

            val q = questions[current]
            content.addView(Ui.title(context, "第 " + (current + 1) + " / 10 题"))
            content.addView(Ui.subtitle(context, "选择对应的印尼语表达").apply {
                setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 15))
            })

            val question = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            question.addView(Ui.label(context, "中文"))
            question.addView(Ui.body(context, q.chinese).apply {
                textSize = 21f
                setPadding(0, Ui.dp(context, 7), 0, 0)
            })
            content.addView(Ui.card(context, question))

            val options = mutableListOf(q)
            options.addAll(pool.filter { it.id != q.id }.take(3))
            options.shuffle()

            for (option in options) {
                content.addView(Ui.card(context, Ui.button(context, option.indonesian) {
                    if (option.id == q.id) score++
                    current++
                    render()
                }))
            }
        }

        render()
        return root
    }
}
