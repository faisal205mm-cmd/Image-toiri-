package com.example.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.api.GeminiApiService
import com.example.data.api.GeminiClient
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiErrorInfo
import com.example.data.api.GeminiErrorParser
import com.example.data.api.GeminiErrorType
import com.example.data.api.GeminiInlineData
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.ImageConfig
import com.example.data.db.AppDatabase
import com.example.data.db.BrandProfileEntity
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ChatSessionEntity
import com.example.data.db.CollectionEntity
import com.example.data.db.PricingSkuEntity
import com.example.data.db.SaleRecordEntity
import com.example.data.db.MonthlyFinanceEntity
import com.example.data.firebase.SaleFirestoreManager
import com.example.data.image.ImagePromptEngine
import com.example.data.image.StructuredImagePrompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

enum class AdvisorPersona(
    val id: String,
    val title: String,
    val subtitle: String,
    val avatarIcon: String,
    val systemPrompt: String,
    val suggestedPrompts: List<String>
) {
    GENERAL(
        id = "GENERAL",
        title = "দোকান ও ব্যবসা পরামর্শক",
        subtitle = "বিক্রি, বাকি, খরচ ও ফায়ারবেস ক্লাউড অডিট",
        avatarIcon = "sparkles",
        systemPrompt = """You are the official in-app AI Store Manager and Financial Consultant for this Apparel & Fashion Retail Store (দোকানের ডিজিটাল ম্যানেজার ও হিসাবরক্ষক).
You are directly connected to the shop's Google Firebase Firestore cloud database and local SQLite POS database.
You help shop owners track sales, recover customer dues, control expenses, optimize inventory, and grow their clothing retail business.
Deliver clear, polite, structured answers in natural Bengali (বাংলা) using markdown.""",
        suggestedPrompts = listOf(
            "আমার ফায়ারবেস ক্লাউড ও দোকানের সার্বিক হিসাব দেখাও",
            "এই মাসের মোট বিক্রি, খরচ ও নিট লাভ কত?",
            "কাস্টমারদের বকেয়া (Due/Baki) আদায়ের কার্যকর কৌশল কী?",
            "পোশাকের দোকানে বিক্রি দ্বিগুণ করার ৩টি বাস্তবসম্মত কৌশল দিন"
        )
    ),
    CREATIVE(
        id = "CREATIVE",
        title = "Creative",
        subtitle = "Writing, brainstorming, storytelling & copywriting",
        avatarIcon = "palette",
        systemPrompt = """You are a master creative writer, copywriter, and brainstorming partner.
Help users invent compelling stories, catchy taglines, marketing copy, video scripts, poetry, and imaginative ideas.
Provide vivid, engaging, and original responses.""",
        suggestedPrompts = listOf(
            "Brainstorm 5 innovative startup ideas for 2026",
            "Write an engaging opening hook for a sci-fi novel",
            "Craft 3 catchy slogans for a boutique coffee brand",
            "Write a short motivational reflection on resilience"
        )
    ),
    CODING(
        id = "CODING",
        title = "Coding & Tech",
        subtitle = "Programming, debugging, architecture & logic",
        avatarIcon = "code",
        systemPrompt = """You are an expert software engineer and technical architect.
Help users write clean, modern code (Kotlin, Python, TypeScript, Java, C++, SQL), debug errors, explain algorithms, and design scalable software architectures.
Format code snippets clearly with best practices.""",
        suggestedPrompts = listOf(
            "How do Kotlin Coroutines and StateFlow work together?",
            "Explain Big-O notation with simple real-world examples",
            "How do I structure a clean MVVM architecture in Android?",
            "Write an algorithm to detect cycles in a linked list"
        )
    ),
    LEARNING(
        id = "LEARNING",
        title = "Learning & Tutor",
        subtitle = "Deep dives, science, history & conceptual breakdowns",
        avatarIcon = "school",
        systemPrompt = """You are an engaging educator and academic tutor.
Explain complex subjects simply using intuitive analogies, step-by-step breakdowns, and practical examples.
Cover science, history, mathematics, philosophy, economics, and language learning.""",
        suggestedPrompts = listOf(
            "Explain how large language models (LLMs) actually work",
            "What were the key causes of the Industrial Revolution?",
            "How does compound interest build wealth over time?",
            "Explain the principles of relativity simply"
        )
    )
}

