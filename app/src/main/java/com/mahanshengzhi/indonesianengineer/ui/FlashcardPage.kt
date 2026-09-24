package com.mahanshengzhi.indonesianengineer.ui

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.LearningRepository
import com.mahanshengzhi.indonesianengineer.data.ProgressStore

object FlashcardPage {
    private const val ROUND_SIZE = 10

    fun build(
        context: Context,
        repository: LearningRepository,
        store: ProgressStore,
        hasAudio: (String) -> Boolean,
        onPlay: (String) -> Unit,
        onFinish: () -> Unit
    ): LinearLayout {
        val source = repository.vocabulary().toList().shuffled().take(ROUND_SIZE)
        val root = Ui.page(context)
        val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        root.addView(content)

        var current = 0
        var known = 0
        var revealed = false

        lateinit var render: () -> Unit

        fun advance(wordKnown: Boolean) {
            store.markDailyFlashcard()
            if (wordKnown) {
                store.markLearned(source[current].id)
                store.markDailyWord()
                known++
            }
            current++
            revealed = false
            render()
        }

        render = {
            content.removeAllViews()

            if (current >= source.size) {
                content.addView(Ui.label(context, "这一轮完成"))
                content.addView(Ui.title(context, "10 张闪卡完成").apply {
                    setPadding(0, Ui.dp(context, 6), 0, Ui.dp(context, 8))
                })
                content.addView(Ui.body(
                    context,
                    "认识了 " + known + " 张 · 需要再看 " + (ROUND_SIZE - known) + " 张"
                ).apply {
                    textSize = 18f
                    setPadding(0, 0, 0, Ui.dp(context, 16))
                })

                val result = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                result.addView(Ui.button(context, "完成复习") { onFinish() })
                content.addView(Ui.card(context, result))
                return
            }

            val word = source[current]

            content.addView(ProgressBar(
                context,
                null,
                android.R.attr.progressBarStyleHorizontal
            ).apply {
                max = ROUND_SIZE
                progress = current
                isIndeterminate = false
            })

            content.addView(Ui.label(
                context,
                "今日闪卡 · " + (current + 1) + " / " + ROUND_SIZE
            ).apply {
                setPadding(0, Ui.dp(context, 10), 0, Ui.dp(context, 6))
            })

            val cardBox = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }

            cardBox.addView(Ui.label(context, word.category + " · " + word.scene).apply {
                gravity = Gravity.CENTER
            })

            cardBox.addView(TextView(context).apply {
                text = word.indonesian
                textSize = 30f
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(context, R.color.teal_deep))
                setPadding(0, Ui.dp(context, 24), 0, Ui.dp(context, 14))
            })

            if (revealed) {
                cardBox.addView(Ui.label(context, "中文").apply {
                    gravity = Gravity.CENTER
                })
                cardBox.addView(Ui.body(context, word.chinese).apply {
                    textSize = 22f
                    gravity = Gravity.CENTER
                    setPadding(0, Ui.dp(context, 4), 0, Ui.dp(context, 14))
                })
                if (word.example.isNotBlank()) {
                    cardBox.addView(Ui.body(context, "例句 · " + word.example).apply {
                        textSize = 14f
                        gravity = Gravity.CENTER
                        setPadding(0, 0, 0, Ui.dp(context, 10))
                    })
                }
            } else {
                cardBox.addView(Ui.body(context, "先想一遍，再看中文").apply {
                    gravity = Gravity.CENTER
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    setPadding(0, Ui.dp(context, 4), 0, Ui.dp(context, 14))
                })
            }

            if (hasAudio(word.indonesian)) {
                cardBox.addView(Ui.outlineButton(context, "听一下") {
                    onPlay(word.indonesian)
                })
            } else {
                cardBox.addView(Ui.label(context, "暂无内置语音").apply {
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, Ui.dp(context, 10))
                })
            }

            content.addView(Ui.card(context, cardBox))

            if (!revealed) {
                content.addView(Ui.button(context, "显示中文") {
                    revealed = true
                    render()
                })
            } else {
                val actions = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                actions.addView(
                    Ui.outlineButton(context, "不熟") {
                        advance(false)
                    },
                    LinearLayout.LayoutParams(0, Ui.dp(context, 50), 1f)
                )
                actions.addView(
                    Ui.secondaryButton(context, "认识了") {
                        advance(true)
                    },
                    LinearLayout.LayoutParams(0, Ui.dp(context, 50), 1f).apply {
                        leftMargin = Ui.dp(context, 8)
                    }
                )
                content.addView(actions)
            }
        }

        render()
        return root
    }
}
