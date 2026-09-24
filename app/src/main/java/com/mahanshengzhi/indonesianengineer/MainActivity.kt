package com.mahanshengzhi.indonesianengineer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.mahanshengzhi.indonesianengineer.audio.AudioPlayer
import com.mahanshengzhi.indonesianengineer.data.LearningRepository
import com.mahanshengzhi.indonesianengineer.data.ProgressStore
import com.mahanshengzhi.indonesianengineer.model.Vocabulary
import com.mahanshengzhi.indonesianengineer.translation.TranslationEngine
import com.mahanshengzhi.indonesianengineer.ui.ProfilePage
import com.mahanshengzhi.indonesianengineer.ui.TranslatePage
import com.mahanshengzhi.indonesianengineer.ui.UiKit
import com.mahanshengzhi.indonesianengineer.ui.pages.FlashcardPage
import com.mahanshengzhi.indonesianengineer.ui.pages.HomePage
import com.mahanshengzhi.indonesianengineer.ui.pages.LearnPage
import com.mahanshengzhi.indonesianengineer.ui.pages.PracticePage
import com.mahanshengzhi.indonesianengineer.ui.pages.PronunciationPage
import com.mahanshengzhi.indonesianengineer.ui.pages.QuizPage
import com.mahanshengzhi.indonesianengineer.ui.pages.ScenePage
import com.mahanshengzhi.indonesianengineer.ui.pages.SentencePage
import com.mahanshengzhi.indonesianengineer.ui.pages.VocabularyPage
import com.mahanshengzhi.indonesianengineer.ui.pages.WordDetailPage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@UnstableApi
class MainActivity : AppCompatActivity() {

    enum class LearnModule { PRONUNCIATION, VOCABULARY, SENTENCE, SCENE }
    enum class PracticeModule { FLASHCARD, QUIZ }

    private lateinit var contentContainer: FrameLayout
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var playerContainer: LinearLayout

    private val repository by lazy { LearningRepository(this) }
    private val progress by lazy { ProgressStore(this) }
    private val translationEngine by lazy { TranslationEngine() }
    private val audioPlayer by lazy { AudioPlayer(this) }
    val backgroundExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    private val playerHandler = Handler(Looper.getMainLooper())
    private var playerSeekBar: SeekBar? = null
    private var playerPositionText: TextView? = null
    private var playerDurationText: TextView? = null
    private var playerPlayPauseButton: MaterialButton? = null
    private var subPageVisible = false
    private var lastBackAt = 0L