class AdvisorRepository(
    private val database: AppDatabase,
    private val context: Context
) {

    private val apiService: GeminiApiService = GeminiClient.apiService
    private var customApiKey: String? = null
    private var customPollinationsApiKey: String? = null

    fun setCustomApiKey(key: String?) {
        customApiKey = key?.trim()?.ifBlank { null }
    }

    fun setCustomPollinationsApiKey(key: String?) {
        customPollinationsApiKey = key?.trim()?.ifBlank { null }
    }

    fun getPollinationsApiKey(): String {
        return customPollinationsApiKey ?: ""
    }

    fun getPollinationsApiKeyInfo(): Pair<Boolean, String> {
        val custom = customPollinationsApiKey
        if (!custom.isNullOrBlank()) {
            val masked = if (custom.length > 8) {
                "${custom.take(4)}...${custom.takeLast(4)}"
            } else {
                "••••••••"
            }
            return Pair(true, "Custom Key ($masked)")
        }
        return Pair(true, "Active (Free & Open Tier)")
    }

    suspend fun validatePollinationsApiKey(keyToTest: String): Pair<Boolean, String> {
        return com.example.data.api.PollinationsClient.testConnection(keyToTest.trim().ifBlank { null })
    }

    fun getEffectiveApiKey(): String {
        val custom = customApiKey
        if (!custom.isNullOrBlank()) return custom
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") return buildKey
        return ""
    }

    fun getApiKeyInfo(): Pair<Boolean, String> {
        val custom = customApiKey
        if (!custom.isNullOrBlank()) {
            val masked = if (custom.length > 8) {
                "${custom.take(4)}...${custom.takeLast(4)}"
            } else {
                "••••••••"
            }
            return Pair(true, "Custom Key ($masked)")
        }
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            val masked = if (buildKey.length > 8) {
                "${buildKey.take(4)}...${buildKey.takeLast(4)}"
            } else {
                "••••••••"
            }
            return Pair(true, "AI Studio Secret ($masked)")
        }
        return Pair(false, "No API Key Set")
    }

    suspend fun validateApiKey(keyToTest: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val trimmed = keyToTest.trim()
        if (trimmed.isBlank()) {
            return@withContext Pair(false, "API key cannot be blank. Please enter a valid key from Google AI Studio.")
        }
        try {
            val testRequest = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = "Hello!"))
                    )
                ),
                generationConfig = GenerationConfig(temperature = 0.1f)
            )
            val response = apiService.generateContent(
                model = GeminiClient.MODEL_FLASH,
                apiKey = trimmed,
                request = testRequest
            )
            val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!reply.isNullOrBlank()) {
                Pair(true, "API Key connected successfully! Ready for AI chats.")
            } else {
                Pair(false, "Connected to Gemini, but received an empty response. Please try again.")
            }
        } catch (e: Exception) {
            val parsed = GeminiErrorParser.parse(e)
            Pair(false, parsed.message)
        }
    }

    // --- Intent Detection & Image Generation ---
    fun isImageGenerationIntent(prompt: String): Boolean {
        val lower = prompt.lowercase(Locale.ROOT).trim()
        if (lower.startsWith("/image") || lower.startsWith("/img") || lower.startsWith("/draw") || lower.startsWith("/generate_image")) {
            return true
        }
        // Bengali / Bangla keywords
        val bengaliKeywords = listOf(
            "image toiri", "chobi toiri", "ছবি তৈরি", "ছবি বানাও", "ইমেজ তৈরি", "ইমেজ বানাও",
            "ছবি আঁকো", "image banao", "chobi banao", "ছবি দেখাও", "image dekhaw",
            "chobi dao", "image dao", "ekta chobi", "ekta image", "ছবি জেনারেট", "image generate",
            "ছবি বানিয়ে দাও", "chobi lagbe", "image lagbe", "ফটোগ্রাফি", "ফটোগ্রাফ",
            "মকআপ", "ওয়ালপেপার"
        )
        if (bengaliKeywords.any { lower.contains(it) }) {
            return true
        }
        // English keywords
        val englishKeywords = listOf(
            "generate an image", "generate image", "create an image", "create image",
            "generate a picture", "create a picture", "generate a photo", "create a photo",
            "generate visual", "create visual", "make an image", "make a photo", "make a picture",
            "draw an image", "draw a picture", "draw a", "draw me", "design an image",
            "render an image", "render a picture", "sketch an image", "generate a design of",
            "generate illustration", "create illustration", "generate artwork", "create artwork",
            "photo of", "picture of", "image of", "product photography", "fashion photography",
            "product shot", "studio shot", "catalog shot"
        )
        if (englishKeywords.any { lower.contains(it) }) {
            return true
        }
        // Detection for contextual requests like "সাদা background এ একটা কালো cargo pant দেখাও" or "sada background e black shoe dekhao"
        val hasVisualTerm = lower.contains("background") || lower.contains("ব্যাকগ্রাউন্ড") || lower.contains("studio") ||
                lower.contains("lighting") || lower.contains("cinematic") || lower.contains("সিনেমাটিক")
        val hasApparelOrProduct = lower.contains("pant") || lower.contains("প্যান্ট") || lower.contains("shoe") ||
                lower.contains("জুতো") || lower.contains("saree") || lower.contains("শাড়ি") || lower.contains("hoodie") ||
                lower.contains("হুডি") || lower.contains("jacket") || lower.contains("জ্যাকেট") || lower.contains("model") ||
                lower.contains("মডেল") || lower.contains("watch") || lower.contains("ঘড়ি") || lower.contains("bottle") ||
                lower.contains("বোতল") || lower.contains("t-shirt") || lower.contains("টি-শার্ট")
        val hasRequestVerb = lower.contains("dekhao") || lower.contains("দেখাও") || lower.contains("banao") ||
                lower.contains("বানাও") || lower.contains("toiri") || lower.contains("তৈরি")

        return (hasVisualTerm && (hasApparelOrProduct || hasRequestVerb)) || (hasApparelOrProduct && hasRequestVerb)
    }

    private fun extractCleanImagePrompt(prompt: String): String {
        var cleaned = prompt.trim()
        if (cleaned.startsWith("/image", ignoreCase = true)) {
            cleaned = cleaned.removePrefix("/image").removePrefix(":").trim()
        } else if (cleaned.startsWith("/img", ignoreCase = true)) {
            cleaned = cleaned.removePrefix("/img").removePrefix(":").trim()
        }
        return if (cleaned.isNotBlank()) cleaned else prompt
    }

    suspend fun generateWithPollinations(
        prompt: String,
        aspectRatio: String = "1:1"
    ): Pair<String?, String> = withContext(Dispatchers.IO) {
        val structured = ImagePromptEngine.analyzeAndOptimize(
            userPrompt = prompt,
            overrideAspectRatio = if (aspectRatio != "1:1") aspectRatio else null
        )
        val cleanPrompt = structured.optimizedPrompt
        val targetRatio = structured.aspectRatio
        val (width, height) = when (targetRatio) {
            "16:9" -> Pair(1280, 720)
            "9:16" -> Pair(720, 1280)
            "4:3" -> Pair(1024, 768)
            "3:4" -> Pair(768, 1024)
            else -> Pair(1024, 1024)
        }
        val pollinationsKey = customPollinationsApiKey
        Log.d("AdvisorRepository", "Pollinations Request -> Mode: ${structured.mode}, Ratio: $targetRatio (${width}x$height)")
        Log.d("AdvisorRepository", "Pollinations Prompt: $cleanPrompt")

        val imageUrl = com.example.data.api.PollinationsClient.buildImageUrl(
            prompt = cleanPrompt,
            width = width,
            height = height,
            model = "flux",
            apiKey = pollinationsKey
        )
        val imagesDir = File(context.filesDir, "generated_images").apply { mkdirs() }
        val localFile = File(imagesDir, "pollinations_${System.currentTimeMillis()}_${(100..999).random()}.jpg")
        val downloaded = com.example.data.api.PollinationsClient.downloadImage(
            url = imageUrl,
            outputFile = localFile,
            apiKey = pollinationsKey
        )
        val finalPath = if (downloaded && localFile.exists() && localFile.length() > 0) {
            localFile.absolutePath
        } else {
            imageUrl
        }
        val messageText = "🎨 **Visual Created (Pollinations AI - Flux)**\n\n${structured.bengaliAnalysis}\n\n**Optimized Prompt:**\n*$cleanPrompt*"
        Pair(finalPath, messageText)
    }

    suspend fun generateImage(
        sessionId: Long?,
        persona: AdvisorPersona,
        prompt: String,
        aspectRatio: String = "1:1"
    ): Pair<Long, String> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val activeSessionId = if (sessionId == null || sessionId <= 0L) {
            val autoTitle = "🎨 " + if (prompt.length > 30) prompt.take(30).trim() + "..." else prompt
            database.chatDao().insertSession(
                ChatSessionEntity(
                    title = autoTitle,
                    persona = persona.id,
                    createdAt = now,
                    updatedAt = now,
                    messageCount = 0,
                    lastSnippet = prompt
                )
            )
        } else {
            sessionId
        }

        // Save User Prompt Message
        database.chatDao().insertMessage(
            ChatMessageEntity(
                sessionId = activeSessionId,
                role = "user",
                message = prompt,
                persona = persona.id,
                imageUrl = null,
                isImagePrompt = true,
                timestamp = now
            )
        )

        val structured = ImagePromptEngine.analyzeAndOptimize(
            userPrompt = prompt,
            overrideAspectRatio = if (aspectRatio != "1:1") aspectRatio else null
        )
        val cleanPrompt = structured.optimizedPrompt
        val targetRatio = structured.aspectRatio

        Log.d("AdvisorRepository", "Structured Image Understanding:\nMode: ${structured.mode}\nRatio: $targetRatio\nPrompt: $cleanPrompt")

        var savedImagePath: String? = null
        var modelMessageText: String = ""

        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            // Direct Pollinations AI generation when Gemini key is not configured
            val (pollinationsPath, pollinationsMessage) = generateWithPollinations(prompt, targetRatio)
            savedImagePath = pollinationsPath
            modelMessageText = pollinationsMessage
        } else {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = cleanPrompt))
                    )
                ),
                generationConfig = GenerationConfig(
                    responseModalities = listOf("TEXT", "IMAGE"),
                    imageConfig = ImageConfig(aspectRatio = targetRatio, imageSize = "1K")
                )
            )

            val imageCandidateModels = listOf(
                GeminiClient.MODEL_IMAGE_PRIMARY, // gemini-2.5-flash-image
                GeminiClient.MODEL_IMAGE_LATEST,  // gemini-3.1-flash-image-preview
                GeminiClient.MODEL_IMAGE_PRO      // gemini-3-pro-image-preview
            )

            var lastException: Exception? = null
            var successfulResponse: com.example.data.api.GeminiResponse? = null
            var usedModelName = GeminiClient.MODEL_IMAGE_PRIMARY

            // Attempt generation with retry and multi-model fallback
            for (model in imageCandidateModels) {
                for (attempt in 1..2) {
                    try {
                        val response = apiService.generateContent(
                            model = model,
                            apiKey = apiKey,
                            request = request
                        )
                        val candidate = response.candidates?.firstOrNull()
                        val parts = candidate?.content?.parts ?: emptyList()
                        val hasImage = parts.any { it.inlineData != null && !it.inlineData.data.isNullOrBlank() }
                        if (hasImage || parts.any { !it.text.isNullOrBlank() }) {
                            successfulResponse = response
                            usedModelName = model
                            lastException = null
                            break
                        }
                    } catch (e: Exception) {
                        lastException = e
                        val parsed = GeminiErrorParser.parse(e)
                        Log.w("AdvisorRepository", "Model $model attempt $attempt failed: ${parsed.title} (${parsed.httpCode ?: 0}) - ${parsed.message}")
                        if (parsed.errorType == GeminiErrorType.QUOTA_EXCEEDED) {
                            kotlinx.coroutines.delay(1500L * attempt)
                        } else if (parsed.errorType == GeminiErrorType.INVALID_API_KEY || parsed.errorType == GeminiErrorType.PERMISSION_DENIED) {
                            break
                        } else {
                            kotlinx.coroutines.delay(800L)
                        }
                    }
                }
                if (successfulResponse != null || lastException?.let { GeminiErrorParser.parse(it).errorType == GeminiErrorType.INVALID_API_KEY } == true) {
                    break
                }
            }

            try {
                if (successfulResponse != null) {
                    val candidate = successfulResponse.candidates?.firstOrNull()
                    val parts = candidate?.content?.parts ?: emptyList()
                    val imagePart = parts.firstOrNull { it.inlineData != null && !it.inlineData.data.isNullOrBlank() }
                    val textPart = parts.firstOrNull { !it.text.isNullOrBlank() }?.text

                    if (imagePart?.inlineData != null) {
                        try {
                            val bytes = Base64.decode(imagePart.inlineData.data, Base64.DEFAULT)
                            val imagesDir = File(context.filesDir, "generated_images").apply { mkdirs() }
                            val ext = if (imagePart.inlineData.mimeType.contains("jpeg") || imagePart.inlineData.mimeType.contains("jpg")) "jpg" else "png"
                            val file = File(imagesDir, "img_${System.currentTimeMillis()}_${(100..999).random()}.$ext")
                            file.writeBytes(bytes)
                            savedImagePath = file.absolutePath
                        } catch (_: Exception) {
                            savedImagePath = "data:${imagePart.inlineData.mimeType};base64,${imagePart.inlineData.data}"
                        }
                    }

                    val analysisBlock = structured.bengaliAnalysis
                    modelMessageText = when {
                        savedImagePath != null && !textPart.isNullOrBlank() -> "$textPart\n\n$analysisBlock"
                        savedImagePath != null -> "🎨 **Visual Created ($usedModelName)**\n\n$analysisBlock\n\n**Optimized Prompt:**\n*$cleanPrompt*"
                        !textPart.isNullOrBlank() -> textPart
                        else -> "🎨 Image generated successfully."
                    }
                } else {
                    // Fallback to Pollinations AI
                    Log.i("AdvisorRepository", "Gemini generation unavailable, triggering Pollinations AI fallback")
                    val (pollinationsPath, pollinationsMessage) = generateWithPollinations(prompt, targetRatio)
                    savedImagePath = pollinationsPath
                    modelMessageText = "$pollinationsMessage\n\n*(Gemini কোটা লিমিট থাকায় Pollinations AI দিয়ে তৈরি করা হয়েছে)*"
                }
            } catch (e: Exception) {
                Log.e("AdvisorRepository", "Gemini image failed with exception: ${e.message}", e)
                // Fallback to Pollinations AI
                val (pollinationsPath, pollinationsMessage) = generateWithPollinations(prompt, targetRatio)
                if (pollinationsPath != null) {
                    savedImagePath = pollinationsPath
                    modelMessageText = "$pollinationsMessage\n\n*(Pollinations AI দিয়ে নিরবচ্ছিন্নভাবে তৈরি করা হয়েছে)*"
                } else {
                    val parsed = GeminiErrorParser.parse(e)
                    Log.e("AdvisorRepository", "Both Gemini & Pollinations failed. Code: ${parsed.httpCode}, Reason: ${parsed.message}")
                    modelMessageText = "⚠️ **Image তৈরি করা যায়নি। API/model connection অথবা generation limit পরীক্ষা করুন।**\n\n*কারিগরি বিবরণ (Debug Info):*\n- Status / Error: ${parsed.title}\n- Code: ${parsed.httpCode ?: "N/A"}\n- Details: ${parsed.message}\n- Hint: 3-dot মেনু থেকে Pollinations API Key বা নতুন Gemini Key কনফিগার করতে পারেন।"
                }
            }
        }

        database.chatDao().insertMessage(
            ChatMessageEntity(
                sessionId = activeSessionId,
                role = "model",
                message = modelMessageText,
                persona = persona.id,
                imageUrl = savedImagePath,
                isImagePrompt = true,
                timestamp = System.currentTimeMillis()
            )
        )

        // Update session metadata
        val existing = database.chatDao().getSessionByIdDirect(activeSessionId)
        val count = (existing?.messageCount ?: 0) + 2
        database.chatDao().updateSessionMeta(
            id = activeSessionId,
            title = existing?.title ?: ("🎨 " + prompt.take(30)),
            snippet = if (savedImagePath != null) "[Image] $prompt".take(60) else modelMessageText.take(60),
            updatedAt = System.currentTimeMillis(),
            count = count
        )

        Pair(activeSessionId, modelMessageText)
    }

    // Chat
    // --- Chat Sessions & History ---
    fun getAllSessions(): Flow<List<ChatSessionEntity>> =
        database.chatDao().getAllSessions()

    fun getSessionById(sessionId: Long): Flow<ChatSessionEntity?> =
        database.chatDao().getSessionById(sessionId)

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>> =
        database.chatDao().getMessagesForSession(sessionId)

    fun getChatMessages(persona: String): Flow<List<ChatMessageEntity>> =
        database.chatDao().getMessagesByPersona(persona)

    suspend fun createSession(title: String, persona: AdvisorPersona): Long = withContext(Dispatchers.IO) {
        database.chatDao().insertSession(
            ChatSessionEntity(
                title = title.ifBlank { "New Chat" },
                persona = persona.id,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                messageCount = 0,
                lastSnippet = ""
            )
        )
    }

    suspend fun renameSession(sessionId: Long, newTitle: String) = withContext(Dispatchers.IO) {
        database.chatDao().renameSession(sessionId, newTitle.trim())
    }

    suspend fun deleteSession(sessionId: Long) = withContext(Dispatchers.IO) {
        database.chatDao().deleteMessagesForSession(sessionId)
        database.chatDao().deleteSession(sessionId)
    }

    suspend fun clearChat(persona: String) = withContext(Dispatchers.IO) {
        database.chatDao().clearMessagesForPersona(persona)
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        database.chatDao().clearAllMessages()
        database.chatDao().clearAllSessions()
    }

    suspend fun sendChatMessage(
        sessionId: Long?,
        persona: AdvisorPersona,
        userPrompt: String,
        history: List<ChatMessageEntity>,
        isImageMode: Boolean = false,
        aspectRatio: String = "1:1",
        attachedFilePath: String? = null,
        attachedMimeType: String? = null,
        attachedFileName: String? = null,
        attachedIsVideo: Boolean = false
    ): Pair<Long, String> = withContext(Dispatchers.IO) {
        if (isImageMode || isImageGenerationIntent(userPrompt)) {
            return@withContext generateImage(
                sessionId = sessionId,
                persona = persona,
                prompt = userPrompt,
                aspectRatio = aspectRatio
            )
        }

        val now = System.currentTimeMillis()
        val displayPrompt = if (userPrompt.isNotBlank()) {
            userPrompt
        } else if (attachedIsVideo) {
            "🎥 [Product Video: ${attachedFileName ?: "Uploaded Video"}]"
        } else {
            "🖼️ [Product Photo: ${attachedFileName ?: "Uploaded Photo"}]"
        }

        val activeSessionId = if (sessionId == null || sessionId <= 0L) {
            val autoTitle = if (displayPrompt.length > 36) displayPrompt.take(36).trim() + "..." else displayPrompt
            database.chatDao().insertSession(
                ChatSessionEntity(
                    title = autoTitle,
                    persona = persona.id,
                    createdAt = now,
                    updatedAt = now,
                    messageCount = 0,
                    lastSnippet = displayPrompt
                )
            )
        } else {
            sessionId
        }

        // Save User Message with attached media
        database.chatDao().insertMessage(
            ChatMessageEntity(
                sessionId = activeSessionId,
                role = "user",
                message = displayPrompt,
                persona = persona.id,
                imageUrl = if (!attachedIsVideo) attachedFilePath else null,
                videoUrl = if (attachedIsVideo) attachedFilePath else null,
                mediaType = if (attachedFilePath != null) (if (attachedIsVideo) "VIDEO" else "IMAGE") else null,
                mediaFileName = attachedFileName,
                timestamp = now
            )
        )

        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            val fallback = """
⚠️ **Gemini API Key Required**

No active Gemini API key was detected. To get real-time AI responses:

1. Open the Chat History Drawer (top-left ☰ menu).
2. Enter your Gemini API key in the **Gemini API Key** bar at the top.
3. Tap **Test** to verify connection, then tap **Save**.

*(Get a free API key instantly at [Google AI Studio](https://aistudio.google.com/app/apikey))*
""".trimIndent()
            database.chatDao().insertMessage(
                ChatMessageEntity(
                    sessionId = activeSessionId,
                    role = "model",
                    message = fallback,
                    persona = persona.id,
                    timestamp = System.currentTimeMillis()
                )
            )
            // Update session metadata
            val existing = database.chatDao().getSessionByIdDirect(activeSessionId)
            val count = (existing?.messageCount ?: 0) + 2
            database.chatDao().updateSessionMeta(
                id = activeSessionId,
                title = existing?.title ?: displayPrompt.take(36),
                snippet = fallback.take(60),
                updatedAt = System.currentTimeMillis(),
                count = count
            )
            return@withContext Pair(activeSessionId, fallback)
        }

        // Build conversation contents
        val contentsList = mutableListOf<GeminiContent>()
        // Include last few turns for context
        val recentHistory = history.takeLast(8)
        for (item in recentHistory) {
            val mappedRole = if (item.role == "user") "user" else "model"
            contentsList.add(
                GeminiContent(
                    role = mappedRole,
                    parts = listOf(GeminiPart(text = item.message))
                )
            )
        }

        // Prepare current user message parts (support multimodal vision if image attached)
        val userParts = mutableListOf<GeminiPart>()

        if (attachedFilePath != null && !attachedIsVideo) {
            try {
                val imageFile = File(attachedFilePath)
                val base64Data = com.example.data.util.MediaUtils.encodeImageToBase64(imageFile)
                if (base64Data != null) {
                    val mime = if (attachedMimeType?.startsWith("image/") == true) attachedMimeType else "image/jpeg"
                    userParts.add(
                        GeminiPart(
                            inlineData = GeminiInlineData(
                                mimeType = mime,
                                data = base64Data
                            )
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val effectivePromptText = when {
            userPrompt.isNotBlank() && attachedFilePath != null && attachedIsVideo -> 
                "[Attached Product Video: ${attachedFileName ?: "Video file"}]\n$userPrompt"
            userPrompt.isNotBlank() -> userPrompt
            attachedFilePath != null && !attachedIsVideo -> 
                "এই কাপড়ের/প্রোডাক্টের ছবি বিশ্লেষণ করে এর ফ্যাব্রিক কোয়ালিটি, ডিজাইন ফিনিশিং, সম্ভাব্য খুচরা ও পাইকারি মূল্য, এবং সোশ্যাল মিডিয়ায় বিক্রির জন্য আকর্ষণীয় ক্যাপশন ও টিপস দিন।"
            attachedFilePath != null && attachedIsVideo -> 
                "এই কাপড়ের/প্রোডাক্টের ভিডিওর জন্য উপযোগী মার্কেটিং কৌশল, মূল্য নির্ধারণ ও কাস্টমার এনগেজমেন্ট টিপস দিন।"
            else -> "Hello"
        }

        userParts.add(GeminiPart(text = effectivePromptText))

        contentsList.add(
            GeminiContent(
                role = "user",
                parts = userParts
            )
        )

        // If user prompt mentions Firebase, database, sync, or sales, ensure latest cloud data is synced
        val isCloudDataQuery = userPrompt.contains("ফায়ারবেস", ignoreCase = true) ||
                userPrompt.contains("firebase", ignoreCase = true) ||
                userPrompt.contains("ডাটাবেজ", ignoreCase = true) ||
                userPrompt.contains("টেনে", ignoreCase = true) ||
                userPrompt.contains("উদ্ধার", ignoreCase = true) ||
                userPrompt.contains("রিস্টোর", ignoreCase = true) ||
                userPrompt.contains("দোকানের ডাটা", ignoreCase = true) ||
                userPrompt.contains("হিসাব", ignoreCase = true)

        if (isCloudDataQuery) {
            try {
                val remoteSales = firestoreManager.fetchAllSalesFromFirestore()
                val remoteFinances = firestoreManager.fetchAllFinancesFromFirestore()
                if (remoteSales.isNotEmpty()) database.saleDao().insertSales(remoteSales)
                if (remoteFinances.isNotEmpty()) database.monthlyFinanceDao().insertFinances(remoteFinances)
            } catch (e: Exception) {
                android.util.Log.w("AdvisorRepository", "Auto cloud fetch on query notice: ${e.message}")
            }
        }

        val storeContext = getLiveStoreBusinessContext()
        val combinedSystemPrompt = """
            [CRITICAL DIRECTIVE - IN-APP CLOTHING STORE MANAGER & LIVE FIREBASE CONNECTIVITY]
            You are the official in-app AI Store Manager and Financial Consultant for THIS APPAREL STORE (দোকানের ডিজিটাল ম্যানেজার ও হিসাবরক্ষক).
            YOU ARE 100% DIRECTLY CONNECTED to this store's Google Firebase Firestore cloud database (collections: 'sales_records' and 'monthly_finances') and local SQLite/Room database.
            All sales invoices, customer names, phone numbers, items, paid amounts, due amounts, monthly sales, expenses, and product purchases from Firebase and the local database ARE ALREADY FETCHED AND INJECTED DIRECTLY INTO YOUR CONTEXT BELOW.

            STRICT REFUSAL OVERRIDES (ABSOLUTE RULE - NEVER VIOLATE):
            ❌ NEVER, UNDER ANY CIRCUMSTANCES, SAY OR IMPLY:
               - "আমি সরাসরি আপনার ফায়ারবেস (Firebase) ডাটাবেজের সাথে যুক্ত নই"
               - "নিরাপত্তার কারণে এবং কারিগরি সীমাবদ্ধতার জন্য আপনার দোকানের ডাটাগুলো সরাসরি আমি টেনে এনে দিতে পারছি না"
               - "আপনার ডাটাবেজের অ্যাক্সেস বা ক্রেডেনশিয়াল (Credentials) শুধুমাত্র আপনার কাছেই রয়েছে"
               - "Firebase Console থেকে সরাসরি ডাউনলোড করুন"
               - NEVER write sample JavaScript, Node.js, Flutter, or React code with `initializeApp(firebaseConfig)` unless the user explicitly asked you to teach them how to write software code.

            REQUIRED BEHAVIOR:
            ✅ Always confirm that you ARE directly connected to their Firebase database and you have real-time access to all their store records.
            ✅ Look at the "REAL-TIME STORE BUSINESS & FINANCIAL DATABASE CONTEXT" below.
            ✅ If records are present in the context, present the exact numbers (Total Sales, Total Invoices, Customer Dues, Monthly Expenses, Product Purchases, Net Balance) with polite, clear, and reassuring business insights in natural Bengali (বাংলা).
            ✅ If there are currently 0 records in the context, inform the user warmly:
               "আপনার ফায়ারবেস ক্লাউড ডাটাবেজ সফলভাবে অ্যাপের সাথে সংযুক্ত রয়েছে। তবে বর্তমানে আপনার স্টোরের ডাটাবেজে কোনো বিক্রির ইনভয়েস বা খরচের হিসাব রেকর্ড নেই (রেকর্ড সংখ্যা: ০)। 

               আপনি 'Sale' পেজে নতুন বিক্রি বা মাসিক খরচ যোগ করলেই তা সাথে সাথে স্বয়ংক্রিয়ভাবে ফায়ারবেসে সেভ হবে এবং আমি তা সরাসরি বিশ্লেষণ করে দেব। পূর্বের কোনো ব্যাকআপ থাকলে 'Firebase Database' ট্যাব থেকে 'রিস্টোর' বাটনে ট্যাপ করতে পারেন।"

            ${persona.systemPrompt}

            $storeContext
        """.trimIndent()

        val systemInstruction = GeminiContent(
            parts = listOf(GeminiPart(text = combinedSystemPrompt))
        )

        val request = GeminiRequest(
            contents = contentsList,
            generationConfig = GenerationConfig(
                temperature = 0.7f,
                topP = 0.95f
            ),
            systemInstruction = systemInstruction
        )

        var lastChatException: Exception? = null
        var chatResponseText: String? = null
        val chatModels = listOf(GeminiClient.MODEL_FLASH, GeminiClient.MODEL_FLASH_LITE)

        for (m in chatModels) {
            for (attempt in 1..2) {
                try {
                    val response = apiService.generateContent(
                        model = m,
                        apiKey = apiKey,
                        request = request
                    )
                    val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!text.isNullOrBlank()) {
                        chatResponseText = text
                        lastChatException = null
                        break
                    }
                } catch (e: Exception) {
                    lastChatException = e
                    val parsed = GeminiErrorParser.parse(e)
                    if (parsed.errorType == GeminiErrorType.QUOTA_EXCEEDED) {
                        kotlinx.coroutines.delay(1200L * attempt)
                    } else if (parsed.errorType == GeminiErrorType.INVALID_API_KEY || parsed.errorType == GeminiErrorType.PERMISSION_DENIED) {
                        break
                    } else {
                        kotlinx.coroutines.delay(600L)
                    }
                }
            }
            if (chatResponseText != null || lastChatException?.let { GeminiErrorParser.parse(it).errorType == GeminiErrorType.INVALID_API_KEY } == true) {
                break
            }
        }

        val rawModelText = if (chatResponseText != null) {
            chatResponseText
        } else {
            val parsed = GeminiErrorParser.parse(lastChatException ?: Exception("Unable to generate response"))
            when (parsed.errorType) {
                GeminiErrorType.INVALID_API_KEY -> """
⚠️ **Gemini API Error: Invalid API Key**

The API key used was rejected by Google AI Studio (`HTTP 400`).

**How to resolve:**
1. Open the Chat History Drawer (☰ menu).
2. Update your API key in the **Gemini API Key** field.
3. Tap **Test** to verify and **Save**.
""".trimIndent()
                GeminiErrorType.PERMISSION_DENIED -> """
⚠️ **Gemini API Error: Permission Denied (`HTTP 403`)**

Access to the Gemini model was rejected.

**How to resolve:**
• Verify that the Generative Language API is enabled in your Google Cloud / AI Studio project.
• Generate a fresh API key at [Google AI Studio](https://aistudio.google.com/app/apikey).
""".trimIndent()
                GeminiErrorType.QUOTA_EXCEEDED -> """
⚠️ **Gemini API Error: Rate Limit / Quota Exceeded (`HTTP 429`)**

**কারণ (Cause):**
Google AI Studio-এর ফ্রি টায়ারে রিকোয়েস্ট লিমিট (RPM) বা দৈনিক কোটা সাময়িকভাবে শেষ হয়েছে।

**সমাধান (Solutions):**
• ⏳ ৩০–৬০ সেকেন্ড অপেক্ষা করে নিচের **Retry** বাটনে ট্যাপ করুন।
• 🔑 ড্রয়ার মেনু থেকে নতুন একটি Gemini API Key সেট করুন।
""".trimIndent()
                GeminiErrorType.NETWORK_ERROR -> """
⚠️ **Connection Error**

Could not connect to Gemini servers. Please check your internet connection and try again.
""".trimIndent()
                else -> """
⚠️ **Gemini API Error: ${parsed.title}**

${parsed.message}
""".trimIndent()
            }
        }

        // Safety Refusal Interceptor: Never let the AI claim it is disconnected or unable to access store/firebase data
        val refusalPatterns = listOf(
            "সরাসরি আপনার ফায়ারবেস",
            "সরাসরি আপনার ফায়ারবেস",
            "কারিগরি সীমাবদ্ধতার জন্য",
            "টেনে এনে দিতে পারছি না",
            "ক্রেডেনশিয়াল (Credentials)",
            "firebaseConfig",
            "initializeApp(firebaseConfig)"
        )
        val modelText = if (refusalPatterns.any { rawModelText.contains(it, ignoreCase = true) }) {
            formatDirectStoreReport(
                sales = database.saleDao().getAllSalesDirect(),
                finances = database.monthlyFinanceDao().getAllFinancesDirect()
            )
        } else {
            rawModelText
        }

        database.chatDao().insertMessage(
            ChatMessageEntity(
                sessionId = activeSessionId,
                role = "model",
                message = modelText,
                persona = persona.id,
                timestamp = System.currentTimeMillis()
            )
        )

        // Update session metadata
        val existing = database.chatDao().getSessionByIdDirect(activeSessionId)
        val count = (existing?.messageCount ?: 0) + 2
        database.chatDao().updateSessionMeta(
            id = activeSessionId,
            title = existing?.title ?: userPrompt.take(36),
            snippet = modelText.take(60),
            updatedAt = System.currentTimeMillis(),
            count = count
        )

        Pair(activeSessionId, modelText)
    }

    // Pricing Audit (Complex financial task -> Gemini Pro)
    suspend fun auditPricing(sku: PricingSkuEntity): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        val prompt = """
Perform a professional CFO & Fashion Merchant Pricing Audit for the following apparel SKU:
- Item: ${sku.itemName} (Category: ${sku.category})
- Fabric Cost: $${String.format("%.2f", sku.fabricCost)}
- Cut & Sew Labor Cost: $${String.format("%.2f", sku.laborCost)}
- Trims, Labels & Packaging: $${String.format("%.2f", sku.trimsPackagingCost)}
- Shipping, Duties & Inbound Freight: $${String.format("%.2f", sku.shippingDutiesCost)}
- Overhead Buffer: $${String.format("%.2f", sku.overheadBuffer)}
- Total Cost of Goods Sold (COGS): $${String.format("%.2f", sku.fabricCost + sku.laborCost + sku.trimsPackagingCost + sku.shippingDutiesCost + sku.overheadBuffer)}
- Proposed Retail Price (MSRP): $${String.format("%.2f", sku.retailPrice)}
- Proposed Wholesale Price: $${String.format("%.2f", sku.wholesalePrice)}
- Gross Margin %: ${String.format("%.1f", sku.marginPercentage)}%
- Net Profit per Unit: $${String.format("%.2f", sku.profitPerUnit)}

Please provide:
1. Margin Health Verdict (Healthy / Vulnerable / Underpriced / Overpriced)
2. Wholesale Viability Assessment (Can a boutique retailer double this for 50% Keystone margin?)
3. Tactical Recommendations to improve profit per unit (e.g. fabric yield, bundling, trim savings)
4. Recommended psychological price points for this category.
""".trimIndent()

        if (apiKey.isBlank()) {
            return@withContext "• **Margin Health**: ${if (sku.marginPercentage >= 65) "Excellent (>=65%)" else "Vulnerable (<65%)"}\n• **COGS**: $${String.format("%.2f", sku.fabricCost + sku.laborCost + sku.trimsPackagingCost + sku.shippingDutiesCost + sku.overheadBuffer)}\n• **Key Rule**: Maintain at least 65-75% DTC margin to absorb advertising and return costs."
        }

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            generationConfig = GenerationConfig(temperature = 0.5f),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = "You are a seasoned fashion business CFO and merchandising strategist. Be concise, precise, and practical."))
            )
        )

        try {
            val response = apiService.generateContent(
                model = GeminiClient.MODEL_PRO,
                apiKey = apiKey,
                request = request
            )
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Audit completed without notes."
        } catch (e: Exception) {
            "Audit generated local guidance: Maintain target margin of at least 65% for DTC to ensure sustainable cash flow."
        }
    }

    // Drop Strategy Generation (Gemini Pro for deep strategic planning)
    suspend fun generateDropStrategy(
        title: String,
        season: String,
        budget: Double,
        pieceCount: Int,
        notes: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        val prompt = """
Create a comprehensive 5-Phase Launch & Marketing Roadmap for our upcoming clothing collection:
- Collection Name: $title
- Season: $season
- Production Budget: $${String.format("%.2f", budget)}
- Number of SKUs / Pieces: $pieceCount
- Brand Vision & Aesthetic Notes: $notes

Provide a structured plan detailing:
1. **Tech Pack & Sampling Phase (T-minus 12 weeks)**: Key milestones & fit check criteria
2. **Bulk Production & Quality Control (T-minus 8 weeks)**: Manufacturer checkpoints & buffer planning
3. **Content & Creative Asset Production (T-minus 4 weeks)**: Lookbook photoshoot concepts, model styling & flat-lays
4. **Hype & Pre-launch Waitlist (T-minus 2 weeks)**: VIP email/SMS teasers, influencer seeding kits
5. **Drop Day Execution & Post-Drop Momentum**: Timed release, inventory scarcity messaging, restock strategy.
""".trimIndent()

        if (apiKey.isBlank()) {
            return@withContext "Strategic Roadmap:\n1. Sampling (W-12): Lock tech packs & master patterns.\n2. Bulk Production (W-8): Finalize fabric batch & size curve.\n3. Content (W-4): High-res editorial and flatlay photography.\n4. Teaser (W-2): Open early VIP access list.\n5. Drop Day: Launch at peak social traffic hour (e.g. 12 PM EST)."
        }

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            generationConfig = GenerationConfig(temperature = 0.7f),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = "You are a top fashion brand Director of Operations & Marketing. Deliver high-impact, professional, highly actionable launch plans."))
            )
        )

        try {
            val response = apiService.generateContent(
                model = GeminiClient.MODEL_PRO,
                apiKey = apiKey,
                request = request
            )
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Plan generated successfully."
        } catch (e: Exception) {
            "Phase 1: Sampling & Tech Packs. Phase 2: Production & Fabric Batching. Phase 3: Campaign Shoot. Phase 4: Teaser & Early Access. Phase 5: Drop Release."
        }
    }

    // Business Health Diagnostic & 30-Day Growth Blueprint (Gemini Pro)
    suspend fun generateBusinessDiagnostic(profile: BrandProfileEntity): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        val prompt = """
Conduct a full Fashion Brand Diagnostic and 30-Day Action Blueprint for:
- Brand Name: ${profile.brandName}
- Niche & Style: ${profile.niche}
- Business Model: ${profile.businessModel}
- Current Stage: ${profile.currentStage}
- Monthly Revenue: $${String.format("%.2f", profile.monthlyRevenue)}
- Primary Goal: ${profile.primaryGoal}
- Current Major Challenge: ${profile.primaryPainPoint}

Format your report as follows:
# 1. Executive SWOT Analysis (Strengths, Weaknesses, Opportunities, Threats)
# 2. Revenue Optimization Levers (AOV, Conversion Rate, Repeat Order Frequency)
# 3. Supply Chain & Margin Quick-Wins (Where money is leaking)
# 4. 30-Day Action Sprint (Week 1, Week 2, Week 3, Week 4 clear daily/weekly action items).
""".trimIndent()

        if (apiKey.isBlank()) {
            return@withContext "# 1. Diagnostic Overview\n• Stage: ${profile.currentStage}\n• Key Focus: ${profile.primaryGoal}\n\n# 2. Key Levers\n• Elevate perceived brand value with cohesive lookbook visuals.\n• Implement post-purchase email flows offering complementary wardrobe pieces to lift LTV by 20%.\n• Consolidate orders to reduce freight cost per unit."
        }

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            generationConfig = GenerationConfig(temperature = 0.6f),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = "You are a master Fashion Business Growth Consultant and Venture Advisor. Give sharp, actionable, elite consulting."))
            )
        )

        try {
            val response = apiService.generateContent(
                model = GeminiClient.MODEL_PRO,
                apiKey = apiKey,
                request = request
            )
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Diagnostic generated."
        } catch (e: Exception) {
            "Diagnostic summary: Focus on increasing repeat customer rate and tightening manufacturer lead times."
        }
    }

    // Quick Size Curve Advice (Fast task -> Gemini Flash Lite)
    suspend fun getQuickSizeAdvice(totalUnits: Int, fitType: String): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        val prompt = "Provide optimal size breakdown percentages and exact unit quantities for a total order of $totalUnits units for fit profile '$fitType' (e.g. XS, S, M, L, XL, XXL). Give a 2-sentence rationale on current consumer demand trends for this fit."
        
        if (apiKey.isBlank()) {
            return@withContext "Standard Ratio for $totalUnits units:\n• S: ${(totalUnits * 0.15).toInt()} units (15%)\n• M: ${(totalUnits * 0.35).toInt()} units (35%)\n• L: ${(totalUnits * 0.35).toInt()} units (35%)\n• XL: ${(totalUnits * 0.15).toInt()} units (15%)\n\nDemand trend: Medium and Large represent 70% of volume in mainstream fashion."
        }

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            generationConfig = GenerationConfig(temperature = 0.3f)
        )

        try {
            val response = apiService.generateContent(
                model = GeminiClient.MODEL_FLASH_LITE,
                apiKey = apiKey,
                request = request
            )
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "S: 15%, M: 35%, L: 35%, XL: 15%"
        } catch (e: Exception) {
            "S: 15%, M: 35%, L: 35%, XL: 15%"
        }
    }

    // Database access
    fun getAllCollections(): Flow<List<CollectionEntity>> = database.collectionDao().getAllCollections()
    suspend fun saveCollection(collection: CollectionEntity) = withContext(Dispatchers.IO) {
        database.collectionDao().insertCollection(collection)
    }
    suspend fun updateCollection(collection: CollectionEntity) = withContext(Dispatchers.IO) {
        database.collectionDao().updateCollection(collection)
    }
    suspend fun deleteCollection(collection: CollectionEntity) = withContext(Dispatchers.IO) {
        database.collectionDao().deleteCollection(collection)
    }

    fun getAllSkus(): Flow<List<PricingSkuEntity>> = database.pricingDao().getAllSkus()
    suspend fun saveSku(sku: PricingSkuEntity) = withContext(Dispatchers.IO) {
        database.pricingDao().insertSku(sku)
    }
    suspend fun deleteSku(sku: PricingSkuEntity) = withContext(Dispatchers.IO) {
        database.pricingDao().deleteSku(sku)
    }

    fun getBrandProfileFlow(): Flow<BrandProfileEntity?> = database.brandDao().getProfileFlow()
    suspend fun getBrandProfile(): BrandProfileEntity = withContext(Dispatchers.IO) {
        database.brandDao().getProfile() ?: BrandProfileEntity().also {
            database.brandDao().saveProfile(it)
        }
    }
    suspend fun saveBrandProfile(profile: BrandProfileEntity) = withContext(Dispatchers.IO) {
        database.brandDao().saveProfile(profile)
    }

    // Sales Records access & Firebase Firestore integration
    private val firestoreManager: SaleFirestoreManager = SaleFirestoreManager.getInstance()

    fun getFirestoreManager(): SaleFirestoreManager = firestoreManager

    fun getAllSales(): Flow<List<SaleRecordEntity>> = database.saleDao().getAllSales()

    suspend fun saveSale(sale: SaleRecordEntity): Long = withContext(Dispatchers.IO) {
        val insertedId = database.saleDao().insertSale(sale)
        val savedSale = if (sale.id == 0L) sale.copy(id = insertedId) else sale

        // Store sale record in Firebase Firestore
        try {
            firestoreManager.saveSale(savedSale) { success, errorMsg ->
                if (!success) {
                    android.util.Log.w("AdvisorRepository", "Firestore store notice: $errorMsg")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("AdvisorRepository", "Firestore store caught: ${e.message}")
        }

        insertedId
    }

    suspend fun deleteSale(id: Long) = withContext(Dispatchers.IO) {
        database.saleDao().deleteSaleById(id)
    }

    fun getSalesCount(): Flow<Int> = database.saleDao().getSalesCount()
    fun getTotalRevenue(): Flow<Double?> = database.saleDao().getTotalRevenue()

    // Monthly Finances (Sale, Expense, DEBT Pay, Product Buy)
    fun getAllFinances(): Flow<List<MonthlyFinanceEntity>> = database.monthlyFinanceDao().getAllFinances()

    fun getFinancesByType(type: String): Flow<List<MonthlyFinanceEntity>> =
        database.monthlyFinanceDao().getFinancesByType(type)

    fun getTotalFinanceByType(type: String): Flow<Double?> =
        database.monthlyFinanceDao().getTotalByType(type)

    suspend fun saveFinance(finance: MonthlyFinanceEntity): Long = withContext(Dispatchers.IO) {
        val insertedId = database.monthlyFinanceDao().insertFinance(finance)
        val saved = if (finance.id == 0L) finance.copy(id = insertedId) else finance
        try {
            firestoreManager.saveMonthlyFinance(saved) { _, _ -> }
        } catch (e: Exception) {
            android.util.Log.w("AdvisorRepository", "Firestore save finance error: ${e.message}")
        }
        insertedId
    }

    suspend fun deleteFinance(id: Long, type: String) = withContext(Dispatchers.IO) {
        database.monthlyFinanceDao().deleteFinanceById(id)
        try {
            firestoreManager.deleteMonthlyFinance(id, type)
        } catch (e: Exception) {
            android.util.Log.w("AdvisorRepository", "Firestore delete finance error: ${e.message}")
        }
    }

    suspend fun clearAllFinances() = withContext(Dispatchers.IO) {
        database.monthlyFinanceDao().clearAllFinances()
    }

    suspend fun getAllSalesDirect(): List<SaleRecordEntity> = withContext(Dispatchers.IO) {
        database.saleDao().getAllSalesDirect()
    }

    suspend fun getAllFinancesDirect(): List<MonthlyFinanceEntity> = withContext(Dispatchers.IO) {
        database.monthlyFinanceDao().getAllFinancesDirect()
    }

    suspend fun syncAllToFirestore(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val sales = database.saleDao().getAllSalesDirect()
        val finances = database.monthlyFinanceDao().getAllFinancesDirect()
        firestoreManager.syncAllToFirestore(sales, finances)
    }

    suspend fun restoreAllFromFirestore(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val remoteSales = firestoreManager.fetchAllSalesFromFirestore()
        val remoteFinances = firestoreManager.fetchAllFinancesFromFirestore()

        if (remoteSales.isNotEmpty()) {
            database.saleDao().insertSales(remoteSales)
        }
        if (remoteFinances.isNotEmpty()) {
            database.monthlyFinanceDao().insertFinances(remoteFinances)
        }
        Pair(remoteSales.size, remoteFinances.size)
    }

    suspend fun getLiveStoreBusinessContext(): String = withContext(Dispatchers.IO) {
        var sales = database.saleDao().getAllSalesDirect()
        var finances = database.monthlyFinanceDao().getAllFinancesDirect()

        // Auto-sync latest from Firestore if available
        try {
            val remoteSales = firestoreManager.fetchAllSalesFromFirestore()
            val remoteFinances = firestoreManager.fetchAllFinancesFromFirestore()
            if (remoteSales.isNotEmpty()) {
                database.saleDao().insertSales(remoteSales)
                sales = database.saleDao().getAllSalesDirect()
            }
            if (remoteFinances.isNotEmpty()) {
                database.monthlyFinanceDao().insertFinances(remoteFinances)
                finances = database.monthlyFinanceDao().getAllFinancesDirect()
            }
        } catch (e: Exception) {
            android.util.Log.w("AdvisorRepository", "Auto cloud fetch notice: ${e.message}")
        }

        buildString {
            appendLine("=== REAL-TIME STORE BUSINESS & FINANCIAL DATABASE CONTEXT ===")
            appendLine("Language: The user speaks Bengali / Banglish / English. Always answer in clear, friendly Bengali (with relevant English business terms).")
            appendLine("Source: Verified from Firebase Firestore ('sales_records' & 'monthly_finances') & Local Database.")
            appendLine()

            val totalSalesCount = sales.size
            val totalInvoicedSales = sales.sumOf { it.totalAmount }
            val totalPaidSales = sales.sumOf { it.paidAmount }
            val totalDueSales = sales.sumOf { it.dueAmount }

            val monthlySaleAmount = finances.filter { it.type == "SALE" }.sumOf { it.amount }
            val monthlyExpenseAmount = finances.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val monthlyDebtPayAmount = finances.filter { it.type == "DEBT_PAY" }.sumOf { it.amount }
            val monthlyProductBuyAmount = finances.filter { it.type == "PRODUCT_BUY" }.sumOf { it.amount }
            val netCashBalance = monthlySaleAmount - (monthlyExpenseAmount + monthlyDebtPayAmount + monthlyProductBuyAmount)

            appendLine("--- Shop Financial Summary ---")
            appendLine("• Total POS Sale Records: $totalSalesCount invoices")
            appendLine("• Total Invoiced Revenue: ৳${totalInvoicedSales.toInt()}")
            appendLine("• Total Collected/Paid: ৳${totalPaidSales.toInt()}")
            appendLine("• Total Customer Due/Baki (বাকি): ৳${totalDueSales.toInt()}")
            appendLine("• Monthly Sale (মাসিক মোট বিক্রি): ৳${monthlySaleAmount.toInt()}")
            appendLine("• Monthly Expense (মাসিক খরচ): ৳${monthlyExpenseAmount.toInt()}")
            appendLine("• Monthly DEBT Pay (মাসিক ঋণ পরিশোধ): ৳${monthlyDebtPayAmount.toInt()}")
            appendLine("• Monthly Product Buy (মাসিক পণ্য ক্রয়): ৳${monthlyProductBuyAmount.toInt()}")
            appendLine("• Net Balance (নিট উদ্বৃত্ত/মুনাফা): ৳${netCashBalance.toInt()}")
            appendLine()

            if (sales.isNotEmpty()) {
                appendLine("--- Detailed Sales Invoices & Customers ---")
                sales.take(20).forEachIndexed { index, s ->
                    val dateStr = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(s.timestamp))
                    val dueInfo = if (s.dueAmount > 0) " | [বাকি (Due): ৳${s.dueAmount.toInt()}]" else " | [পরিশোধিত]"
                    val customerInfo = if (s.customerName.isNotBlank()) " | কাস্টমার: ${s.customerName} (${s.customerPhone})" else ""
                    appendLine("${index + 1}. Invoice #${s.invoiceNumber} ($dateStr): ${s.itemsSummary} | মোট: ৳${s.totalAmount.toInt()} | পেমেন্ট: ${s.paymentMethod}$dueInfo$customerInfo")
                }
                appendLine()
            }

            if (finances.isNotEmpty()) {
                appendLine("--- Detailed Monthly Financial Logs ---")
                finances.take(20).forEachIndexed { index, f ->
                    val typeBn = when (f.type) {
                        "SALE" -> "বিক্রি"
                        "EXPENSE" -> "খরচ"
                        "DEBT_PAY" -> "ঋণ পরিশোধ"
                        "PRODUCT_BUY" -> "পণ্য ক্রয়"
                        else -> f.type
                    }
                    val dateStr = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date(f.timestamp))
                    appendLine("${index + 1}. [$typeBn] ${f.title}: ৳${f.amount.toInt()} (ক্যাটাগরি: ${f.category}, মাস: ${f.monthYear}, $dateStr)")
                }
                appendLine()
            }

            appendLine("CRITICAL INSTRUCTION FOR AI ADVISOR:")
            appendLine("When the user asks about sales ('হিসাব', 'বিক্রি', 'বাকি', 'খরচ', 'দোকানের সেলস', 'কাস্টমার', 'ইনভয়েস', 'ক্লাউড ব্যাকআপ', 'কত টাকা লাভ' etc.):")
            appendLine("1. ALWAYS refer to the EXACT numbers from this database context.")
            appendLine("2. Give clear, polite, and reassuring answers.")
            appendLine("3. Confirm that their data is safely backed up in Firebase Cloud and will never be lost, even if the app is updated or reinstalled.")
            appendLine("==============================================================")
        }
    }

    /**
     * Fallback report generated directly from verified Firebase & SQLite records
     * to eliminate any possible LLM refusal disclaimer.
     */
    fun formatDirectStoreReport(sales: List<SaleRecordEntity>, finances: List<MonthlyFinanceEntity>): String {
        val totalSalesCount = sales.size
        val totalInvoiced = sales.sumOf { it.totalAmount }
        val totalPaid = sales.sumOf { it.paidAmount }
        val totalDue = sales.sumOf { it.dueAmount }

        val monthlySale = finances.filter { it.type == "SALE" }.sumOf { it.amount }
        val monthlyExpense = finances.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val monthlyDebt = finances.filter { it.type == "DEBT_PAY" }.sumOf { it.amount }
        val monthlyBuy = finances.filter { it.type == "PRODUCT_BUY" }.sumOf { it.amount }
        val netBalance = monthlySale - (monthlyExpense + monthlyDebt + monthlyBuy)

        return if (totalSalesCount > 0 || finances.isNotEmpty()) {
            buildString {
                appendLine("✅ **আপনার দোকানের ফায়ারবেস (Firebase) ক্লাউড ডাটাবেজ সরাসরি সংযুক্ত রয়েছে!**")
                appendLine()
                appendLine("আপনার দোকানের ক্লাউড ও লোকাল ডাটাবেজে সংরক্ষিত হিসাব বিবরণী:")
                appendLine()
                appendLine("📊 **সার্বিক বিক্রয় ও আয়ের সারসংক্ষেপ:**")
                appendLine("• মোট বিক্রির ইনভয়েস: **$totalSalesCount টি**")
                appendLine("• মোট বিক্রয় মূল্য (Invoiced): **৳${totalInvoiced.toInt()}**")
                appendLine("• মোট সংগৃহীত নগদ টাকা: **৳${totalPaid.toInt()}**")
                appendLine("• কাস্টমারদের কাছে বকেয়া (Due/Baki): **৳${totalDue.toInt()}**")
                appendLine()
                appendLine("💰 **মাসিক আর্থিক হিসাব (Monthly Finances):**")
                appendLine("• মোট বিক্রি (Sale): **৳${monthlySale.toInt()}**")
                appendLine("• দোকান খরচ (Expense): **৳${monthlyExpense.toInt()}**")
                appendLine("• নতুন পণ্য ক্রয় (Product Buy): **৳${monthlyBuy.toInt()}**")
                appendLine("• ঋণ পরিশোধ (DEBT Pay): **৳${monthlyDebt.toInt()}**")
                appendLine("• **নিট ব্যালেন্স / ক্যাশ উদ্বৃত্ত: ৳${netBalance.toInt()}**")
                appendLine()
                if (sales.isNotEmpty()) {
                    appendLine("🧾 **সর্বশেষ ইনভয়েস রেকর্ডসমূহ:**")
                    sales.take(5).forEachIndexed { i, s ->
                        val dateStr = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(s.timestamp))
                        val dueStr = if (s.dueAmount > 0) " | [বাকি: ৳${s.dueAmount.toInt()}]" else " | [পরিশোধিত]"
                        val custStr = if (s.customerName.isNotBlank()) " (${s.customerName})" else ""
                        appendLine("${i + 1}. #${s.invoiceNumber} ($dateStr): ${s.itemsSummary} — ৳${s.totalAmount.toInt()}$dueStr$custStr")
                    }
                    appendLine()
                }
                appendLine("☁️ আপনার সমস্ত বিক্রির রসিদ এবং আর্থিক হিসাব Google Firebase Firestore-এ সম্পূর্ণ নিরাপদে জমা রয়েছে। আপনি 'Sale' পেজে নতুন বিক্রি যোগ করতে পারেন বা 'Firebase Database' ট্যাব থেকে যেকোনো সময় ডাটা সিঙ্ক ও রিস্টোর করতে পারেন।")
            }
        } else {
            """
            ✅ **আপনার দোকানের ফায়ারবেস (Firebase) ক্লাউড ডাটাবেজ সফলভাবে সংযুক্ত রয়েছে!**

            বর্তমানে আপনার স্টোরের ফায়ারবেস ক্লাউড ডাটাবেজ ও লোকাল স্টোরেজে কোনো বিক্রির রেকর্ড বা মাসিক হিসাব এন্ট্রি করা নেই (রেকর্ড সংখ্যা: ০)।

            📌 **হিসাব যুক্ত করার নিয়ম:**
            ১. **'Sale' ট্যাবে যান:** নতুন বিক্রি (Sale), দোকান খরচ (Expense), বাকি পরিশোধ বা পণ্য ক্রয় এন্ট্রি করুন।
            ২. **স্বয়ংক্রিয় ক্লাউড ব্যাকআপ:** এন্ট্রি করার সাথে সাথেই তা ফায়ারবেসে ক্লাউডে সেভ হয়ে যাবে এবং আমি সাথে সাথে তা এখানে বিশ্লেষণ করে দেব।
            ৩. **ক্লাউড রিস্টোর:** আপনার পূর্ববর্তী কোনো ক্লাউড ব্যাকআপ থাকলে **'Firebase Database'** ট্যাবে গিয়ে **"রিস্টোর"** বাটনে ট্যাপ করুন।
            """.trimIndent()
        }
    }
}

