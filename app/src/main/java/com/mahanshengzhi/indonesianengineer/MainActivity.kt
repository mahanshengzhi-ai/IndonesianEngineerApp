package com.mahanshengzhi.indonesianengineer

import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mahanshengzhi.indonesianengineer.audio.AudioPlayer
import com.mahanshengzhi.indonesianengineer.data.LearningRepository
import com.mahanshengzhi.indonesianengineer.data.ProgressStore
import com.mahanshengzhi.indonesianengineer.model.Vocabulary
import com.mahanshengzhi.indonesianengineer.translation.TranslationEngine
import com.mahanshengzhi.indonesianengineer.ui.MiniPlayerController
import com.mahanshengzhi.indonesianengineer.ui.pages.FlashcardPage
import com.mahanshengzhi.indonesianengineer.ui.pages.HomePage
import com.mahanshengzhi.indonesianengineer.ui.pages.LearnPage
import com.mahanshengzhi.indonesianengineer.ui.pages.PracticePage
import com.mahanshengzhi.indonesianengineer.ui.pages.ProfilePage
import com.mahanshengzhi.indonesianengineer.ui.pages.PronunciationPage
import com.mahanshengzhi.indonesianengineer.ui.pages.QuizPage
import com.mahanshengzhi.indonesianengineer.ui.pages.ScenePage
import com.mahanshengzhi.indonesianengineer.ui.pages.SentencePage
import com.mahanshengzhi.indonesianengineer.ui.pages.TranslatePage
import com.mahanshengzhi.indonesianengineer.ui.pages.VocabularyPage
import com.mahanshengzhi.indonesianengineer.ui.pages.WordDetailPage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(UnstableApi::class)
class MainActivity : AppCompatActivity() {

    enum class LearnModule {
        PRONUNCIATION,
        VOCABULARY,
        SENTENCE,
        SCENE
    }

    enum class PracticeModule {
        FLASHCARD,
        QUIZ
    }

    lateinit var repository: LearningRepository
        private set

    lateinit var progressStore: ProgressStore
        private set

    lateinit var audioPlayer: AudioPlayer
        private set

    lateinit var translationEngine: TranslationEngine
        private set

    lateinit var miniPlayer: MiniPlayerController
        private set

    val backgroundExecutor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor()
    }

    private lateinit var contentContainer: FrameLayout
    private lateinit var bottomNavigation: BottomNavigationView
    private var lastBackAt = 0L
    private var showingSubpage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = LearningRepository(this)
        progressStore = ProgressStore(this)
        audioPlayer = AudioPlayer(this)
        translationEngine = TranslationEngine()

        contentContainer = findViewById(R.id.contentContainer)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        bottomNavigation.inflateMenu(R.menu.bottom_nav)
        bottomNavigation.setOnItemSelectedListener { item ->
            renderRootPage(item.itemId)
            true
        }

        miniPlayer = MiniPlayerController(
            this,
            findViewById(R.id.playerContainer),
            audioPlayer,
            progressStore
        )

        bottomNavigation.selectedItemId = R.id.nav_home

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (showingSubpage || bottomNavigation.selectedItemId != R.id.nav_home) {
                        showingSubpage = false
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
            }
        )
    }

    private fun renderRootPage(itemId: Int) {
        showingSubpage = false
        contentContainer.removeAllViews()

        val view = when (itemId) {
            R.id.nav_home -> HomePage(this, repository, progressStore).build()
            R.id.nav_learn -> LearnPage(this).build()
            R.id.nav_practice -> PracticePage(this, progressStore).build()
            R.id.nav_translate -> TranslatePage(this, translationEngine, progressStore).build()
            R.id.nav_profile -> ProfilePage(this, progressStore).build()
            else -> HomePage(this, repository, progressStore).build()
        }

        addContent(view)
    }

    private fun addContent(view: View) {
        contentContainer.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
    }

    fun showSubpage(navId: Int, view: View) {
        showingSubpage = true
        if (bottomNavigation.selectedItemId != navId) {
            bottomNavigation.selectedItemId = navId
        }
        contentContainer.removeAllViews()
        addContent(view)
    }

    fun openLearnModule(module: LearnModule) {
        when (module) {
            LearnModule.PRONUNCIATION ->
                showSubpage(R.id.nav_learn, PronunciationPage(this, repository).build())

            LearnModule.VOCABULARY ->
                showSubpage(
                    R.id.nav_learn,
                    VocabularyPage(this, repository, progressStore).build()
                )

            LearnModule.SENTENCE ->
                showSubpage(
                    R.id.nav_learn,
                    SentencePage(this, repository, progressStore).build()
                )

            LearnModule.SCENE ->
                showSubpage(
                    R.id.nav_learn,
                    ScenePage(this, repository, progressStore).build()
                )
        }
    }

    fun openPracticeModule(module: PracticeModule) {
        when (module) {
            PracticeModule.FLASHCARD ->
                showSubpage(
                    R.id.nav_practice,
                    FlashcardPage(this, repository, progressStore).build()
                )

            PracticeModule.QUIZ ->
                showSubpage(
                    R.id.nav_practice,
                    QuizPage(this, repository, progressStore).build()
                )
        }
    }

    fun openWord(item: Vocabulary) {
        showSubpage(
            R.id.nav_learn,
            WordDetailPage(this, item, progressStore).build()
        )
    }

    fun hasAudio(text: String): Boolean = audioPlayer.hasAudio(text)

    fun playAudio(text: String) {
        if (!miniPlayer.play(text)) {
            Toast.makeText(this, "暂无内置语音", Toast.LENGTH_SHORT).show()
        }
    }

    fun setAudioSpeed(speed: Float) {
        audioPlayer.setSpeed(speed)
    }

    override fun onDestroy() {
        miniPlayer.dispose()
        audioPlayer.release()
        translationEngine.close()
        backgroundExecutor.shutdownNow()
        super.onDestroy()
    }
}
