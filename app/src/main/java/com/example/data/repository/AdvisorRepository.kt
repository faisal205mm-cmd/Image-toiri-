package com.example.data.repository

import android.content.Context
import android.util.Base64
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
        title = "General",
        subtitle = "Helpful, versatile AI assistant for any query",
        avatarIcon = "sparkles",
        systemPrompt = """You are an intelligent, thoughtful, and versatile AI assistant. 
You can answer questions, explain concepts, brainstorm ideas, write content, and assist with any topic.
Deliver high-quality, clear, accurate, and structured answers using markdown formatting (headers, bold text, bullet points) when appropriate.""",
        suggestedPrompts = listOf(
            "Explain quantum computing in simple terms",
            "Give me 5 practical tips to boost daily productivity",
            "Help me write a concise, professional follow-up email",
            "What are the most exciting emerging tech trends?"
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
            "ছবি বানিয়ে দাও", "chobi lagbe", "image lagbe"
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
            "photo of", "picture of", "image of"
        )
        return englishKeywords.any { lower.contains(it) }
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
        val cleanPrompt = extractCleanImagePrompt(prompt)
        val (width, height) = when (aspectRatio) {
            "16:9" -> Pair(1280, 720)
            "9:16" -> Pair(720, 1280)
            "4:3" -> Pair(1024, 768)
            "3:4" -> Pair(768, 1024)
            else -> Pair(1024, 1024)
        }
        val pollinationsKey = customPollinationsApiKey
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
        Pair(finalPath, "🎨 **Visual Created (Pollinations AI)**\n\nPrompt: *$cleanPrompt*")
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

        var savedImagePath: String? = null
        var modelMessageText: String = ""

        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            // Direct Pollinations AI generation when Gemini key is not configured
            val (pollinationsPath, pollinationsMessage) = generateWithPollinations(prompt, aspectRatio)
            savedImagePath = pollinationsPath
            modelMessageText = pollinationsMessage
        } else {

        val cleanPrompt = extractCleanImagePrompt(prompt)
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = cleanPrompt))
                )
            ),
            generationConfig = GenerationConfig(
                responseModalities = listOf("TEXT", "IMAGE"),
                imageConfig = ImageConfig(aspectRatio = aspectRatio, imageSize = "1K")
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
                    if (parsed.errorType == GeminiErrorType.QUOTA_EXCEEDED) {
                        // Rate limit / 429 hit - back off briefly before retrying or switching models
                        kotlinx.coroutines.delay(1500L * attempt)
                    } else if (parsed.errorType == GeminiErrorType.INVALID_API_KEY || parsed.errorType == GeminiErrorType.PERMISSION_DENIED) {
                        // Unrecoverable with key, break early
                        break
                    } else {
                        // Transient or model specific error, back off and try next
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

                modelMessageText = when {
                    savedImagePath != null && !textPart.isNullOrBlank() -> textPart
                    savedImagePath != null -> "🎨 **Visual Created ($usedModelName)**\n\nPrompt: *$cleanPrompt*"
                    !textPart.isNullOrBlank() -> textPart
                    else -> "🎨 Image generated successfully."
                }
                } else {
                    // Fallback to Pollinations AI
                    val (pollinationsPath, pollinationsMessage) = generateWithPollinations(prompt, aspectRatio)
                    savedImagePath = pollinationsPath
                    modelMessageText = "$pollinationsMessage\n\n*(Gemini কোটা লিমিট থাকায় Pollinations AI দিয়ে তৈরি করা হয়েছে)*"
                }
            } catch (e: Exception) {
                // Fallback to Pollinations AI
                val (pollinationsPath, pollinationsMessage) = generateWithPollinations(prompt, aspectRatio)
                if (pollinationsPath != null) {
                    savedImagePath = pollinationsPath
                    modelMessageText = "$pollinationsMessage\n\n*(Pollinations AI দিয়ে নিরবচ্ছিন্নভাবে তৈরি করা হয়েছে)*"
                } else {
                    val parsed = GeminiErrorParser.parse(e)
                    modelMessageText = "⚠️ Image Generation Error: ${parsed.title}\n\n${parsed.message}"
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

        val systemInstruction = GeminiContent(
            parts = listOf(GeminiPart(text = persona.systemPrompt))
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

        val modelText = if (chatResponseText != null) {
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
}

