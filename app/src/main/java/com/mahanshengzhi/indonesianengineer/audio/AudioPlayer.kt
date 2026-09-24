package com.mahanshengzhi.indonesianengineer.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

@UnstableApi
class AudioPlayer(context: Context) {
    private val appContext = context.applicationContext
    private var player: ExoPlayer? = null
    private val index = AudioIndex(appContext)
    private var currentText = ""
    private var currentSpeed = 1.0f

    fun hasAudio(text: String): Boolean = index.find(text) != null

    fun play(text: String): Boolean {
        val segment = index.find(text) ?: return false
        val exists = runCatching {
            appContext.assets.open("tts_audio.m4a").use { }
        }.isSuccess
        if (!exists) return false

        val item = MediaItem.Builder()
            .setUri("asset:///tts_audio.m4a")
            .setMediaMetadata(MediaMetadata.Builder().setTitle(segment.originalText).build())
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(segment.startMs)
                    .setEndPositionMs(segment.startMs + segment.durationMs)
                    .build()
            )
            .build()

        if (player == null) {
            player = ExoPlayer.Builder(appContext).build()
        }

        currentText = segment.originalText
        player?.setMediaItem(item)
        player?.prepare()
        player?.playbackParameters = PlaybackParameters(currentSpeed)
        player?.play()
        return true
    }

    fun pause() {
        player?.pause()
    }

    fun resume() {
        player?.play()
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun setSpeed(speed: Float) {
        currentSpeed = speed.coerceIn(0.8f, 1.0f)
        player?.playbackParameters = PlaybackParameters(currentSpeed)
    }

    fun speed(): Float = currentSpeed
    fun currentText(): String = currentText
    fun isPlaying(): Boolean = player?.isPlaying == true
    fun positionMs(): Long = player?.currentPosition?.coerceAtLeast(0L) ?: 0L
    fun durationMs(): Long = player?.duration?.takeIf { it > 0L } ?: 0L

    fun release() {
        player?.release()
        player = null
    }
}
