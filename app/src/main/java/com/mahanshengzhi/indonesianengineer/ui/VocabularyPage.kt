package com.mahanshengzhi.indonesianengineer.ui

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.core.content.ContextCompat
import com.google.android.material.materialswitch.MaterialSwitch
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.LearningRepository
import com.mahanshengzhi.indonesianengineer.model.Vocabulary

object VocabularyPage {
    fun build(
        context: Context,
        repository: LearningRepository,
        hasAudio: (String) -> Boolean,
        onPlay: (String) -> Unit,
        onLearned: (String) -> Unit,
        isLearned: (String) -> Boolean
    ): LinearLayout {
        val root = Ui.page(context)
        root.addView(Ui.title(context, "工程词汇"))
        root.addView(Ui.subtitle(
            context,
            "先找你今天会用到的词。搜索、分类和学习状态都放在这里，不把整本词库一次压给你。"
        ).apply {
            setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 13))
        })

        val source = repository.vocabulary().toList()
        val categories = mutableListOf("全部分类").apply {
            addAll(source.map { it.category }.filter { it.isNotBlank() }.distinct().sorted())
        }

        val search = EditText(context).apply {
            hint = "搜索：beton / 混凝土 / 材料"
            isSingleLine = true
            textSize = 16f
        }
        root.addView(Ui.card(context, search))

        val category = Spinner(context).apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                categories
            )
        }
        root.addView(Ui.card(context, category))

        val onlyUnlearned = MaterialSwitch(context).apply {
            text = "只看还没学会的词"
            textSize = 14.5f
        }
        root.addView(Ui.card(context, onlyUnlearned))

        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)

        fun render() {
            list.removeAllViews()
            val query = search.text?.toString().orEmpty().trim().lowercase()
            val selectedCategory = category.selectedItem?.toString().orEmpty()

            val filtered = source.asSequence()
                .filter {
                    query.isEmpty() ||
                        it.indonesian.lowercase().contains(query) ||
                        it.chinese.contains(query) ||
                        it.category.lowercase().contains(query)
                }
                .filter {
                    selectedCategory == "全部分类" || it.category == selectedCategory
                }
                .filter {
                    !onlyUnlearned.isChecked || !isLearned(it.id)
                }
                .take(40)
                .toList()

            list.addView(Ui.label(
                context,
                "当前显示 " + filtered.size + " 条 · " +
                    if (selectedCategory == "全部分类") "全部分类" else selectedCategory
            ).apply {
                setPadding(0, Ui.dp(context, 3), 0, Ui.dp(context, 8))
            })

            if (filtered.isEmpty()) {
                list.addView(Ui.card(
                    context,
                    Ui.body(context, "没有找到匹配词汇。换一个中文、印尼语或分类试试。")
                ))
                return
            }

            filtered.forEach { word ->
                list.addView(
                    wordCard(context, word, hasAudio, onPlay, onLearned, isLearned)
                )
            }
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = render()
            override fun afterTextChanged(s: Editable?) = Unit
        })

        category.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) = render()

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        onlyUnlearned.setOnCheckedChangeListener { _, _ -> render() }
        render()
        return root
    }

    private fun wordCard(
        context: Context,
        word: Vocabulary,
        hasAudio: (String) -> Boolean,
        onPlay: (String) -> Unit,
        onLearned: (String) -> Unit,
        isLearned: (String) -> Boolean
    ): View {
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        box.addView(Ui.label(context, word.category + " · " + word.scene))

        box.addView(Ui.body(context, word.indonesian).apply {
            textSize = 23f
            setTextColor(ContextCompat.getColor(context, R.color.teal_primary))
            setPadding(0, Ui.dp(context, 6), 0, 0)
        })

        box.addView(Ui.body(context, word.chinese).apply {
            setPadding(0, Ui.dp(context, 3), 0, 0)
        })

        if (word.example.isNotBlank()) {
            box.addView(Ui.body(context, "例句 · " + word.example).apply {
                textSize = 14f
                setPadding(0, Ui.dp(context, 6), 0, 0)
            })
        }

        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val audioAvailable = hasAudio(word.indonesian)
        val audio = if (audioAvailable) {
            Ui.button(context, "听一下") { onPlay(word.indonesian) }
        } else {
            Ui.outlineButton(context, "暂无内置语音") {}
        }
        audio.isEnabled = audioAvailable
        actions.addView(
            audio,
            LinearLayout.LayoutParams(0, Ui.dp(context, 46), 1f)
        )

        val learnedButton = if (isLearned(word.id)) {
            Ui.outlineButton(context, "已学会") {}
        } else {
            Ui.secondaryButton(context, "我会了") {
                onLearned(word.id)
            }
        }
        actions.addView(
            learnedButton,
            LinearLayout.LayoutParams(0, Ui.dp(context, 46), 1f).apply {
                leftMargin = Ui.dp(context, 8)
            }
        )
        box.addView(actions.apply {
            setPadding(0, Ui.dp(context, 10), 0, 0)
        })

        return Ui.card(context, box)
    }
}
