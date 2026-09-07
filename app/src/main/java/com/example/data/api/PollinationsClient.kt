package com.example.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object PollinationsClient {
    private const val BASE_IMAGE_URL = "https://image.pollinations.ai/prompt/"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(35, TimeUnit.SECONDS)
            .readTimeout(50, TimeUnit.SECONDS)
            .writeTimeout(35, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Build direct image URL for Pollinations AI
     */
    fun buildImageUrl(
        prompt: String,
        width: Int = 1024,
        height: Int = 1024,
        model: String = "flux",
        seed: Long = (100000L..999999999L).random(),
        apiKey: String? = null
    ): String {
        val cleanPrompt = prompt.trim()
        val encodedPrompt = URLEncoder.encode(cleanPrompt, "UTF-8")
        val base = "$BASE_IMAGE_URL$encodedPrompt?width=$width&height=$height&model=$model&nologo=true&seed=$seed"
        return if (!apiKey.isNullOrBlank()) {
            "$base&key=${apiKey.trim()}"
        } else {
            base
        }
    }

    /**
     * Download the generated image to local disk cache so it is permanently saved and offline-viewable
     */
    suspend fun downloadImage(
        url: String,
        outputFile: File,
        apiKey: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder().url(url)
            if (!apiKey.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer ${apiKey.trim()}")
            }
            val request = requestBuilder.build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    outputFile.parentFile?.mkdirs()
                    outputFile.writeBytes(bytes)
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Validate connection to Pollinations AI image service
     */
    suspend fun testConnection(apiKey: String?): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val testUrl = buildImageUrl(
                prompt = "apparel test fabric texture",
                width = 256,
                height = 256,
                model = "turbo",
                apiKey = apiKey
            )
            val requestBuilder = Request.Builder().url(testUrl)
            if (!apiKey.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer ${apiKey.trim()}")
            }
            val response = httpClient.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                if (!apiKey.isNullOrBlank()) {
                    Pair(true, "✅ Pollinations API Key ভেরিফাইড! হাই-স্পিড ইমেজ জেনারেশন সক্রিয়।")
                } else {
                    Pair(true, "✅ Pollinations সংযোগ সফল! ফ্রি ওপেন টায়ার দিয়ে যেকোনো ইমেজ তৈরি সম্ভব।")
                }
            } else {
                Pair(false, "Pollinations সার্ভার রেসপন্স কোড: ${response.code}। অনুগ্রহ করে কি বা ইন্টারনেট চেক করুন।")
            }
        } catch (e: Exception) {
            Pair(false, "সংযোগ সমস্যা: ${e.localizedMessage ?: "Timeout"}")
        }
    }
}
