package com.example.data.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

/**
 * Generates telephone, connection, and voice agent chimes
 * so the 1v1 live call sounds responsive and alive like a real voice agent.
 */
class CallSoundEffects {

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            Log.w("CallSoundEffects", "Could not initialize ToneGenerator: ${e.message}")
        }
    }

    /**
     * Dual-tone connecting ring for dialing the AI voice call.
     */
    fun playCallConnecting() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 250)
        } catch (e: Exception) {
            Log.d("CallSoundEffects", "playCallConnecting skipped: ${e.message}")
        }
    }

    /**
     * Pleasant harmonic chime when the AI live call connects successfully.
     */
    fun playCallConnected() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 220)
        } catch (e: Exception) {
            Log.d("CallSoundEffects", "playCallConnected skipped: ${e.message}")
        }
    }

    /**
     * Subtle acknowledgment beep when the user finishes speaking.
     */
    fun playSpeechRecognized() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
        } catch (e: Exception) {
            Log.d("CallSoundEffects", "playSpeechRecognized skipped: ${e.message}")
        }
    }

    /**
     * Audio prompt chirp right as Gemini begins replying.
     */
    fun playAiSpeakStart() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 100)
        } catch (e: Exception) {
            Log.d("CallSoundEffects", "playAiSpeakStart skipped: ${e.message}")
        }
    }

    /**
     * Soft two-tone beep when the voice call is terminated.
     */
    fun playCallEnded() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
        } catch (e: Exception) {
            Log.d("CallSoundEffects", "playCallEnded skipped: ${e.message}")
        }
    }

    /**
     * Clean up system audio resources.
     */
    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            Log.w("CallSoundEffects", "Error releasing ToneGenerator: ${e.message}")
        }
    }
}
