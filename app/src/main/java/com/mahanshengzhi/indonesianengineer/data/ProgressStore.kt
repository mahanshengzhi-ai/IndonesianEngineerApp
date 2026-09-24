package com.mahanshengzhi.indonesianengineer.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("learning_progress", Context.MODE_PRIVATE)

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun markLearned(id: String) {
        if (isLearned(id)) return
        prefs.edit()
            .putBoolean("learned_\$id", true)
            .putInt("learned_total", learnedTotal() + 1)
            .apply()
    }

    fun isLearned(id: String): Boolean = prefs.getBoolean("learned_\$id", false)
    fun learnedTotal(): Int = prefs.getInt("learned_total", 0)

    fun markDailyWord() = increment("daily_words_\${today()}")
    fun dailyWords(): Int = prefs.getInt("daily_words_\${today()}", 0)

    fun incrementAudio() = increment("daily_audio_\${today()}")
    fun incrementQuiz() = increment("daily_quiz_\${today()}")
    fun incrementTranslation() = increment("daily_translate_\${today()}")

    fun dailyAudio(): Int = prefs.getInt("daily_audio_\${today()}", 0)
    fun dailyQuiz(): Int = prefs.getInt("daily_quiz_\${today()}", 0)
    fun dailyTranslation(): Int = prefs.getInt("daily_translate_\${today()}", 0)

    fun updateBestQuiz(score: Int) {
        if (score > bestQuiz()) {
            prefs.edit().putInt("quiz_best", score).apply()
        }
    }

    fun bestQuiz(): Int = prefs.getInt("quiz_best", 0)

    fun isFavorite(id: String): Boolean = prefs.getBoolean("favorite_\$id", false)

    fun setFavorite(id: String, favorite: Boolean) {
        prefs.edit().putBoolean("favorite_\$id", favorite).apply()
    }

    fun checkIn() {
        val todayKey = today()
        val editor = prefs.edit()
        if (!prefs.getBoolean("checkin_\$todayKey", false)) {
            val previous = previousDate()
            val streak = if (prefs.getBoolean("checkin_\$previous", false)) {
                prefs.getInt("streak", 0) + 1
            } else {
                1
            }
            editor.putBoolean("checkin_\$todayKey", true)
                .putString("last_checkin", todayKey)
                .putInt("streak", streak)
                .putInt("checkins", prefs.getInt("checkins", 0) + 1)
                .apply()
        }
    }

    fun checkins(): Int = prefs.getInt("checkins", 0)
    fun streak(): Int = prefs.getInt("streak", 0)
    fun lastCheckIn(): String = prefs.getString("last_checkin", "") ?: ""

    fun recordTranslation(text: String) {
        val normalized = text.trim()
        if (normalized.isEmpty()) return
        val old = recentTranslations().toMutableList()
        old.remove(normalized)
        old.add(0, normalized)
        while (old.size > 6) old.removeAt(old.lastIndex)
        prefs.edit().putString("recent_translations", old.joinToString("\u0001")).apply()
    }

    fun recentTranslations(): List<String> =
        prefs.getString("recent_translations", "")
            .orEmpty()
            .split("\u0001")
            .filter { it.isNotEmpty() }

    private fun previousDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    private fun increment(key: String) {
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }
}
