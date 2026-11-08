package com.example.data.util

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * High-performance Voice Assistant Manager providing Speech-To-Text (STT),
 * natural Bengali & English Text-To-Speech (TTS), audio sound effects,
 * and conversational voice agent flow for the Apparel Advisor.
 */
class VoiceAssistantManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())

    // --- TTS States ---
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var isBengaliTtsAvailable = false
    private var pendingSpeechText: String? = null
    private var pendingSpeechMessageId: Long? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentSpeakingMessageId = MutableStateFlow<Long?>(null)
    val currentSpeakingMessageId: StateFlow<Long?> = _currentSpeakingMessageId.asStateFlow()

    // Callbacks for live voice call cycle
    var onTtsDoneCallback: (() -> Unit)? = null
    var onTtsStartCallback: (() -> Unit)? = null
    var onTtsErrorCallback: (() -> Unit)? = null
    var onSpeechErrorCallback: ((Int, String) -> Unit)? = null

    // Audio routing (Speakerphone)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    // Watchdog timer to prevent TTS hanging permanently on speaking state
    private var ttsWatchdogRunnable: Runnable? = null
    // Waveform simulation during speaking so avatar dances to AI voice
    private var speakWaveformRunnable: Runnable? = null

    @Suppress("DEPRECATION")
    fun toggleSpeaker(enable: Boolean? = null) {
        val next = enable ?: !_isSpeakerOn.value
        _isSpeakerOn.value = next
        try {
            audioManager?.isSpeakerphoneOn = next
        } catch (e: Exception) {
            Log.w("VoiceAssistantManager", "Failed to toggle speakerphone: ${e.message}")
        }
    }

    /**
     * Ensure audio volume is audible on speakerphone so user clearly hears the AI voice.
     */
    @Suppress("DEPRECATION")
    fun ensureAudioLoudspeaker() {
        try {
            audioManager?.mode = AudioManager.MODE_NORMAL
            audioManager?.isSpeakerphoneOn = _isSpeakerOn.value
            val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
            val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
            if (curVol < (maxVol * 0.6f).toInt()) {
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVol * 0.8f).toInt(), 0)
            }
        } catch (e: Exception) {
            Log.w("VoiceAssistantManager", "Failed to ensure loudspeaker: ${e.message}")
        }
    }

    // --- STT (Speech Recognizer) States ---
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _speechPartialText = MutableStateFlow("")
    val speechPartialText: StateFlow<String> = _speechPartialText.asStateFlow()

    private val _speechRmsDb = MutableStateFlow(0f)
    val speechRmsDb: StateFlow<Float> = _speechRmsDb.asStateFlow()

    private val _speechError = MutableStateFlow<String?>(null)
    val speechError: StateFlow<String?> = _speechError.asStateFlow()

    private var onSpeechResultCallback: ((String) -> Unit)? = null
    private var isTtsInitializing = false

    fun ensureTtsInitialized() {
        if (textToSpeech != null || isTtsInitializing) return
        isTtsInitializing = true
        mainHandler.post {
            try {
                textToSpeech = TextToSpeech(context.applicationContext, this)
            } catch (e: Exception) {
                Log.e("VoiceAssistantManager", "Failed to init TextToSpeech: ${e.message}")
                isTtsInitializing = false
            }
        }
    }

    init {
        // TextToSpeech is initialized on-demand or in background to ensure instant app launch
    }

    // --- TextToSpeech Initialization Callback ---
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                try {
                    // Check if Bengali voice package is available on device
                    val bnBdLocale = Locale.forLanguageTag("bn-BD")
                    val bnLocale = Locale.forLanguageTag("bn")
                    val bnBdAvailable = tts.isLanguageAvailable(bnBdLocale)
                    val bnAvailable = tts.isLanguageAvailable(bnLocale)
                    isBengaliTtsAvailable = bnBdAvailable >= TextToSpeech.LANG_AVAILABLE || bnAvailable >= TextToSpeech.LANG_AVAILABLE

                    if (isBengaliTtsAvailable) {
                        val setRes = tts.setLanguage(bnBdLocale)
                        if (setRes < TextToSpeech.LANG_AVAILABLE) {
                            tts.setLanguage(bnLocale)
                        }
                        // Try selecting best Bengali voice
                        val bnVoice = tts.voices?.firstOrNull { it.locale.language == "bn" && !it.isNetworkConnectionRequired }
                            ?: tts.voices?.firstOrNull { it.locale.language == "bn" }
                        if (bnVoice != null) {
                            tts.voice = bnVoice
                        }
                    } else {
                        // Fallback to default/US locale for phonetic transliteration
                        tts.language = Locale.US
                    }

                    // Natural voice agent pitch and speed
                    tts.setPitch(1.0f)
                    tts.setSpeechRate(1.0f)

                    // Configure optimal audio attributes for speech
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    tts.setAudioAttributes(audioAttributes)

                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                            startWaveformSimulation()
                            mainHandler.post { onTtsStartCallback?.invoke() }
                        }

                        override fun onDone(utteranceId: String?) {
                            cancelTtsWatchdog()
                            stopWaveformSimulation()
                            _isSpeaking.value = false
                            _currentSpeakingMessageId.value = null
                            mainHandler.post { onTtsDoneCallback?.invoke() }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            cancelTtsWatchdog()
                            stopWaveformSimulation()
                            _isSpeaking.value = false
                            _currentSpeakingMessageId.value = null
                            mainHandler.post { onTtsErrorCallback?.invoke() }
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            cancelTtsWatchdog()
                            stopWaveformSimulation()
                            _isSpeaking.value = false
                            _currentSpeakingMessageId.value = null
                            mainHandler.post { onTtsErrorCallback?.invoke() }
                        }
                    })

                    isTtsInitialized = true
                    Log.d("VoiceAssistantManager", "TTS ready. Bengali available: $isBengaliTtsAvailable, language: ${tts.voice?.locale?.toLanguageTag() ?: "unknown"}")

                    // Process any speech queued before init
                    pendingSpeechText?.let { text ->
                        val id = pendingSpeechMessageId
                        pendingSpeechText = null
                        pendingSpeechMessageId = null
                        speak(text, id)
                    }
                } catch (e: Exception) {
                    Log.e("VoiceAssistantManager", "Error setting up TTS parameters: ${e.message}")
                    isTtsInitialized = true
                }
            }
        } else {
            Log.e("VoiceAssistantManager", "TextToSpeech init failed with status: $status")
            // Still mark initialized so we don't block callers permanently
            isTtsInitialized = false
        }
    }

    /**
     * Start watchdog timer to guarantee the conversation turn finishes
     * even if the underlying TTS engine fails to call onDone.
     */
    private fun startTtsWatchdog(textLength: Int) {
        cancelTtsWatchdog()
        // Roughly ~85ms per character + 3s buffer
        val timeoutMs = (textLength * 85L + 3000L).coerceIn(3500L, 35000L)
        val runnable = Runnable {
            if (_isSpeaking.value) {
                Log.w("VoiceAssistantManager", "TTS watchdog triggered after $timeoutMs ms; recovering state.")
                _isSpeaking.value = false
                _currentSpeakingMessageId.value = null
                stopWaveformSimulation()
                onTtsDoneCallback?.invoke()
            }
        }
        ttsWatchdogRunnable = runnable
        mainHandler.postDelayed(runnable, timeoutMs)
    }

    private fun cancelTtsWatchdog() {
        ttsWatchdogRunnable?.let { mainHandler.removeCallbacks(it) }
        ttsWatchdogRunnable = null
    }

    /**
     * Simulates dynamic RMS audio waves while Gemini speaks so the avatar
     * visualizer pulsates naturally to the sound of the voice.
     */
    private fun startWaveformSimulation() {
        stopWaveformSimulation()
        var step = 0
        val runnable = object : Runnable {
            override fun run() {
                if (_isSpeaking.value) {
                    step = (step + 1) % 8
                    val simulatedRms = when (step) {
                        0 -> 3.2f
                        1 -> 6.8f
                        2 -> 8.5f
                        3 -> 5.1f
                        4 -> 7.4f
                        5 -> 9.0f
                        6 -> 4.5f
                        else -> 2.0f
                    }
                    _speechRmsDb.value = simulatedRms
                    mainHandler.postDelayed(this, 120)
                } else {
                    _speechRmsDb.value = 0f
                }
            }
        }
        speakWaveformRunnable = runnable
        mainHandler.post(runnable)
    }

    private fun stopWaveformSimulation() {
        speakWaveformRunnable?.let { mainHandler.removeCallbacks(it) }
        speakWaveformRunnable = null
        _speechRmsDb.value = 0f
    }

    /**
     * Speak text aloud using TTS. Cleans formatting and applies phonetic transliteration
     * if device lacks native Bengali speech packages, ensuring audible, loud voice output.
     */
    fun speak(text: String, messageId: Long? = null) {
        mainHandler.post {
            ensureTtsInitialized()
            val cleaned = cleanTextForSpeech(text)
            if (cleaned.isBlank()) return@post

            ensureAudioLoudspeaker()

            if (!isTtsInitialized || textToSpeech == null) {
                pendingSpeechText = cleaned
                pendingSpeechMessageId = messageId
                // If TTS isn't ready after 2s, gracefully advance so user isn't stuck
                mainHandler.postDelayed({
                    if (!isTtsInitialized && _isSpeaking.value) {
                        _isSpeaking.value = false
                        onTtsDoneCallback?.invoke()
                    }
                }, 2000)
                return@post
            }

            stopSpeaking()

            _currentSpeakingMessageId.value = messageId
            _isSpeaking.value = true

            // Determine text for TTS engine:
            // If device cannot speak native Bengali, transliterate to natural Romanized Bangla
            // so standard Android TTS speaks it aloud clearly instead of remaining silent.
            val speechOutput = if (!isBengaliTtsAvailable && BengaliPhoneticHelper.containsBengali(cleaned)) {
                BengaliPhoneticHelper.toPhoneticBangla(cleaned)
            } else {
                cleaned
            }

            startTtsWatchdog(speechOutput.length)

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            }

            val maxChunkLength = 3500
            if (speechOutput.length <= maxChunkLength) {
                val utteranceId = "utterance_${System.currentTimeMillis()}"
                val result = textToSpeech?.speak(speechOutput, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
                if (result == TextToSpeech.ERROR) {
                    Log.e("VoiceAssistantManager", "TTS speak returned ERROR immediately")
                    cancelTtsWatchdog()
                    _isSpeaking.value = false
                    _currentSpeakingMessageId.value = null
                    onTtsErrorCallback?.invoke()
                }
            } else {
                val chunks = speechOutput.chunked(maxChunkLength)
                chunks.forEachIndexed { index, chunk ->
                    val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                    val utteranceId = "utterance_${System.currentTimeMillis()}_$index"
                    textToSpeech?.speak(chunk, queueMode, params, utteranceId)
                }
            }
        }
    }

    /**
     * Stop active audio playback.
     */
    fun stopSpeaking() {
        mainHandler.post {
            cancelTtsWatchdog()
            stopWaveformSimulation()
            try {
                textToSpeech?.stop()
            } catch (e: Exception) {
                Log.w("VoiceAssistantManager", "Error stopping TTS: ${e.message}")
            } finally {
                _isSpeaking.value = false
                _currentSpeakingMessageId.value = null
            }
        }
    }

    // --- Speech Recognition (STT) ---

    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Start listening to the user's voice in Bengali & English.
     */
    fun startListening(onResult: (String) -> Unit) {
        mainHandler.post {
            ensureTtsInitialized()
            // Stop any ongoing speech playback while listening
            stopSpeaking()

            _speechError.value = null
            _speechPartialText.value = ""
            onSpeechResultCallback = onResult

            if (!isSpeechRecognitionAvailable()) {
                _speechError.value = "আপনার ডিভাইসে Speech Recognition সার্ভিস পাওয়া যায়নি।"
                _isListening.value = false
                return@post
            }

            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _speechError.value = null
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        _speechRmsDb.value = rmsdB.coerceIn(0f, 10f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "অডিও রেকর্ডিংয়ে সমস্যা হয়েছে"
                            SpeechRecognizer.ERROR_CLIENT -> "ভয়েস প্রসেসিং ব্যর্থ হয়েছে"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "মাইক্রোফোন পারমিশন প্রয়োজন"
                            SpeechRecognizer.ERROR_NETWORK -> "ইন্টারনেট সংযোগ চেক করুন"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ইন্টারনেট টাইমআউট হয়েছে"
                            SpeechRecognizer.ERROR_NO_MATCH -> "কোনো কথা বুঝতে পারিনি, আবার বলুন"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ভয়েস সার্ভিস ব্যস্ত আছে"
                            SpeechRecognizer.ERROR_SERVER -> "সার্ভার এরর, পুনরায় চেষ্টা করুন"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কোনো শব্দ পাওয়া যায়নি"
                            else -> "ভয়েস ইনপুট নেওয়া সম্ভব হয়নি ($error)"
                        }
                        _speechError.value = errorMsg
                        Log.w("VoiceAssistantManager", "Speech recognition error: $error ($errorMsg)")
                        mainHandler.post { onSpeechErrorCallback?.invoke(error, errorMsg) }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim().orEmpty()
                        if (text.isNotBlank()) {
                            _speechPartialText.value = text
                            onSpeechResultCallback?.invoke(text)
                        } else {
                            _speechError.value = "কোনো কথা শনাক্ত করা যায়নি।"
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim().orEmpty()
                        if (partial.isNotBlank()) {
                            _speechPartialText.value = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("bn-BD", "bn-IN", "en-US"))
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "আপনার প্রশ্ন বা নির্দেশ বলুন...")
                }

                speechRecognizer?.startListening(intent)
                _isListening.value = true
            } catch (e: Exception) {
                Log.e("VoiceAssistantManager", "Failed to start listening: ${e.message}")
                _isListening.value = false
                _speechError.value = "ভয়েস রেকর্ড শুরু করা যায়নি: ${e.message}"
            }
        }
    }

    /**
     * Stop listening immediately and process whatever was heard.
     */
    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.w("VoiceAssistantManager", "Error stopping listening: ${e.message}")
            } finally {
                _isListening.value = false
            }
        }
    }

    /**
     * Cancel listening and discard partial results.
     */
    fun cancelListening() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.w("VoiceAssistantManager", "Error canceling listening: ${e.message}")
            } finally {
                _isListening.value = false
                _speechPartialText.value = ""
                _speechError.value = null
            }
        }
    }

    /**
     * Clean and strip markdown syntax, links, bold tags, code blocks, and weird punctuation
     * so that TTS reads fluent, clean sentences without reading punctuation literally.
     */
    private fun cleanTextForSpeech(raw: String): String {
        var text = raw

        // Remove markdown images and links: ![alt](url) -> "", [text](url) -> text
        text = text.replace(Regex("!\\[.*?\\]\\(.*?\\)"), "")
        text = text.replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")

        // Remove code blocks and inline code
        text = text.replace(Regex("```[\\s\\S]*?```"), "কোড বাদ দেওয়া হয়েছে।")
        text = text.replace(Regex("`([^`]+)`"), "$1")

        // Remove markdown table pipes and dashes
        text = text.replace(Regex("\\|[-:\\s|]+\\|"), " ")
        text = text.replace("|", " ")

        // Remove header symbols (###, ##, #)
        text = text.replace(Regex("(?m)^#{1,6}\\s*"), "")

        // Remove bold, italic, strikethrough, bullet points
        text = text.replace("**", "")
        text = text.replace("__", "")
        text = text.replace("~~", "")
        text = text.replace(Regex("(?m)^[\\*\\-\\+]\\s+"), "")
        text = text.replace(Regex("(?m)^\\d+\\.\\s+"), "")

        // Remove decorative emojis that TTS struggles with or reads loudly
        text = text.replace(Regex("[🎨📊💵👗👔🧵✨🏪📈⚠️💡🔍🛒📦🚀📌🎯🔔]"), " ")

        // Clean up excessive whitespace
        text = text.replace(Regex("\\n{2,}"), ". ")
        text = text.replace("\n", " ")
        text = text.replace(Regex("\\s{2,}"), " ")

        return text.trim()
    }

    /**
     * Clean up resources on app exit or ViewModel clear.
     */
    fun shutdown() {
        mainHandler.post {
            cancelTtsWatchdog()
            stopWaveformSimulation()

            try {
                textToSpeech?.stop()
                textToSpeech?.shutdown()
                textToSpeech = null
            } catch (e: Exception) {
                Log.w("VoiceAssistantManager", "Error shutting down TTS: ${e.message}")
            }

            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.w("VoiceAssistantManager", "Error destroying SpeechRecognizer: ${e.message}")
            }
        }
    }
}

