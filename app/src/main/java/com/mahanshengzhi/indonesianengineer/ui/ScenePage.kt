package com.mahanshengzhi.indonesianengineer.ui

import android.content.Context
import android.widget.LinearLayout
import com.mahanshengzhi.indonesianengineer.data.LearningRepository

object ScenePage {
    fun build(context: Context, repository: LearningRepository, onOpen: (String) -> Unit): LinearLayout {
        val root = Ui.page(context)
        root.addView(Ui.title(context, "真实场景"))
        root.addView(Ui.subtitle(context, "不是词表，而是一段可以真正走完的现场交流。").apply {
            setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 18))
        })

        repository.scenes().forEachIndexed { index, scene ->
            val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            box.addView(Ui.label(context, "场景 " + (index + 1)))
            box.addView(Ui.body(context, scene.title).apply { textSize = 21f })
            box.addView(Ui.body(context, scene.description).apply {
                setPadding(0, Ui.dp(context, 5), 0, Ui.dp(context, 10))
            })
            box.addView(Ui.button(context, "进入对话") { onOpen(scene.id) })
            root.addView(Ui.card(context, box))
        }
        return root
    }
}
