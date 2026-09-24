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
        onPlay: (String) -> Unit,
        onSceneDone: () -> Unit
    ): LinearLayout {
        val root = Ui.page(context)
        val scene = repository.scenes().firstOrNull { it.id == sceneId } ?: return root

        root.addView(Ui.label(context, "真实场景"))
        root.addView(Ui.title(context, scene.title))
        root.addView(Ui.subtitle(context, scene.description).apply {
            setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 16))
        })

        root.addView(Ui.card(
            context,
            Ui.body(context, "方法：先听 A/B 对话 → 看中文确认 → 不看屏幕复述 → 再听一遍")
        ))

        scene.lines.forEachIndexed { index, line ->
            val bubble = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = if (line.speaker == "A") Gravity.START else Gravity.END
            }

            bubble.addView(Ui.label(context, "第 " + (index + 1) + " 句 · " + line.speaker))

            bubble.addView(TextView(context).apply {
                text = line.indonesian
                textSize = 19f
                setTextColor(ContextCompat.getColor(context, R.color.teal_primary))
                includeFontPadding = true
                setLineSpacing(0f, 1.2f)
                setPadding(
                    Ui.dp(context, 14),
                    Ui.dp(context, 11),
                    Ui.dp(context, 14),
                    Ui.dp(context, 4)
                )
            })

            bubble.addView(Ui.body(context, line.chinese).apply {
                setPadding(Ui.dp(context, 14), 0, Ui.dp(context, 14), Ui.dp(context, 10))
            })

            val available = hasAudio(line.indonesian)
            val audio = if (available) {
                Ui.button(context, "播放这一句") { onPlay(line.indonesian) }
            } else {
                Ui.outlineButton(context, "暂无内置语音") {}
            }
            audio.isEnabled = available
            bubble.addView(audio)

            root.addView(Ui.card(context, bubble))
        }

        val done = Ui.secondaryButton(context, "我已经走完这段对话") {}
        done.setOnClickListener {
            onSceneDone()
            done.text = "已记录今天场景"
            done.isEnabled = false
        }
        root.addView(done.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(context, 48)
            )
        })

        return root
    }
}
