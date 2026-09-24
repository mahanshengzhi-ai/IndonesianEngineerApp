package com.mahanshengzhi.indonesianengineer

import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
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
import com.mahanshengzhi.indonesianengineer.ui.ProfilePage
import com.mahanshengzhi.indonesianengineer.ui.StudyPage
import com.mahanshengzhi.indonesianengineer.ui.TranslatePage
import com.mahanshengzhi.indonesianengineer.ui.Ui

@UnstableApi
class MainActivity : AppCompatActivity() {

    private lateinit var contentContainer: FrameLayout
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var playerContainer: LinearLayout

    private val repository by lazy { LearningRepository(this) }
    private val progress by lazy { ProgressStore(this) }
    private val translationEngine by lazy { TranslationEngine() }
    private val audioPlayer by lazy { AudioPlayer(this) }

    private var lastBackAt = 0L
    private var playingText = ""

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
                learned = progress.learnedTotal(),
                streak = progress.streak(),
                dailyWords = progress.dailyWords(),
                dailyAudio = progress.dailyAudio(),
                dailyQuiz = progress.dailyQuiz(),
                dailyTranslation = progress.dailyTranslation(),
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
                onPlay = ::playAudio,
                hasAudio = audioPlayer::hasAudio,
                onMarkLearned = {
                    progress.markLearned(it)
                    progress.markDailyWord()
                }
            )
            R.id.nav_practice -> PracticePage.build(
                this,
                progress,
                onMarkQuiz = {
                    progress.incrementQuiz()
                    progress.updateBestQuiz(7)
                    Toast.makeText(this, "这一组完成，已记录 7 / 10", Toast.LENGTH_SHORT).show()
                    renderPage(R.id.nav_practice)
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
        val scroll = Ui.scroll(this, page)
        contentContainer.addView(scroll)
    }

    private fun playAudio(text: String) {
        if (text.isBlank()) {
            Toast.makeText(this, "暂无内置语音", Toast.LENGTH_SHORT).show()
            return
        }
        if (!audioPlayer.hasAudio(text)) {
            Toast.makeText(this, "暂无内置语音", Toast.LENGTH_SHORT).show()
            return
        }
        if (audioPlayer.play(text)) {
            playingText = text
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
            setPadding(Ui.dp(this@MainActivity, 16), Ui.dp(this@MainActivity, 10), Ui.dp(this@MainActivity, 16), Ui.dp(this@MainActivity, 10))
        }

        val title = TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            includeFontPadding = true
            maxLines = 2
        }
        box.addView(title)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        actions.addView(Ui.button(this, "暂停") { audioPlayer.pause() }, LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f))
        actions.addView(Ui.button(this, "0.8x") { audioPlayer.setSpeed(0.8f) }, LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f).apply {
            leftMargin = Ui.dp(this@MainActivity, 6)
        })
        actions.addView(Ui.button(this, "1.0x") { audioPlayer.setSpeed(1.0f) }, LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f).apply {
            leftMargin = Ui.dp(this@MainActivity, 6)
        })
        actions.addView(Ui.button(this, "关闭") {
            audioPlayer.pause()
            playerContainer.removeAllViews()
            playerContainer.visibility = View.GONE
            playingText = ""
        }, LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f).apply {
            leftMargin = Ui.dp(this@MainActivity, 6)
        })
        box.addView(actions)
        playerContainer.addView(box)
        playerContainer.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        audioPlayer.release()
        translationEngine.close()
        super.onDestroy()
    }
}
