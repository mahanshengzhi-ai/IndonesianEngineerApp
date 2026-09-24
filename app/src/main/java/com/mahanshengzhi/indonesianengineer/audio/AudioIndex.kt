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
        val result = LinkedHashMap<String, AudioSegment>()
        val stream = runCatching { context.assets.open("tts_index.tsv") }.getOrNull() ?: return result
        stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach
                val parts = line.split('\t')
                if (parts.size < 4) return@forEach

                val sha = parts[0].trim().lowercase()
                val start = parts[1].toLongOrNull() ?: return@forEach
                val duration = parts[2].toLongOrNull() ?: return@forEach
                val originalText = parts[3]

                if (sha.length != 64 || sha256(originalText) != sha) return@forEach
                if (start < 0L || duration <= 0L) return@forEach

                val segment = AudioSegment(sha, start, duration, originalText)
                if (!result.containsKey(sha)) {
                    result[sha] = segment
                }
            }
        }
        return result
    }

    fun find(text: String): AudioSegment? = entries[sha256(text)]

    fun size(): Int = entries.size

    companion object {
        fun sha256(text: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(text.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
