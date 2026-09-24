package com.mahanshengzhi.indonesianengineer.audio

import android.content.Context
import java.security.MessageDigest

data class AudioSegment(
    val sha256: String,
    val startMs: Long,
    val durationMs: Long,
    val originalText: String
)

class AudioIndex(private val context: Context) {
    private val entries by lazy { load() }

    private fun load(): Map<String, AudioSegment> {
        val result = HashMap<String, AudioSegment>()
        val stream = runCatching { context.assets.open("tts_index.tsv") }.getOrNull() ?: return result
        stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach
                val p = line.split('	')
                if (p.size < 4) return@forEach
                val start = p[1].toLongOrNull() ?: return@forEach
                val duration = p[2].toLongOrNull() ?: return@forEach
                result[p[0]] = AudioSegment(p[0], start, duration, p[3])
            }
        }
        return result
    }

    fun find(text: String): AudioSegment? = entries[sha256(text)]
    fun size(): Int = entries.size

    companion object {
        fun sha256(text: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
