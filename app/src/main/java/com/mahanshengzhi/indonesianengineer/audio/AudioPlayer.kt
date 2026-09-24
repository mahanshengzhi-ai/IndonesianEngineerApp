package com.mahanshengzhi.indonesianengineer.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

@UnstableApi
class AudioPlayer(context: Context) {
    private val appContext = context.applicationContext
    private var player: ExoPlayer? = null
    private val index = AudioIndex(appContext)

    fun hasAudio(text: String): Boolean = index.find(text) != null

    fun play(text: String): Boolean {
        val segment = index.find(text) ?: return false
        if (runCatching { appContext.assets.openFd("tts_audio.m4a") }.isFailure) return false

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
        player?.setMediaItem(item)
        player?.prepare()
        player?.play()
        return true
    }

    fun pause() { player?.pause() }

    fun release() {
        player?.release()
        player = null
    }
}
