package com.mahanshengzhi.indonesianengineer.ui.pages

import android.view.View
import android.widget.LinearLayout
import com.mahanshengzhi.indonesianengineer.MainActivity
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.ui.UiKit

class LearnPage(private val activity: MainActivity) {
    fun build(): View {
        val pair = UiKit.pageScroll(activity)
        val body = pair.second

        body.addView(UiKit.title(activity, "学习"))
        body.addView(
            UiKit.subtitle(
                activity,
                "按「发音 → 词汇 → 句型 → 场景」顺着学，不用自己猜下一步。"
            )
        )
        UiKit.addGap(body, activity, 16)

        addModule(
            body,
            "01 · 发音入门",
            "26 个字母 + c / j / g / ng / ny / sy / kh / h / r / e 等规律",
            "先听懂印尼语的声音，再记单词",
            "发音入门",
            MainActivity.LearnModule.PRONUNCIATION,
            R.color.teal_light
        )
        addModule(
            body,
            "02 · 工程词汇",
            "3,574 个词，按施工、安全、测量、材料、机械、质量等场景组织",
            "学会可以马上在现场用的词",
            "进入词库",
            MainActivity.LearnModule.VOCABULARY,
            R.color.card_green
        )
        addModule(
            body,
            "03 · 核心句型",
            "40 个可以直接套用的现场表达",
            "从“会词”走到“会说一句话”",
            "开始句型",
            MainActivity.LearnModule.SENTENCE,
            R.color.card_blue
        )
        addModule(
            body,
            "04 · 真实场景",
            "10 个完整 A/B 对话：进工地、材料、图纸、安全、测量、餐厅、打车等",
            "把学到的表达放进真实交流里",
            "练一个场景",
            MainActivity.LearnModule.SCENE,
            R.color.warm_orange_light
        )

        return pair.first
    }

    private fun addModule(
        parent: LinearLayout,
        title: String,
        desc: String,
        outcome: String,
        buttonText: String,
        module: MainActivity.LearnModule,
        background: Int
    ) {
        val card = UiKit.card(activity, background)
        val content = UiKit.cardContent(activity)
        content.addView(UiKit.section(activity, title))
        UiKit.addGap(content, activity, 5)
        content.addView(UiKit.body(activity, desc, 15f))
        UiKit.addGap(content, activity, 7)
        content.addView(UiKit.muted(activity, "学完之后：" + outcome, 13f))
        val button = UiKit.primaryButton(activity, buttonText)
        button.setOnClickListener { activity.openLearnModule(module) }
        content.addView(button)
        card.addView(content)
        parent.addView(card)
    }
}
