package com.mahanshengzhi.indonesianengineer.ui.pages

import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import com.mahanshengzhi.indonesianengineer.MainActivity
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.LearningRepository
import com.mahanshengzhi.indonesianengineer.model.LetterPronunciation
import com.mahanshengzhi.indonesianengineer.ui.UiKit

class PronunciationPage(
    private val activity: MainActivity,
    private val repository: LearningRepository
) {
    private lateinit var detailContainer: LinearLayout
    private var letters: List<LetterPronunciation> = emptyList()

    fun build(): View {
        val pair = UiKit.pageScroll(activity)
        val body = pair.second

        body.addView(UiKit.title(activity, "发音入门"))
        body.addView(
            UiKit.subtitle(
                activity,
                "不做中文谐音。这里用原文、规律、示例和标准音频，帮助你建立印尼语的声音感觉。"
            )
        )
        UiKit.addGap(body, activity, 12)

        body.addView(UiKit.section(activity, "26 个字母"))
        UiKit.addGap(body, activity, 6)

        letters = repository.letters()
        val scrollLetters = HorizontalScrollView(activity)
        val letterRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        letters.forEachIndexed { index, letter ->
            val button = UiKit.secondaryButton(activity, letter.letter)
            button.minWidth = UiKit.dp(activity, 48)
            button.setOnClickListener { showLetter(index) }
            letterRow.addView(
                button,
                LinearLayout.LayoutParams(
                    UiKit.dp(activity, 48),
                    UiKit.dp(activity, 44)
                ).apply {
                    rightMargin = UiKit.dp(activity, 6)
                }
            )
        }

        scrollLetters.addView(letterRow)
        body.addView(scrollLetters)
        UiKit.addGap(body, activity, 12)

        detailContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        body.addView(detailContainer)

        if (letters.isNotEmpty()) {
            showLetter(0)
        }

        addRules(body)
        return pair.first
    }

    private fun showLetter(index: Int) {
        if (!::detailContainer.isInitialized || letters.isEmpty()) return

        val letter = letters[index.coerceIn(0, letters.lastIndex)]
        detailContainer.removeAllViews()

        val card = UiKit.card(activity)
        val content = UiKit.cardContent(activity)
        content.addView(UiKit.section(activity, letter.letter))
        UiKit.addGap(content, activity, 4)
        content.addView(UiKit.body(activity, "字母名称：" + letter.name, 16f))
        UiKit.addGap(content, activity, 4)
        content.addView(UiKit.body(activity, "发音提示：" + letter.hint, 15f))
        UiKit.addGap(content, activity, 4)
        content.addView(UiKit.body(activity, "示例：" + letter.example, 18f))

        val audioRow = UiKit.buttonRow(activity)
        val hasAudio = activity.hasAudio(letter.example)
        val slow = UiKit.secondaryButton(activity, if (hasAudio) "慢速听" else "暂无内置语音")
        val normal = UiKit.secondaryButton(activity, if (hasAudio) "正常听" else "暂无内置语音")
        slow.isEnabled = hasAudio
        normal.isEnabled = hasAudio
        UiKit.addWeightedButton(audioRow, slow, activity, 0.5f, 6)
        UiKit.addWeightedButton(audioRow, normal, activity, 0.5f)

        slow.setOnClickListener {
            activity.setAudioSpeed(0.8f)
            activity.playAudio(letter.example)
        }
        normal.setOnClickListener {
            activity.setAudioSpeed(1.0f)
            activity.playAudio(letter.example)
        }

        content.addView(audioRow)
        card.addView(content)
        detailContainer.addView(card)
    }

    private fun addRules(parent: LinearLayout) {
        val card = UiKit.card(activity, R.color.card_blue)
        val content = UiKit.cardContent(activity)
        content.addView(UiKit.section(activity, "中国人最值得先听熟的规律"))
        UiKit.addGap(content, activity, 8)

        val rules = listOf(
            "c：常见时读接近 /tʃ/，如 cari",
            "j：常见时读接近 /dʒ/，如 jalan",
            "g：词首通常是清晰的硬 g，如 gambar",
            "ng：常见鼻音组合，不要拆成两个字母分别读",
            "ny：常见鼻音组合，如 nyanyi",
            "sy：常见外来词组合，接近 /ʃ/",
            "kh：多见于阿拉伯语来源词，注意摩擦音",
            "h：不要吞掉，很多现场词中清晰可闻",
            "r：练习舌尖振动或闪音，保持连续而轻",
            "e：结合词形和示例去听，不用中文拼音硬套"
        )
        rules.forEach { content.addView(UiKit.muted(activity, it, 14f)) }

        card.addView(content)
        parent.addView(card)
    }
}
