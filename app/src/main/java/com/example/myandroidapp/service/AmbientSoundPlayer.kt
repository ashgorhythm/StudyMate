package com.example.myandroidapp.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri

/**
 * Manages ambient sound playback (looping) for the Focus Mode timer.
 *
 * Usage:
 *  AmbientSoundPlayer.play(context, "Rain")   // start ambient sound
 *  AmbientSoundPlayer.stop()                   // stop ambient sound
 *  AmbientSoundPlayer.setVolume(0.8f)          // adjust volume
 */
object AmbientSoundPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSound: String = "Silence"

    /**
     * Available ambient sounds mapped to their raw resource IDs or URL sources.
     * Since bundling audio assets increases APK size, we use free online streaming URLs.
     * If offline support is needed, add mp3 assets in res/raw/ and use R.raw.xxx.
     */
    private val soundUrls = mapOf(
        "Rain"   to "https://www.soundjay.com/nature/rain-01.mp3",
        "Forest" to "https://www.soundjay.com/nature/crickets-1.mp3",
        "Lo-fi"  to "https://www.soundjay.com/ambient/sounds/coffee-shop.mp3"
    )

    /**
     * Play an ambient sound by name. Stops the previous sound first.
     * Pass "Silence" to stop all sounds.
     */
    fun play(context: Context, soundName: String) {
        stop()
        currentSound = soundName

        if (soundName == "Silence") return

        val url = soundUrls[soundName] ?: return

        mediaPlayer = MediaPlayer().apply {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            setAudioAttributes(audioAttributes)
            setDataSource(url)
            isLooping = true
            setOnPreparedListener { start() }
            setOnErrorListener { _, _, _ ->
                // Silent fail — don't crash if sound can't load
                false
            }
            prepareAsync() // Non-blocking
        }
    }

    /**
     * Stop the currently playing sound and release MediaPlayer resources.
     */
    fun stop() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) player.stop()
                player.release()
            } catch (_: Exception) {}
        }
        mediaPlayer = null
        currentSound = "Silence"
    }

    /**
     * Set playback volume (0.0 to 1.0).
     */
    fun setVolume(volume: Float) {
        mediaPlayer?.setVolume(volume, volume)
    }

    /**
     * Returns the name of the currently playing sound.
     */
    fun getCurrentSound(): String = currentSound

    /**
     * Returns true if audio is currently playing.
     */
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true
}
