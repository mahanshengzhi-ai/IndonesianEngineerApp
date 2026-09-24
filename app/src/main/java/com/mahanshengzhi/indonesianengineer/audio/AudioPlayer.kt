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
    private var requestedSpeed = 0.88f

    fun hasAudio(text: String): Boolean = index.find(text) != null

    fun play(text: String): Boolean {
        val segment = index.find(text) ?: return false
        val exists = runCatching {
            appContext.assets.open("tts_audio.m4a").use { }
        }.isSuccess
        if (!exists) return false

        val item = MediaItem.Builder()
            .setUri("asset:///tts_audio.m4a")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(segment.originalText)
                    .build()
            )
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(segment.startMs)
                    .setEndPositionMs(segment.startMs + segment.durationMs)
                    .build()
            )
            .build()

        if (player == null) {
            player = ExoPlayer.Builder(appContext).build().also {
                it.playbackParameters = PlaybackParameters(requestedSpeed)
            }
        }

        player?.setMediaItem(item)
        player?.playbackParameters = PlaybackParameters(requestedSpeed)
        player?.prepare()
        player?.play()
        return true
    }

    fun playOrPause() {
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun pause() {
        player?.pause()
    }

    fun setSpeed(speed: Float) {
        requestedSpeed = speed.coerceIn(0.8f, 1.0f)
        player?.playbackParameters = PlaybackParameters(requestedSpeed)
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun isPlaying(): Boolean = player?.isPlaying == true
    fun currentPosition(): Long = player?.currentPosition ?: 0L
    fun duration(): Long = player?.duration?.takeIf { it > 0L } ?: 0L

    fun release() {
        player?.release()
        player = null
    }
}
