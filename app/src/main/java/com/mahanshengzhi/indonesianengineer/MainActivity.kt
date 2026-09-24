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
import com.mahanshengzhi.indonesianengineer.audio.AudioPlayer
import com.mahanshengzhi.indonesianengineer.data.LearningRepository
import com.mahanshengzhi.indonesianengineer.data.ProgressStore
import com.mahanshengzhi.indonesianengineer.translation.TranslationEngine
import com.mahanshengzhi.indonesianengineer.ui.HomePage
import com.mahanshengzhi.indonesianengineer.ui.PracticePage
import com.mahanshengzhi.indonesianengineer.ui.PronunciationPage
import com.mahanshengzhi.indonesianengineer.ui.ProfilePage
import com.mahanshengzhi.indonesianengineer.ui.QuizPage
import com.mahanshengzhi.indonesianengineer.ui.SceneDetailPage
import com.mahanshengzhi.indonesianengineer.ui.ScenePage
import com.mahanshengzhi.indonesianengineer.ui.SentencePage
import com.mahanshengzhi.indonesianengineer.ui.StudyPage
import com.mahanshengzhi.indonesianengineer.ui.TranslatePage
import com.mahanshengzhi.indonesianengineer.ui.Ui
import com.mahanshengzhi.indonesianengineer.ui.VocabularyPage

@UnstableApi
class MainActivity : AppCompatActivity() {

    private lateinit var contentContainer: FrameLayout
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var playerContainer: LinearLayout

    private val repository by lazy { LearningRepository(this) }
    private val progress by lazy { ProgressStore(this) }
    private val translationEngine by lazy { TranslationEngine() }
    private val audioPlayer by lazy { AudioPlayer(this) }

    private val playerHandler = Handler(Looper.getMainLooper())
    private var playerSeekBar: SeekBar? = null
    private var playerPositionText: TextView? = null
    private var playerDurationText: TextView? = null
    private var playerPlayPauseButton: com.google.android.material.button.MaterialButton? = null

    private val playerTicker = object : Runnable {
        override fun run() {
            updatePlayerProgress()
            playerHandler.postDelayed(this, 300L)
        }
    }

    private var lastBackAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        contentContainer = findViewById(R.id.contentContainer)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        playerContainer = findViewById(R.id.playerContainer)

