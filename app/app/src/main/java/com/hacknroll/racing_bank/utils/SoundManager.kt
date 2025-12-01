package com.hacknroll.racing_bank.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.hacknroll.racing_bank.R

object SoundManager {
    
    private var soundPool: SoundPool? = null
    private var vibrator: Vibrator? = null
    private var sessionManager: SessionManager? = null
    
    private var soundIds = mutableMapOf<SoundType, Int>()
    private var streamIds = mutableMapOf<SoundType, Int>()
    
    enum class SoundType {
        CLICK,
        SUCCESS,
        ERROR,
        COIN,
        NOTIFICATION,
        LOGIN,
        LOGOUT,
        TRANSFER,
        TYPING,
        SCAN
    }
    
    fun initialize(context: Context) {
        sessionManager = SessionManager(context)
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()
        
        // Load sounds (these would be actual sound files in res/raw/)
        loadSounds(context)
    }
    
    private fun loadSounds(context: Context) {
        soundPool?.let { pool ->
            soundIds[SoundType.CLICK] = pool.load(context, R.raw.retro_click, 1)
            soundIds[SoundType.SUCCESS] = pool.load(context, R.raw.retro_success, 1)
            soundIds[SoundType.ERROR] = pool.load(context, R.raw.retro_error, 1)
            soundIds[SoundType.COIN] = pool.load(context, R.raw.retro_coin, 1)
            soundIds[SoundType.NOTIFICATION] = pool.load(context, R.raw.retro_notification, 1)
            soundIds[SoundType.LOGIN] = pool.load(context, R.raw.retro_login, 1)
            soundIds[SoundType.LOGOUT] = pool.load(context, R.raw.retro_logout, 1)
            soundIds[SoundType.TRANSFER] = pool.load(context, R.raw.retro_transfer, 1)
            soundIds[SoundType.TYPING] = pool.load(context, R.raw.retro_typing, 1)
            soundIds[SoundType.SCAN] = pool.load(context, R.raw.retro_scan, 1)
        }
    }
    
    fun playSound(soundType: SoundType, volume: Float = 1.0f) {
        if (sessionManager?.isSoundEnabled() != true) return
        
        soundIds[soundType]?.let { soundId ->
            soundPool?.play(soundId, volume, volume, 1, 0, 1.0f)?.let { streamId ->
                streamIds[soundType] = streamId
            }
        }
    }
    
    fun playSoundLoop(soundType: SoundType, volume: Float = 0.7f) {
        if (sessionManager?.isSoundEnabled() != true) return
        
        soundIds[soundType]?.let { soundId ->
            soundPool?.play(soundId, volume, volume, 1, -1, 1.0f)?.let { streamId ->
                streamIds[soundType] = streamId
            }
        }
    }
    
    fun stopSound(soundType: SoundType) {
        streamIds[soundType]?.let { streamId ->
            soundPool?.stop(streamId)
            streamIds.remove(soundType)
        }
    }
    
    fun vibrate(duration: Long = 50) {
        if (sessionManager?.isSoundEnabled() != true) return
        
        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(duration)
            }
        }
    }
    
    fun vibratePattern(pattern: LongArray, repeat: Int = -1) {
        if (sessionManager?.isSoundEnabled() != true) return
        
        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitudes = IntArray(pattern.size) { VibrationEffect.DEFAULT_AMPLITUDE }
                vib.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, repeat))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, repeat)
            }
        }
    }
    
    fun vibrateSuccess() {
        vibratePattern(longArrayOf(0, 100, 50, 100))
    }
    
    fun vibrateError() {
        vibratePattern(longArrayOf(0, 200, 100, 200, 100, 200))
    }
    
    fun release() {
        soundPool?.release()
        soundPool = null
        soundIds.clear()
        streamIds.clear()
    }
}
