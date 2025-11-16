package com.uhftag.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.util.Log

object SoundUtils {
    private const val TAG = "SoundUtils"
    
    private var soundPool: SoundPool? = null
    private val soundMap = HashMap<Int, Int>()
    private var audioManager: AudioManager? = null
    private var volumeRatio: Float = 1.0f

    /**
     * Initialize sound pool and load sound files
     */
    fun initSound(context: Context) {
        try {
            // Create SoundPool based on API level
            soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                
                SoundPool.Builder()
                    .setMaxStreams(10)
                    .setAudioAttributes(audioAttributes)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                SoundPool(10, AudioManager.STREAM_MUSIC, 5)
            }

            // Load sound files from raw resources
            soundPool?.let { pool ->
                val resourceId1 = context.resources.getIdentifier("barcodebeep", "raw", context.packageName)
                val resourceId2 = context.resources.getIdentifier("serror", "raw", context.packageName)
                
                if (resourceId1 != 0) {
                    soundMap[SOUND_SUCCESS] = pool.load(context, resourceId1, 1)
                }
                if (resourceId2 != 0) {
                    soundMap[SOUND_ERROR] = pool.load(context, resourceId2, 1)
                }
            }

            // Get audio manager
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            
            Log.d(TAG, "Sound system initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing sound: ${e.message}", e)
        }
    }

    /**
     * Play a sound effect
     * 
     * @param soundId SOUND_SUCCESS (1) for success, SOUND_ERROR (2) for error
     */
    fun playSound(soundId: Int) {
        try {
            audioManager?.let { am ->
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
                val currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                volumeRatio = if (maxVolume > 0) currentVolume / maxVolume else 1.0f
            }

            soundMap[soundId]?.let { soundResourceId ->
                soundPool?.play(
                    soundResourceId,
                    volumeRatio,  // Left volume
                    volumeRatio,  // Right volume
                    1,            // Priority
                    0,            // Loop (0 = no loop)
                    1.0f          // Playback rate
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound: ${e.message}", e)
        }
    }

    /**
     * Release sound pool resources
     */
    fun releaseSound() {
        try {
            soundPool?.release()
            soundPool = null
            soundMap.clear()
            Log.d(TAG, "Sound system released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing sound: ${e.message}", e)
        }
    }

    const val SOUND_SUCCESS = 1
    const val SOUND_ERROR = 2
}
