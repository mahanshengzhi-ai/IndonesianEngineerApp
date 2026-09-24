package com.mahanshengzhi.indonesianengineer.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("learning_progress", Context.MODE_PRIVATE)

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun markLearned(id: String) {
        if (isLearned(id)) return
        prefs.edit()
            .putBoolean("learned_${id}", true)
            .putInt("learned_total", learnedTotal() + 1)
            .apply()
    }

    fun isLearned(id: String): Boolean = prefs.getBoolean("learned_${id}", false)
    fun learnedTotal(): Int = prefs.getInt("learned_total", 0)
    fun incrementAudio() = increment("daily_audio_${today()}")
    fun incrementQuiz() = increment("daily_quiz_${today()}")
    fun incrementTranslation() = increment("daily_translate_${today()}")
    fun dailyAudio(): Int = prefs.getInt("daily_audio_${today()}", 0)
    fun dailyQuiz(): Int = prefs.getInt("daily_quiz_${today()}", 0)
    fun dailyTranslation(): Int = prefs.getInt("daily_translate_${today()}", 0)

    fun checkIn() {
        val key = "checkin_${today()}"
        if (!prefs.getBoolean(key, false)) {
            prefs.edit()
                .putBoolean(key, true)
                .putInt("checkins", prefs.getInt("checkins", 0) + 1)
                .apply()
        }
    }

    fun checkins(): Int = prefs.getInt("checkins", 0)

    private fun increment(key: String) {
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }
}
