package com.mahanshengzhi.indonesianengineer.ui

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.mahanshengzhi.indonesianengineer.R
import com.mahanshengzhi.indonesianengineer.audio.AudioPlayer
import com.mahanshengzhi.indonesianengineer.data.ProgressStore
import android.app.Activity
import android.os.Handler
import android.os.Looper

class MiniPlayerController(
    private val activity: Activity,
    private val container: ViewGroup,
    private val audioPlayer: AudioPlayer,
    private val progressStore: ProgressStore
) {
    private val titleView: TextView
    private val currentView: TextView
    private val totalView: TextView
    private val seekBar: SeekBar
    private val playButton: MaterialButton
    private val speed08: MaterialButton
    private val speed09: MaterialButton
    private val speed10: MaterialButton
    private val handler = Handler(Looper.getMainLooper())
    private var visible = false

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!visible) return
            val duration = audioPlayer.durationMs()
            val position = audioPlayer.positionMs()
            seekBar.max = duration.toInt().coerceAtLeast(1)
            seekBar.progress = position.coerceIn(0L, duration).toInt()
            currentView.text = formatMs(position)
            totalView.text = formatMs(duration)
            playButton.text = if (audioPlayer.isPlaying()) "暂停" else "播放"
            handler.postDelayed(this, 250L)
        }
    }

    init {
        val card = MaterialCardView(activity).apply {
            radius = UiKit.dp(activity, 16).toFloat()
            cardElevation = 0f
            strokeWidth = UiKit.dp(activity, 1)
            strokeColor = ContextCompat.getColor(activity, R.color.card_stroke)
            setCardBackgroundColor(android.graphics.Color.WHITE)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val body = UiKit.cardContent(activity)
        titleView = UiKit.body(activity, "正在播放", 15f)
        titleView.setTypeface(titleView.typeface, android.graphics.Typeface.BOLD)

        val controls = UiKit.buttonRow(activity)
        playButton = UiKit.secondaryButton(activity, "播放")
        val closeButton = UiKit.secondaryButton(activity, "关闭")
        currentView = UiKit.muted(activity, "0:00", 11f)
        totalView = UiKit.muted(activity, "0:00", 11f)
        seekBar = SeekBar(activity)

        UiKit.addWeightedButton(controls, playButton, activity, 0.15f, 6)
        controls.addView(
            seekBar,
            LinearLayout.LayoutParams(0, UiKit.dp(activity, 32), 0.55f)
        )
        val timeWrap = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        timeWrap.addView(currentView)
        timeWrap.addView(UiKit.muted(activity, " / ", 10f))
        timeWrap.addView(totalView)
        controls.addView(timeWrap, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f))
        UiKit.addWeightedButton(controls, closeButton, activity, 0.1f)

        val speedRow = UiKit.buttonRow(activity)
        speed08 = UiKit.secondaryButton(activity, "0.8x")
        speed09 = UiKit.secondaryButton(activity, "0.9x")
        speed10 = UiKit.secondaryButton(activity, "1.0x")
        speedRow.addView(speed08, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        speedRow.addView(speed09, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        speedRow.addView(speed10, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        body.addView(titleView)
        UiKit.addGap(body, activity, 4)
        body.addView(controls)
        body.addView(speedRow, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = UiKit.dp(activity, 4) })
        card.addView(body)
        container.addView(card)

        playButton.setOnClickListener {
            if (audioPlayer.isPlaying()) audioPlayer.pause() else audioPlayer.resume()
            refresh()
        }
        closeButton.setOnClickListener { hide() }
        speed08.setOnClickListener { audioPlayer.setSpeed(0.8f); refresh() }
        speed09.setOnClickListener { audioPlayer.setSpeed(0.9f); refresh() }
        speed10.setOnClickListener { audioPlayer.setSpeed(1.0f); refresh() }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) currentView.text = formatMs(progress.toLong())
            }
            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) {
                audioPlayer.seekTo(seekBar.progress.toLong())
                refresh()
            }
        })
    }

    fun play(text: String): Boolean {
        if (!audioPlayer.hasAudio(text)) return false
        val played = audioPlayer.play(text)
        if (!played) return false
        progressStore.incrementAudio()
        show(text)
        return true
    }

    fun show(text: String) {
        titleView.text = text
        visible = true
        container.visibility = android.view.View.VISIBLE
        refresh()
        handler.removeCallbacks(updateRunnable)
        handler.post(updateRunnable)
    }

    fun hide() {
        visible = false
        handler.removeCallbacks(updateRunnable)
        audioPlayer.pause()
        container.visibility = android.view.View.GONE
    }

    fun dispose() {
        visible = false
        handler.removeCallbacks(updateRunnable)
        container.visibility = android.view.View.GONE
    }

    private fun refresh() {
        activity.runOnUiThread(updateRunnable)
    }

    private fun formatMs(value: Long): String {
        val totalSeconds = value.coerceAtLeast(0L) / 1000L
        return (totalSeconds / 60L).toString() + ":" + (totalSeconds % 60L).toString().padStart(2, '0')
    }
}
