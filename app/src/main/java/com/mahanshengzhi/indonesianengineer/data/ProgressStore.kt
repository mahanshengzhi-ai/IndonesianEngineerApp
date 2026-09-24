package com.mahanshengzhi.indonesianengineer.data

import android.content.Context
import com.mahanshengzhi.indonesianengineer.translation.TranslationDirection
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TranslationRecord(
    val direction: TranslationDirection,
    val input: String,
    val output: String
)

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("learning_progress", Context.MODE_PRIVATE)

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun markLearned(id: String) {
        if (isLearned(id)) return
        prefs.edit()
            .putBoolean("learned_" + id, true)
            .putInt("learned_total", learnedTotal() + 1)
            .apply()
    }

    fun isLearned(id: String): Boolean = prefs.getBoolean("learned_" + id, false)
    fun learnedTotal(): Int = prefs.getInt("learned_total", 0)

    fun markDailyWord() = increment("daily_words_" + today())
    fun dailyWords(): Int = prefs.getInt("daily_words_" + today(), 0)

    fun markDailySentence() = increment("daily_sentences_" + today())
    fun dailySentences(): Int = prefs.getInt("daily_sentences_" + today(), 0)

    fun markDailyScene() = increment("daily_scene_" + today())
    fun dailyScene(): Int = prefs.getInt("daily_scene_" + today(), 0)

    fun markDailyFlashcard() = increment("daily_flashcards_" + today())
    fun dailyFlashcards(): Int = prefs.getInt("daily_flashcards_" + today(), 0)

    fun incrementAudio() = increment("daily_audio_" + today())
    fun incrementQuiz() = increment("daily_quiz_" + today())
    fun incrementTranslation() = increment("daily_translate_" + today())

    fun dailyAudio(): Int = prefs.getInt("daily_audio_" + today(), 0)
    fun dailyQuiz(): Int = prefs.getInt("daily_quiz_" + today(), 0)
    fun dailyTranslation(): Int = prefs.getInt("daily_translate_" + today(), 0)

    fun updateBestQuiz(score: Int) {
        if (score > bestQuiz()) {
            prefs.edit().putInt("quiz_best", score).apply()
        }
    }

    fun bestQuiz(): Int = prefs.getInt("quiz_best", 0)

    fun isFavorite(id: String): Boolean = prefs.getBoolean("favorite_" + id, false)

    fun setFavorite(id: String, favorite: Boolean) {
        prefs.edit().putBoolean("favorite_" + id, favorite).apply()
    }

    fun checkIn() {
        val todayKey = today()
        if (isCheckedInToday()) return

        val previous = previousDate()
        val streak = if (prefs.getBoolean("checkin_" + previous, false)) {
            prefs.getInt("streak", 0) + 1
        } else {
            1
        }

        prefs.edit()
            .putBoolean("checkin_" + todayKey, true)
            .putString("last_checkin", todayKey)
            .putInt("streak", streak)
            .putInt("checkins", prefs.getInt("checkins", 0) + 1)
            .apply()
    }

    fun isCheckedInToday(): Boolean =
        prefs.getBoolean("checkin_" + today(), false)

    fun checkins(): Int = prefs.getInt("checkins", 0)
    fun streak(): Int = prefs.getInt("streak", 0)
    fun lastCheckIn(): String = prefs.getString("last_checkin", "") ?: ""

    fun recordTranslation(
        direction: TranslationDirection,
        input: String,
        output: String
    ) {
        val normalizedInput = input.trim()
        val normalizedOutput = output.trim()
        if (normalizedInput.isEmpty() || normalizedOutput.isEmpty()) return

        val records = recentTranslationRecords().toMutableList()
        records.removeAll {
            it.direction == direction &&
                it.input == normalizedInput &&
                it.output == normalizedOutput
        }
        records.add(
            0,
            TranslationRecord(direction, normalizedInput, normalizedOutput)
        )

        while (records.size > 6) {
            records.removeAt(records.lastIndex)
        }

        val encoded = records.joinToString("\u0001") {
            listOf(it.direction.name, it.input, it.output).joinToString("\u0002")
        }
        prefs.edit().putString("recent_translation_records", encoded).apply()
    }

    fun recentTranslationRecords(): List<TranslationRecord> =
        prefs.getString("recent_translation_records", "")
            .orEmpty()
            .split("\u0001")
            .filter { it.isNotBlank() }
            .mapNotNull { item ->
                val fields = item.split("\u0002", limit = 3)
                if (fields.size != 3) return@mapNotNull null
                val direction = runCatching {
                    TranslationDirection.valueOf(fields[0])
                }.getOrNull() ?: return@mapNotNull null
                TranslationRecord(direction, fields[1], fields[2])
            }

    fun recentTranslations(): List<String> =
        recentTranslationRecords().map { it.input }

    private fun previousDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    private fun increment(key: String) {
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }
}