        bottomNavigation.inflateMenu(R.menu.bottom_nav)
        bottomNavigation.setOnItemSelectedListener {
            renderPage(it.itemId)
            true
        }
        bottomNavigation.selectedItemId = R.id.nav_home

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (bottomNavigation.selectedItemId != R.id.nav_home) {
                    bottomNavigation.selectedItemId = R.id.nav_home
                    return
                }
                val now = SystemClock.elapsedRealtime()
                if (now - lastBackAt < 1800L) {
                    finish()
                } else {
                    lastBackAt = now
                    Toast.makeText(this@MainActivity, "再按一次退出软件", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun renderPage(itemId: Int) {
        val page = when (itemId) {
            R.id.nav_home -> HomePage.build(
                this,
                progress.learnedTotal(),
                progress.streak(),
                progress.dailyWords(),
                progress.dailyAudio(),
                progress.dailyQuiz(),
                progress.dailyTranslation(),
                onStart = { bottomNavigation.selectedItemId = R.id.nav_learn },
                onCheckIn = {
                    progress.checkIn()
                    Toast.makeText(this, "今日已打卡，明天继续", Toast.LENGTH_SHORT).show()
                    renderPage(R.id.nav_home)
                },
                onOpenStudy = { bottomNavigation.selectedItemId = R.id.nav_learn }
            )
            R.id.nav_learn -> StudyPage.build(
                this,
                repository,
                audioPlayer::hasAudio,
                ::playAudio,
                onOpenPronunciation = {
                    showSubPage(PronunciationPage.build(this, repository, audioPlayer::hasAudio, ::playAudio))
                },
                onOpenVocabulary = {
                    showSubPage(
                        VocabularyPage.build(
                            this, repository, audioPlayer::hasAudio, ::playAudio
                        ) {
                            progress.markLearned(it)
                            progress.markDailyWord()
                        }
                    )
                },
                onOpenSentences = {
                    showSubPage(SentencePage.build(this, repository, audioPlayer::hasAudio, ::playAudio))
                },
                onOpenScenes = {
                    showSubPage(
                        ScenePage.build(this, repository) { id ->
                            showSubPage(SceneDetailPage.build(this, repository, id, audioPlayer::hasAudio, ::playAudio))
                        }
                    )
                },
                onOpenPractice = { bottomNavigation.selectedItemId = R.id.nav_practice },
                onMarkLearned = {
                    progress.markLearned(it)
                    progress.markDailyWord()
                }
            )
            R.id.nav_practice -> PracticePage.build(
                this,
                progress,
                onStartQuiz = {
                    showSubPage(
                        QuizPage.build(this, repository, progress) {
                            bottomNavigation.selectedItemId = R.id.nav_practice
                        }
                    )
                }
            )
            R.id.nav_translate -> TranslatePage.build(this, translationEngine, progress)
            R.id.nav_profile -> ProfilePage.build(this, progress)
            else -> HomePage.build(
                this, progress.learnedTotal(), progress.streak(), progress.dailyWords(),
                progress.dailyAudio(), progress.dailyQuiz(), progress.dailyTranslation(), {}, {}, {}
            )
        }

        contentContainer.removeAllViews()
        contentContainer.addView(Ui.scroll(this, page))
    }

    private fun showSubPage(page: LinearLayout) {
        contentContainer.removeAllViews()
        contentContainer.addView(Ui.scroll(this, page))
    }

    private fun playAudio(text: String) {
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

    private fun showPlayer(text: String) {
        playerContainer.removeAllViews()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                Ui.dp(this@MainActivity, 16),
                Ui.dp(this@MainActivity, 10),
                Ui.dp(this@MainActivity, 16),
                Ui.dp(this@MainActivity, 10)
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
            addView(playerPositionText, LinearLayout.LayoutParams(0, Ui.dp(this@MainActivity, 22), 1f))
            addView(playerDurationText, LinearLayout.LayoutParams(0, Ui.dp(this@MainActivity, 22), 1f))
        }
        box.addView(timeRow)

        playerSeekBar = SeekBar(this).apply {
            max = 1000
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progressValue: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val duration = audioPlayer.duration()
                        audioPlayer.seekTo(duration * progressValue / 1000L)
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
        playerPlayPauseButton = Ui.button(this, "暂停") {
            audioPlayer.playOrPause()
            updatePlayerProgress()
        }
        actions.addView(playerPlayPauseButton, LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f))
        actions.addView(Ui.button(this, "0.8x") {
            audioPlayer.setSpeed(0.8f)
        }, LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f).apply {
            leftMargin = Ui.dp(this@MainActivity, 6)
        })
        actions.addView(Ui.button(this, "0.9x") {
            audioPlayer.setSpeed(0.9f)
        }, LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f).apply {
            leftMargin = Ui.dp(this@MainActivity, 6)
        })
        actions.addView(Ui.button(this, "1.0x") {
            audioPlayer.setSpeed(1.0f)
        }, LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f).apply {
            leftMargin = Ui.dp(this@MainActivity, 6)
        })
        actions.addView(Ui.button(this, "关闭") {
            audioPlayer.pause()
            closePlayer()
        }, LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f).apply {
            leftMargin = Ui.dp(this@MainActivity, 6)
        })
        box.addView(actions)

        playerContainer.addView(box)
        playerContainer.visibility = View.VISIBLE
        updatePlayerProgress()
        playerHandler.removeCallbacks(playerTicker)
        playerHandler.post(playerTicker)
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
            if (duration > 0L) {
                it.progress = (position * 1000L / duration).toInt().coerceIn(0, 1000)
            }
        }
        playerPositionText?.text = formatTime(position)
        playerDurationText?.text = formatTime(duration)
        playerPlayPauseButton?.text = if (audioPlayer.isPlaying()) "暂停" else "继续"
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    override fun onDestroy() {
        playerHandler.removeCallbacks(playerTicker)
        audioPlayer.release()
        translationEngine.close()
        super.onDestroy()
    }
}
