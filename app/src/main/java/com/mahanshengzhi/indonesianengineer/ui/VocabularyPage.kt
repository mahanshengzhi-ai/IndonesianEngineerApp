package com.mahanshengzhi.indonesianengineer.ui

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.LearningRepository

object VocabularyPage {
    fun build(
        context: Context,
        repository: LearningRepository,
        hasAudio: (String) -> Boolean,
        onPlay: (String) -> Unit,
        onLearned: (String) -> Unit
    ): LinearLayout {
        val root = Ui.page(context)
        root.addView(Ui.title(context, "词卡"))
        root.addView(Ui.subtitle(context, "搜索中文、印尼语或分类，只看一小组词，不一次把整本词库压给你。").apply {
            setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 14))
        })

        val source = repository.vocabulary().take(80).toList()
        val search = EditText(context).apply {
            hint = "搜索：beton / 混凝土 / 材料"
            singleLine = true
            textSize = 16f
        }
        root.addView(Ui.card(context, search))

        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)

        fun render(filter: String) {
            list.removeAllViews()
            val q = filter.trim().lowercase()
            val result = source.filter {
                q.isEmpty() ||
                    it.indonesian.lowercase().contains(q) ||
                    it.chinese.contains(q) ||
                    it.category.lowercase().contains(q)
            }.take(30)

            if (result.isEmpty()) {
                list.addView(Ui.card(context, Ui.body(context, "没有找到匹配词汇。换一个词试试。")))
                return
            }

            for (word in result) {
                val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                box.addView(Ui.label(context, word.category + " · " + word.scene))
                box.addView(Ui.body(context, word.indonesian).apply {
                    textSize = 24f
                    setTextColor(ContextCompat.getColor(context, R.color.teal_primary))
                    setPadding(0, Ui.dp(context, 6), 0, 0)
                })
                box.addView(Ui.body(context, word.chinese).apply {
                    setPadding(0, Ui.dp(context, 3), 0, 0)
                })

                val actions = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                val audio = Ui.button(context, if (hasAudio(word.indonesian)) "听一下" else "暂无内置语音") {
                    if (hasAudio(word.indonesian)) onPlay(word.indonesian)
                }
                audio.isEnabled = hasAudio(word.indonesian)
                actions.addView(audio, LinearLayout.LayoutParams(0, Ui.dp(context, 46), 1f))
                actions.addView(Ui.button(context, "我会了") {
                    onLearned(word.id)
                }, LinearLayout.LayoutParams(0, Ui.dp(context, 46), 1f).apply {
                    leftMargin = Ui.dp(context, 8)
                })
                box.addView(actions.apply { setPadding(0, Ui.dp(context, 10), 0, 0) })
                list.addView(Ui.card(context, box))
            }
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                render(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        render("")
        return root
    }
}