    private val playerTicker = object : Runnable {
        override fun run() {
            if (!playerContainer.isShown) return
            updatePlayerProgress()
            playerHandler.postDelayed(this, 300L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        contentContainer = findViewById(R.id.contentContainer)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        playerContainer = findViewById(R.id.playerContainer)

        bottomNavigation.inflateMenu(R.menu.bottom_nav)
        bottomNavigation.setOnItemSelectedListener {
            subPageVisible = false
            renderPage(it.itemId)
            true
        }
        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.nav_home
        } else {
            renderPage(bottomNavigation.selectedItemId)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (subPageVisible) {
                    subPageVisible = false
                    bottomNavigation.selectedItemId = R.id.nav_home
                    return
                }
                if (bottomNavigation.selectedItemId != R.id.nav_home) {
                    bottomNavigation.selectedItemId = R.id.nav_home
                    return
                }

                val now = SystemClock.elapsedRealtime()
                if (now - lastBackAt < 1800L) {
                    finish()
                } else {
                    lastBackAt = now
                    Toast.makeText(
                        this@MainActivity,
                        "再按一次退出软件",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    private fun renderPage(itemId: Int) {
        val page: View = when (itemId) {
            R.id.nav_home -> HomePage(this, repository, progress).build()
            R.id.nav_learn -> LearnPage(this).build()
            R.id.nav_practice -> PracticePage(this, progress).build()
            R.id.nav_translate -> TranslatePage.build(this, translationEngine, progress)
            R.id.nav_profile -> ProfilePage.build(this, progress)
            else -> HomePage(this, repository, progress).build()
        }
        contentContainer.removeAllViews()
        contentContainer.addView(page)
    }

    private fun showSubPage(page: View) {
        subPageVisible = true
        contentContainer.removeAllViews()
        contentContainer.addView(page)
    }

    fun openLearnModule(module: LearnModule) {
        when (module) {
            LearnModule.PRONUNCIATION -> showSubPage(
                PronunciationPage(this, repository).build()
            )
            LearnModule.VOCABULARY -> showSubPage(
                VocabularyPage(this, repository, progress).build()
            )
            LearnModule.SENTENCE -> showSubPage(
                SentencePage(this, repository, progress).build()
            )
            LearnModule.SCENE -> showSubPage(
                ScenePage(this, repository, progress).build()
            )
        }
    }

    fun openPracticeModule(module: PracticeModule) {
        when (module) {
            PracticeModule.FLASHCARD -> showSubPage(
                FlashcardPage(this, repository, progress).build()
            )
            PracticeModule.QUIZ -> showSubPage(
                QuizPage(this, repository, progress).build()
            )
        }
    }

    fun openWord(item: Vocabulary) {
        showSubPage(WordDetailPage(this, item, progress).build())
    }

    fun hasAudio(text: String): Boolean = audioPlayer.hasAudio(text)

    fun playAudio(text: String) {
        if (text.isBlank() || !audioPlayer.hasAudio(text)) {
            Toast.makeText(this, "暂无内置语音", Toast.LENGTH_SHORT).show()
            return
        }
        if (audioPlayer.play(text)) {
            progress.incrementAudio()
            showPlayer(text)
        } else {
            Toast.makeText(this, "音频播放失败", Toast.LENGTH_SHORT).show()
        }
    }

    fun setAudioSpeed(speed: Float) {
        audioPlayer.setSpeed(speed)
        updatePlayerProgress()
    }

    private fun showPlayer(text: String) {
        playerContainer.removeAllViews()

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                UiKit.dp(this@MainActivity, 16),
                UiKit.dp(this@MainActivity, 10),
                UiKit.dp(this@MainActivity, 16),
                UiKit.dp(this@MainActivity, 10)
            )
        }

        box.addView(TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            includeFontPadding = true
            maxLines = 2
        })

        playerPositionText = TextView(this).apply {
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            text = "0:00"
        }
        playerDurationText = TextView(this).apply {
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            text = "0:00"
            gravity = android.view.Gravity.END
        }

        val timeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(
                playerPositionText,
                LinearLayout.LayoutParams(0, UiKit.dp(this@MainActivity, 22), 1f)
            )
            addView(
                playerDurationText,
                LinearLayout.LayoutParams(0, UiKit.dp(this@MainActivity, 22), 1f)
            )
        }
        box.addView(timeRow)

        playerSeekBar = SeekBar(this).apply {
            max = 1000
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progressValue: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        val duration = audioPlayer.duration()
                        if (duration > 0L) {
                            audioPlayer.seekTo(duration * progressValue / 1000L)
                        }
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        box.addView(playerSeekBar)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        playerPlayPauseButton = UiKit.primaryButton(this, "暂停").apply {
            minHeight = UiKit.dp(this@MainActivity, 44)
            setOnClickListener {
                audioPlayer.playOrPause()
                updatePlayerProgress()
            }
        }
        actions.addView(
            playerPlayPauseButton,
            LinearLayout.LayoutParams(0, UiKit.dp(this, 44), 1f)
        )

        addPlayerAction(actions, "0.8x") { audioPlayer.setSpeed(0.8f) }
        addPlayerAction(actions, "0.9x") { audioPlayer.setSpeed(0.9f) }
        addPlayerAction(actions, "1.0x") { audioPlayer.setSpeed(1.0f) }
        addPlayerAction(actions, "关闭") {
            audioPlayer.pause()
            closePlayer()
        }

        box.addView(actions)
        playerContainer.addView(box)
        playerContainer.visibility = View.VISIBLE

        updatePlayerProgress()
        playerHandler.removeCallbacks(playerTicker)
        playerHandler.post(playerTicker)
    }

    private fun addPlayerAction(parent: LinearLayout, label: String, action: () -> Unit) {
        val button = UiKit.secondaryButton(this, label)
        button.setOnClickListener { action() }
        parent.addView(
            button,
            LinearLayout.LayoutParams(0, UiKit.dp(this, 44), 1f).apply {
                leftMargin = UiKit.dp(this@MainActivity, 6)
            }
        )
    }

    private fun closePlayer() {
        playerHandler.removeCallbacks(playerTicker)
        playerContainer.removeAllViews()
        playerContainer.visibility = View.GONE
        playerSeekBar = null
        playerPositionText = null
        playerDurationText = null
        playerPlayPauseButton = null
    }

    private fun updatePlayerProgress() {
        val duration = audioPlayer.duration()
        val position = audioPlayer.currentPosition()

        playerSeekBar?.let {
            it.max = 1000
            it.progress = if (duration > 0L) {
                (position * 1000L / duration).toInt().coerceIn(0, 1000)
            } else {
                0
            }
        }

        playerPositionText?.text = formatTime(position)
        playerDurationText?.text = formatTime(duration)
        playerPlayPauseButton?.text =
            if (audioPlayer.isPlaying()) "暂停" else "继续"
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms.coerceAtLeast(0L) / 1000L
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    override fun onDestroy() {
        playerHandler.removeCallbacks(playerTicker)
        backgroundExecutor.shutdownNow()
        audioPlayer.release()
        translationEngine.close()
        super.onDestroy()
    }
}
