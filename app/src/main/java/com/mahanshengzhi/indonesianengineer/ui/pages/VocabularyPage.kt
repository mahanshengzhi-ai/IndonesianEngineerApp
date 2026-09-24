package com.mahanshengzhi.indonesianengineer.ui.pages

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mahanshengzhi.indonesianengineer.MainActivity
import com.mahanshengzhi.indonesianengineer.data.LearningRepository
import com.mahanshengzhi.indonesianengineer.data.ProgressStore
import com.mahanshengzhi.indonesianengineer.model.Vocabulary
import com.mahanshengzhi.indonesianengineer.ui.UiKit
import java.util.Locale

class VocabularyPage(
    private val activity: MainActivity,
    private val repository: LearningRepository,
    private val progressStore: ProgressStore
) {
    private var allItems = emptyList<Vocabulary>()
    private val shownItems = mutableListOf<Vocabulary>()
    private var selectedCategory = "全部"

    private lateinit var recyclerView: RecyclerView
    private lateinit var loading: android.widget.TextView
    private lateinit var search: TextInputEditText
    private lateinit var spinner: Spinner
    private lateinit var adapter: VocabularyAdapter

    fun build(): View {
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(
                androidx.core.content.ContextCompat.getColor(
                    activity,
                    com.mahanshengzhi.indonesianengineer.R.color.page_background
                )
            )
            setPadding(
                UiKit.dp(activity, 18),
                UiKit.dp(activity, 18),
                UiKit.dp(activity, 18),
                UiKit.dp(activity, 14)
            )
        }

        root.addView(UiKit.title(activity, "工程词汇"))
        root.addView(
            UiKit.subtitle(
                activity,
                "搜索中文、印尼语或分类。列表先显示匹配结果，避免把 3,574 个词一次挤在屏幕上。"
            )
        )
        UiKit.addGap(root, activity, 8)

        val searchLayout = TextInputLayout(activity).apply {
            hint = "搜索词汇"
        }
        search = TextInputEditText(activity).apply {
            setSingleLine(true)
        }
        searchLayout.addView(search)
        root.addView(searchLayout)

        spinner = Spinner(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                UiKit.dp(activity, 48)
            ).apply {
                topMargin = UiKit.dp(activity, 6)
            }
        }
        root.addView(spinner)

        loading = UiKit.muted(activity, "正在整理 3,574 个词…", 13f)
        root.addView(loading)

        recyclerView = RecyclerView(activity).apply {
            layoutManager = LinearLayoutManager(activity)
            setHasFixedSize(false)
            itemAnimator = null
        }
        root.addView(
            recyclerView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = UiKit.dp(activity, 8)
            }
        )

        adapter = VocabularyAdapter()
        recyclerView.adapter = adapter

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                applyFilter()
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        activity.backgroundExecutor.execute {
            val loaded = repository.vocabulary().toList()
            activity.runOnUiThread {
                allItems = loaded
                val categories = listOf("全部") + loaded
                    .map { it.category }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()

                spinner.adapter = ArrayAdapter(
                    activity,
                    android.R.layout.simple_spinner_item,
                    categories
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }

                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        selectedCategory = categories.getOrElse(position) { "全部" }
                        applyFilter()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                }

                loading.text = "共 " + loaded.size + " 个词"
                applyFilter()
            }
        }

        return root
    }

    private fun applyFilter() {
        val query = search.text?.toString()?.trim()?.lowercase(Locale.getDefault()).orEmpty()

        val filtered = allItems.asSequence()
            .filter {
                selectedCategory == "全部" || it.category == selectedCategory
            }
            .filter {
                query.isBlank() ||
                    it.indonesian.lowercase(Locale.getDefault()).contains(query) ||
                    it.chinese.contains(query) ||
                    it.scene.contains(query)
            }
            .take(200)
            .toList()

        shownItems.clear()
        shownItems.addAll(filtered)
        adapter.submit(shownItems)

        loading.text = if (allItems.isEmpty()) {
            "正在整理 3,574 个词…"
        } else {
            "显示 " + filtered.size + " 个匹配词"
        }
    }

    private inner class VocabularyAdapter : RecyclerView.Adapter<VocabularyHolder>() {
        private val data = mutableListOf<Vocabulary>()

        fun submit(list: List<Vocabulary>) {
            data.clear()
            data.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VocabularyHolder {
            return VocabularyHolder(UiKit.card(activity))
        }

        override fun onBindViewHolder(holder: VocabularyHolder, position: Int) {
            holder.bind(data[position])
        }

        override fun getItemCount(): Int = data.size
    }

    private inner class VocabularyHolder(
        private val card: MaterialCardView
    ) : RecyclerView.ViewHolder(card) {

        fun bind(item: Vocabulary) {
            val content = UiKit.cardContent(activity)
            content.addView(UiKit.body(activity, item.indonesian, 21f))
            UiKit.addGap(content, activity, 3)
            content.addView(UiKit.body(activity, item.chinese, 16f))
            UiKit.addGap(content, activity, 6)
            content.addView(UiKit.muted(activity, item.category + " · " + item.scene, 13f))

            if (item.example.isNotBlank()) {
                UiKit.addGap(content, activity, 6)
                content.addView(UiKit.body(activity, "例句：" + item.example, 14f))
            }

            val actions = UiKit.buttonRow(activity)

            if (activity.hasAudio(item.indonesian)) {
                val audio = UiKit.secondaryButton(activity, "听发音")
                audio.setOnClickListener { activity.playAudio(item.indonesian) }
                UiKit.addWeightedButton(actions, audio, activity, 0.34f, 6)
            }

            val learned = UiKit.secondaryButton(
                activity,
                if (progressStore.isLearned(item.id)) "已掌握" else "我会了"
            )
            learned.setOnClickListener {
                progressStore.markLearned(item.id)
                progressStore.markDailyWord()
                learned.text = "已掌握"
            }
            UiKit.addWeightedButton(actions, learned, activity, 0.33f, 6)

            val favorite = UiKit.secondaryButton(
                activity,
                if (progressStore.isFavorite(item.id)) "已收藏" else "收藏"
            )
            favorite.setOnClickListener {
                val next = !progressStore.isFavorite(item.id)
                progressStore.setFavorite(item.id, next)
                favorite.text = if (next) "已收藏" else "收藏"
            }
            UiKit.addWeightedButton(actions, favorite, activity, 0.33f)

            content.addView(actions)
            card.removeAllViews()
            card.addView(content)
            card.setOnClickListener { activity.openWord(item) }
        }
    }
}
