package com.mahanshengzhi.indonesianengineer.ui.pages

import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import com.mahanshengzhi.indonesianengineer.MainActivity
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.LearningRepository
import com.mahanshengzhi.indonesianengineer.data.ProgressStore
import com.mahanshengzhi.indonesianengineer.model.LearningScene
import com.mahanshengzhi.indonesianengineer.model.SceneLine
import com.mahanshengzhi.indonesianengineer.ui.UiKit

class ScenePage(
    private val activity: MainActivity,
    private val repository: LearningRepository,
    private val progressStore: ProgressStore
) {
    private lateinit var sceneContainer: LinearLayout
    private var scenes: List<LearningScene> = emptyList()

    fun build(): View {
        val pair = UiKit.pageScroll(activity)
        val body = pair.second

        body.addView(UiKit.title(activity, "真实场景"))
        body.addView(
            UiKit.subtitle(
                activity,
                "不要只记单词。把一句一句的表达放进完整交流里，才能真正用于工地和日常生活。"
            )
        )
        UiKit.addGap(body, activity, 12)

        scenes = repository.scenes()

        val tabs = HorizontalScrollView(activity)
        val tabRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        scenes.forEachIndexed { index, scene ->
            val button = UiKit.secondaryButton(
                activity,
                (index + 1).toString() + ". " + scene.title
            )
            button.setOnClickListener { showScene(index) }
            tabRow.addView(
                button,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    rightMargin = UiKit.dp(activity, 6)
                }
            )
        }

        tabs.addView(tabRow)
        body.addView(tabs)
        UiKit.addGap(body, activity, 12)

        sceneContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        body.addView(sceneContainer)

        if (scenes.isNotEmpty()) {
            showScene(0)
        }

        return pair.first
    }

    private fun showScene(index: Int) {
        if (!::sceneContainer.isInitialized || scenes.isEmpty()) return

        val scene = scenes[index.coerceIn(0, scenes.lastIndex)]
        sceneContainer.removeAllViews()

        val intro = UiKit.card(activity, R.color.warm_orange_light)
        val introContent = UiKit.cardContent(activity)
        introContent.addView(UiKit.section(activity, scene.title))
        UiKit.addGap(introContent, activity, 4)
        introContent.addView(UiKit.body(activity, scene.description, 15f))
        intro.addView(introContent)
        sceneContainer.addView(intro)

        scene.lines.forEachIndexed { lineIndex, line ->
            addLine(sceneContainer, lineIndex + 1, line)
        }

        val finish = UiKit.primaryButton(activity, "完成这个场景")
        finish.setOnClickListener {
            progressStore.markDailyScene()
            finish.text = "今天已完成这个场景"
            finish.isEnabled = false
        }
        sceneContainer.addView(finish)
    }

    private fun addLine(parent: LinearLayout, index: Int, line: SceneLine) {
        val card = UiKit.card(activity)
        val content = UiKit.cardContent(activity)

        content.addView(UiKit.kicker(activity, line.speaker + " · " + index))
        content.addView(UiKit.body(activity, line.indonesian, 19f))
        UiKit.addGap(content, activity, 6)

        val chinese = UiKit.body(activity, line.chinese, 15f)
        chinese.visibility = View.GONE
        content.addView(chinese)

        val actions = UiKit.buttonRow(activity)
        val listen = UiKit.secondaryButton(activity, "播放")
        val reveal = UiKit.secondaryButton(activity, "看中文")

        listen.setOnClickListener {
            activity.playAudio(line.indonesian)
        }

        reveal.setOnClickListener {
            chinese.visibility = if (chinese.visibility == View.VISIBLE) {
                View.GONE
            } else {
                View.VISIBLE
            }
            reveal.text = if (chinese.visibility == View.VISIBLE) "隐藏中文" else "看中文"
        }

        UiKit.addWeightedButton(actions, listen, activity, 0.5f, 6)
        UiKit.addWeightedButton(actions, reveal, activity, 0.5f)
        content.addView(actions)

        card.addView(content)
        parent.addView(card)
    }
}
