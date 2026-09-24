package com.mahanshengzhi.indonesianengineer.ui

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.data.LearningRepository

object SceneDetailPage {
    fun build(
        context: Context,
        repository: LearningRepository,
        sceneId: String,
        hasAudio: (String) -> Boolean,
        onPlay: (String) -> Unit
    ): LinearLayout {
        val root = Ui.page(context)
        val scene = repository.scenes().firstOrNull { it.id == sceneId } ?: return root

        root.addView(Ui.label(context, "真实场景"))
        root.addView(Ui.title(context, scene.title))
        root.addView(Ui.subtitle(context, scene.description).apply {
            setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 18))
        })

        scene.lines.forEachIndexed { index, line ->
            val bubble = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = if (line.speaker == "A") Gravity.START else Gravity.END
            }

            val speaker = Ui.label(context, "第 " + (index + 1) + " 句 · " + line.speaker)
            bubble.addView(speaker)

            val indonesian = TextView(context).apply {
                text = line.indonesian
                textSize = 19f
                setTextColor(ContextCompat.getColor(context, R.color.teal_primary))
                includeFontPadding = true
                setLineSpacing(0f, 1.2f)
                setPadding(Ui.dp(context, 14), Ui.dp(context, 12), Ui.dp(context, 14), Ui.dp(context, 4))
            }
            bubble.addView(indonesian)

            bubble.addView(Ui.body(context, line.chinese).apply {
                setPadding(Ui.dp(context, 14), 0, Ui.dp(context, 14), Ui.dp(context, 10))
            })

            val available = hasAudio(line.indonesian)
            val audio = Ui.button(context, if (available) "播放这一句" else "暂无内置语音") {
                if (available) onPlay(line.indonesian)
            }
            audio.isEnabled = available
            bubble.addView(audio.apply {
                setPadding(Ui.dp(context, 10), 0, Ui.dp(context, 10), Ui.dp(context, 8))
            })

            root.addView(Ui.card(context, bubble))
        }

        return root
    }
}
